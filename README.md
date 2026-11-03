<p align="center">
  <img src="icon-orca.png" width="140" alt="Orca icon">
</p>

# Orca — SQL Practice Game (Windows Desktop)

**The Orca SQL practice game in a desktop window — double-click `Orca.exe`, fully offline, nothing to install.**

![Platform](https://img.shields.io/badge/platform-Windows%20x64-2EE6A8)
![Offline](https://img.shields.io/badge/offline-100%25-blue)
![Version](https://img.shields.io/badge/version-1.0-orange)
![License](https://img.shields.io/badge/license-MIT-green)

## About

I built Orca as a game for practicing SQL seriously: real queries against
tables with thousands of rows, checked by actually running your SQL and
comparing result sets — not by matching text. This desktop edition is the
same app as Orca for Android v1.2 — the same problem bank, the same dataset
generators, the same checker and rules — ported to a Windows window that
launches from a single **Orca.exe** and never touches the network.

Open it, pick a dataset — an online store's 2,500 orders, bank transactions
with fraud planted in them, 1,200 employees, a product app's event stream —
and write SQL. Get a problem right and you earn XP; peek at the solution
first and that problem's XP is gone for good. Import your own CSV and Orca
generates practice problems from your own columns.

The teaching dialect is MySQL; the engine is SQLite, and the differences
are flagged in the Learn section.

## Features

- **29 original problems** across Stage 1 (SELECT, WHERE, ORDER BY/LIMIT,
  DISTINCT) and Stage 2 (COUNT/SUM/AVG, GROUP BY/HAVING)
- **4 built-in datasets** generated locally on first run into real SQLite
  tables, identical row-for-row to the Android edition:
  Retail & E-Commerce (2,500 orders) · UPI/Bank (2,547 transactions,
  36 planted fraud rows) · HR Employees (1,200 employees) ·
  Product App Analytics (2,969 events, 100 power users)
- **Result-set judging** — your query and the reference solution both run
  against the same database; values compared with a 0.01 numeric tolerance,
  row order enforced only on ORDER BY problems
- **XP & streaks** — +10 XP for a first-time solve, a daily streak with a
  gap rule, mistake counting; progress persists across restarts
- **Interview framing on every problem** — difficulty, role tag (12 analyst
  tracks), an interview-chance % labelled as an estimate with a reason, and
  "This type asked at" company tags (pattern-level)
- **CSV import** — your file becomes a typed, persistent table plus 4
  generated practice problems built from its real columns
- **Learn & Guide built in** — 6 theory cards and a step-by-step guide
- **Read-only by design** — every query passes a hardened validator;
  writes/DDL are rejected, including disguised `WITH … DELETE` attempts
- **Truly offline** — zero network code in the app; no account, no
  telemetry, no CDN. The runtime is bundled, so there is nothing to install

## Screenshots

Screenshots will be added here — the table below is the shot list.

| Home | Datasets | Problem | Learn |
|------|----------|---------|-------|
| _coming soon_ | _coming soon_ | _coming soon_ | _coming soon_ |

## Download & Install

1. Go to the **Releases** page of this repository and download
   `Orca-Desktop.zip`.
2. Extract the whole ZIP to any folder. Keep `Orca.exe`,
   `orca-desktop.jar` and the `jre` folder together — don't move
   `Orca.exe` out on its own.
3. Double-click **Orca.exe**.
4. On first run Windows may show a SmartScreen note for an unsigned app:
   choose **"More info" → "Run anyway"**. That's expected — the app is not
   code-signed with a paid certificate.

Your progress, generated datasets and imported tables are stored per-user
at `%LOCALAPPDATA%\Orca` and survive restarts. Delete that folder to start
completely fresh.

> Honest packaging note: the download is a small folder in a ZIP, not one
> bare .exe — a single self-contained .exe wasn't achievable with the
> toolkits available when I built this, and I'd rather say so than dress
> it up. The experience is the same: extract, double-click, play.

## Build from Source

```bash
bash build.sh
```

You need JDK 17, MinGW-w64 (`x86_64-w64-mingw32-gcc` + `windres`), and two
fetched dependencies (sqlite-jdbc and a Temurin JRE 17 for Windows) — the
script's header and **[docs/BUILD.md](docs/BUILD.md)** explain exactly how
they are obtained and assembled. Game rules and logic in depth:
**[docs/FEATURES.md](docs/FEATURES.md)**.

## Tech Stack

- **App:** Java 17 + Swing UI; the Android edition's `Engine.java` and
  `Problems.java` compiled **verbatim** over a small JDBC-backed shim of
  the Android APIs they use (`Context`, `Cursor`, `SQLiteDatabase`,
  `SQLiteStatement`)
- **Database:** SQLite via Xerial sqlite-jdbc (shaded into the app jar)
- **Launcher:** a tiny native C stub cross-compiled with MinGW-w64
  (PE32+ GUI x86-64) with the orca icon and version info embedded; it
  starts the bundled JRE's `javaw.exe -jar orca-desktop.jar`
- **Runtime:** bundled Temurin JRE 17 (Windows x64) — end users install
  nothing
- **Data:** progress in a properties file, datasets/imports as SQLite
  files, all under `%LOCALAPPDATA%\Orca`

## Project Structure

```
orca-desktop/
├── README.md                  ← you are here
├── CHANGELOG.md
├── LICENSE
├── build.sh                   ← reproducible build, end to end
├── dist-readme.txt            ← the readme packed into the release ZIP
├── icon-orca.png / orca.ico   ← app icon (PNG master + Windows ICO)
├── launcher/
│   ├── launcher.c             ← native Orca.exe stub
│   └── orca.rc                ← icon + version-info resources
├── src/
│   ├── com/ahmad/sqlchallenge/
│   │   ├── Engine.java        ← datasets, query runner, checker, CSV import
│   │   └── Problems.java      ← problem bank + theory lessons
│   ├── android/…              ← JDBC shim of the Android APIs Engine uses
│   └── desktop/
│       ├── OrcaApp.java       ← Swing UI (port of the Android main activity)
│       └── Progress.java      ← persistent progress store
├── testharness/               ← verification harnesses (audit + desktop suites)
└── docs/
    ├── BUILD.md               ← full build instructions
    ├── FEATURES.md            ← game rules & engine logic in depth
    └── DEVELOPMENT-NOTES.md   ← design & release notes
```

## Roadmap

- **Web edition (already exists):** 22 datasets and 199 problems, all 6
  stages, Mistake Book with spaced revision, company packs, mini-projects
  and timed mock interviews — the native apps are catching up to it.
- **Stages 3–6** on desktop and Android: CASE/functions/NULL handling,
  joins, subqueries/CTEs/window functions, business-analysis capstones.
- Boss fights per stage, partial XP (1–9), and the Mistake Book schedule.

## License

MIT — see [LICENSE](LICENSE). Copyright © 2026 Md Ahmad.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
