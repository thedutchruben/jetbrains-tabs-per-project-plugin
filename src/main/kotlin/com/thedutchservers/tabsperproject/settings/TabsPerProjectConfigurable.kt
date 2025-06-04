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
import com.intellij.ui.ColorChooserService
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.FormBuilder
import com.thedutchservers.tabsperproject.TabsPerProjectBundle
import com.thedutchservers.tabsperproject.model.SortOrder
import com.thedutchservers.tabsperproject.model.TextCase
import com.thedutchservers.tabsperproject.model.FontStyle
import com.thedutchservers.tabsperproject.toolWindow.TabsPerProjectPanel
import java.awt.Color
import java.awt.Font
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
               component.toolWindowPosition != settings.toolWindowPosition ||
               component.hideEditorTabs != settings.hideEditorTabs ||
               component.groupByModule != settings.groupByModule ||
               component.projectHeaderTextCase != settings.projectHeaderTextCase ||
               component.projectHeaderFontStyle != settings.projectHeaderFontStyle ||
               component.projectHeaderFontSize != settings.projectHeaderFontSize ||
               component.projectHeaderTextColor != settings.getProjectHeaderTextColor() ||
               component.projectHeaderBackgroundColor != settings.getProjectHeaderBackgroundColor() ||
               component.moduleHeaderTextCase != settings.moduleHeaderTextCase ||
               component.moduleHeaderFontStyle != settings.moduleHeaderFontStyle ||
               component.moduleHeaderFontSize != settings.moduleHeaderFontSize ||
               component.moduleHeaderTextColor != settings.getModuleHeaderTextColor() ||
               component.moduleHeaderBackgroundColor != settings.getModuleHeaderBackgroundColor()
    }

    override fun apply() {
        val settings = TabsPerProjectSettings.getInstance()
        val component = mySettingsComponent ?: return

        settings.sortOrder = component.sortOrder
        settings.toolWindowPosition = component.toolWindowPosition
        settings.hideEditorTabs = component.hideEditorTabs
        settings.groupByModule = component.groupByModule
        settings.projectHeaderTextCase = component.projectHeaderTextCase
        settings.projectHeaderFontStyle = component.projectHeaderFontStyle
        settings.projectHeaderFontSize = component.projectHeaderFontSize
        settings.setProjectHeaderTextColor(component.projectHeaderTextColor)
        settings.setProjectHeaderBackgroundColor(component.projectHeaderBackgroundColor)
        settings.moduleHeaderTextCase = component.moduleHeaderTextCase
        settings.moduleHeaderFontStyle = component.moduleHeaderFontStyle
        settings.moduleHeaderFontSize = component.moduleHeaderFontSize
        settings.setModuleHeaderTextColor(component.moduleHeaderTextColor)
        settings.setModuleHeaderBackgroundColor(component.moduleHeaderBackgroundColor)

        // Apply editor tabs visibility immediately
        applyEditorTabsVisibility(settings.hideEditorTabs)
        
        // Refresh all TabsPerProject panels to apply styling changes
        refreshAllTabsPerProjectPanels()
    }

    override fun reset() {
        val settings = TabsPerProjectSettings.getInstance()
        val component = mySettingsComponent ?: return

        component.sortOrder = settings.sortOrder
        component.toolWindowPosition = settings.toolWindowPosition
        component.hideEditorTabs = settings.hideEditorTabs
        component.groupByModule = settings.groupByModule
        component.projectHeaderTextCase = settings.projectHeaderTextCase
        component.projectHeaderFontStyle = settings.projectHeaderFontStyle
        component.projectHeaderFontSize = settings.projectHeaderFontSize
        component.projectHeaderTextColor = settings.getProjectHeaderTextColor()
        component.projectHeaderBackgroundColor = settings.getProjectHeaderBackgroundColor()
        component.moduleHeaderTextCase = settings.moduleHeaderTextCase
        component.moduleHeaderFontStyle = settings.moduleHeaderFontStyle
        component.moduleHeaderFontSize = settings.moduleHeaderFontSize
        component.moduleHeaderTextColor = settings.getModuleHeaderTextColor()
        component.moduleHeaderBackgroundColor = settings.getModuleHeaderBackgroundColor()
    }

    private fun applyEditorTabsVisibility(hide: Boolean) {
        // Execute the operation on the UI thread
        ApplicationManager.getApplication().invokeLater {
            try {
                val registry = Registry.get("editor.tabs.show")
                registry.setValue(!hide)

                val uiSettings = UISettings.getInstance()

                uiSettings.fireUISettingsChanged()

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

    private fun refreshAllTabsPerProjectPanels() {
        ApplicationManager.getApplication().invokeLater {
            // Update all open projects to reflect styling changes
            for (project in ProjectManager.getInstance().openProjects) {
                if (project.isDisposed) continue
                
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TabsPerProject")
                if (toolWindow != null) {
                    val content = toolWindow.contentManager.getContent(0)
                    if (content != null) {
                        val panel = content.component as? TabsPerProjectPanel
                        panel?.refreshFileList()
                    }
                }
            }
        }
    }
    
    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

class TabsPerProjectSettingsComponent {
    val panel: JPanel
    private val sortOrderCombo = ComboBox(SortOrder.entries.toTypedArray())
    private val hideEditorTabsCheckBox = JBCheckBox(TabsPerProjectBundle.message("settings.hideEditorTabs"))
    private val groupByModuleCheckBox = JBCheckBox(TabsPerProjectBundle.message("settings.groupByModule"))
    private val positionCombo = ComboBox(arrayOf("left", "right", "bottom"))
    
    // Project header styling controls
    private val projectHeaderTextCaseCombo = ComboBox(TextCase.entries.toTypedArray())
    private val projectHeaderFontStyleCombo = ComboBox(FontStyle.entries.toTypedArray())
    private val projectHeaderFontSizeCombo = ComboBox(arrayOf(8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24))
    private val projectHeaderTextColorPanel = ColorPanel()
    private val projectHeaderBackgroundColorPanel = ColorPanel()
    
    // Module header styling controls
    private val moduleHeaderTextCaseCombo = ComboBox(TextCase.entries.toTypedArray())
    private val moduleHeaderFontStyleCombo = ComboBox(FontStyle.entries.toTypedArray())
    private val moduleHeaderFontSizeCombo = ComboBox(arrayOf(8, 9, 10, 11, 12, 13, 14, 16, 18, 20))
    private val moduleHeaderTextColorPanel = ColorPanel()
    private val moduleHeaderBackgroundColorPanel = ColorPanel()

    var sortOrder: SortOrder
        get() = sortOrderCombo.selectedItem as SortOrder
        set(value) { sortOrderCombo.selectedItem = value }

    var hideEditorTabs: Boolean
        get() = hideEditorTabsCheckBox.isSelected
        set(value) { hideEditorTabsCheckBox.isSelected = value }
    
    var groupByModule: Boolean
        get() = groupByModuleCheckBox.isSelected
        set(value) { groupByModuleCheckBox.isSelected = value }

    var toolWindowPosition: String
        get() = positionCombo.selectedItem as String
        set(value) { positionCombo.selectedItem = value }
        
    var projectHeaderTextCase: TextCase
        get() = projectHeaderTextCaseCombo.selectedItem as TextCase
        set(value) { projectHeaderTextCaseCombo.selectedItem = value }
        
    var projectHeaderFontStyle: FontStyle
        get() = projectHeaderFontStyleCombo.selectedItem as FontStyle
        set(value) { projectHeaderFontStyleCombo.selectedItem = value }
        
    var projectHeaderFontSize: Int
        get() = projectHeaderFontSizeCombo.selectedItem as Int
        set(value) { projectHeaderFontSizeCombo.selectedItem = value }
        
    var projectHeaderTextColor: Color?
        get() = projectHeaderTextColorPanel.selectedColor
        set(value) { projectHeaderTextColorPanel.selectedColor = value }
        
    var projectHeaderBackgroundColor: Color?
        get() = projectHeaderBackgroundColorPanel.selectedColor
        set(value) { projectHeaderBackgroundColorPanel.selectedColor = value }
        
    var moduleHeaderTextCase: TextCase
        get() = moduleHeaderTextCaseCombo.selectedItem as TextCase
        set(value) { moduleHeaderTextCaseCombo.selectedItem = value }
        
    var moduleHeaderFontStyle: FontStyle
        get() = moduleHeaderFontStyleCombo.selectedItem as FontStyle
        set(value) { moduleHeaderFontStyleCombo.selectedItem = value }
        
    var moduleHeaderFontSize: Int
        get() = moduleHeaderFontSizeCombo.selectedItem as Int
        set(value) { moduleHeaderFontSizeCombo.selectedItem = value }
        
    var moduleHeaderTextColor: Color?
        get() = moduleHeaderTextColorPanel.selectedColor
        set(value) { moduleHeaderTextColorPanel.selectedColor = value }
        
    var moduleHeaderBackgroundColor: Color?
        get() = moduleHeaderBackgroundColorPanel.selectedColor
        set(value) { moduleHeaderBackgroundColorPanel.selectedColor = value }

    init {
        // Initialize color panels to allow null values
        projectHeaderTextColorPanel.selectedColor = null
        projectHeaderBackgroundColorPanel.selectedColor = null
        moduleHeaderTextColorPanel.selectedColor = null
        moduleHeaderBackgroundColorPanel.selectedColor = null

        // Reset Style button
        val resetStyleButton = javax.swing.JButton("Reset Style")
        resetStyleButton.toolTipText = "Restore all style settings to default values"
        resetStyleButton.addActionListener {
            // Project header defaults
            projectHeaderTextCase = TextCase.UPPERCASE
            projectHeaderFontStyle = FontStyle.BOLD
            projectHeaderFontSize = 18
            projectHeaderTextColor = null
            projectHeaderBackgroundColor = null
            // Module header defaults
            moduleHeaderTextCase = TextCase.NORMAL
            moduleHeaderFontStyle = FontStyle.BOLD_ITALIC
            moduleHeaderFontSize = 12
            moduleHeaderTextColor = null
            moduleHeaderBackgroundColor = null
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.defaultPosition")), positionCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.sortOrder")), sortOrderCombo, 1, false)
            .addComponent(hideEditorTabsCheckBox, 1)
            .addComponent(groupByModuleCheckBox, 1)
            .addSeparator()
            .addComponent(JBLabel(TabsPerProjectBundle.message("settings.projectHeaderStyling")).apply { 
                font = font.deriveFont(Font.BOLD)
            }, 1)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.textCase")), projectHeaderTextCaseCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.fontStyle")), projectHeaderFontStyleCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.fontSize")), projectHeaderFontSizeCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.textColor")), projectHeaderTextColorPanel, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.backgroundColor")), projectHeaderBackgroundColorPanel, 1, false)
            .addSeparator()
            .addComponent(JBLabel(TabsPerProjectBundle.message("settings.moduleHeaderStyling")).apply { 
                font = font.deriveFont(Font.BOLD)
            }, 1)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.textCase")), moduleHeaderTextCaseCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.fontStyle")), moduleHeaderFontStyleCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.fontSize")), moduleHeaderFontSizeCombo, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.textColor")), moduleHeaderTextColorPanel, 1, false)
            .addLabeledComponent(JBLabel(TabsPerProjectBundle.message("settings.backgroundColor")), moduleHeaderBackgroundColorPanel, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .addSeparator()
            .addComponent(resetStyleButton, 1)
            .panel
    }
}