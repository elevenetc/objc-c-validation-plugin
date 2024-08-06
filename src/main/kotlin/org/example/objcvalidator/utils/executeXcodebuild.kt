package org.example.objcvalidator.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.example.objcvalidator.PLUGIN_DIR_NAME
import org.example.objcvalidator.Source
import java.awt.BorderLayout
import java.awt.event.ActionListener
import java.io.BufferedReader
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

fun executeXcodebuild(
    objCCode: String,
    source: Source,
    project: Project?
) {

    ProgressManager.getInstance().run(object : Task.Modal(project, "Running xcodebuild", true) {
        var result: String = ""
        var rawErrors: String = ""
        lateinit var errors: List<Error>

        override fun run(indicator: ProgressIndicator) {
            try {

                copyResource("app", "dependencies.h", "$PLUGIN_DIR_NAME/app")
                copyResource("app", "/main.m", "$PLUGIN_DIR_NAME/app")
                writeFile("header.h", objCCode, "$PLUGIN_DIR_NAME/app")
                getOrCreateDirectory("objc-header-analyzer.xcodeproj")
                copyResource(
                    "objc-header-analyzer.xcodeproj",
                    "project.pbxproj",
                    "$PLUGIN_DIR_NAME/objc-header-analyzer.xcodeproj"
                )

                //val processBuilder = ProcessBuilder("xcodebuild", "-configuration", "Debug").apply {
                val processBuilder = ProcessBuilder("xcodebuild").apply {
                    directory(getPluginDirFile())
                }
                val process = processBuilder.start()

                result = process.inputStream.bufferedReader().use(BufferedReader::readText)
                rawErrors = process.errorStream.bufferedReader().use(BufferedReader::readText)
                errors = parseErrors(result, rawErrors)

                writeFile("result.txt", result, PLUGIN_DIR_NAME)
                writeFile("errors.txt", rawErrors, PLUGIN_DIR_NAME)
            } catch (e: Exception) {
                e.printStackTrace()
                rawErrors = e.message ?: "Unknown error"
            }
        }

        override fun onFinished() {
            showDialog(errors, source)
        }
    })
}

private fun showDialog(errors: List<Error>, source: Source) {
    FormattedTextDialog(
        if (errors.isEmpty())
            buildNoErrorMessage("No errors, header is valid.", source)
        else
            buildErrorMessage(errors, source)
    ).show()
}

private fun buildNoErrorMessage(message: String, source: Source): String {
    return "<html><body>" +
            "<div style='color: green;'>$message</div>" +
            sourceDiv(source) +
            "</body></html>"
}

private fun sourceDiv(source: Source) = "<div style='color: white;'>Source: ${source.name}</div>"

private fun buildErrorMessage(errors: List<Error>, source: Source): String {
    val htmlBuilder = StringBuilder("<html><body>")
    for (error in errors) {
        htmlBuilder.append("<div>")
        htmlBuilder.append("<p style='color: red;'><strong>Error (${error.line},${error.column}):</strong> ${error.id}</p>")
        error.codeLines.forEach { line ->
            htmlBuilder.append("<pre style='margin: 0; padding: 0; color: white; background-color: black; font-family: monospace;'>$line</pre>")
        }
        htmlBuilder.append("</div>")
    }
    htmlBuilder.append(sourceDiv(source))
    htmlBuilder.append("</body></html>")
    return htmlBuilder.toString()
}

private class FormattedTextDialog(private val message: String) : DialogWrapper(true) {
    init {
        title = "ObjC Header validation result"
        init()
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }

    override fun createCancelAction(): ActionListener? {
        return null
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(
                JLabel(message), BorderLayout.CENTER
            )
        }
    }
}