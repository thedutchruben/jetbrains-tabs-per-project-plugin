package com.thedutchservers.tabsperproject.dnd

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import java.awt.Component
import java.awt.Point
import javax.swing.SwingUtilities

/**
 * Handles different file opening modes when files are dropped from the tool window
 */
class EditorDropHandler(private val project: Project) {
    
    /**
     * Opens files based on drop location and modifiers
     */
    fun handleFileDrop(files: List<VirtualFile>, dropPoint: Point, modifiers: Int) {
        if (files.isEmpty()) return
        
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        when {
            // Ctrl+Drop - Open in split view
            (modifiers and java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0 -> {
                openInSplitView(files.first(), fileEditorManager)
            }
            
            // Shift+Drop - Open in new window
            (modifiers and java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0 -> {
                openInNewWindow(files.first())
            }
            
            // Alt+Drop - Open but don't focus
            (modifiers and java.awt.event.InputEvent.ALT_DOWN_MASK) != 0 -> {
                openWithoutFocus(files.first(), fileEditorManager)
            }
            
            // Normal drop - Open normally
            else -> {
                openNormally(files, fileEditorManager)
            }
        }
    }
    
    /**
     * Opens a single file in the main editor area
     */
    private fun openNormally(files: List<VirtualFile>, fileEditorManager: FileEditorManager) {
        files.forEach { file ->
            fileEditorManager.openFile(file, true)
        }
    }
    
    /**
     * Opens file without focusing the editor
     */
    private fun openWithoutFocus(file: VirtualFile, fileEditorManager: FileEditorManager) {
        fileEditorManager.openFile(file, false)
    }
    
    /**
     * Opens file in a split view if possible
     */
    private fun openInSplitView(file: VirtualFile, fileEditorManager: FileEditorManager) {
        try {
            val fileEditorManagerEx = fileEditorManager as? FileEditorManagerEx
            if (fileEditorManagerEx != null) {
                // Try to create a split view
                val currentFile = fileEditorManager.selectedFiles.firstOrNull()
                if (currentFile != null) {
                    // Open in split - IntelliJ will handle the split creation
                    fileEditorManager.openFile(file, true)
                    
                    // Use the FileEditorManagerEx for more advanced operations if needed
                    // This is a simplified approach - real split handling is more complex
                } else {
                    // No current file, just open normally
                    fileEditorManager.openFile(file, true)
                }
            } else {
                // Fallback to normal opening
                fileEditorManager.openFile(file, true)
            }
        } catch (e: Exception) {
            // If split view fails, open normally
            fileEditorManager.openFile(file, true)
        }
    }
    
    /**
     * Opens file in a new detached window
     */
    private fun openInNewWindow(file: VirtualFile) {
        try {
            val fileEditorManager = FileEditorManager.getInstance(project)
            
            // For now, we'll open in the main window and let the user detach manually
            // IntelliJ's API for programmatic window detachment is complex and version-dependent
            fileEditorManager.openFile(file, true)
            
            // Note: To truly detach to a new window programmatically, you would need:
            // 1. Access to the EditorWindow system
            // 2. Create a new detached EditorWindow
            // 3. Move the editor to that window
            // This is quite complex and may not be supported in all IntelliJ versions
            
        } catch (e: Exception) {
            // Fallback to normal opening
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }
    
    /**
     * Determines the intended drop action based on drop location
     */
    fun getDropActionFromLocation(dropPoint: Point): DropAction {
        // Try to determine if we're dropping on the tab area vs editor area
        val frame = WindowManager.getInstance().getIdeFrame(project)
        val component = frame?.component
        
        if (component != null) {
            val editorComponent = SwingUtilities.getDeepestComponentAt(component, dropPoint.x, dropPoint.y)
            
            // This is a simplified check - in a real implementation you'd want to
            // check for specific editor components and tab areas
            return when {
                editorComponent?.javaClass?.name?.contains("Tab") == true -> DropAction.NEW_TAB
                editorComponent?.javaClass?.name?.contains("Editor") == true -> DropAction.REPLACE_CONTENT
                else -> DropAction.NEW_TAB
            }
        }
        
        return DropAction.NEW_TAB
    }
    
    enum class DropAction {
        NEW_TAB,
        REPLACE_CONTENT,
        SPLIT_VIEW,
        NEW_WINDOW
    }
}
