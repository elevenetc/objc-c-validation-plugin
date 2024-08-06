package org.example.objcvalidator

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.firstOrNull
import org.example.objcvalidator.utils.executeXcodebuild
import org.example.objcvalidator.utils.getClipboardText
import org.jetbrains.annotations.NotNull

const val PLUGIN_DIR_NAME = "objc-validation"

class ValidateObjCHeader : AnAction() {

    override fun update(@NotNull event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = event.getData(CommonDataKeys.EDITOR)

        if (virtualFile != null || editor != null) {
            if (virtualFile != null) {
                event.presentation.isEnabledAndVisible = !virtualFile.isDirectory
            } else if (editor != null) {
                val selectionModel = editor.selectionModel
                val selectedText = selectionModel.selectedText
                event.presentation.isEnabledAndVisible = !selectedText.isNullOrBlank()
            }
        } else {
            event.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText
        val editorText = editor?.document?.text
        val clipboardText = getClipboardText()

        fun executeFirstNonNullSource(sourcesByPriority: Map<Source, String?>) {
            sourcesByPriority
                .filterValues { it != null }
                .mapValues { it.value!! }
                .firstOrNull()
                ?.run {
                    executeXcodebuild(this.value, this.key, project)
                    return
                }
        }

        if (event.place == "EditorPopup") {

            val sourcesByPriority = buildMap {
                put(Source.SelectedCode, selectedText)
                put(Source.Editor, editorText)
                put(Source.File(virtualFile), virtualFile.toString())
                put(Source.Clipboard, clipboardText)
            }
            executeFirstNonNullSource(
                sourcesByPriority
            )
        } else if (event.place == "MainMenu" && virtualFile != null) {

            executeFirstNonNullSource(
                buildMap {
                    put(Source.Editor, editorText)
                    put(Source.File(virtualFile), virtualFile.toString())
                    put(Source.SelectedCode, selectedText)
                    put(Source.Clipboard, clipboardText)
                }
            )
        } else if (event.place == "ProjectViewPopup" && virtualFile != null) {
            executeFirstNonNullSource(
                buildMap {
                    put(Source.Editor, editorText)
                    put(Source.File(virtualFile), virtualFile.toString())
                }
            )
        }
    }

}

sealed class Source(val name: String) {
    data class File(val vFile: VirtualFile?) : Source("File ${vFile?.name}")
    data object Clipboard : Source("Clipboard")
    data object Editor : Source("Editor")
    data object SelectedCode : Source("Selected code")
    data object Undefined : Source("Undefined")
}

private fun VirtualFile?.toString(): String {
    return if (this == null) ""
    else String(contentsToByteArray())
}