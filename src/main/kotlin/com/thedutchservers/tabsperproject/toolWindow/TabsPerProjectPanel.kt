package com.thedutchservers.tabsperproject.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorChooser
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.thedutchservers.tabsperproject.TabsPerProjectBundle
import com.thedutchservers.tabsperproject.actions.RefreshTabsAction
import com.thedutchservers.tabsperproject.model.OpenFileInfo
import com.thedutchservers.tabsperproject.model.ProjectFileGroup
import com.thedutchservers.tabsperproject.model.SortOrder
import com.thedutchservers.tabsperproject.settings.TabsPerProjectSettings
import java.awt.*
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
        setupToolbar()
    }
    
    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(RefreshTabsAction())
        
        val toolbar = ActionManager.getInstance().createActionToolbar(
            "Tabs per project",
            actionGroup,
            true
        )
        toolbar.targetComponent = this
        setToolbar(toolbar.component)
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
        headerPanel.border = JBUI.Borders.empty(5, 10, 5, 10)
        headerPanel.background = UIUtil.getPanelBackground()
        
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 5, 0))
        titlePanel.isOpaque = false
        
        // Project color indicator
        if (TabsPerProjectSettings.getInstance().showProjectColors && group.color != null) {
            val colorLabel = JBLabel()
            colorLabel.preferredSize = Dimension(10, 10)
            colorLabel.isOpaque = true
            colorLabel.background = group.color
            colorLabel.border = BorderFactory.createLineBorder(JBColor.GRAY)
            titlePanel.add(colorLabel)
        }
        
        // Project name
        val projectLabel = JBLabel(project.name)
        projectLabel.font = projectLabel.font.deriveFont(Font.BOLD)
        projectLabel.toolTipText = TabsPerProjectBundle.message("tooltip.configureColor")
        titlePanel.add(projectLabel)
        
        // File count
        val countLabel = JBLabel("(${group.files.size})")
        countLabel.foreground = JBColor.GRAY
        titlePanel.add(countLabel)
        
        // Add click listener for color configuration
        projectLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1 && e.clickCount == 2) {
                    val newColor = ColorChooser.chooseColor(
                        headerPanel,
                        "Choose Project Color",
                        group.color ?: JBColor.BLUE
                    )
                    if (newColor != null) {
                        TabsPerProjectSettings.getInstance().setProjectColor(project, newColor)
                        refreshFileList()
                    }
                }
            }
        })
        
        headerPanel.add(titlePanel, BorderLayout.WEST)
        
        // Close all button
        val closeAllButton = JButton("×")
        closeAllButton.toolTipText = TabsPerProjectBundle.message("action.closeAll")
        closeAllButton.preferredSize = Dimension(20, 20)
        closeAllButton.addActionListener {
            closeAllFilesInProject(project)
        }
        headerPanel.add(closeAllButton, BorderLayout.EAST)
        
        return headerPanel
    }
    
    private fun createModuleHeader(moduleName: String, fileCount: Int): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        headerPanel.border = JBUI.Borders.empty(3, 30, 3, 10)  // Indented more than project
        headerPanel.background = UIUtil.getPanelBackground()
        
        val titlePanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 5, 0))
        titlePanel.isOpaque = false
        
        // Module icon
        val moduleIcon = AllIcons.Nodes.Module
        val iconLabel = JBLabel(moduleIcon)
        titlePanel.add(iconLabel)
        
        // Module name - clean up common patterns for better display
        val displayName = cleanModuleName(moduleName, project.name)
        val moduleLabel = JBLabel(displayName)
        moduleLabel.font = moduleLabel.font.deriveFont(Font.ITALIC)
        titlePanel.add(moduleLabel)
        
        // File count
        val countLabel = JBLabel("($fileCount)")
        countLabel.foreground = JBColor.GRAY
        titlePanel.add(countLabel)
        
        headerPanel.add(titlePanel, BorderLayout.WEST)
        
        return headerPanel
    }
    
    private fun createFilePanel(fileInfo: OpenFileInfo, indentLevel: Int = 1): JPanel {
        val filePanel = JBPanel<JBPanel<*>>(BorderLayout())
        val leftIndent = if (indentLevel == 2) 40 else 20
        filePanel.border = JBUI.Borders.empty(2, leftIndent, 2, 10)
        
        val fileEditorManager = FileEditorManager.getInstance(fileInfo.project)
        val isActive = fileEditorManager.selectedFiles.contains(fileInfo.file)
        val isModified = fileEditorManager.isFileOpen(fileInfo.file) && 
                        fileEditorManager.getEditors(fileInfo.file).any { editor ->
                            editor.isModified
                        }
        
        // Set background based on file state
        filePanel.background = when {
            isActive -> UIUtil.getListSelectionBackground(true)
            else -> UIUtil.getListBackground()
        }
        
        // Check if we need to show parent folder for disambiguation
        val needsParentFolder = fileGroups.values.any { group ->
            group.files.count { it.file.name == fileInfo.file.name } > 1
        }
        
        // File name label with state indicators
        val fileName = buildString {
            if (isModified) append("* ")
            if (needsParentFolder && fileInfo.file.parent != null) {
                append(fileInfo.file.parent.name)
                append("/")
            }
            append(fileInfo.file.name)
        }
        val fileLabel = JBLabel(fileName)
        fileLabel.toolTipText = buildString {
            append(fileInfo.file.path)
            if (isActive) append(" [Active]")
            if (isModified) append(" [Modified]")
        }
        fileLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        // Apply styling based on state
        if (isActive) {
            fileLabel.font = fileLabel.font.deriveFont(Font.BOLD)
        }
        if (isModified) {
            fileLabel.foreground = JBColor(Color(0, 100, 0), Color(100, 200, 100))
        }
        
        // Make clickable to open file
        fileLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    FileEditorManager.getInstance(fileInfo.project).openFile(fileInfo.file, true)
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
        })
        
        filePanel.add(fileLabel, BorderLayout.CENTER)
        
        // Close button
        val closeButton = JButton("×")
        closeButton.toolTipText = TabsPerProjectBundle.message("tooltip.closeFile")
        closeButton.preferredSize = Dimension(16, 16)
        closeButton.font = closeButton.font.deriveFont(10f)
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
