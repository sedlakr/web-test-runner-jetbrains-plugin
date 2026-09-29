package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile
import com.intellij.terminal.TerminalExecutionConsole
import javax.swing.Icon

open class RunProfile(
    private val profileCommandLine: GeneralCommandLine,
    originalCommand: String,
    private val testNameFull: String,
) :
    RunAnythingRunProfile(profileCommandLine, originalCommand) {


    override fun getIcon(): Icon {
        return runIcon
    }

    override fun getName(): String {
        return "Run tests: $testNameFull"
    }

    /**
     * Runs the pty command line this profile was built with in a terminal emulator.
     *
     * The test runner draws a live progress bar with colour and cursor movement, so its output only makes
     * sense to something that interprets a terminal stream. The stock state feeds a plain text console
     * through an ANSI decoder, which handles colour but leaves the cursor sequences in the text.
     *
     * The handler must be uncoloured: TerminalExecutionConsole wants the raw stream, and a colouring
     * handler strips the sequences it decodes before the emulator sees them, which also loses line breaks.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val processHandler = KillableProcessHandler(profileCommandLine)

                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }

            override fun createConsole(executor: Executor): ConsoleView {
                val console = TerminalExecutionConsole(environment.project, null)
                console.addMessageFilter(StackLocationFilter(environment.project, profileCommandLine.workDirectory))
                return console
            }
        }
    }
}
