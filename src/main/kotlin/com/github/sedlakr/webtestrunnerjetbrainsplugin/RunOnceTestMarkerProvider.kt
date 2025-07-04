package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.ide.actions.runAnything.commands.RunAnythingCommandCustomizer
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.execution.ParametersListUtil
import java.io.File
import java.nio.charset.Charset

const val TEST_TYPE_IT = "it"
const val TEST_TYPE_DESCRIBE = "describe"

open class RunOnceTestMarkerProvider() : RunLineMarkerProvider() {

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        val testedVirtualFile = psiElement.containingFile.virtualFile
        val (isSupported, isWithWtrPluginRunner) = wtrInfo(psiElement.project, testedVirtualFile)
        if (!isSupported) {
            return null
        }
        if (psiElement is LeafPsiElement) {
            var testType = TEST_TYPE_IT
            if (isDescribe(psiElement)) {
                testType = TEST_TYPE_DESCRIBE
            }
            if (isWTrtest(psiElement)) {
                val testName = traverseTestNameFUll(psiElement);
                return LineMarkerInfo(
                    psiElement,
                    psiElement.textRange,
                    getLineMarkerInfoIcon(),
                    { _: PsiElement? ->
                        getTestLabel(testType, testName)
                    },
                    { _, _ ->
                        runCommand(psiElement, testName, testType, testedVirtualFile, isWithWtrPluginRunner)

                    },
                    GutterIconRenderer.Alignment.CENTER,
                    {
                        "Run WTR 1 $testName"
                    }
                )
            }
        }
        return null
    }

    open fun getTestLabel(testType: String, testName: String): String {
        return "Run " + (if (testType == TEST_TYPE_IT) "test" else "test suite") + " \"$testName\""
    }

    open fun getLineMarkerInfoIcon(): javax.swing.Icon {
        return runIcon
    }

    open fun commandAppend(): String {
        return ""
    }

    private fun runCommand(
        psiElement: PsiElement,
        testNameFull: String,
        testType: String,
        testFile: VirtualFile,
        isWithWtrPluginRunner: Boolean
    ) {
        thisLogger().info("run test $testNameFull")
        // execute node peon.js
        val project = psiElement.project
        val workDirectory = getWorkingDir(project, psiElement.containingFile.virtualFile);
        thisLogger().info("Working directory: $workDirectory")
        var testNameSuffix = ""
        if (testType == TEST_TYPE_IT) {
            testNameSuffix = "$"
        }
        val dataContext = SimpleDataContext.getProjectContext(project)
        val commandDataContext =
            RunAnythingCommandCustomizer.customizeContext(dataContext);

        var commandString = "node --no-warnings wtr.plugin.runner.js"
        if (!isWithWtrPluginRunner) {
            // old way with peonRunner
            commandString = "node --no-warnings peonRunner.js test"
        };

        commandString += " --runTestsByPath=\"${testFile.path}\" --testNamePattern=\"^${testNameFull}${testNameSuffix}\""
        if (commandAppend() != "") {
            commandString += " " + commandAppend();
        }
        val initialCommandLine = PtyCommandLine(ParametersListUtil.parse(commandString, false, true))
            .withInitialColumns(1024)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
            .withWorkDirectory(workDirectory.path)
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val commandLine =
            RunAnythingCommandCustomizer.customizeCommandLine(commandDataContext, workDirectory, initialCommandLine)
        useNodeJsInterpreterInCommandLine(project, commandLine)
        commandLine.charset = Charset.forName("UTF-8")
        val profile = getProfile(commandLine, commandString, testNameFull);
        ExecutionEnvironmentBuilder.create(project, executor, profile)
            .dataContext(commandDataContext)
            .buildAndExecute()

        thisLogger().info("Done")
    }

    open fun getProfile(commandLine: GeneralCommandLine, commandString: String, testNameFull: String): RunProfile {
        return RunProfile(commandLine, commandString, testNameFull)
    }

    private fun useNodeJsInterpreterInCommandLine(
        project: Project,
        commandLine: GeneralCommandLine
    ) {
        // use configured nodejs interpreter
        val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        if (nodeJsInterpreter != null) {
            val effectiveEnvironment = commandLine.effectiveEnvironment
            val nodePath = nodeJsInterpreter.referenceName
            val nodeBinDir = nodePath.substring(0, nodePath.lastIndexOfAny(charArrayOf('/', '\\')))
            commandLine.environment["PATH"] = nodeBinDir + File.pathSeparator + effectiveEnvironment["PATH"]
        }
    }

    fun getWorkingDir(project: Project, testedVirtualFile: VirtualFile): VirtualFile {
        var wd = project.guessProjectDir()!!

        // Check if "project.js" exists in the guessed working directory
        val projectJs = wd.findChild("project.js")
        if (projectJs != null) {
            return wd
        }
        var packageJson = wd.findChild("package.json")
        if (packageJson != null) {
            return wd
        }

        packageJson = PackageJsonUtil.findUpPackageJson(testedVirtualFile)
        if (packageJson != null) {
            val packageJsonDir = packageJson.parent
            if (packageJsonDir != wd) {
                wd = packageJsonDir
            }
        }
        return wd

    }

    fun isWTrtest(leaf: LeafPsiElement): Boolean {
        if (leaf.parent is JSReferenceExpression && listOf(
                "it",
                "describe"
            ).contains(leaf.text)
        ) {
            return true
        }
        return false
    }

    fun isDescribe(leaf: LeafPsiElement): Boolean {
        if (leaf.parent is JSReferenceExpression && leaf.text == "describe") {
            return true
        }
        return false
    }

    fun getTestDisplayName(jsCallExpression: JSCallExpression): String {
        val testName = jsCallExpression.arguments[0].text
        return testName.trim {
            it == '\'' || it == '"' || it == '`'
        }
    }

    fun traverseTestNameFUll(psiElement: PsiElement): String {
        var processed = psiElement;
        var testname = ""
        while (processed.parent !== null) {
            if (processed is JSCallExpression) {
                val escapeProcessedVitestTestName = escapeVitestTestName(processed)
                testname =
                    if (testname === "") {
                        escapeProcessedVitestTestName
                    } else if (escapeProcessedVitestTestName === "") {
                        testname
                    } else {
                        escapeProcessedVitestTestName + " " + testname
                    }
            }
            processed = processed.parent
        }
        return testname
    }

    fun escapeVitestTestName(jsCallExpression: JSCallExpression): String {
        val arguments = jsCallExpression.arguments
        if (arguments.isEmpty()) {
            return ""
        }
        var commandName = arguments[0].text
        if (commandName[0] == commandName[commandName.length - 1]) {
            if (arrayListOf('\'', '"', '`').contains(commandName[0])) {
                commandName = commandName.substring(1, commandName.length - 1)
            }
        }
        return commandName.replace("\"", "\\\"")
            .replace("`", "\\`")
            .replace(")", "\\)")
            .replace("(", "\\(")
    }

    fun wtrInfo(project: Project, testedVirtualFile: VirtualFile): Pair<Boolean, Boolean> {
        val wd = getWorkingDir(project, testedVirtualFile)
        // check if file wtr.config.mjs exists
        val config = File(wd.path).resolve("wtr.config.mjs")

        val exists = config.exists()
        // not exist, not supported
        if (!exists) {
            return Pair(false, false);
        }
        // supported, decide wtr.plugin.runner.js or fallback to peon
        val wtrPluginRunner = File(wd.path).resolve("wtr.plugin.runner.js");

        val isWithWtrPluginRunner = wtrPluginRunner.exists();
        return Pair(true, isWithWtrPluginRunner);
    }

}
