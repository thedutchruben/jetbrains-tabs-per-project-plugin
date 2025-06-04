package com.thedutchservers.tabsperproject.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import java.awt.Font

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

enum class TextCase {
    UPPERCASE,
    LOWERCASE,
    NORMAL;

    override fun toString(): String {
        return when (this) {
            UPPERCASE -> "UPPERCASE"
            LOWERCASE -> "lowercase"
            NORMAL -> "Normal Case"
        }
    }
    
    fun applyTo(text: String): String {
        return when (this) {
            UPPERCASE -> text.uppercase()
            LOWERCASE -> text.lowercase()
            NORMAL -> text
        }
    }
}

enum class FontStyle {
    NORMAL,
    BOLD,
    ITALIC,
    BOLD_ITALIC;

    override fun toString(): String {
        return when (this) {
            NORMAL -> "Normal"
            BOLD -> "Bold"
            ITALIC -> "Italic"
            BOLD_ITALIC -> "Bold + Italic"
        }
    }
    
    fun toAwtStyle(): Int {
        return when (this) {
            NORMAL -> java.awt.Font.PLAIN
            BOLD -> java.awt.Font.BOLD
            ITALIC -> java.awt.Font.ITALIC
            BOLD_ITALIC -> java.awt.Font.BOLD or java.awt.Font.ITALIC
        }
    }
}
