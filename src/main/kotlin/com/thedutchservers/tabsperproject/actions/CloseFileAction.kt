package com.thedutchservers.tabsperproject.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.thedutchservers.tabsperproject.TabsPerProjectBundle

class CloseFileAction(
    private val file: VirtualFile? = null,
    private val project: Project? = null
) : AnAction(
    TabsPerProjectBundle.message("action.closeFile"),
    TabsPerProjectBundle.message("action.closeFile"),
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val targetFile = file ?: return
        val targetProject = project ?: e.project ?: return
        
        FileEditorManager.getInstance(targetProject).closeFile(targetFile)
    }
    
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = file != null && project != null
    }
}
