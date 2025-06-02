package com.thedutchservers.tabsperproject.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TabsPerProjectToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val tabsPerProjectPanel = TabsPerProjectPanel(project)
        val content = contentFactory.createContent(tabsPerProjectPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
