package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile
import javax.swing.Icon

class RunProfile(commandLine: GeneralCommandLine, originalCommand: String) : RunAnythingRunProfile(commandLine, originalCommand) {
    override fun getIcon(): Icon {
        return runIcon
    }

    override fun getName(): String {
        return "Run tests WTR"
    }
}
