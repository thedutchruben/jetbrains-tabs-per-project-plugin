package com.thedutchservers.tabsperproject.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.thedutchservers.tabsperproject.TabsPerProjectBundle
import com.thedutchservers.tabsperproject.actions.CloseFileAction
import com.thedutchservers.tabsperproject.model.OpenFileInfo
import com.thedutchservers.tabsperproject.model.ProjectFileGroup
import com.thedutchservers.tabsperproject.model.SortOrder
import com.thedutchservers.tabsperproject.settings.TabsPerProjectSettings
import io.ktor.client.plugins.cache.storage.FileStorage
import java.awt.*
import java.awt.Color
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class TabsPerProjectPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {
    private val contentPanel = JBPanel<JBPanel<*>>(VerticalFlowLayout(0, 0))
    private val fileGroups = mutableMapOf<Project, ProjectFileGroup>()
    
    init {
        setupUI()
        setupListeners()
        refreshFileList()
    }
    
    private fun setupUI() {
        val scrollPane = ScrollPaneFactory.createScrollPane(contentPanel)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        
        setContent(scrollPane)
    }
    
    private fun setupListeners() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                ApplicationManager.getApplication().invokeLater {
                    refreshFileList()
                }
            }
            
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                ApplicationManager.getApplication().invokeLater {
                    refreshFileList()
                }
            }
            
            override fun selectionChanged(event: FileEditorManagerEvent) {
                ApplicationManager.getApplication().invokeLater {
                    refreshFileList()
                }
            }
        })
    }
    
    fun refreshFileList() {
        fileGroups.clear()
        
        // Only collect files from the current project window
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles

        if (openFiles.isNotEmpty()) {
            val group = fileGroups.getOrPut(project) {
                ProjectFileGroup(
                    project,
                    mutableListOf(),
                    TabsPerProjectSettings.getInstance().getProjectColor(project)
                )
            }

            openFiles.forEach { file ->
                // Find the module for this file
                val module = ProjectRootManager.getInstance(project)
                    .fileIndex
                    .getModuleForFile(file)
                
                val fileInfo = OpenFileInfo(file, project, module)
                group.files.add(fileInfo)
                
                // Determine the actual project/module name
                val effectiveModuleName = when {
                    // Special handling for Rider with .NET solutions
                    module != null && module.name.contains("rider.module", ignoreCase = true) -> {
                        detectDotNetProject(file, project)
                    }
                    module != null -> module.name
                    else -> null
                }
                
                // Group by effective module name if it exists
                if (effectiveModuleName != null) {
                    group.moduleGroups.getOrPut(effectiveModuleName) { mutableListOf() }
                        .add(fileInfo)
                }
            }
        }
        
        // Sort files according to settings
        val settings = TabsPerProjectSettings.getInstance()
        when (settings.sortOrder) {
            SortOrder.ALPHABETICAL -> {
                fileGroups.values.forEach { group ->
                    group.files.sortBy { it.file.name.lowercase() }
                    group.moduleGroups.values.forEach { moduleFiles ->
                        moduleFiles.sortBy { it.file.name.lowercase() }
                    }
                }
            }
            SortOrder.LAST_MODIFIED -> {
                fileGroups.values.forEach { group ->
                    group.files.sortByDescending { it.lastModified }
                    group.moduleGroups.values.forEach { moduleFiles ->
                        moduleFiles.sortByDescending { it.lastModified }
                    }
                }
            }
            SortOrder.PROJECT_THEN_ALPHA -> {
                fileGroups.values.forEach { group ->
                    group.files.sortBy { it.file.name.lowercase() }
                    group.moduleGroups.values.forEach { moduleFiles ->
                        moduleFiles.sortBy { it.file.name.lowercase() }
                    }
                }
            }
            SortOrder.MODULE_THEN_ALPHA -> {
                fileGroups.values.forEach { group ->
                    // Sort files by their module group key
                    val moduleMap = mutableMapOf<OpenFileInfo, String>()
                    group.moduleGroups.forEach { (moduleName, files) ->
                        files.forEach { file ->
                            moduleMap[file] = moduleName
                        }
                    }
                    
                    group.files.sortWith(compareBy(
                        { moduleMap[it] ?: "zzz_no_module" },
                        { it.file.name.lowercase() }
                    ))
                    
                    // Also sort within module groups
                    group.moduleGroups.values.forEach { moduleFiles ->
                        moduleFiles.sortBy { it.file.name.lowercase() }
                    }
                }
            }
        }
        
        updateFileListUI()
    }
    
    private fun updateFileListUI() {
        contentPanel.removeAll()
        
        val settings = TabsPerProjectSettings.getInstance()
        
        fileGroups.entries.sortedBy { it.key.name }.forEach { (project, group) ->
            // Project header
            val headerPanel = createProjectHeader(project, group)
            contentPanel.add(headerPanel)
            
            // Show files grouped by module if enabled
            if (settings.groupByModule && group.moduleGroups.isNotEmpty()) {
                // Show files grouped by module
                group.moduleGroups.entries.sortedBy { it.key }.forEach { (moduleName, moduleFiles) ->
                    // Module sub-header
                    val moduleHeader = createModuleHeader(moduleName, moduleFiles.size)
                    contentPanel.add(moduleHeader)
                    
                    // Files in this module
                    moduleFiles.forEach { fileInfo ->
                        val filePanel = createFilePanel(fileInfo, indentLevel = 2)
                        contentPanel.add(filePanel)
                    }
                }
                
                // Show files without module
                val filesWithoutModule = group.files.filter { it.module == null }
                if (filesWithoutModule.isNotEmpty()) {
                    val noModuleHeader = createModuleHeader("No Module", filesWithoutModule.size)
                    contentPanel.add(noModuleHeader)
                    
                    filesWithoutModule.forEach { fileInfo ->
                        val filePanel = createFilePanel(fileInfo, indentLevel = 2)
                        contentPanel.add(filePanel)
                    }
                }
            } else {
                // Original behavior - just show all files under project
                group.files.forEach { fileInfo ->
                    val filePanel = createFilePanel(fileInfo)
                    contentPanel.add(filePanel)
                }
            }
            
            // Add spacing between projects
            contentPanel.add(Box.createVerticalStrut(10))
        }
        
        contentPanel.revalidate()
        contentPanel.repaint()
    }
    
    private fun createProjectHeader(project: Project, group: ProjectFileGroup): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        headerPanel.border = JBUI.Borders.empty(8, 2, 8, 5) // Reduced left and right padding
        
        // Use project color as background if configured
        val settings = TabsPerProjectSettings.getInstance()
        val customBackgroundColor = settings.getProjectHeaderBackgroundColor()
        headerPanel.background = if (customBackgroundColor != null) {
            // Use custom background color
            customBackgroundColor
        } else if (group.color != null) {
            // Use the project color with moderate opacity for header background
            Color(group.color!!.red, group.color!!.green, group.color!!.blue, 120)
        } else {
            UIUtil.getTreeSelectionBackground(false)
        }
        
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 0))
        titlePanel.isOpaque = false
        
        // Project color indicator - smaller since background shows the color
        if (group.color != null) {
            val colorLabel = JBLabel()
            colorLabel.preferredSize = Dimension(8, 8)
            colorLabel.isOpaque = true
            colorLabel.background = group.color
            colorLabel.border = BorderFactory.createLineBorder(JBColor.BLACK, 1)
            titlePanel.add(colorLabel)
        }
        
        // Project name - apply customizable styling
        val projectText = settings.projectHeaderTextCase.applyTo(project.name)
        val projectLabel = JBLabel(projectText)
        projectLabel.font = projectLabel.font.deriveFont(
            settings.projectHeaderFontStyle.toAwtStyle(),
            settings.projectHeaderFontSize.toFloat()
        )
        // Use white text on colored backgrounds for better contrast
        val customTextColor = settings.getProjectHeaderTextColor()
        projectLabel.foreground = if (customTextColor != null) {
            // Use custom text color
            customTextColor
        } else if ((customBackgroundColor != null) || (group.color != null)) {
            JBColor.WHITE
        } else {
            JBColor.namedColor("Label.foreground", UIUtil.getLabelForeground())
        }
        projectLabel.toolTipText = TabsPerProjectBundle.message("tooltip.configureColor")
        titlePanel.add(projectLabel)
        
        // File count
        val countLabel = JBLabel("(${group.files.size})")
        countLabel.foreground = if (customTextColor != null) {
            customTextColor  // Use same custom text color as project label
        } else if ((customBackgroundColor != null) || (group.color != null)) {
            JBColor.WHITE  // White text on colored background
        } else {
            JBColor.GRAY
        }
        countLabel.font = countLabel.font.deriveFont(Font.BOLD)
        titlePanel.add(countLabel)
        
        headerPanel.add(titlePanel, BorderLayout.WEST)
        
        // Close all button
        val closeAllButton = JButton(AllIcons.Actions.Close)
        closeAllButton.toolTipText = TabsPerProjectBundle.message("action.closeAll")
        closeAllButton.preferredSize = Dimension(20, 20)
        closeAllButton.isContentAreaFilled = false
        closeAllButton.isBorderPainted = false
        closeAllButton.isFocusPainted = false
        closeAllButton.addActionListener {
            closeAllFilesInProject(project)
        }
        headerPanel.add(closeAllButton, BorderLayout.EAST)
        
        return headerPanel
    }
    
    private fun createModuleHeader(moduleName: String, fileCount: Int): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        headerPanel.border = JBUI.Borders.empty(5, 25, 5, 5)  // Reduced indentation and right padding
        
        // Use module color as background if configured
        val settings = TabsPerProjectSettings.getInstance()
        val moduleColor = settings.getModuleColor(project, moduleName)
        val customBackgroundColor = settings.getModuleHeaderBackgroundColor()
        headerPanel.background = if (customBackgroundColor != null) {
            // Use custom background color
            customBackgroundColor
        } else if (moduleColor != null) {
            // Use the module color with moderate opacity for header background
            Color(moduleColor.red, moduleColor.green, moduleColor.blue, 100)
        } else {
            UIUtil.getListBackground()
        }
        
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 6, 0))
        titlePanel.isOpaque = false
        
        // Module color indicator if configured - smaller since background shows color
        if (moduleColor != null) {
            val colorLabel = JBLabel()
            colorLabel.preferredSize = Dimension(6, 6)
            colorLabel.isOpaque = true
            colorLabel.background = moduleColor
            colorLabel.border = BorderFactory.createLineBorder(JBColor.GRAY)
            titlePanel.add(colorLabel)
        }
        
        // Module icon
        val moduleIcon = AllIcons.Nodes.Module
        val iconLabel = JBLabel(moduleIcon)
        titlePanel.add(iconLabel)
        
        // Module name - apply customizable styling
        val cleanName = cleanModuleName(moduleName, project.name)
        val displayName = settings.moduleHeaderTextCase.applyTo(cleanName)
        val moduleLabel = JBLabel(displayName)
        moduleLabel.font = moduleLabel.font.deriveFont(
            settings.moduleHeaderFontStyle.toAwtStyle(),
            settings.moduleHeaderFontSize.toFloat()
        )
        // Use white text on colored backgrounds for better contrast
        val customTextColor = settings.getModuleHeaderTextColor()
        moduleLabel.foreground = if (customTextColor != null) {
            // Use custom text color
            customTextColor
        } else if ((customBackgroundColor != null) || (moduleColor != null)) {
            JBColor.WHITE
        } else {
            JBColor.namedColor("Component.infoForeground", JBColor.BLUE)
        }
        moduleLabel.toolTipText = "Double-click to configure module color"
        titlePanel.add(moduleLabel)
        
        // File count
        val countLabel = JBLabel("($fileCount)")
        countLabel.foreground = if (customTextColor != null) {
            customTextColor  // Use same custom text color as module label
        } else if ((customBackgroundColor != null) || (moduleColor != null)) {
            JBColor.WHITE  // White text on colored background
        } else {
            JBColor.GRAY
        }
        countLabel.font = countLabel.font.deriveFont(Font.ITALIC)
        titlePanel.add(countLabel)
        
        headerPanel.add(titlePanel, BorderLayout.WEST)
        
        return headerPanel
    }
    
    private fun createFilePanel(fileInfo: OpenFileInfo, indentLevel: Int = 1): JPanel {
        val filePanel = JBPanel<JBPanel<*>>(BorderLayout())
        val leftIndent = when (indentLevel) {
            2 -> 45  // Files under modules - reduced from 60
            1 -> 15  // Files under projects - reduced from 25  
            else -> 15
        }
        filePanel.border = JBUI.Borders.empty(3, leftIndent, 3, 5) // Reduced right padding too
        
        val fileEditorManager = FileEditorManager.getInstance(fileInfo.project)
        val isActive = fileEditorManager.selectedFiles.contains(fileInfo.file)
        val isModified = fileEditorManager.isFileOpen(fileInfo.file) && 
                        fileEditorManager.getEditors(fileInfo.file).any { editor ->
                            editor.isModified
                        }
        
        // Check for file errors - with safety checks
        val hasErrors = try {
            !fileInfo.project.isDisposed && 
            WolfTheProblemSolver.getInstance(fileInfo.project).isProblemFile(fileInfo.file)
        } catch (e: Exception) {
            false // Fallback to no errors if detection fails
        }
        
        // Set background color - simple list background for files
        filePanel.background = when {
            isActive -> UIUtil.getListSelectionBackground(true)
            else -> UIUtil.getListBackground()
        }
        
        // Check if we need to show parent folder for disambiguation
        val needsParentFolder = fileGroups.values.any { group ->
            group.files.count { it.file.name == fileInfo.file.name } > 1
        }
        
        // File icon based on file type
        val fileIcon = fileInfo.file.fileType.icon ?: AllIcons.FileTypes.Text
        
        // File name label with state indicators
        val fileName = buildString {
            // Add status indicators
            when {
                hasErrors -> append("⚠ ")  // Warning triangle for errors
                isModified -> append("• ")  // Bullet point for unsaved changes
                else -> append("  ")  // Consistent spacing
            }
            
            if (needsParentFolder && fileInfo.file.parent != null) {
                append(fileInfo.file.parent.name)
                append("/")
            }
            append(fileInfo.file.name)
        }
        
        val filePanel_inner = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 4, 0))
        filePanel_inner.isOpaque = false
        
        // File type icon
        val iconLabel = JBLabel(fileIcon)
        filePanel_inner.add(iconLabel)
        
        // Set up text and tooltip
        val fullText = fileName
        val fullTooltipText = buildString {
            append(fileInfo.file.path)
            if (isActive) append(" [Active]")
            if (isModified) append(" [Unsaved Changes]")
            if (hasErrors) append(" [Has Errors]")
        }
        
        val fileLabel = TruncatingLabel(fullText, fullTooltipText)
        fileLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        // Apply styling based on state - more subtle than headers
        when {
            hasErrors -> {
                fileLabel.font = fileLabel.font.deriveFont(Font.BOLD)
                fileLabel.foreground = JBColor(Color(180, 0, 0), Color(255, 100, 100)) // Red for errors
            }
            isActive -> {
                fileLabel.font = fileLabel.font.deriveFont(Font.BOLD)
                fileLabel.foreground = UIUtil.getTreeSelectionForeground()
            }
            isModified -> {
                fileLabel.font = fileLabel.font.deriveFont(Font.BOLD)
                fileLabel.foreground = JBColor(Color(0, 120, 0), Color(120, 200, 120)) // Green for modified
            }
            else -> {
                fileLabel.font = fileLabel.font.deriveFont(Font.PLAIN)
            }
        }
        
        filePanel_inner.add(fileLabel)

        val mouseHandler = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    FileEditorManager.getInstance(fileInfo.project).openFile(fileInfo.file, true)
                }

                if (e.button == MouseEvent.BUTTON3) {
                    FileEditorManager.getInstance(fileInfo.project).closeFile(fileInfo.file)
                }
            }
            
            override fun mouseEntered(e: MouseEvent) {
                if (!isActive) {
                    filePanel.background = UIUtil.getListSelectionBackground(false)
                }
            }
            
            override fun mouseExited(e: MouseEvent) {
                if (!isActive) {
                    filePanel.background = UIUtil.getListBackground()
                }
            }
        }
        
        fileLabel.addMouseListener(mouseHandler)
        filePanel.addMouseListener(mouseHandler)  // Also add to panel for better UX
        
        filePanel.add(filePanel_inner, BorderLayout.CENTER)
        
        // Close button - transparent and subtle
        val closeButton = JButton("×")
        closeButton.toolTipText = TabsPerProjectBundle.message("tooltip.closeFile")
        closeButton.preferredSize = Dimension(16, 16)
        closeButton.font = closeButton.font.deriveFont(10f)
        closeButton.isOpaque = false
        closeButton.isBorderPainted = false
        closeButton.isContentAreaFilled = false
        closeButton.isFocusPainted = false
        closeButton.foreground = JBColor.GRAY
        closeButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        // Add hover effect for close button
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                closeButton.foreground = JBColor.RED
            }
            override fun mouseExited(e: MouseEvent) {
                closeButton.foreground = JBColor.GRAY
            }
        })
        
        closeButton.addActionListener {
            FileEditorManager.getInstance(fileInfo.project).closeFile(fileInfo.file)
        }
        
        filePanel.add(closeButton, BorderLayout.EAST)
        
        return filePanel
    }
    
    private fun closeAllFilesInProject(project: Project) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileGroups[project]?.files?.forEach { fileInfo ->
            fileEditorManager.closeFile(fileInfo.file)
        }
    }
    
    private fun detectDotNetProject(file: VirtualFile, project: Project): String? {
        val projectPath = project.basePath ?: return null
        val filePath = file.path
        
        // Find the relative path from project root
        if (!filePath.startsWith(projectPath)) {
            return null
        }
        
        val relativePath = filePath.substring(projectPath.length).replace('\\', '/')
        val pathParts = relativePath.trim('/').split('/')
        
        // Look for common .NET project patterns
        // Usually: /src/ProjectName/... or /ProjectName/...
        for (i in pathParts.indices) {
            val part = pathParts[i]
            
            // Check if this directory contains a project file
            val potentialProjectDir = project.baseDir
            var currentDir = potentialProjectDir
            
            for (j in 0..i) {
                currentDir = currentDir?.findChild(pathParts[j])
                if (currentDir == null) break
            }
            
            if (currentDir != null) {
                // Look for project files in this directory
                val projectFiles = currentDir.children.filter { child ->
                    !child.isDirectory && (
                        child.name.endsWith(".csproj") ||
                        child.name.endsWith(".vbproj") ||
                        child.name.endsWith(".fsproj")
                    )
                }
                
                if (projectFiles.isNotEmpty()) {
                    // Use the project file name without extension as the module name
                    return projectFiles.first().nameWithoutExtension
                }
            }
        }
        
        // Fallback: try to guess from path structure
        // Common patterns: src/lib/ProjectName, src/web/ProjectName
        if (pathParts.size >= 3) {
            when {
                pathParts[0] == "src" && pathParts.size > 2 -> return pathParts[2]
                pathParts[0] == "test" && pathParts.size > 1 -> return pathParts[1]
                pathParts.size > 1 -> return pathParts[0]
            }
        }
        
        return null
    }
    
    private fun cleanModuleName(moduleName: String, projectName: String): String {
        var cleanName = moduleName
            .removeSuffix(".csproj")
            .removeSuffix(".vbproj")
            .removeSuffix(".fsproj")
        
        // Remove project name prefix if present (e.g., "WebShop.Data" -> "Data")
        val projectPrefix = projectName.replace(" ", "")
        if (cleanName.startsWith("$projectPrefix.", ignoreCase = true)) {
            cleanName = cleanName.substring(projectPrefix.length + 1)
        }
        
        return cleanName
    }
    
    /**
     * Truncates text to fit within the available width, adding ellipsis if needed
     */
    private fun truncateText(text: String, font: Font, availableWidth: Int): String {
        if (availableWidth <= 0) return text
        
        val metrics = contentPanel.getFontMetrics(font)
        val textWidth = metrics.stringWidth(text)
        
        if (textWidth <= availableWidth) {
            return text
        }
        
        val ellipsis = "..."
        val ellipsisWidth = metrics.stringWidth(ellipsis)
        val availableForText = availableWidth - ellipsisWidth
        
        if (availableForText <= 0) return ellipsis
        
        var truncated = text
        while (truncated.isNotEmpty() && metrics.stringWidth(truncated) > availableForText) {
            truncated = truncated.substring(0, truncated.length - 1)
        }
        
        return if (truncated.isEmpty()) ellipsis else truncated + ellipsis
    }
}

// Custom label that truncates text with ellipsis and shows full text on hover
private class TruncatingLabel(private val fullText: String, fullTooltip: String) : JBLabel() {
    
    init {
        text = fullText
        toolTipText = fullTooltip
    }
    
    override fun paintComponent(g: Graphics) {
        val metrics = g.fontMetrics
        val availableWidth = width - insets.left - insets.right
        
        if (availableWidth > 0) {
            val textWidth = metrics.stringWidth(fullText)
            
            if (textWidth > availableWidth) {
                // Text needs truncation
                val ellipsis = "..."
                val ellipsisWidth = metrics.stringWidth(ellipsis)
                val availableForText = availableWidth - ellipsisWidth
                
                if (availableForText > 0) {
                    var truncated = fullText
                    while (truncated.isNotEmpty() && metrics.stringWidth(truncated) > availableForText) {
                        truncated = truncated.substring(0, truncated.length - 1)
                    }
                    text = if (truncated.isEmpty()) ellipsis else truncated + ellipsis
                } else {
                    text = ellipsis
                }
            } else {
                // Text fits, show full text
                text = fullText
            }
        }
        
        super.paintComponent(g)
    }
}

// Custom layout for vertical stacking
private class VerticalFlowLayout(private val hgap: Int, private val vgap: Int) : LayoutManager {
    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}
    
    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            var height = insets.top + insets.bottom
            var width = 0
            
            for (i in 0 until parent.componentCount) {
                val comp = parent.getComponent(i)
                if (comp.isVisible) {
                    val d = comp.preferredSize
                    height += d.height + vgap
                    width = maxOf(width, d.width)
                }
            }
            
            return Dimension(width + insets.left + insets.right + hgap * 2, height)
        }
    }
    
    override fun minimumLayoutSize(parent: Container): Dimension = preferredLayoutSize(parent)
    
    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val maxWidth = parent.width - insets.left - insets.right - hgap * 2
            var y = insets.top
            
            for (i in 0 until parent.componentCount) {
                val comp = parent.getComponent(i)
                if (comp.isVisible) {
                    val d = comp.preferredSize
                    comp.setBounds(insets.left + hgap, y, maxWidth, d.height)
                    y += d.height + vgap
                }
            }
        }
    }
}
