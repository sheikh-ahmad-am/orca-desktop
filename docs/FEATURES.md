# Orca — Features & Game Logic (Windows Desktop)

The desktop edition runs the Android app's engine **verbatim** — the same
`Engine.java` and `Problems.java`, compiled over a JDBC shim — so every rule
below is identical to Orca for Android v1.2. Only the shell differs
(Swing window, files under `%LOCALAPPDATA%\Orca` instead of phone storage).

## The loop

Pick a dataset → open a problem → write SQL → **Run** to see your result →
**Submit/Check** for a verdict → earn XP, keep the streak alive.

## XP and scoring rules

- **Accepted = +10 XP, first solve only.** Re-solving a solved problem
  awards 0 XP (it still counts toward your daily streak). There is no
  partial XP in the native apps: a verdict is Accepted or not.
- **Wrong answer = 0 XP and the mistake counter goes up by 1.** Retries are
  unlimited; only the first Accepted solve pays out.
- **Submitting a query that errors also counts as a mistake.** Errors from
  the free **Run** button do not — Run is scratch space, Submit is the exam.
- **Viewing the solution first forfeits that problem's XP.** The problem is
  marked revealed the moment you view the solution; solving it afterwards
  pays 0 XP. The forfeit is *persisted* with your progress, so restarting
  the app does not restore it.
- Hints are free. Looking at a hint never costs XP.

## The checker (how Submit judges you)

Submit runs **your query and the reference solution against the same
database** and compares the two result sets. Your SQL text is never compared
to the solution's text — any query producing the right result is Accepted.

- Column **count** must match, and every cell value must match.
- Numbers compare with a **0.01 absolute tolerance** (`100`, `100.0` and
  float residue like `100.0000004` agree; beyond 0.01 is a miss).
- Text comparison is case-sensitive.
- **Row order is ignored — except on problems whose solution uses
  ORDER BY**, where your order must match.
- Duplicate rows are enforced as a multiset.
- Two empty result sets match each other.
- Column *names* are deliberately not compared — Orca judges your result,
  not your exact words or aliases.
- Results are capped at **5,000 rows** while running; more is cut with a
  visible "Result cut at 5,000 rows" note instead of loading unbounded
  rows into memory.
- The verdict reports expected vs your rows × columns when they differ.

## The validator (read-only by design)

Every query — Run and Submit — passes a read-only guard first:

- Comments and string literals are stripped before keyword checks, so a
  leading `-- Write your SQL here` line is harmless and a blocked word
  inside a string (e.g. `WHERE status = 'deleted'`) does not false-trigger.
- Allowed: a plain `SELECT`, or a `WITH` whose main statement (the depth-0
  keyword after the CTE definitions) is `SELECT`. `WITH RECURSIVE` is fine.
- Write/DDL keywords as real tokens anywhere outside literals — `INSERT`,
  `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `REPLACE INTO`, `ATTACH`,
  `DETACH`, `PRAGMA`, `VACUUM` — reject the query. This closes the
  `WITH … DELETE` bypass: SQLite would execute it, so the guard is the wall.
- Only a single statement is allowed. The `REPLACE()` function stays legal;
  only `REPLACE INTO` is blocked.

## Datasets

All four built-in datasets are **synthetic** and generated locally on first
run (fixed seeds) into real SQLite tables. Because `java.util.Random` is
identical on desktop and Android, the generated data matches the Android
edition row-for-row.

| Dataset | Tables | Contents |
|---|---|---|
| Retail & E-Commerce Store | customers (300), products (120), orders (2,500) | A growing online store's sales |
| UPI / Bank Transactions | accounts (250), transactions (2,547) | Bank accounts and transfers, with fraud planted |
| HR Employees | departments (8), employees (1,200) | Designations and salaries across 8 departments |
| Product App Analytics | users (800), events (2,969) | Login/view/purchase/share events, 100 power users mixed in |

- **UPI fraud pattern:** 36 planted burst rows land on exactly 3 accounts,
  lifting them to 23, 22 and 21 transactions in total while normal accounts
  sit at 8–12 — so `HAVING COUNT(*) > 18` isolates exactly the fraud
  accounts.
- **HR:** exactly 4 departments have ≥160 employees (220/200/190/180).
- **Product:** the 100 power users have 12–20 events each; everyone else
  has at most 3, so a `> 8` cutoff finds exactly the power users.

Plus **My Data**: tables you import yourself from CSV (below).

## Problems

29 original problems — Retail 11, UPI/Bank 6, HR 6, Product 6 — in two
stages:

- **Stage 1:** SELECT, WHERE, ORDER BY/LIMIT, DISTINCT
- **Stage 2:** aggregates (COUNT/SUM/AVG/MIN/MAX), GROUP BY, HAVING

Every problem carries a difficulty (Easy/Medium/Hard), a **role tag** across
12 analyst tracks (Data, Business, Product, Risk, MIS, Operations,
Financial, Marketing, Sales, HR/People, Supply Chain, Healthcare), an
**interview-chance % labelled as an estimate** (scored from topic frequency:
core patterns 75–92%, medium 45–70%, niche 20–44%) with a one-line reason,
**"This type asked at"** company tags (pattern-level — never a claim the
exact problem appeared verbatim), a hint, the reference solution, and an
explanation.

## CSV import ("My Data")

- The parser reads the **whole document character-wise**: quoted commas,
  escaped quotes and quoted fields containing newlines parse correctly;
  CRLF handled, BOM stripped, blank lines skipped, trailing empty fields
  preserved, fields trimmed. Cap: 20,000 rows.
- Headers are sanitized into safe SQL identifiers; collisions (`Price`,
  `PRICE`, `price `) are de-duplicated with `_2`, `_3` suffixes instead of
  failing the import.
- **Type inference** checks every non-empty value: INTEGER, else REAL,
  else TEXT; an all-empty column becomes TEXT. Short rows are padded with
  NULL; extra fields are dropped. Imports run in one transaction.
- Imported tables **persist** as SQLite files and survive restarts.
- **4 practice problems are generated per table** from its real schema:
  a first-look peek, an "above average" WHERE built from the first numeric
  column's *actual* average, a GROUP BY count, and a top-5 — each with a
  reference solution that runs against your table. The Above Average
  template is skipped when its answer would be empty (e.g. a constant
  column), so it can never be a free solve.

## Streaks

- Solving on consecutive days builds your streak. **Any** Accepted solve
  counts the day — first-time or re-solve (XP is still first-time only).
- If your last solve was neither today nor yesterday, the home screen
  shows the streak as **0** and the next solve restarts it at 1.

## Learn

Six short theory cards matching the problem stages: SELECT, WHERE,
ORDER BY & LIMIT, DISTINCT, aggregates, and GROUP BY & HAVING. One honest
dialect note: the engine is SQLite, so `strftime` stands in for MySQL's
`DATE_FORMAT` where a date problem needs it, and the cards say so.

## Persistence & offline behaviour

Everything lives under **`%LOCALAPPDATA%\Orca`** on your machine: progress
(XP, solved ids, the revealed set, streak and last-solve day, mistakes,
current dataset) in a properties file, and each dataset / imported table
as its own SQLite file. Delete that folder to start completely fresh.
The app contains no network code at all — no accounts, no telemetry,
no update checks.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
