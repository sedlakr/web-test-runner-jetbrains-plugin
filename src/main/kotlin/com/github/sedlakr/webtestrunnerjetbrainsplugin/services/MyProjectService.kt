package com.github.sedlakr.webtestrunnerjetbrainsplugin.services

import com.intellij.lang.javascript.buildTools.npm.PackageJsonUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    var workingDir: VirtualFile? = null
    var wtrInfoInitialized = false;
    var isWithWtrPluginRunner = false
    var isSupported = false;

    init {
        thisLogger().info("AAAHOJ" + project.name)
    }

    fun wtrSupportedInfo(): Pair<Boolean, Boolean> {
        if (!wtrInfoInitialized) {
            throw Exception("WTR info not initialized yet")
        }
        return Pair(isSupported, isWithWtrPluginRunner);
    }

    fun wtrInfo(project: Project, testedVirtualFile: VirtualFile) {
        if (wtrInfoInitialized) {
            return
        }
        thisLogger().info("Detect support of WTR")
        wtrInfoInitialized = true;
        val wd = getWorkingDir(project, testedVirtualFile)
        workingDir = wd;
        // check if file wtr.config.mjs exists
        val config = File(wd.path).resolve("wtr.config.mjs")

        val exists = config.exists()
        // not exist, not supported
        if (!exists) {
            isSupported = false;
            isWithWtrPluginRunner = false;
            thisLogger().info("Detect support of WTR - wtr.config.mjs not found")
            return;
        }
        // supported, decide wtr.plugin.runner.js or fallback to peon
        val wtrPluginRunner = File(wd.path).resolve("wtr.plugin.runner.js");

        val isWithWtrPluginRunner = wtrPluginRunner.exists();
        isSupported = true;
        thisLogger().info("Detect support of WTR - supported, wtr.plugin.runner.js exists: " + isWithWtrPluginRunner)
        this.isWithWtrPluginRunner = isWithWtrPluginRunner;
    }

    fun getProjectWorkingDir(): VirtualFile {
        if (workingDir == null) {
            throw Exception("Working dir not initialized yet")
        }
        return workingDir as VirtualFile;
    }

    companion object {
        fun getInstance(project: Project): MyProjectService {
            return project.getService<MyProjectService>(MyProjectService::class.java)
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
    }
}
