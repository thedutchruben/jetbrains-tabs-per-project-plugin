package com.thedutchservers.tabsperproject.startup

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.thedutchservers.tabsperproject.settings.TabsPerProjectSettings
import java.util.concurrent.TimeUnit

class TabsPerProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val settings = TabsPerProjectSettings.getInstance()

            // Apply editor tabs visibility setting on startup
            try {
                // Use Registry to control tab visibility
                val registry = Registry.get("editor.tabs.show")
                registry.setValue(!settings.hideEditorTabs)

                // Update the UISettings
                val uiSettings = UISettings.getInstance()

                // Notify the system that UI settings have changed
                uiSettings.fireUISettingsChanged()

                // Add a small delay to ensure changes are applied before refreshing UI
                AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    // Trigger a layout update for the project
                    val frame = WindowManager.getInstance().getIdeFrame(project)
                    frame?.component?.validate()
                    frame?.component?.repaint()
                }, 300, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                Logger.getInstance(TabsPerProjectStartupActivity::class.java)
                    .warn("Failed to apply editor tabs visibility on startup", e)
            }

            // Apply saved tool window position
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("TabsPerProject") ?: return@invokeLater

            val anchor = when (settings.toolWindowPosition) {
                "left" -> ToolWindowAnchor.LEFT
                "bottom" -> ToolWindowAnchor.BOTTOM
                "top" -> ToolWindowAnchor.TOP
                else -> ToolWindowAnchor.RIGHT
            }
            toolWindow.setAnchor(anchor, null)
        }
    }
}
