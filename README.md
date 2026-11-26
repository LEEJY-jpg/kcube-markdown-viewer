# KCube Markdown Viewer

[English](README.md) | [한국어](README_ko.md)

An Eclipse plug-in that lets you edit Markdown (`.md`, `.markdown`) files and preview them right away, using **Source / Preview** tabs.

## Features

- **Source / Preview / Split tabs**: Edit in the standard Eclipse text editor on the Source tab, see the rendered result on the Preview tab, or both side by side on the Split tab. The Preview tab is shown first when a file is opened for the first time.
- **Syntax colors in the Source tab**: Headings, lists, quotes, code, links, and emphasis are colored, with a separate set for dark themes.
- **Scroll sync**: Switching tabs keeps your place, and the Split tab and the Markdown Preview view follow the Source as you scroll.
- **Live Markdown Preview view**: Window > Show View > Other… > Markdown > *Markdown Preview* shows your edits in a side view as you type.
- **Outline**: The Outline view lists the headings; click one to jump there in the Source or Preview.
- **Preview search**: Press Ctrl/Cmd+F to open the search bar. The search is case-insensitive and supports Korean; use Enter / Shift+Enter to move between matches.
- **Same font and theme as Source**: The Preview uses the Eclipse text font and switches to a dark theme when the editor background is dark.
- **Links**: Web links open in the system browser; links to other files open them in Eclipse (`#L10` jumps to a line).
- **GitHub extensions**: Tables, strikethrough, task lists, alerts (`> [!NOTE]`), and footnotes.
- **Code highlighting**: Syntax highlighting for code blocks, a copy button, and collapse/expand. Clicking inline code copies it. ```` ```mermaid ```` blocks are drawn as diagrams.
- **Math**: `$...$`, `$$...$$` and ```` ```math ```` blocks are rendered with KaTeX.
- **Images**: Images with paths relative to the md file are displayed.
- **Fully offline**: markdown-it, highlight.js, Mermaid, KaTeX, and CSS are bundled in the plug-in. No CDN is used.

## Requirements

- Eclipse 4.33 (2024-09) or later
- Java 17 or later (build and runtime)
- Windows: [Microsoft Edge WebView2 Runtime](https://developer.microsoft.com/microsoft-edge/webview2/) for the Preview

## Installation

Copy a single jar into the Eclipse `dropins` folder and restart with `-clean`. See the *End-user installation guide* in [INSTALL.md](docs/INSTALL.md) for the full procedure and troubleshooting.

## Build

JDK 17 and Maven are required. The build uses Maven Tycho.

```bash
./build.sh             # builds the plug-in jar and the update-site zip into dist/
./build.sh --install   # builds, then installs into the Eclipse dropins folder
```

See [DEVELOPMENT.md](docs/DEVELOPMENT.md) for the project structure and design.

```
com.kcube.md              Plug-in (editor, view, web/ viewer)
com.kcube.md.feature      Eclipse feature
com.kcube.md.update-site  p2 update site
```

## Third-party components

| Component | License |
|---|---|
| [markdown-it](https://github.com/markdown-it/markdown-it) 13.0.1 | MIT |
| [highlight.js](https://highlightjs.org/) 11.8.0 | BSD-3-Clause |
| [Bootstrap](https://getbootstrap.com/) CSS | MIT |
| [Mermaid](https://mermaid.js.org/) 11.4.1 | MIT |
| [KaTeX](https://katex.org/) 0.16.11 | MIT |

See [THIRD_PARTY_NOTICES.txt](com.kcube.md/web/THIRD_PARTY_NOTICES.txt) for details.

## License

[Apache License 2.0](LICENSE)
