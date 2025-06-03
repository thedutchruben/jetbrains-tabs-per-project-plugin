package com.thedutchservers.tabsperproject.dnd

import com.intellij.ide.dnd.DnDAction
import com.intellij.ide.dnd.DnDDragStartBean
import com.intellij.ide.dnd.DnDSource
import com.intellij.openapi.project.Project
import com.thedutchservers.tabsperproject.model.OpenFileInfo
import java.awt.Point
import javax.swing.JComponent

/**
 * Drag source implementation for dragging files from the tool window to the editor
 */
class FileDragSource(
    private val project: Project,
    private val component: JComponent
) : DnDSource {
    
    private var draggedFile: OpenFileInfo? = null
    private var dragStarted = false
    
    fun setDraggedFile(fileInfo: OpenFileInfo?) {
        draggedFile = fileInfo
    }
    
    override fun canStartDragging(action: DnDAction, dragOrigin: Point): Boolean {
        return draggedFile != null && !dragStarted
    }
    
    override fun startDragging(action: DnDAction, dragOrigin: Point): DnDDragStartBean {
        val fileInfo = draggedFile ?: return DnDDragStartBean("")
        
        dragStarted = true
        
        // Create transferable with the file
        val transferable = FileTransferable(fileInfo.file)
        
        // Return the drag start bean with our transferable
        return DnDDragStartBean(transferable)
    }
    
    override fun dragDropEnd() {
        dragStarted = false
        draggedFile = null
    }
    
    override fun dropActionChanged(gestureModifiers: Int) {
        // Handle modifier key changes during drag
    }
}
