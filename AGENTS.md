# AGENTS.md — KCube Markdown Viewer

## What this project is

Eclipse plug-in that edits Markdown (`.md`, `.markdown`) with **Source / Preview** tabs
plus a live **Markdown Preview** view. Renders via a bundled offline JS viewer
(markdown-it 13.0.1, highlight.js 11.8.0, Bootstrap CSS) loaded in an SWT `Browser`.
No CDN is used. License: Apache-2.0.

Key IDs (do not rename casually — they are extension-point contracts):

- Bundle: `com.kcube.md` (`KCube Markdown Viewer`), root package `com.kcube.md`
- Editor: `com.kcube.md.editor` (default for `md,markdown`), view: `com.kcube.md.preview`
- Feature: `com.kcube.md.feature`, update site: `com.kcube.md.update-site`

Full requirements, architecture, and distribution design live in
[DEVELOPMENT.md](DEVELOPMENT.md). Read it before changing structure or behavior.

## Build, run, test

Prerequisites: JDK 17, Maven 3.9+, Eclipse 4.33 (2024-09)+ for manual verification.

```bash
./build.sh             # mvn clean verify; copies plug-in jar + update-site zip into dist/
./build.sh --install   # same, then installs jar into $ECLIPSE_HOME/dropins (default: /Applications/Eclipse.app/Contents/Eclipse)
ECLIPSE_HOME=/path/to/.../Eclipse ./build.sh --install
```

- Raw equivalent: `mvn -B clean verify`. `dist/` is git-ignored build output.
- There is **no unit-test suite**; `mvn clean verify` (Tycho build success) is the
  automated gate. Do not add a test framework just to verify a change — verify with
  the build plus manual checks in Eclipse instead.
- After install, restart Eclipse with `-clean` (required when structure/resources changed):
  `open -a Eclipse --args -clean`.
- Manual verification checklist: open a `.md` file → Source/Preview tabs render;
  Window > Show View > Other… > Markdown > Markdown Preview follows the active editor;
  Ctrl/Cmd+F in Preview opens the search bar. On failure check `<workspace>/.metadata/.log`
  for `com.kcube.md` errors. Full release checklist: `DEVELOPMENT.md` §8.8.

## Code organization

Flat Maven-Tycho multi-module layout (parent `pom.xml` lists all three modules):

- `com.kcube.md/` — the plug-in (all real code):
  - `META-INF/MANIFEST.MF`, `plugin.xml` (editor + view extensions), `build.properties`
  - `src/com/kcube/md/editor/` — `MarkdownMultiPageEditor` (Source/Preview tabs),
    `MarkdownEditorContributor` (routes Undo/Copy/Find to the active tab)
  - `src/com/kcube/md/preview/` — `MarkdownBrowser` (loads `web/viewer.html`, calls rendering)
  - `src/com/kcube/md/views/` — `MarkdownPreviewView` (live preview, 300 ms debounce)
  - `src/com/kcube/md/util/` — `JsUtils` (Java string → JS literal), `EditorInputs` (md detection, image base path)
  - `web/` — offline viewer: `viewer.html`, `js/` (markdown-it, highlight.js, `viewer.js`, `search.js`),
    `css/` (bootstrap, highlight theme, viewer styles). `icons/md.png`.
- `com.kcube.md.feature/` — installable feature wrapping the plug-in (`unpack="false"`).
- `com.kcube.md.update-site/` — p2 repository (`eclipse-repository` packaging; `category.xml`
  defines the `KCube Tools` category).
- Docs: `README.md` (users), `INSTALL.md` (install/troubleshooting), `DEVELOPMENT.md` (rules/design).

## Conventions for coding agents

- Java 17 is allowed (pattern-matching `instanceof`, arrow switch, text blocks).
- **Javadoc on every Java method** (`@param`/`@return`), comments on non-obvious fields;
  **JSDoc on every JS function**. Follow the examples in `DEVELOPMENT.md` §6.2.
- Logging: SLF4J field named `_log`; **always guard with a level check**
  (`if (_log.isDebugEnabled())`), messages in **English**, `{}` placeholders.
- Java ↔ JS boundary (hard rules):
  - Java never converts Markdown; it passes text to the JS `renderMarkdown(markdown)` entry point.
  - Escape every string with `JsUtils.toJsString()` (never inline your own escaping).
  - Never call render before page load completes (`ProgressListener.completed`);
    queue the latest request instead. Never use `browser.setText()` for refresh
    (resets scroll) — replace only the body from JS, preserving scroll position.
  - Keep everything offline: never add CDN/external URLs; vendor libraries into `web/`
    and record them in `web/THIRD_PARTY_NOTICES.txt`.
- Resources: `build.properties` `bin.includes` must keep `META-INF/, ., plugin.xml, icons/, web/`
  — a missing `web/` or `icons/` ships a blank Preview.
- Dependencies: new Eclipse bundles go in `Require-Bundle` in `MANIFEST.MF`;
  SLF4J stays an `Import-Package` (1.x/2.x compatible). Do not add default Eclipse
  bundles to the feature — declare them as plug-in dependencies only.
- Naming: bundle IDs/packages are lowercase reverse-domain, no hyphens, `kcube`-prefixed;
  user-facing name is `KCube Markdown Viewer` (never leading with "Eclipse").
- Versioning: `MANIFEST.MF`/`feature.xml` use `major.minor.micro.qualifier`,
  poms use `major.minor.micro-SNAPSHOT`; all three major.minor.micro versions must match.
  Bulk bump: `mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=<v>-SNAPSHOT`.
- Bilingual docs: user/requirement docs ship as `X.md` + `X_ko.md` — update both when
  changing behavior they describe. Keep this `AGENTS.md` English-only.
- Do not commit build output (`dist/`, `target/`, `bin/` are git-ignored).
