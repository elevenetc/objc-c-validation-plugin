package org.example.objcvalidator.utils

import com.intellij.util.containers.addIfNotNull


data class Error(
    val id: String,
    val codeLines: List<String>,
    val line: Int,
    val column: Int
)

fun parseErrors(result: String, error: String): List<Error> {
    return if (error.isEmpty() && result.contains("** BUILD SUCCEEDED **")) {
        emptyList()
    } else {
        result.filterOutHeaderAndFooter().parseErrors()
    }
}

fun String.parseErrors(): List<Error> {
    if (isEmpty()) return emptyList()
    return filterOutHeaderAndFooter().parseErrors()
}

fun String.filterOutHeaderAndFooter(): List<String> {
    val result = this
    return result.lines()
        .dropWhile { !it.contains("header.h:") }
        .filter { !it.contains("error generated") && !it.contains("errors generated") }
        .filter { it.isNotBlank() }
}

fun List<String>.parseErrors(): List<Error> {
    val result = mutableListOf<Error>()
    var errorKey: String? = null
    var location: Pair<Int, Int>? = null
    val errorCodeLines = mutableListOf<String>()

    fun makeError(): Error? {
        return errorKey?.run {
            return Error(
                this,
                errorCodeLines.toList(),
                location?.first ?: -1,
                location?.second ?: -1,
            )
        }
    }

    fun collectErrorAndReset() {
        if (errorKey != null) {
            result.addIfNotNull(makeError())
            errorCodeLines.clear()
            errorKey = null
        }
    }

    for (line in this) {
        if (line.isError) {

            collectErrorAndReset()

            location = line.parseLocation()
            errorKey = line.substringAfter("error: ").trim()

        } else if (line.isWarning) {
            collectErrorAndReset()
        } else if (line.isNote) {
            collectErrorAndReset()
        } else if (errorKey != null) {
            errorCodeLines.add(line)
        }
    }

    result.addIfNotNull(makeError())// last one

    return result
}

private val String.isWarning: Boolean
    get() {
        return this.contains("warning: ") && (this.contains("header.h:") || this.contains("main.m:"))
    }

private val String.isNote: Boolean
    get() {
        return this.contains("note: ") && (this.contains("header.h:") || this.contains("main.m:"))
    }

private val String.isError: Boolean
    get() {
        return this.contains("error: ") && this.contains("header.h:")
    }

fun String.parseLocation(): Pair<Int, Int> {
    val raw = substringAfter(".h:").substringBefore(": error").split(":")
    return Pair(raw[0].toInt(), raw[1].toInt())
}