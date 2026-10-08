# KCube Markdown Viewer

[English](README.md) | [한국어](README_ko.md)

An Eclipse plug-in that lets you edit Markdown (`.md`, `.markdown`) files and preview them right away, using **Source / Preview** tabs.

## Features

- **Source / Preview tabs**: Edit in the standard Eclipse text editor on the Source tab and see the rendered result on the Preview tab. The Preview tab is shown first when a file is opened for the first time.
- **Live Markdown Preview view**: Window > Show View > Other… > Markdown > *Markdown Preview* shows your edits in a side view as you type.
- **Preview search**: Press Ctrl/Cmd+F to open the search bar. The search is case-insensitive and supports Korean; use Enter / Shift+Enter to move between matches.
- **Same font as Source**: The Preview uses the Eclipse text font setting.
- **Code highlighting**: Syntax highlighting for code blocks, a copy button, and collapse/expand. Clicking inline code copies it.
- **Images**: Images with paths relative to the md file are displayed.
- **Fully offline**: markdown-it, highlight.js, and CSS are bundled in the plug-in. No CDN is used.

## Requirements

- Eclipse 4.33 (2024-09) or later
- Java 17 or later (build and runtime)

## Installation

Copy a single jar into the Eclipse `dropins` folder and restart with `-clean`. See the *End-user installation guide* in [INSTALL.md](INSTALL.md) for the full procedure and troubleshooting.

## Build

JDK 17 and Maven are required. The build uses Maven Tycho.

```bash
./build.sh             # builds the plug-in jar and the update-site zip into dist/
./build.sh --install   # builds, then installs into the Eclipse dropins folder
```

See [DEVELOPMENT.md](DEVELOPMENT.md) for the project structure and design.

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

See [THIRD_PARTY_NOTICES.txt](com.kcube.md/web/THIRD_PARTY_NOTICES.txt) for details.

## License

[Apache License 2.0](LICENSE)
