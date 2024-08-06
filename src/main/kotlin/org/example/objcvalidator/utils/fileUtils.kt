package org.example.objcvalidator.utils

import com.intellij.openapi.application.PathManager
import org.example.objcvalidator.PLUGIN_DIR_NAME
import java.io.File
import java.io.IOException


object Plugin {
    val ClassLoader = this::class.java.classLoader
}

fun copyResource(targetPath: String, targetFileName: String, destinationDirectoryName: String) {

    val resource = Plugin.ClassLoader.getResource("$targetPath/$targetFileName") ?: return

    val stream = resource.openStream() ?: return
    val content = stream.readAllBytes() ?: return
    writeFile(targetFileName, content, destinationDirectoryName)
}

fun writeFile(fileName: String, content: String, directoryName: String = "") {
    val directory = getOrCreateDirectory(directoryName)
    val file = File(directory, fileName)
    try {
        file.writeText(content)  // Write content to the file
        println("File created: ${file.absolutePath}")
    } catch (e: IOException) {
        println("An error occurred: ${e.message}")
    }
}

fun writeFile(fileName: String, content: ByteArray, directoryName: String = "") {
    val directory = getOrCreateDirectory(directoryName)
    val file = File(directory, fileName)
    try {
        file.writeBytes(content)  // Write content to the file
        println("File created: ${file.absolutePath}")
    } catch (e: IOException) {
        println("An error occurred: ${e.message}")
    }
}

fun getPluginDirFile(): File {
    return getOrCreateDirectory(PLUGIN_DIR_NAME)
}

fun getOrCreateDirectory(directoryName: String): File {
    val pluginDirectoryPath = PathManager.getPluginTempPath()
    val directory = if (directoryName.isEmpty()) {
        File(pluginDirectoryPath)
    } else {
        File(pluginDirectoryPath, directoryName).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    return directory
}