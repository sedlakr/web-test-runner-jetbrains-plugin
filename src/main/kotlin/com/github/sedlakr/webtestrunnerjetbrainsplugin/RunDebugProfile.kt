package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.configurations.GeneralCommandLine
import javax.swing.Icon

class RunDebugProfile(commandLine: GeneralCommandLine, originalCommand: String, testNameFull: String) : RunProfile(
    commandLine,
    originalCommand,
    testNameFull
) {
    override fun getIcon(): Icon {
        return debugIcon
    }

}