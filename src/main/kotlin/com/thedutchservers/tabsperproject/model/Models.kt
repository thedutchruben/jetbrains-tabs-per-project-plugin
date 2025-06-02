package com.thedutchservers.tabsperproject.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color

data class ProjectFileGroup(
    val project: Project,
    val files: MutableList<OpenFileInfo>,
    var color: Color? = null,
    val moduleGroups: MutableMap<String, MutableList<OpenFileInfo>> = mutableMapOf()
)

data class OpenFileInfo(
    val file: VirtualFile,
    val project: Project,
    val module: Module? = null,
    val lastModified: Long = System.currentTimeMillis()
)

enum class SortOrder {
    ALPHABETICAL,
    LAST_MODIFIED,
    PROJECT_THEN_ALPHA,
    MODULE_THEN_ALPHA;

    override fun toString(): String {
        return when (this) {
            ALPHABETICAL -> "Alphabetical"
            LAST_MODIFIED -> "Last Modified"
            PROJECT_THEN_ALPHA -> "Project then Alphabetical"
            MODULE_THEN_ALPHA -> "Module then Alphabetical"
        }
    }
}
