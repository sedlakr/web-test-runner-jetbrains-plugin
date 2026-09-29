package com.github.sedlakr.webtestrunnerjetbrainsplugin

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Turns `path:line:column` in the test output into a link that opens the file there.
 *
 * The runner prints locations relative to the directory it runs in, for example
 * `(tests\bidi\basic.ts:102:15)`, which the IDE has no way to resolve on its own.
 */
class StackLocationFilter(
    private val project: Project,
    private val workDirectory: File?,
) : Filter {

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val lineStart = entireLength - line.length
        val items = LOCATION.findAll(line).mapNotNull { match ->
            val file = resolve(match.groupValues[1]) ?: return@mapNotNull null
            val lineIndex = match.groupValues[2].toInt() - 1
            val columnIndex = (match.groups[3]?.value?.toInt() ?: 1) - 1
            Filter.ResultItem(
                lineStart + match.range.first,
                lineStart + match.range.last + 1,
                OpenFileHyperlinkInfo(project, file, maxOf(lineIndex, 0), maxOf(columnIndex, 0)),
            )
        }.toList()
        return if (items.isEmpty()) null else Filter.Result(items)
    }

    private fun resolve(rawPath: String): VirtualFile? {
        val path = rawPath.replace('\\', '/')
        val candidate = if (File(path).isAbsolute) {
            File(path)
        } else {
            File(workDirectory ?: project.basePath?.let(::File) ?: return null, path)
        }
        return LocalFileSystem.getInstance().findFileByIoFile(candidate)?.takeIf { !it.isDirectory }
    }

    private companion object {
        val LOCATION = Regex("""((?:[A-Za-z]:)?[\w.\-\\/]+\.[cm]?[jt]sx?):(\d+)(?::(\d+))?""")
    }
}
