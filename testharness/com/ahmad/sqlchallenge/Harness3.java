package com.ahmad.sqlchallenge;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import desktop.Progress;

/**
 * Desktop verification harness (not shipped): dataset generation counts,
 * all 29 reference solutions executed end-to-end through the real shim
 * stack, CSV import behaviours, and a progress persistence write/read
 * cycle. Mirrors the checks the APK audits ran.
 */
public class Harness3 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) {
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS " : "FAIL ") + name);
    }
    static void checkEq(String name, long actual, long expected) {
        boolean ok = actual == expected;
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + actual + " (expected " + expected + ")");
    }

    static long scalar(SQLiteDatabase db, String sql) {
        Cursor c = db.rawQuery(sql, null);
        long v = c.moveToFirst() ? c.getLong(0) : -999999;
        c.close();
        return v;
    }

    static void deleteRec(File f) {
        if (f.isDirectory()) for (File k : f.listFiles()) deleteRec(k);
        f.delete();
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "/tmp/orca-harness-data");
        deleteRec(dir);
        dir.mkdirs();
        Context ctx = new Context(dir);

        // ---------- dataset generation counts ----------
        SQLiteDatabase retail = Engine.openDataset(ctx, "retail");
        checkEq("retail customers", Engine.rowCount(retail, "customers"), 300);
        checkEq("retail products", Engine.rowCount(retail, "products"), 120);
        checkEq("retail orders", Engine.rowCount(retail, "orders"), 2500);
        retail.close();

        SQLiteDatabase upi = Engine.openDataset(ctx, "upi");
        checkEq("upi accounts", Engine.rowCount(upi, "accounts"), 250);
        checkEq("upi transactions", Engine.rowCount(upi, "transactions"), 2547);
        checkEq("upi fraud rows (Unknown Merchant)", scalar(upi,
            "SELECT COUNT(*) FROM transactions WHERE merchant = 'Unknown Merchant'"), 36);
        checkEq("upi accounts with >18 txns", scalar(upi,
            "SELECT COUNT(*) FROM (SELECT account_id FROM transactions GROUP BY account_id HAVING COUNT(*) > 18)"), 3);
        checkEq("upi normal-account max txns", scalar(upi,
            "SELECT MAX(n) FROM (SELECT COUNT(*) AS n FROM transactions WHERE account_id NOT IN (17,103,231) GROUP BY account_id)"), 12);
        upi.close();

        SQLiteDatabase hr = Engine.openDataset(ctx, "hr");
        checkEq("hr departments", Engine.rowCount(hr, "departments"), 8);
        checkEq("hr employees", Engine.rowCount(hr, "employees"), 1200);
        checkEq("hr depts with >=160 employees", scalar(hr,
            "SELECT COUNT(*) FROM (SELECT dept_id FROM employees GROUP BY dept_id HAVING COUNT(*) >= 160)"), 4);
        hr.close();

        SQLiteDatabase product = Engine.openDataset(ctx, "product");
        checkEq("product users", Engine.rowCount(product, "users"), 800);
        checkEq("product events", Engine.rowCount(product, "events"), 2969);
        checkEq("product power users >8 events", scalar(product,
            "SELECT COUNT(*) FROM (SELECT user_id FROM events GROUP BY user_id HAVING COUNT(*) > 8)"), 100);
        product.close();

        // ---------- all 29 reference solutions ----------
        int solved = 0, empty = 0, errored = 0;
        for (Problems.Problem p : Problems.all()) {
            SQLiteDatabase db = Engine.openDataset(ctx, p.dataset);
            Engine.QueryResult mine = Engine.runQuery(db, p.solution);
            Engine.QueryResult expected = Engine.runQuery(db, p.solution);
            boolean ok = mine.error == null && Engine.resultsMatch(mine, expected, p.orderSensitive);
            if (ok) solved++;
            if (mine.error != null) { errored++; System.out.println("  ERROR " + p.id + ": " + mine.error); }
            else if (mine.rows.isEmpty()) { empty++; System.out.println("  EMPTY " + p.id); }
            db.close();
        }
        checkEq("solutions accepted", solved, 29);
        checkEq("solutions errored", errored, 0);
        checkEq("solutions empty", empty, 0);
        checkEq("problem bank size", Problems.all().size(), 29);

        // the web-app P0 regression case: default editor comment + correct query
        SQLiteDatabase r2 = Engine.openDataset(ctx, "retail");
        Engine.QueryResult p0 = Engine.runQuery(r2, "-- Write your SQL here\nselect * from customers;");
        check("comment-prefixed correct query runs", p0.error == null && p0.rows.size() == 300);
        // a genuinely wrong query must NOT match r3's solution
        Engine.QueryResult wrong = Engine.runQuery(r2, "SELECT name, price FROM products WHERE price > 50000");
        Engine.QueryResult r3exp = Engine.runQuery(r2, "SELECT name, price FROM products WHERE price > 500");
        check("wrong query rejected by checker", !Engine.resultsMatch(wrong, r3exp, false));
        // runaway-query cap note fires on a cartesian product
        Engine.QueryResult big = Engine.runQuery(r2, "SELECT * FROM orders a, orders b");
        check("cartesian capped at 5000 with note", big.rows.size() == 5000 && big.note != null);
        r2.close();

        // ---------- CSV import ----------
        SQLiteDatabase my = Engine.openDataset(ctx, "mydata");
        String dupCsv = "Price,PRICE,Unit Price,unit_price\n10,20,x,1\n30,40,y,2\n";
        Engine.ImportSummary s1 = Engine.importCsv(my,
            new ByteArrayInputStream(dupCsv.getBytes("UTF-8")), "dupes.csv");
        check("dup-header import succeeds", s1.error == null);
        checkEq("dup-header columns", s1.columns.size(), 4);
        check("dup-header deduped names", s1.columns.toString().equals("[price, price_2, unit_price, unit_price_2]"));
        checkEq("dup-header rows", s1.rows, 2);

        String mlCsv = "id,note\n1,\"line one\nline two\"\n2,plain\n3,\"a,b\"\n";
        Engine.ImportSummary s2 = Engine.importCsv(my,
            new ByteArrayInputStream(mlCsv.getBytes("UTF-8")), "notes.csv");
        check("multiline import succeeds", s2.error == null);
        checkEq("multiline rows", s2.rows, 3);
        checkEq("multiline field stored whole", scalar(my,
            "SELECT COUNT(*) FROM " + s2.table + " WHERE note = 'line one\nline two'"), 1);

        // First NUMERIC column is the constant one — the audit's degenerate case:
        // avg == every value, so "above average" would have an empty answer.
        String constCsv = "score,note\n5,apple\n5,banana\n5,cherry\n";
        Engine.ImportSummary s3 = Engine.importCsv(my,
            new ByteArrayInputStream(constCsv.getBytes("UTF-8")), "const.csv");
        check("constant-column import succeeds", s3.error == null);
        List<String> cols = Engine.columnsOf(my, s3.table);
        java.util.List<String> cn = new java.util.ArrayList<String>();
        java.util.List<String> ct = new java.util.ArrayList<String>();
        for (String c : cols) { String[] pp = c.split(" ", 2); cn.add(pp[0]); ct.add(pp.length > 1 ? pp[1] : "TEXT"); }
        List<Problems.Problem> cps = Engine.csvProblems(my, s3.table, cn, ct);
        boolean hasAboveAvg = false;
        for (Problems.Problem cp : cps) if (cp.title.startsWith("Above Average")) hasAboveAvg = true;
        check("constant column: Above Average template skipped", !hasAboveAvg);
        checkEq("constant column: other templates still generated", cps.size(), 3); // _1, _4 (top-5), _3 (counts) — only _2 skipped

        String normCsv = "city,amount\nPatna,100\nDelhi,300\nPatna,50\n";
        Engine.ImportSummary s4 = Engine.importCsv(my,
            new ByteArrayInputStream(normCsv.getBytes("UTF-8")), "sales.csv");
        List<String> cols4 = Engine.columnsOf(my, s4.table);
        java.util.List<String> cn4 = new java.util.ArrayList<String>();
        java.util.List<String> ct4 = new java.util.ArrayList<String>();
        for (String c : cols4) { String[] pp = c.split(" ", 2); cn4.add(pp[0]); ct4.add(pp.length > 1 ? pp[1] : "TEXT"); }
        List<Problems.Problem> cps4 = Engine.csvProblems(my, s4.table, cn4, ct4);
        checkEq("normal table: 4 templates generated", cps4.size(), 4);
        boolean csvSolved = true;
        for (Problems.Problem cp : cps4) {
            Engine.QueryResult res = Engine.runQuery(my, cp.solution);
            if (res.error != null || res.rows.isEmpty()) { csvSolved = false; System.out.println("  CSV template problem bad: " + cp.id + " err=" + res.error); }
        }
        check("all 4 generated template solutions run non-empty", csvSolved);
        my.close();

        // ---------- persistence write/read cycle ----------
        File pf = new File(dir, "progress.properties");
        Progress w = new Progress(pf);
        Set<String> solvedSet = new HashSet<String>(); solvedSet.add("r1"); solvedSet.add("u5");
        Set<String> revealedSet = new HashSet<String>(); revealedSet.add("h3");
        w.edit().putInt("xp", 20).putStringSet("solved", solvedSet)
            .putStringSet("revealed", revealedSet).putInt("mistakes", 3)
            .putInt("streak", 2).putString("lastDay", "2026-11-03")
            .putString("dataset", "upi").apply();
        Progress r = new Progress(pf); // fresh instance = simulated relaunch
        checkEq("persist xp", r.getInt("xp", 0), 20);
        check("persist solved set", r.getStringSet("solved", new HashSet<String>()).equals(solvedSet));
        check("persist revealed set (forfeit survives restart)", r.getStringSet("revealed", new HashSet<String>()).equals(revealedSet));
        checkEq("persist mistakes", r.getInt("mistakes", 0), 3);
        checkEq("persist streak", r.getInt("streak", 0), 2);
        check("persist lastDay", "2026-11-03".equals(r.getString("lastDay", "")));
        check("persist dataset", "upi".equals(r.getString("dataset", "")));

        System.out.println();
        System.out.println("==== Harness3: " + pass + " passed, " + fail + " failed ====");
        if (fail > 0) System.exit(1);
    }
}
