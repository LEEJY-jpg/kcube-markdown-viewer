# KCube Markdown Viewer — Requirements and Development Rules

[English](DEVELOPMENT.md) | [한국어](DEVELOPMENT_ko.md)

> Markdown viewer/editor plug-in for Eclipse
> Written: 2026-10-08 · Status: draft

---

## 1. Project overview

| Item | Description |
|---|---|
| Purpose | Edit Markdown files in Eclipse and preview the rendered result |
| Form | Eclipse plug-in (PDE, Plug-in Project) |
| Target files | `*.md`, `*.markdown` |
| Base asset | Reuses the existing **JavaScript-based Markdown viewer** |

## 2. Naming rules

| Category | Value |
|---|---|
| Project/repository name | `kcube-markdown-viewer` |
| Prefix | `kcube` (required) |
| Bundle ID (Bundle-SymbolicName) | `com.kcube.md` |
| Display name (Bundle-Name) | `KCube Markdown Viewer` |
| Root package | `com.kcube.md` |
| Editor ID | `com.kcube.md.editor` |
| Preview view ID | `com.kcube.md.preview` |
| Marketplace name | `KCube Markdown Viewer for Eclipse` |

- Bundle IDs and package names are **lower case, without hyphens**, in reverse-domain form.
- Do not put `Eclipse` at the beginning of the name (Eclipse Foundation trademark guidelines) → use the `... for Eclipse` form.
- When functionality is split, distinguish by suffix: `com.kcube.md.core`, `com.kcube.md.ui`.

## 3. Functional requirements

### 3.1 Multi-page editor (main feature)
- Works as the default editor when a `.md` file is opened (`default="true"`).
- **Source tab**: embeds the standard Eclipse `TextEditor`.
  - Supports save, save as, undo/redo, find, and the dirty (*) indicator.
- **Preview tab**: loads the JS viewer into an SWT `Browser`.
  - Renders the latest content when switching to the Preview tab.
  - When a file is opened for the first time, the Preview tab is shown first (the tab order stays Source, Preview).
- When switching tabs, global actions such as Undo/Copy/Find are connected to / disconnected from the active tab.

### 3.2 Markdown Preview view (supplementary feature)
- A separate view (`ViewPart`) placed next to the editor for **live preview**.
- Tracks the active editor (`IPartListener2`); only `.md` files are targeted.
- Detects document changes (`IDocumentListener`) and refreshes with a **300 ms debounce**.

### 3.3 Rendering approach
- Java does not convert Markdown itself; it **passes only the text to the JS viewer**.
- Calls `browser.execute("renderMarkdown(...)")`.
- Does not call it before the page has finished loading (`ProgressListener.completed`).
- The JS side replaces only the body and **keeps the scroll position**.

### 3.4 Preview search
- Ctrl/Cmd+F opens a search bar at the top right of the Preview (**case-insensitive**, Korean supported).
- Enter / F3 moves to the next match, Shift+Enter to the previous one (wraps around); Esc or the ✕ button closes the bar and removes the highlights. A `n/m` counter is displayed.
- Implemented in JS (`web/js/search.js`): matches in text nodes are wrapped in `<mark class="kc-hit">`, and the current match gets `kc-hit-active`.
- Not searched: the code block header (language label, copy button) and collapsed (hidden) code.
- While Hangul/IME composition is in progress the search is not run; it runs when composition ends.
- If the preview is re-rendered while the search bar is open (the user edits the source), the search is re-applied and the current position is kept (`reapplySearch()`).
- When the Preview tab is active, the Eclipse **Find** global action is connected to opening this search bar (`MarkdownEditorContributor`, `MarkdownMultiPageEditor.openPreviewSearch()`). Special regular-expression characters in the query are treated as plain text.

### 3.5 Preview font
- The Preview uses the same font as the Source tab, i.e. the Eclipse text font (`JFaceResources.TEXT_FONT`).
- Java passes the font name and size (converted from pt to CSS px using the display DPI) with `applyFont(family, sizePx)` before each rendering.
- Headings are sized relative to the body (h1 1.6em, h2 1.4em, h3 1.25em, h4 1.1em); code blocks and inline code use the body size.
- When the text font is changed in Preferences, the Preview is updated immediately.

### 3.6 Items to consider later
- [ ] Markdown syntax highlighting in the Source tab (headings, bold, code blocks, etc.)
- [ ] Side-by-side Source | Preview tab
- [ ] Scroll synchronization between editor and preview
- [x] Code highlighting (highlight.js) — done
- [x] Search in the Preview — done (3.4)
- [ ] Dark theme (`prefers-color-scheme`)
- [ ] Open files in Eclipse when a preview link is clicked (`BrowserFunction`, JS → Java)

## 4. Architecture

### 4.1 Layout

For update-site distribution, the project is organized as a **Maven Tycho multi-module** build. The module layout follows the same **flat layout directly under the root** as `kcube-maven-goals-plugin`. (See chapter 8 for details.)

```
kcube-markdown-viewer/
├── pom.xml                                 # parent POM (Tycho settings, p2 repository, module list)
├── .gitignore
├── com.kcube.md/                           # plug-in (the actual code)
│   ├── META-INF/MANIFEST.MF
│   ├── plugin.xml
│   ├── build.properties                    # must include web/ and icons/
│   ├── pom.xml                             # eclipse-plugin
│   ├── icons/md.png
│   ├── web/                                # existing JS viewer (works offline)
│   │   ├── viewer.html
│   │   ├── css/                            # bootstrap, highlight theme, MarkDownViewer.css, viewer.css
│   │   └── js/                             # markdown-it, highlight.js, viewer.js, search.js
│   └── src/com/kcube/md/
│       ├── editor/
│       │   ├── MarkdownMultiPageEditor.java    # Source/Preview tab editor
│       │   └── MarkdownEditorContributor.java  # connects global actions
│       ├── views/
│       │   └── MarkdownPreviewView.java        # live preview view
│       ├── preview/
│       │   └── MarkdownBrowser.java            # wrapper that loads the JS viewer and calls rendering
│       └── util/
│           ├── JsUtils.java                    # Java string → JS literal conversion
│           └── EditorInputs.java               # md detection, image base path
├── com.kcube.md.feature/                   # installation unit (Feature)
│   ├── feature.xml
│   ├── build.properties
│   └── pom.xml                             # eclipse-feature
└── com.kcube.md.update-site/               # update site (p2 repository)
    ├── category.xml
    └── pom.xml                             # eclipse-repository
```

### 4.2 Required bundles
- `org.eclipse.ui`
- `org.eclipse.ui.editors`
- `org.eclipse.jface.text`
- `org.eclipse.core.runtime`
- `org.slf4j.api`

### 4.3 plugin.xml extension points
- `org.eclipse.ui.editors` — registers the multi-page editor (`extensions="md,markdown"`)
- `org.eclipse.ui.views` — registers the Markdown category and the Preview view

### 4.4 Java ↔ JS integration rules
- JS entry points (called from Java with `browser.execute`): `renderMarkdown(markdown, baseUri)`, `applyFont(family, sizePx)`, `openSearch()`
- Loading the viewer page: `FrameworkUtil.getBundle(...).getEntry("web/viewer.html")` → `FileLocator.toFileURL()` → `browser.setUrl()`
- Always use `JsUtils.toJsString()` to pass strings
  - Escape `"`, `\`, `\n`, `\r`, ` `, ` `
  - Replace `<` with `<` (prevents `</script>` injection)

## 5. Technical constraints and notes

| Item | Description |
|---|---|
| Java version | Java 17 |
| Browser engine | Edge (WebView2) is recommended on Windows: `-Dorg.eclipse.swt.browser.DefaultType=edge` |
| JS compatibility | Verified against Edge/WebKit, not legacy IE mode |
| External resources | **No CDN** — to support internal/offline networks, all libraries are included locally |
| Image paths | Relative to the md file → the base path must be passed to JS for correction |
| Scrolling | Do not reload with `setText()` (it resets scrolling); replace only the body with JS |

## 6. Coding rules

### 6.1 General
- If the project already has a framework, **follow that framework's way of doing things**.
- Java 17 syntax is allowed (pattern-matching `instanceof`, arrow-form switch, text blocks, etc.).

### 6.2 Comments
- Write **Javadoc for every Java method** (including `@param` and `@return`).
- Also comment **fields/variables** whose meaning is not obvious.
- Write **JSDoc for every JavaScript function**.

```java
/**
 * Passes the document being edited to the JS viewer for rendering.
 */
private void refreshPreview() { ... }
```

```javascript
/**
 * Rendering entry point called from Java (Eclipse).
 * @param {string} markdown Original Markdown text
 */
function renderMarkdown(markdown) { ... }
```

### 6.3 Logging
- Logger variable name: `_log` (SLF4J)
- **Always wrap log calls in a level check**
- Write log messages in **English**
- Use `{}` placeholders for parameters

```java
/** Logger */
private static final Logger _log = LoggerFactory.getLogger(MarkdownMultiPageEditor.class);

if (_log.isDebugEnabled()) {
    _log.debug("Tracking markdown editor: {}", name);
}

if (_log.isErrorEnabled()) {
    _log.error("Failed to load viewer page", e);
}
```

## 7. Development environment

- Eclipse IDE for RCP and RCP Developers
- New project: File > New > Plug-in Project
- Run/debug: Run As > Eclipse Application (runtime workbench)
- Release build: Maven 3.9+ / JDK 17, `mvn clean verify` (see chapter 8)

## 8. Distribution — update-site installation structure

### 8.1 Goals
- Users install by adding the update-site URL in **Help > Install New Software...**.
- New versions are picked up through **Help > Check for Updates**.
- In internal (offline) networks, a zipped update site can be installed with **Archive...**.

### 8.2 Units

| Unit | ID | Role |
|---|---|---|
| Plugin (Bundle) | `com.kcube.md` | Actual code, plugin.xml, web resources |
| Feature | `com.kcube.md.feature` | Installation/update unit. Groups plug-ins and carries license/description |
| Update Site | `com.kcube.md.update-site` | Generates the p2 repository (`content.jar`, `artifacts.jar`, `plugins/`, `features/`) |

- The installation unit visible to users is the **Feature**; plug-ins are not exposed directly.
- If functionality grows and bundles are split (`com.kcube.md.core`, `com.kcube.md.ui`), just add them to the Feature.

### 8.3 Build tools
- Uses **Maven + Eclipse Tycho 4.x** (CI/command-line builds, reproducible p2 repository generation).
- Build command: `mvn clean verify`
- Outputs:
  - `com.kcube.md.update-site/target/repository/` → update-site folder (for uploading to a web server)
  - `com.kcube.md.update-site/target/com.kcube.md.update-site-<version>.zip` → for offline installation

### 8.4 Main configuration files

**pom.xml (parent)** — key parts (same approach as `kcube-maven-goals-plugin`)
```xml
<groupId>com.kcube</groupId>
<artifactId>kcube-markdown-viewer-parent</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>

<properties>
  <tycho.version>4.0.13</tycho.version>
  <eclipse.release>2024-12</eclipse.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<modules>
  <module>com.kcube.md</module>
  <module>com.kcube.md.feature</module>
  <module>com.kcube.md.update-site</module>
</modules>

<repositories>
  <repository>
    <id>eclipse-release</id>
    <layout>p2</layout>
    <url>https://download.eclipse.org/releases/${eclipse.release}</url>
  </repository>
</repositories>

<build>
  <plugins>
    <plugin>  <!-- tycho-maven-plugin, extensions=true -->
    <plugin>  <!-- target-platform-configuration: win32 / macosx (aarch64, x86_64) / linux environments -->
  </plugins>
</build>
```
- The p2 repository is **declared directly in the parent POM**, without a separate target-platform module or `.mvn/extensions.xml`.
- The compiler level is set by `Bundle-RequiredExecutionEnvironment: JavaSE-17` (MANIFEST) and `.settings/org.eclipse.jdt.core.prefs`.
- `IURIEditorInput` is marked as non-API in `org.eclipse.ui.ide`, so the plug-in's `.settings/org.eclipse.jdt.core.prefs` sets `forbiddenReference=warning`.

**MANIFEST.MF (com.kcube.md)**
```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-SymbolicName: com.kcube.md;singleton:=true
Bundle-Name: KCube Markdown Viewer
Bundle-Vendor: KCube
Bundle-Version: 1.0.0.qualifier
Bundle-RequiredExecutionEnvironment: JavaSE-17
Automatic-Module-Name: com.kcube.md
Require-Bundle: org.eclipse.ui.ide,
 org.eclipse.ui,
 org.eclipse.ui.editors,
 org.eclipse.ui.workbench.texteditor,
 org.eclipse.jface.text,
 org.eclipse.core.runtime
Import-Package: org.slf4j;version="[1.7.0,3.0.0)"
Bundle-ActivationPolicy: lazy
```
- `singleton:=true` is required because `plugin.xml` uses extension points.
- SLF4J is referenced through **`Import-Package`**, not `Require-Bundle` (1.x/2.x compatible).

**build.properties (com.kcube.md)**
```
source.. = src/
output.. = bin/
bin.includes = META-INF/,\
               .,\
               plugin.xml,\
               icons/,\
               web/
```
- If `web/` or `icons/` is missing, the Preview is blank in the installed plug-in, so they must be included.

**feature.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<feature id="com.kcube.md.feature"
         label="KCube Markdown Viewer"
         version="1.0.0.qualifier"
         provider-name="KCube">
   <description>Markdown editor and preview for Eclipse.</description>
   <copyright>Copyright (c) KCube.</copyright>
   <license url="https://www.apache.org/licenses/LICENSE-2.0">License text</license>

   <plugin id="com.kcube.md" version="0.0.0" unpack="false"/>
</feature>
```
- `unpack="false"`: installs the jar as-is. `FileLocator.toFileURL()` extracts the web resources into a cache folder at runtime, so it works in jar form.
- Default Eclipse bundles (UI, editors, SLF4J) are **not included** in the Feature; they are declared only as dependencies.

**category.xml (com.kcube.md.update-site)**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<site>
   <feature id="com.kcube.md.feature" version="0.0.0">
      <category name="kcube"/>
   </feature>
   <category-def name="kcube" label="KCube Tools">
      <description>KCube Eclipse plug-ins</description>
   </category-def>
</site>
```
- Without a category, the item does not appear in the install dialog when "Group items by category" is checked, so one must be defined.
- Other KCube plug-ins (for example a link view) can be grouped in the same `KCube Tools` category and distributed from **a single update site**.

**com.kcube.md.update-site/pom.xml**
```xml
<artifactId>com.kcube.md.update-site</artifactId>
<packaging>eclipse-repository</packaging>
```
- Packaging per module: bundle → `eclipse-plugin`, feature → `eclipse-feature`, site → `eclipse-repository`.

### 8.5 Versioning rules

| Location | Format | Example |
|---|---|---|
| MANIFEST.MF / feature.xml | `major.minor.micro.qualifier` | `1.0.0.qualifier` |
| pom.xml | `major.minor.micro-SNAPSHOT` | `1.0.0-SNAPSHOT` |
| Build result | qualifier replaced by build time | `1.0.0.202610081530` |

- The versions (major.minor.micro) in all three places must **always match** (a mismatch fails the Tycho build).
- Bulk change: `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.1.0-SNAPSHOT`
- The qualifier increases with every build, so even the same version is **recognized as an update by Check for Updates**.
- Bump rules: bug fix → micro, new feature → minor, breaking change → major.

### 8.6 Distribution and hosting
- Upload the contents of `target/repository/` as-is to a **static web server** (internal web server, Nginx, Apache, GitHub Pages, etc.).
- Example update-site URL: `https://<server>/eclipse/kcube/`
- To keep per-version history, a version folder plus a **Composite Repository** is recommended.
  ```
  /eclipse/kcube/
  ├── compositeContent.xml
  ├── compositeArtifacts.xml
  ├── 1.0.0/
  └── 1.1.0/
  ```
- HTTPS is recommended (recent Eclipse versions warn about HTTP).

### 8.7 Signing (optional)
- Unsigned content triggers an **"Unsigned content" warning** during installation, but installation still works.
- Unsigned is acceptable for internal distribution; for external (marketplace) distribution, signing the jars with a code-signing certificate is recommended.
  - Tycho: use `tycho-gpg-plugin` (PGP signing) or `maven-jarsigner-plugin`.

### 8.8 Installation verification checklist
- [ ] Installs successfully from the update-site URL into a clean Eclipse (2024-12 or later)
- [ ] Offline installation from the zip file (Archive) succeeds
- [ ] After installation, `.md` files open with KCube Markdown Viewer
- [ ] Web resources load correctly on the Preview tab (not blank)
- [ ] Window > Show View > Markdown > Markdown Preview is displayed
- [ ] After bumping the version and redeploying, Check for Updates detects the update
- [ ] Browser (Edge/WebKit) works on both Windows and macOS
