# Changelog — Orca for Windows Desktop

## v1.0 — 2026-11-03 · Initial desktop release
- Orca comes to Windows: the same app as Orca for Android v1.2 — same problem
  bank, same dataset generators, same checker, validator and CSV import —
  ported to a desktop window that launches from **Orca.exe** and runs
  **fully offline**.
- Distribution is a ZIP containing a small folder (native launcher +
  `orca-desktop.jar` + a bundled Temurin JRE 17), honestly not a single bare
  .exe: extract anywhere, double-click `Orca.exe`. No installation, no
  internet, nothing to install.
- How it works: the Android app's `Engine.java` and `Problems.java` are used
  **verbatim**, compiled over a small JDBC-backed shim of the Android APIs
  they touch (Xerial sqlite-jdbc). The UI is a Swing port of the Android
  main activity. Because `java.util.Random` behaves identically on desktop,
  the generated datasets match the Android app row-for-row.
- `Orca.exe` is a tiny native C launcher (MinGW-w64, PE32+ GUI x86-64) with
  the orca icon and version info embedded; it starts the bundled
  `jre\bin\javaw.exe -jar orca-desktop.jar` and falls back to a system Java
  only if the bundled runtime is removed.
- Progress, generated datasets and imported tables live per-user at
  `%LOCALAPPDATA%\Orca` and survive restarts — including the persisted
  solution-view XP forfeit.
- Verified before release: dataset counts identical to Android (Retail
  300/120/2,500 · UPI 2,547 transactions with 36 fraud rows · HR 1,200 ·
  Product 2,969 events with 100 power users); all 29 reference solutions
  accepted and non-empty; the shared 47-case audit harness and a 40-case
  desktop harness all passing (checker rules, validator, CSV import,
  5,000-row cap, persistence write/read cycle); the shipped jar launched
  headlessly and walked Home → Problems → Problem screen; zero network
  APIs anywhere in the app sources.
