# Installation

[English](INSTALL.md) | [한국어](INSTALL_ko.md)

- Users who just **install and use** the plug-in: see the [End-user installation guide](#end-user-installation-guide).
- **Developers** who build from source: follow sections 1 to 4 below.

KCube Markdown Viewer targets Eclipse 4.33 (2024-09) or later and JDK 17.

## 1. Build

JDK 17 and Maven are required. The build output is placed in `dist/` (not tracked by git).

```bash
./build.sh
```

| Artifact | Purpose |
|---|---|
| `dist/com.kcube.md.kcube-markdown-viewer-<version>.jar` | Plug-in jar for dropins installation |
| `dist/kcube-markdown-viewer-update-site-<version>.zip` | p2 update site for Eclipse's Install New Software |

If `JAVA_HOME` is not set, JDK 17 is detected automatically on macOS. The build may fail if the default `mvn` uses another JDK, so make sure JDK 17 is used.

## 2. Install

### Option A. dropins (recommended)

```bash
./build.sh --install
```

After building, this removes the existing `com.kcube.md_*.jar` / `com.kcube.md.kcube-markdown-viewer-*.jar` from `/Applications/Eclipse.app/Contents/Eclipse/dropins/` and copies the new jar. For an Eclipse in another location, set `ECLIPSE_HOME`.

```bash
ECLIPSE_HOME=/path/to/Eclipse.app/Contents/Eclipse ./build.sh --install
```

To install manually, copy `dist/com.kcube.md.kcube-markdown-viewer-*.jar` into `<Eclipse>/dropins/`. Always delete older jars first: if an old jar remains, Eclipse may load that one instead.

### Option B. Update site

1. Help > Install New Software… > Add… > Archive… and select `dist/kcube-markdown-viewer-update-site-<version>.zip`.
2. Select KCube Markdown Viewer under the **KCube Tools** category and install it.

## 3. Restart

Restart Eclipse with the `-clean` option. This is required after the plug-in structure or resources have changed.

```bash
open -a Eclipse --args -clean
```

## 4. Verify

- Opening a `.md` / `.markdown` file shows **Source** / **Preview** tabs.
- Open the live preview view via Window > Show View > Other… > **Markdown** > Markdown Preview.
- In Preview, press Ctrl/Cmd+F to open the search bar (case-insensitive; Enter / Shift+Enter to move, Esc to close).

## Troubleshooting

- If the editor does not open, check `<workspace>/.metadata/.log` for errors related to `com.kcube.md`.
- If an older version keeps running, check whether an old jar remains in `dropins/` and restart with `-clean`.

---

# End-user installation guide

All you need is a single jar file from the developer. No source code, Maven, or JDK build environment is required.

## Prerequisites

- Eclipse 4.33 (2024-09) or later (any standard distribution, such as Eclipse IDE for Java Developers)
- A JRE/JDK 17 or later to run Eclipse (a requirement of Eclipse 4.33 itself)
- The distributed `com.kcube.md.kcube-markdown-viewer-<version>.jar`

## Installation (dropins)

1. **Quit Eclipse.**
2. Copy the jar file into the `dropins` folder of your Eclipse installation.

   | OS | dropins location (default install) |
   |---|---|
   | macOS | `/Applications/Eclipse.app/Contents/Eclipse/dropins/` |
   | Windows | `C:\eclipse\dropins\` (under the folder where Eclipse was extracted) |
   | Linux | `~/eclipse/dropins/` (under the folder where Eclipse was extracted) |

   On macOS, either go to Eclipse.app > Show Package Contents > Contents > Eclipse > dropins in Finder, or run the following in a terminal.

   ```bash
   cp ~/Downloads/com.kcube.md.kcube-markdown-viewer-*.jar /Applications/Eclipse.app/Contents/Eclipse/dropins/
   ```

3. **Start Eclipse once with the `-clean` option.** (Required for the first installation.)

   ```bash
   # macOS
   open -a Eclipse --args -clean
   # Windows (Command Prompt)
   C:\eclipse\eclipse.exe -clean
   # Linux
   ~/eclipse/eclipse -clean
   ```

   After that, start Eclipse as usual.

> On macOS, if the downloaded jar carries the "downloaded file" quarantine attribute and causes problems, run `xattr -d com.apple.quarantine <path to jar>` before copying it.

## Verifying the installation

1. Open any `.md` file (double-click it in Package Explorer).
2. If you see **Source** / **Preview** tabs at the bottom of the editor, the plug-in is installed.
3. Check that the Preview tab shows the rendered page and that Ctrl/Cmd+F opens the search bar.
4. (Optional) Open the live preview view via Window > Show View > Other… > **Markdown** > Markdown Preview.

If `.md` files open in another editor, right-click the file > Open With > **KCube Markdown Viewer**, or set KCube Markdown Viewer as the default editor for `*.md` in Preferences > General > Editors > File Associations.

## Updating

1. Quit Eclipse.
2. **Delete** the existing `com.kcube.md.kcube-markdown-viewer-*.jar` (or the old-named `com.kcube.md_*.jar`) in the `dropins` folder and copy the new jar. If an old jar remains, the old version may be loaded.
3. Start Eclipse with the `-clean` option.

## Uninstalling

Quit Eclipse, delete the jar from the `dropins` folder, and start Eclipse with `-clean`.

## Troubleshooting

| Symptom | What to check |
|---|---|
| The Source/Preview tabs do not appear | Make sure you restarted with `-clean` and that the jar is in `dropins`. |
| The Preview is blank | This is unrelated to Window > Preferences > General > Web Browser. Close and reopen the editor; if it persists, send `<workspace>/.metadata/.log` to the developer. |
| An error says the Eclipse version is too old | Upgrade to Eclipse 4.33 (2024-09) or later. |
| Log location | `<workspace>/.metadata/.log` (also available via Window > Show View > Error Log) |
