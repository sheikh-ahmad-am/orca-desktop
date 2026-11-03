package com.ahmad.sqlchallenge;

import java.util.Arrays;
import java.util.List;

public class Harness2 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean actual, boolean expected) {
        boolean ok = actual == expected;
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + actual + " (expected " + expected + ")");
    }
    static void checkEq(String name, Object actual, Object expected) {
        boolean ok = String.valueOf(actual).equals(String.valueOf(expected));
        if (ok) pass++; else fail++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + actual + " (expected " + expected + ")");
    }
    static Engine.QueryResult qr(String[] cols, String[][] rows) {
        Engine.QueryResult q = new Engine.QueryResult();
        q.columns = cols;
        for (String[] r : rows) q.rows.add(r);
        return q;
    }
    static boolean accepted(String sql) { return Engine.validateQuery(sql) == null; }

    public static void main(String[] args) {
        String[] one = {"x"};
        // ---------- original 12 checker cases (audit regression) ----------
        check("reorder-insensitive", Engine.resultsMatch(
            qr(one, new String[][]{{"a"},{"b"}}), qr(one, new String[][]{{"b"},{"a"}}), false), true);
        check("reorder-sensitive", Engine.resultsMatch(
            qr(one, new String[][]{{"a"},{"b"}}), qr(one, new String[][]{{"b"},{"a"}}), true), false);
        check("tolerance-within", Engine.resultsMatch(
            qr(one, new String[][]{{"100.004"}}), qr(one, new String[][]{{"100.00"}}), false), true);
        check("tolerance-beyond", Engine.resultsMatch(
            qr(one, new String[][]{{"100.02"}}), qr(one, new String[][]{{"100.00"}}), false), false);
        check("colcount-mismatch", Engine.resultsMatch(
            qr(new String[]{"a","b"}, new String[][]{{"1","2"}}), qr(one, new String[][]{{"1"}}), false), false);
        check("both-empty", Engine.resultsMatch(
            qr(one, new String[][]{}), qr(one, new String[][]{}), false), true);
        check("rowcount-mismatch", Engine.resultsMatch(
            qr(one, new String[][]{{"1"}}), qr(one, new String[][]{{"1"},{"2"}}), false), false);
        check("text-case", Engine.resultsMatch(
            qr(one, new String[][]{{"Patna"}}), qr(one, new String[][]{{"patna"}}), false), false);
        check("int-vs-decimal", Engine.resultsMatch(
            qr(one, new String[][]{{"100.0"}}), qr(one, new String[][]{{"100"}}), false), true);
        Engine.QueryResult err = qr(one, new String[][]{{"1"}}); err.error = "boom";
        check("user-error", Engine.resultsMatch(err, qr(one, new String[][]{{"1"}}), false), false);
        check("multiset-dupes", Engine.resultsMatch(
            qr(one, new String[][]{{"a"},{"a"}}), qr(one, new String[][]{{"a"}}), false), false);
        check("float-residue", Engine.resultsMatch(
            qr(one, new String[][]{{"100.0000004"}}), qr(one, new String[][]{{"100"}}), false), true);

        // ---------- original parseCsvLine behaviours ----------
        checkEq("line quoted-comma", Engine.parseCsvLine("a,\"b,c\",d"), Arrays.asList("a", "b,c", "d"));
        checkEq("line escaped-quote", Engine.parseCsvLine("\"say \"\"hi\"\"\",x"), Arrays.asList("say \"hi\"", "x"));
        checkEq("line trailing-empty", Engine.parseCsvLine("a,b,"), Arrays.asList("a", "b", ""));
        checkEq("line trim", Engine.parseCsvLine("  spaced , y "), Arrays.asList("spaced", "y"));

        // ---------- NEW: validateQuery (read-only hardening) ----------
        check("plain SELECT", accepted("SELECT * FROM orders"), true);
        check("comment-prefixed SELECT", accepted("-- write here\nSELECT * FROM customers;"), true);
        check("WITH...SELECT (CTE)", accepted(
            "WITH big AS (SELECT customer_id FROM orders GROUP BY customer_id HAVING COUNT(*) > 3) "
            + "SELECT * FROM big"), true);
        check("WITH RECURSIVE...SELECT", accepted(
            "WITH RECURSIVE cnt(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM cnt WHERE x < 3) SELECT x FROM cnt"), true);
        check("WITH...DELETE rejected", accepted(
            "WITH t AS (SELECT 1) DELETE FROM customers"), false);
        check("WITH...UPDATE rejected", accepted(
            "WITH t AS (SELECT 1) UPDATE customers SET city='X'"), false);
        check("WITH...INSERT rejected", accepted(
            "WITH t AS (SELECT 1) INSERT INTO customers VALUES (1,'a','b','c')"), false);
        check("plain DELETE rejected", accepted("DELETE FROM customers"), false);
        check("plain INSERT rejected", accepted("INSERT INTO customers VALUES (1,'a','b','c')"), false);
        check("plain UPDATE rejected", accepted("UPDATE customers SET city='X'"), false);
        check("DROP rejected", accepted("DROP TABLE customers"), false);
        check("multi-statement rejected", accepted("SELECT 1; DELETE FROM customers"), false);
        check("REPLACE INTO rejected", accepted("REPLACE INTO customers VALUES (1,'a','b','c')"), false);
        check("blocked word inside string literal ok", accepted(
            "SELECT * FROM orders WHERE note = 'delete this later'"), true);
        check("blocked word inside comment ok", accepted(
            "/* delete nothing */ SELECT name FROM customers"), true);
        check("REPLACE() function ok", accepted(
            "SELECT REPLACE(name, 'a', 'b') FROM customers"), true);
        check("literal semicolon ok", accepted("SELECT * FROM t WHERE x = 'a;b'"), true);
        check("PRAGMA rejected", accepted("PRAGMA table_info(customers)"), false);

        // ---------- NEW: parseCsv (whole-document parser) ----------
        List<List<String>> ml = Engine.parseCsv("id,note\n1,\"line one\nline two\"\n2,plain\n");
        checkEq("csv multiline row count", ml.size(), 3);
        checkEq("csv multiline field is one field", ml.get(1).get(1), "line one\nline two");
        checkEq("csv multiline next row intact", ml.get(2), Arrays.asList("2", "plain"));
        List<List<String>> qc = Engine.parseCsv("a,b\n\"x,y\",\"say \"\"hi\"\"\"\n");
        checkEq("csv quoted comma", qc.get(1).get(0), "x,y");
        checkEq("csv escaped quotes", qc.get(1).get(1), "say \"hi\"");
        List<List<String>> te = Engine.parseCsv("a,b,\n1,2,\n");
        checkEq("csv trailing empty header", te.get(0), Arrays.asList("a", "b", ""));
        checkEq("csv trailing empty row", te.get(1), Arrays.asList("1", "2", ""));
        List<List<String>> crlf = Engine.parseCsv("a,b\r\n1,2\r\n");
        checkEq("csv CRLF rows", crlf.size(), 2);
        checkEq("csv CRLF row", crlf.get(1), Arrays.asList("1", "2"));
        List<List<String>> bom = Engine.parseCsv("\uFEFFa,b\n1,2");
        checkEq("csv BOM stripped", bom.get(0).get(0), "a");
        List<List<String>> notrail = Engine.parseCsv("a,b\n1,2");
        checkEq("csv no trailing newline", notrail.size(), 2);

        // ---------- NEW: duplicate-header de-duplication ----------
        List<String> dedup = Engine.columnNamesFor(
            Arrays.asList("Price", "PRICE", "price ", "Unit Price", "unit_price", "", "9lives"));
        checkEq("dedup names", dedup,
            Arrays.asList("price", "price_2", "price_3", "unit_price", "unit_price_2", "col_6", "t_9lives"));
        System.out.println("DEDUPED=" + dedup);

        // ---------- row cap constant ----------
        checkEq("MAX_RESULT_ROWS", Engine.MAX_RESULT_ROWS, 5000);

        System.out.println();
        System.out.println("==== Harness2: " + pass + " passed, " + fail + " failed ====");
        if (fail > 0) System.exit(1);
    }
}
