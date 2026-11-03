# Development Notes — Orca for Windows Desktop

How the desktop edition came together: the route I chose, why, and what
was verified — adapted from the project's build log (machine-local paths
removed).

## The goal

The same Orca as the phone app, in a desktop window: **double-click
Orca.exe, fully offline, nothing to install.** The user-facing promise
mattered more than the packaging shape — and where the packaging fell
short of the ideal (a folder in a ZIP instead of one bare .exe), the
release says so plainly rather than dressing it up.

## Route chosen — and what I rejected

- **Not Electron:** it would have meant rewriting the UI in HTML and
  shipping a browser to run a SQL checker. Heavy, and it throws away the
  tested Java engine.
- **Not a rewrite:** the Android app's `Engine.java` and `Problems.java`
  already contain the dataset generators, validator, checker, problem
  bank and texts — all verified. So the desktop build compiles those two
  files **verbatim** and puts a small JDBC-backed shim underneath them:
  just the Android APIs they touch (`android.content.Context`,
  `android.database.Cursor`, `android.database.sqlite.SQLiteDatabase` /
  `SQLiteStatement`) re-implemented over Xerial sqlite-jdbc.
- **UI:** a Swing port of the Android main activity — same screens, same
  palette, same verdict texts and scoring rules. A few "on this phone"
  strings became "on this computer"; nothing else was reworded.
- **Launcher:** Launch4j's download was unavailable when I built this, so
  `Orca.exe` is a tiny C stub I cross-compiled myself with MinGW-w64
  (GUI subsystem, static, orca icon + version info embedded). It runs the
  bundled Temurin JRE 17's `javaw.exe -jar orca-desktop.jar`, with a
  system-Java fallback only if the bundled runtime is deleted.
- **Data:** progress (XP / solved / revealed-set / streak / mistakes /
  current dataset) is a properties file at `%LOCALAPPDATA%\Orca`;
  datasets and imports are SQLite files in the same place.
- A nice consequence of the verbatim port: `java.util.Random` behaves
  identically on desktop, so the generated datasets match the Android
  app exactly — same seeds, same rows, same reference answers.

## Verification (all executed on the build machine)

- `file Orca.exe` → PE32+ executable (GUI) x86-64 for MS Windows; its
  resources contain the multi-size orca icon and version info.
- Dataset counts through the real engine: Retail 300/120/2,500 · UPI
  2,547 transactions (36 fraud rows; exactly 3 accounts >18; normal max
  12) · HR 1,200 (exactly 4 departments ≥160) · Product 2,969 events
  (exactly 100 power users) — identical to Android.
- All **29/29 reference solutions accepted and non-empty** end to end; a
  wrong query is rejected; a comment-prefixed correct query (a real bug
  class from the web edition's history) runs fine; a cartesian query is
  capped at 5,000 rows with the visible note.
- The shared audit harness: **47/47**. A desktop-specific harness:
  **40/40** — counts, solutions, CSV import (including a multiline quoted
  field stored whole, and the constant-column template skip), and a
  persistence write→fresh-read cycle proving the solution-view forfeit
  survives a restart.
- The shipped jar itself was launched headlessly and walked through
  Home → Problems → first Problem screen ("UI SELFTEST OK"), generating
  its Retail database through the UI path on the way.
- Offline, proven by inspection: a grep over all app sources finds **zero
  network APIs** — no sockets, no HTTP, no URL code. Nothing to phone
  home with.
- Two test-expectation bugs showed up in my harnesses during verification;
  both times the shipped engine logic was right and the test was wrong.
  The suites as committed are green against the shipped code.

## Honest limits

- What can't be proven from a Linux build machine: the actual
  double-click launch and window rendering on a real Windows 11 PC, and
  SmartScreen's first-run prompt for an unsigned app (expected:
  "More info → Run anyway"). The exe, the JRE binaries and the jar are
  each verified valid, and the jar's UI was really launched headlessly —
  but the Windows double-click itself is the user's test.
- A single bare .exe wasn't achievable with the toolkits available on
  the build machine, so the deliverable is a ZIP'd folder with
  `Orca.exe` at its root. Extract anywhere; keep the folder together.

## What the user gets in the ZIP

From `dist-readme.txt` (packed into every release as `Orca-README.txt`):

- **Orca.exe** — tiny native launcher (starts the bundled runtime)
- **orca-desktop.jar** — the app itself; same problems, datasets and
  rules as the phone app
- **jre/** — bundled Java 17 runtime (Temurin); nothing to install
- Data at `%LOCALAPPDATA%\Orca` — XP, solved problems, streak and
  mistakes survive restarts; delete the folder to start completely fresh

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
