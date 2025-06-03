package com.thedutchservers.tabsperproject.dnd

import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

/**
 * Transferable class for dragging VirtualFile objects from the tool window to other components
 */
class FileTransferable(private val files: List<VirtualFile>) : Transferable {
    
    companion object {
        // Custom data flavor for VirtualFile objects
        val VIRTUAL_FILE_FLAVOR = DataFlavor(
            "${DataFlavor.javaJVMLocalObjectMimeType};class=\"${VirtualFile::class.java.name}\"",
            "IntelliJ VirtualFile"
        )
        
        // Alternative flavor for single file
        val SINGLE_VIRTUAL_FILE_FLAVOR = DataFlavor(
            "${DataFlavor.javaJVMLocalObjectMimeType};class=\"${VirtualFile::class.java.name}\"",
            "Single IntelliJ VirtualFile"
        )
    }
    
    constructor(file: VirtualFile) : this(listOf(file))
    
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            VIRTUAL_FILE_FLAVOR,
            SINGLE_VIRTUAL_FILE_FLAVOR,
            DataFlavor.stringFlavor,
            DataFlavor.javaFileListFlavor
        )
    }
    
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor in transferDataFlavors
    }
    
    override fun getTransferData(flavor: DataFlavor): Any {
        return when (flavor) {
            VIRTUAL_FILE_FLAVOR -> files
            SINGLE_VIRTUAL_FILE_FLAVOR -> files.firstOrNull() 
                ?: throw UnsupportedFlavorException(flavor)
            DataFlavor.stringFlavor -> files.joinToString("\n") { it.path }
            DataFlavor.javaFileListFlavor -> files.mapNotNull { 
                try {
                    File(it.path)
                } catch (e: Exception) {
                    null
                }
            }
            else -> throw UnsupportedFlavorException(flavor)
        }
    }
    
    fun getFiles(): List<VirtualFile> = files
    
    fun getSingleFile(): VirtualFile? = files.firstOrNull()
}
