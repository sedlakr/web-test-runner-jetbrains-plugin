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
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.execution.ParametersListUtil
import java.io.File
import java.nio.charset.Charset
import java.nio.file.FileSystem
import javax.swing.SwingUtilities


open class MyLineMarkerProvider : RunLineMarkerProvider() {

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        val testedVirtualFile = psiElement.containingFile.virtualFile
        if (!supportsWtr(psiElement.project, testedVirtualFile)) {
            return null
        }
        if (psiElement is LeafPsiElement) {
            if (isWTrtest(psiElement)) {

                val testName = traverseTestNameFUll(psiElement);
                return LineMarkerInfo(
                    psiElement,
                    psiElement.textRange,
                    runIcon,
                    { _: PsiElement? ->
                        "Run WTR test $testName"
                    },
                    { _, _ ->
                        runCommand(psiElement, testName,testedVirtualFile)

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

    private fun runCommand(psiElement: PsiElement, testNameFull: String, testFile: VirtualFile) {
        thisLogger().info("run test $testNameFull")
        // execute node peon.js
        val project = psiElement.project
        val workDirectory = getWorkingDir(project, psiElement.containingFile.virtualFile);
        thisLogger().info("Working directory: $workDirectory")
        val dataContext = SimpleDataContext.getProjectContext(project)
        val commandDataContext =
            RunAnythingCommandCustomizer.customizeContext(dataContext);
        val commandString = "node --no-warnings peonRunner.js test --runTestsByPath=${testFile.path} --verbose --testNamePattern=\"${testNameFull}\""
        val initialCommandLine = GeneralCommandLine(ParametersListUtil.parse(commandString, false, true))
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
            .withWorkDirectory(workDirectory.path)
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val commandLine =
            RunAnythingCommandCustomizer.customizeCommandLine(commandDataContext, workDirectory, initialCommandLine)
        // use configured nodejs interpreter
        val nodeJsInterpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        if (nodeJsInterpreter != null) {
            val effectiveEnvironment = commandLine.effectiveEnvironment
            val nodePath = nodeJsInterpreter.referenceName
            val nodeBinDir = nodePath.substring(0, nodePath.lastIndexOfAny(charArrayOf('/', '\\')))
            commandLine.environment["PATH"] = nodeBinDir + File.pathSeparator + effectiveEnvironment["PATH"]
        }
        val generalCommandLine =
            if (Registry.`is`("run.anything.use.pty", false)) PtyCommandLine(commandLine) else commandLine
        generalCommandLine.charset = Charset.forName("UTF-8")
        val profile = RunProfile(generalCommandLine, commandString)
        ExecutionEnvironmentBuilder.create(project, executor, profile)
            .dataContext(commandDataContext)
            .buildAndExecute()
//        SwingUtilities.invokeLater {
//            thisLogger().info("Invoking cmd")
//            environment.runner.execute(environment)
//            thisLogger().info("After invoking cmd")
//        }

        thisLogger().info("Done")
    }

    fun getWorkingDir(project: Project, testedVirtualFile: VirtualFile): VirtualFile {
        var wd = project.guessProjectDir()!!
        val packageJson = PackageJsonUtil.findUpPackageJson(testedVirtualFile)
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
                "test",
                "it",
                "describe"
            ).contains(leaf.text.split(".")[0])
        ) {
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
                testname = escapeVitestTestName(processed) + " " + testname
            }
            processed = processed.parent
        }
        return testname
    }

    fun escapeVitestTestName(jsCallExpression: JSCallExpression): String {
        val arguments = jsCallExpression.arguments
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

    fun supportsWtr(project: Project, testedVirtualFile: VirtualFile): Boolean {
        val wd = getWorkingDir(project, testedVirtualFile)
        // check if file wtr.config.mjs exists
        val config = File(wd.path).resolve("wtr.config.mjs")

        println(config.path)
        return config.exists();
    }

}