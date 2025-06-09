# Tabs Per Project Plugin for JetBrains IDEs

A JetBrains plugin that groups open editor tabs by project in a convenient tool window. Perfect for developers working with multiple projects simultaneously in IntelliJ IDEA, WebStorm, **Rider** (with special .NET solution support), and other JetBrains IDEs.

<!-- Plugin description -->
This plugin helps you organize your open files by grouping them according to their project. Perfect for developers working with multiple projects simultaneously in IntelliJ IDEA, WebStorm, Rider, and other JetBrains IDEs.

![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/27536)
![JetBrains Plugin Version](https://img.shields.io/jetbrains/plugin/v/27536)


## Features

- **Current window focus**: Shows only files from the current project window, not all open projects
- **Module/project grouping**: Files are grouped by their module (perfect for solutions with multiple projects like .NET solutions)
- **Rider/.NET support**: Special detection for .NET projects in Rider - properly separates .csproj/.vbproj projects instead of showing "rider.module"
- **Smart file naming**: Files with duplicate names show their parent folder for easy identification
- **Customizable project colors**: Assign colors to projects for easy visual identification
- **Flexible sorting options**: Sort files alphabetically, by last modified time, by project, or by module
- **Quick file management**: Close individual files or all files in a project with one click
- **Configurable tool window position**: Place the tool window on the left, right, or bottom of your IDE
- **Real-time updates**: The file list updates automatically as you open and close files
- **Active file highlighting**: Currently active file is highlighted with bold text and selection background
- **Modified file indicators**: Files with unsaved changes show a "*" prefix and green color
- **Hide IDE editor tabs**: Option to hide the IDE's built-in editor tabs when using this plugin
- **Clean module names**: Automatically cleans up module names (removes file extensions and common prefixes)

## Usage

1. After installation, you'll find the "Tabs Per Project" tool window on the right side of your IDE (configurable)
2. Open files from different projects and see them automatically grouped
3. Double-click a project name to assign a custom color
4. Click on any file name to switch to it
5. Use the × button to close individual files or all files in a project

## Configuration

Access the plugin settings via:
- **Windows/Linux**: File → Settings → Tools → Tabs Per Project
- **macOS**: IntelliJ IDEA → Preferences → Tools → Tabs Per Project

Available settings:
- Tool window position (left, right, bottom)
- Sort order (alphabetical, last modified, project then alphabetical, module then alphabetical)
- Show/hide project colors
- Hide IDE editor tabs (use plugin tabs only)
- Group files by module within projects (enabled by default)

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Tabs Per Project"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/yourusername/tabs-per-project/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development

This plugin is built using:
- Kotlin
- IntelliJ Platform Plugin SDK
- Gradle

To build the plugin locally:
```bash
./gradlew buildPlugin
```

To run the plugin in a sandboxed IDE:
```bash
./gradlew runIde
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
