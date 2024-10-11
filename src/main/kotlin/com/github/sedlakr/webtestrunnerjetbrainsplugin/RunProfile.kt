package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile
import javax.swing.Icon

open class RunProfile(
    commandLine: GeneralCommandLine,
    originalCommand: String,
    private val testNameFull: String,
) :
    RunAnythingRunProfile(commandLine, originalCommand) {


    override fun getIcon(): Icon {
        return runIcon
    }

    override fun getName(): String {
        return "Run tests: $testNameFull"
    }
}
