package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.configurations.GeneralCommandLine

class RunDebugTestMarkerProvider : RunOnceTestMarkerProvider() {
    override fun commandAppend(): String {
        return "--headless=false --devtools --watch";
    }

    override fun getProfile(commandLine: GeneralCommandLine, commandString: String, testNameFull: String): RunProfile {
        return RunDebugProfile(commandLine, commandString, testNameFull)
    }

    override fun getLineMarkerInfoIcon(): javax.swing.Icon {
        return debugIcon
    }

    override fun getTestLabel(testType: String, testName: String): String {
        return "Debug " + (if (testType == TEST_TYPE_IT) "test" else "test suite") + " \"$testName\""
    }
}