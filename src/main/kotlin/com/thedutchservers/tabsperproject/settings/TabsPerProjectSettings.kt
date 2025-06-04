package com.thedutchservers.tabsperproject.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.thedutchservers.tabsperproject.model.SortOrder
import com.thedutchservers.tabsperproject.model.TextCase
import com.thedutchservers.tabsperproject.model.FontStyle
import java.awt.Color

@State(
    name = "TabsPerProjectSettings",
    storages = [Storage("TabsPerProjectSettings.xml")]
)
@Service
class TabsPerProjectSettings : PersistentStateComponent<TabsPerProjectSettings> {
    
    var toolWindowPosition: String = "right"
    var sortOrder: SortOrder = SortOrder.MODULE_THEN_ALPHA
    var hideEditorTabs: Boolean = false
    var groupByModule: Boolean = true
    
    // Project header styling
    var projectHeaderTextCase: TextCase = TextCase.UPPERCASE
    var projectHeaderFontStyle: FontStyle = FontStyle.BOLD
    var projectHeaderFontSize: Int = 14
    var projectHeaderTextColor: String? = null // null = use default theme color
    var projectHeaderBackgroundColor: String? = null // null = use project color if available
    
    // Module header styling
    var moduleHeaderTextCase: TextCase = TextCase.NORMAL
    var moduleHeaderFontStyle: FontStyle = FontStyle.BOLD_ITALIC
    var moduleHeaderFontSize: Int = 12
    var moduleHeaderTextColor: String? = null // null = use default theme color
    var moduleHeaderBackgroundColor: String? = null // null = use module color if available
    
    @MapAnnotation
    var projectColors: MutableMap<String, String> = mutableMapOf()
    
    @MapAnnotation
    var moduleColors: MutableMap<String, String> = mutableMapOf()
    
    override fun getState(): TabsPerProjectSettings = this
    
    override fun loadState(state: TabsPerProjectSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    fun getProjectColor(project: Project): Color? {
        val colorHex = projectColors[project.name] ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setProjectColor(project: Project, color: Color?) {
        if (color == null) {
            projectColors.remove(project.name)
        } else {
            projectColors[project.name] = String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    fun getModuleColor(project: Project, moduleName: String): Color? {
        val colorKey = "${project.name}:$moduleName"
        val colorHex = moduleColors[colorKey] ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setModuleColor(project: Project, moduleName: String, color: Color?) {
        val colorKey = "${project.name}:$moduleName"
        if (color == null) {
            moduleColors.remove(colorKey)
        } else {
            moduleColors[colorKey] = String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    // Header text color methods
    fun getProjectHeaderTextColor(): Color? {
        val colorHex = projectHeaderTextColor ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setProjectHeaderTextColor(color: Color?) {
        projectHeaderTextColor = if (color == null) {
            null
        } else {
            String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    fun getModuleHeaderTextColor(): Color? {
        val colorHex = moduleHeaderTextColor ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setModuleHeaderTextColor(color: Color?) {
        moduleHeaderTextColor = if (color == null) {
            null
        } else {
            String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    // Header background color methods
    fun getProjectHeaderBackgroundColor(): Color? {
        val colorHex = projectHeaderBackgroundColor ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setProjectHeaderBackgroundColor(color: Color?) {
        projectHeaderBackgroundColor = if (color == null) {
            null
        } else {
            String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    fun getModuleHeaderBackgroundColor(): Color? {
        val colorHex = moduleHeaderBackgroundColor ?: return null
        return try {
            Color.decode(colorHex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun setModuleHeaderBackgroundColor(color: Color?) {
        moduleHeaderBackgroundColor = if (color == null) {
            null
        } else {
            String.format("#%06X", color.rgb and 0xFFFFFF)
        }
    }
    
    companion object {
        fun getInstance(): TabsPerProjectSettings = 
            service<TabsPerProjectSettings>()
    }
}