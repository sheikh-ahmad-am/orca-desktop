# Building Orca for Windows Desktop

One script — `build.sh` — reproduces the release end to end: it compiles
the app, **runs the verification harnesses** (a build that fails its checks
does not ship), builds the fat jar, cross-compiles the native launcher and
packs the release ZIP.

## Prerequisites

| Tool | Notes |
|---|---|
| JDK 17 | `javac`, `jar`, `java` — the script uses `$HOME/jdk17` |
| MinGW-w64 | `x86_64-w64-mingw32-gcc` + `x86_64-w64-mingw32-windres`, to cross-compile `Orca.exe` for Windows from Linux |
| bash, zip, unzip | packaging |

## Dependencies (fetched once, into `downloads/`)

`downloads/` is git-ignored — fetch these two files before the first build:

1. **sqlite-jdbc** — `downloads/sqlite-jdbc.jar`
   org.xerial:sqlite-jdbc:**3.36.0.3** from Maven Central
   (`https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.36.0.3/sqlite-jdbc-3.36.0.3.jar`).
   It is shaded into the app jar at build time.
2. **Temurin JRE 17, Windows x64** — `downloads/jre-win64.zip`
   from Adoptium (`https://adoptium.net/temurin/releases/`). This is the
   runtime bundled into the release so end users install nothing.
   The build extracts it to `downloads/jre-extract/` and copies it into
   `dist/jre/`.

No other downloads. In particular, **Launch4j is not used** — the launcher
is a small C program I compile directly with MinGW (see `launcher/`).

## Build

```bash
bash build.sh
```

Output: **`Orca-Desktop.zip`** in the repo root, and the unpacked form in
`dist/`:

```
dist/
├── Orca.exe          ← native launcher (what the user double-clicks)
├── orca-desktop.jar  ← the app (fat jar, sqlite-jdbc shaded in)
├── jre/              ← bundled Temurin JRE 17 (win x64)
└── Orca-README.txt   ← end-user readme (from dist-readme.txt)
```

## What the script does, step by step

1. **Compile the app** — everything under `src/` against sqlite-jdbc:
   the Android edition's `Engine.java` + `Problems.java` (**verbatim**),
   the JDBC shim in `src/android/…`, and the Swing UI in `src/desktop/…`.
2. **Verification harnesses** — compiles and runs `testharness/`:
   the 47-case audit suite (checker order/tolerance/multiset rules, the
   validator incl. `WITH … DELETE` rejection, CSV parser, duplicate-header
   dedup) and the 40-case desktop suite (dataset counts, all 29 reference
   solutions, CSV import incl. multiline fields and the constant-column
   template skip, the 5,000-row cap, and a persistence write→read cycle
   including the revealed-set forfeit surviving a restart). Output is
   tailed; any failure stops the build (`set -e`).
3. **Fat jar** — unpacks sqlite-jdbc into `build/fatjar/`, strips its
   signature files, prunes the bundled SQLite natives to Windows x86_64 +
   Linux x86_64 (the two platforms that matter here), copies the app
   classes and `icon-orca.png` in, and jars it with
   `Main-Class: desktop.OrcaApp`.
4. **Launcher exe** — in `launcher/`: `windres` compiles `orca.rc` (the
   orca icon — from `../orca.ico` — plus version info) and
   `x86_64-w64-mingw32-gcc -mwindows -static` links it with `launcher.c`
   into `Orca.exe`, a PE32+ GUI x86-64 executable. At runtime it starts
   `jre\bin\javaw.exe -jar orca-desktop.jar` from its own folder, falling
   back to a system `javaw` (PATH, then `JAVA_HOME`) only if the bundled
   runtime is missing, and shows a clear error if the jar is gone.
5. **Assemble + zip** — `dist/` is packed into `Orca-Desktop.zip`, and the
   size + SHA-256 are printed.

## Running without packaging

```bash
java -cp build/classes:downloads/sqlite-jdbc.jar desktop.OrcaApp
```

(after step 1). The app stores its data under `%LOCALAPPDATA%\Orca` on
Windows; on a Linux dev machine it uses the equivalent per-user local
data directory.

## Notes & honest limits

- The release is an **unsigned** app: Windows SmartScreen warns once on
  first run ("More info → Run anyway"). Code signing needs a paid
  certificate; the warning is documented for users in the README.
- The launcher is Windows-only (it's a PE executable), but the jar itself
  is plain Java 17 and runs anywhere a JRE exists — only the natives
  pruning in the release jar is Windows-focused.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
