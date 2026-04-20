package com.github.sedlakr.webtestrunnerjetbrainsplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) {

    data class WtrInfo(val isSupported: Boolean, val isWithWtrPluginRunner: Boolean, val workingDir: VirtualFile?)

    // Keyed by wtr.config.mjs directory path so all files under the same sub-project share one entry.
    private val cache = mutableMapOf<String, WtrInfo>()

    init {
        thisLogger().info("AAAHOJ" + project.name)
    }

    fun wtrInfo(testedVirtualFile: VirtualFile) {
        val projectRoot = project.guessProjectDir()
        val wtrConfigDir = findWtrConfigDir(testedVirtualFile, projectRoot)
        val key = wtrConfigDir?.path ?: "none"
        if (cache.containsKey(key)) return

        thisLogger().info("Detect support of WTR for ${testedVirtualFile.path}")
        if (wtrConfigDir == null) {
            cache[key] = WtrInfo(false, false, null)
            thisLogger().info("Detect support of WTR - wtr.config.mjs not found")
            return
        }
        val wtrPluginRunner = File(wtrConfigDir.path).resolve("wtr.plugin.runner.js")
        val isWithWtrPluginRunner = wtrPluginRunner.exists()
        thisLogger().info("Detect support of WTR - supported at ${wtrConfigDir.path}, wtr.plugin.runner.js exists: $isWithWtrPluginRunner")
        cache[key] = WtrInfo(true, isWithWtrPluginRunner, wtrConfigDir)
    }

    fun wtrSupportedInfo(testedVirtualFile: VirtualFile): Pair<Boolean, Boolean> {
        val info = getCached(testedVirtualFile)
            ?: throw Exception("WTR info not initialized yet for ${testedVirtualFile.path}")
        return Pair(info.isSupported, info.isWithWtrPluginRunner)
    }

    fun getProjectWorkingDir(testedVirtualFile: VirtualFile): VirtualFile {
        val info = getCached(testedVirtualFile)
            ?: throw Exception("Working dir not initialized yet for ${testedVirtualFile.path}")
        return info.workingDir
            ?: throw Exception("Working dir is null (WTR not supported) for ${testedVirtualFile.path}")
    }

    private fun getCached(testedVirtualFile: VirtualFile): WtrInfo? {
        val projectRoot = project.guessProjectDir()
        val key = findWtrConfigDir(testedVirtualFile, projectRoot)?.path ?: "none"
        return cache[key]
    }

    companion object {
        fun getInstance(project: Project): MyProjectService {
            return project.getService<MyProjectService>(MyProjectService::class.java)
        }

        // Walk up from the test file to find the nearest directory containing wtr.config.mjs.
        // Stops at projectRoot so it never escapes the project.
        fun findWtrConfigDir(file: VirtualFile, projectRoot: VirtualFile?): VirtualFile? {
            var dir = if (file.isDirectory) file else file.parent
            while (dir != null) {
                if (dir.findChild("wtr.config.mjs") != null) {
                    return dir
                }
                if (projectRoot != null && dir.path == projectRoot.path) {
                    break
                }
                dir = dir.parent
            }
            return null
        }
    }
}
