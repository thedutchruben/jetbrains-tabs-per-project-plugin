package com.thedutchservers.tabsperproject.settings

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.FormBuilder
import com.thedutchservers.tabsperproject.TabsPerProjectBundle
import com.thedutchservers.tabsperproject.model.SortOrder
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel

class TabsPerProjectConfigurable : Configurable {
    private var mySettingsComponent: TabsPerProjectSettingsComponent? = null

    override fun getDisplayName(): String = TabsPerProjectBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        mySettingsComponent = TabsPerProjectSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = TabsPerProjectSettings.getInstance()
        val component = mySettingsComponent ?: return false

        return component.sortOrder != settings.sortOrder ||
               component.showProjectColors != settings.showProjectColors ||
               component.toolWindowPosition != settings.toolWindowPosition ||
               component.hideEditorTabs != settings.hideEditorTabs ||
               component.groupByModule != settings.groupByModule
    }

    override fun apply() {
        val settings = TabsPerProjectSettings.getInstance()
        val component = mySettingsComponent ?: return

        settings.sortOrder = component.sortOrder
        settings.showProjectColors = component.showProjectColors
        settings.toolWindowPosition = component.toolWindowPosition
        settings.hideEditorTabs = component.hideEditorTabs
        settings.groupByModule = component.groupByModule

        // Apply editor tabs visibility immediately
        applyEditorTabsVisibility(settings.hideEditorTabs)
    }

    override fun reset() {
        val settings = TabsPerProjectSettings.getInstance()
        val component = mySettingsComponent ?: return

        component.sortOrder = settings.sortOrder
        component.showProjectColors = settings.showProjectColors
        component.toolWindowPosition = settings.toolWindowPosition
        component.hideEditorTabs = settings.hideEditorTabs
        component.groupByModule = settings.groupByModule
    }

    private fun applyEditorTabsVisibility(hide: Boolean) {
        // Execute the operation on the UI thread
        ApplicationManager.getApplication().invokeLater {
            try {
                // Use Registry to control tab visibility - this is the key setting that controls tab visibility
                val registry = Registry.get("editor.tabs.show")
                registry.setValue(!hide)

                // Update the UISettings
                val uiSettings = UISettings.getInstance()

                // Note: We're no longer using reflection to set EDITOR_TAB_PLACEMENT
                // as it's not available in newer versions of the IntelliJ Platform API.
                // The registry key "editor.tabs.show" is sufficient to control tab visibility.

                // Notify the system that UI settings have changed
                uiSettings.fireUISettingsChanged()

                // Add a small delay to ensure changes are applied before refreshing UI
                AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    // Update all open projects to reflect the change
                    for (project in ProjectManager.getInstance().openProjects) {
                        if (project.isDisposed) continue

                        // Force EditorsSplitters to rebuild their UI
                        IdeFocusManager.getInstance(project).doWhenFocusSettlesDown {
                            ToolWindowManager.getInstance(project).invokeLater {
                                // Trigger a layout update
                                val frame = WindowManager.getInstance().getIdeFrame(project)
                                frame?.component?.validate()
                                frame?.component?.repaint()
                            }
                        }
                    }
                }, 300, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                // Log detailed error
                Logger.getInstance(TabsPerProjectConfigurable::class.java)
                    .warn("Failed to change editor tab visibility: ${e.message}", e)
            }
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

class TabsPerProjectSettingsComponent {
    val panel: JPanel
    private val sortOrderCombo = ComboBox(SortOrder.values())
    private val showColorsCheckBox = JBCheckBox(TabsPerProjectBundle.message("settings.showProjectColors"))
    private val hideEditorTabsCheckBox = JBCheckBox(TabsPerProjectBundle.message("settings.hideEditorTabs"))
    private val groupByModuleCheckBox = JBCheckBox(TabsPerProjectBundle.message("settings.groupByModule"))
    private val positionCombo = ComboBox(arrayOf("left", "right", "bottom"))

    var sortOrder: SortOrder
        get() = sortOrderCombo.selectedItem as SortOrder
        set(value) { sortOrderCombo.selectedItem = value }

    var showProjectColors: Boolean
        get() = showColorsCheckBox.isSelected
        set(value) { showColorsCheckBox.isSelected = value }

    var hideEditorTabs: Boolean
        get() = hideEditorTabsCheckBox.isSelected
        set(value) { hideEditorTabsCheckBox.isSelected = value }
    
    var groupByModule: Boolean
        get() = groupByModuleCheckBox.isSelected
        set(value) { groupByModuleCheckBox.isSelected = value }

    var toolWindowPosition: String
        get() = positionCombo.selectedItem as String
        set(value) { positionCombo.selectedItem = value }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.defaultPosition")), positionCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.sortOrder")), sortOrderCombo, 1, false)
            .addComponent(showColorsCheckBox, 1)
            .addComponent(hideEditorTabsCheckBox, 1)
            .addComponent(groupByModuleCheckBox, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
