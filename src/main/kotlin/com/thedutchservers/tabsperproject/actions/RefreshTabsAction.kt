package com.thedutchservers.tabsperproject.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.thedutchservers.tabsperproject.TabsPerProjectBundle
import com.thedutchservers.tabsperproject.toolWindow.TabsPerProjectPanel

class RefreshTabsAction : AnAction(
    TabsPerProjectBundle.message("action.refresh"),
    TabsPerProjectBundle.message("action.refresh"),
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TabsPerProject") ?: return
        
        val content = toolWindow.contentManager.getContent(0) ?: return
        val panel = content.component as? TabsPerProjectPanel ?: return
        
        panel.refreshFileList()
    }
}
