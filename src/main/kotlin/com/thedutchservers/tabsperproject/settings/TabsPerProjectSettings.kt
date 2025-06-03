package com.thedutchservers.tabsperproject.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.thedutchservers.tabsperproject.model.SortOrder
import java.awt.Color

@State(
    name = "TabsPerProjectSettings",
    storages = [Storage("TabsPerProjectSettings.xml")]
)
@Service
class TabsPerProjectSettings : PersistentStateComponent<TabsPerProjectSettings> {
    
    var toolWindowPosition: String = "right"
    var sortOrder: SortOrder = SortOrder.MODULE_THEN_ALPHA
    var showProjectColors: Boolean = true
    var hideEditorTabs: Boolean = false
    var groupByModule: Boolean = true
    
    @MapAnnotation
    var projectColors: MutableMap<String, String> = mutableMapOf()
    
    @MapAnnotation
    var fileOrders: MutableMap<String, Int> = mutableMapOf()
    
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
    
    fun getFileOrder(filePath: String): Int {
        return fileOrders[filePath] ?: -1
    }
    
    fun setFileOrder(filePath: String, order: Int) {
        if (order < 0) {
            fileOrders.remove(filePath)
        } else {
            fileOrders[filePath] = order
        }
    }
    
    fun clearFileOrders() {
        fileOrders.clear()
    }
    
    companion object {
        fun getInstance(): TabsPerProjectSettings = 
            service<TabsPerProjectSettings>()
    }
}
