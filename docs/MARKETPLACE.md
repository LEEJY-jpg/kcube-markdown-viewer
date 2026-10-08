# Eclipse Marketplace Listing

Content to enter at <https://marketplace.eclipse.org> (Add Content > Solution).

## Fields

| Field | Value |
|---|---|
| Title | KCube Markdown Viewer |
| Categories | Editors, Documentation (or Tools if unavailable) |
| Tags | markdown, md, preview, editor, viewer |
| Update URL | `https://leejy-jpg.github.io/kcube-markdown-viewer/` |
| Installable Unit | `com.kcube.md.feature.feature.group` |
| License | Apache-2.0 |
| Status | Production/Stable |
| Source / Repository URL | <https://github.com/LEEJY-jpg/kcube-markdown-viewer> |
| Bug tracker | <https://github.com/LEEJY-jpg/kcube-markdown-viewer/issues> |
| Support / Website | <https://github.com/LEEJY-jpg/kcube-markdown-viewer> |
| Eclipse versions | 2024-09 (4.33) or later |
| Java | 17+ |
| Operating systems | macOS, Windows, Linux |
| Dependency | None beyond the Eclipse platform (the Eclipse text editor and UI bundles are included in every Eclipse package) |

## Short description

Edit Markdown files in Eclipse with Source / Preview tabs, a live preview view, and in-page search. Works fully offline.

## Full description

KCube Markdown Viewer lets you edit Markdown (`.md`, `.markdown`) files in Eclipse and see the rendered result right away. A **Source** tab gives you the standard Eclipse text editor, and a **Preview** tab shows the rendered page.

**Features**
- Source / Preview tabs; the Preview tab is shown first when a file is opened for the first time
- Live **Markdown Preview** view that updates as you type
- Preview search (Ctrl/Cmd+F): case-insensitive, works with Korean, Enter / Shift+Enter to move between matches
- The Preview uses the same font as the Source editor (Eclipse text font setting)
- Code block syntax highlighting, a copy button, and collapse/expand; click inline code to copy it
- Images with paths relative to the md file are displayed
- Fully offline: markdown-it, highlight.js, and CSS are bundled, so no CDN or network access is needed

**Requirements:** Eclipse 2024-09 (4.33) or later and Java 17+.

**Usage:** Open any `.md` file; the plug-in is registered as the default editor for `*.md` and `*.markdown`. If another editor opens instead, right-click the file > Open With > KCube Markdown Viewer, or set it as the default in Preferences > General > Editors > File Associations. The live preview view is under Window > Show View > Other... > Markdown > Markdown Preview.

Open source under the Apache License 2.0.

## Screenshots

- Use screenshots that show only this repository's own files (for example, this project's README); do not include internal or private project names or file paths.
- Prefer a wide window showing the whole Eclipse workbench: the Source and Preview tabs, and the Preview with the search bar open and highlighted matches.
- A second image of the Markdown Preview view next to the editor is useful.

## Checklist before submitting

- [x] Release workflow green and the update site is reachable
- [x] Install from the update site verified in Eclipse
- [ ] Install verified in a clean Eclipse (no other KCube plug-ins, no local `dropins` jar)
- [ ] GitHub Issues enabled
- [ ] Screenshots prepared (no private project names)
- [ ] Logo/icon prepared (optional)
