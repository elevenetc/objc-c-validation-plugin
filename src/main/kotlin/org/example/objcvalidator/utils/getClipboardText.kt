package org.example.objcvalidator.utils

import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor


fun getClipboardText(): String? {
    val copyPasteManager = CopyPasteManager.getInstance()
    return if (copyPasteManager.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
        copyPasteManager.getContents<Any>(DataFlavor.stringFlavor) as String?
    } else {
        null
    }
}