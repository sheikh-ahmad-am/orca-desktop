package com.ahmad.sqlchallenge;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** On-device SQL engine: seeded synthetic datasets in real SQLite databases,
 *  query running, and result-set comparison for answer checking. */
public class Engine {

    public static class DatasetInfo {
        public String id, name, blurb;
        public String[] tables;
        DatasetInfo(String id, String name, String blurb, String[] tables) {
            this.id = id; this.name = name; this.blurb = blurb; this.tables = tables;
        }
    }

    public static List<DatasetInfo> datasets() {
        List<DatasetInfo> d = new ArrayList<DatasetInfo>();
        d.add(new DatasetInfo("retail", "Retail & E-Commerce Store",
            "Customers, products and 2,500 orders for a growing online store.",
            new String[]{"customers", "products", "orders"}));
        d.add(new DatasetInfo("upi", "UPI / Bank Transactions",
            "Bank accounts and 2,547 transactions — a few fraud patterns are planted in it.",
            new String[]{"accounts", "transactions"}));
        d.add(new DatasetInfo("hr", "HR Employees",
            "1,200 employees across 8 departments with designations and salaries.",
            new String[]{"departments", "employees"}));
        d.add(new DatasetInfo("product", "Product App Analytics",
            "App users and 3,000 events (login, view, purchase, share) with power users mixed in.",
            new String[]{"users", "events"}));
        d.add(new DatasetInfo("mydata", "My Data (your CSVs)",
            "Tables you import yourself from CSV files.",
            new String[]{}));
        return d;
    }

    public static DatasetInfo datasetById(String id) {
        for (DatasetInfo d : datasets()) if (d.id.equals(id)) return d;
        return datasets().get(0);
    }

    // ---------------- DB open + generation ----------------

    public static SQLiteDatabase openDataset(Context ctx, String datasetId) {
        SQLiteDatabase db = ctx.openOrCreateDatabase("ds_" + datasetId + ".db",
            Context.MODE_PRIVATE, null);
        if ("retail".equals(datasetId) && !hasRows(db, "orders")) genRetail(db);
        else if ("upi".equals(datasetId) && !hasRows(db, "transactions")) genUpi(db);
        else if ("hr".equals(datasetId) && !hasRows(db, "employees")) genHr(db);
        else if ("product".equals(datasetId) && !hasRows(db, "events")) genProduct(db);
        return db;
    }

    private static boolean hasRows(SQLiteDatabase db, String table) {
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            boolean ok = c.moveToFirst() && c.getInt(0) > 0;
            c.close();
            return ok;
        } catch (Exception e) { return false; }
    }

    public static long rowCount(SQLiteDatabase db, String table) {
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
            long n = c.moveToFirst() ? c.getLong(0) : 0;
            c.close();
            return n;
        } catch (Exception e) { return 0; }
    }

    private static final String[] FIRST = {"Aarav","Priya","Rahul","Sneha","Amit","Kavya","Rohan","Ananya","Vikram","Pooja","Arjun","Meera","Karan","Divya","Nikhil","Ritu","Sanjay","Neha","Aditya","Ishita","Farhan","Zoya","Imran","Sara","Deepak","Lakshmi","Manoj","Rekha","Suresh","Anita"};
    private static final String[] LAST = {"Kumar","Sharma","Singh","Verma","Gupta","Khan","Das","Yadav","Mishra","Iyer","Patel","Rao","Choudhary","Akhtar","Prasad"};
    private static final String[] CITIES = {"Patna","Delhi","Mumbai","Kolkata","Bengaluru","Lucknow","Hyderabad","Pune"};

    private static String name(Random r) { return FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)]; }

    private static String datePlus(Random r, int baseYear, int spreadDays) {
        Calendar cal = Calendar.getInstance();
        cal.set(baseYear, 0, 1);
        cal.add(Calendar.DAY_OF_YEAR, r.nextInt(spreadDays));
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    private static void genRetail(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS orders");
        db.execSQL("DROP TABLE IF EXISTS products");
        db.execSQL("DROP TABLE IF EXISTS customers");
        db.execSQL("CREATE TABLE customers (customer_id INTEGER PRIMARY KEY, name TEXT, city TEXT, signup_date TEXT)");
        db.execSQL("CREATE TABLE products (product_id INTEGER PRIMARY KEY, name TEXT, category TEXT, price INTEGER)");
        db.execSQL("CREATE TABLE orders (order_id INTEGER PRIMARY KEY, customer_id INTEGER, product_id INTEGER, order_date TEXT, quantity INTEGER, amount INTEGER)");
        Random r = new Random(4201);
        db.beginTransaction();
        try {
            SQLiteStatement s = db.compileStatement("INSERT INTO customers VALUES (?,?,?,?)");
            for (int i = 1; i <= 300; i++) {
                s.bindLong(1, i); s.bindString(2, name(r));
                s.bindString(3, CITIES[r.nextInt(CITIES.length)]);
                s.bindString(4, datePlus(r, 2023, 900)); s.executeInsert();
            }
            String[][] cats = {
                {"Electronics", "Smartphone, Laptop, Headphones, Tablet, Smartwatch, Earbuds, Monitor, Keyboard"},
                {"Clothing", "T-Shirt, Jeans, Kurta, Jacket, Shoes, Saree, Hoodie, Cap"},
                {"Grocery", "Rice Bag, Atta Pack, Cooking Oil, Tea Pack, Sugar, Dal Pack, Spice Box, Honey Jar"},
                {"Furniture", "Chair, Table, Bookshelf, Bed Frame, Lamp, Wardrobe, Sofa, Desk"},
                {"Sports", "Cricket Bat, Football, Yoga Mat, Dumbbell Set, Bicycle, Badminton Racket, Shoes, Gym Bag"}};
            int[] priceBase = {4000, 800, 300, 3000, 900};
            int[] priceVar = {12000, 2500, 900, 15000, 3500};
            int pid = 1;
            int[] prices = new int[121];
            SQLiteStatement sp = db.compileStatement("INSERT INTO products VALUES (?,?,?,?)");
            for (int c = 0; c < cats.length; c++) {
                String[] items = cats[c][1].split(", ");
                for (int k = 0; k < 24; k++) {
                    String pname = items[k % items.length] + " " + (char) ('A' + (k / items.length)) + (k + 1);
                    int price = priceBase[c] + r.nextInt(priceVar[c]);
                    prices[pid] = price;
                    sp.bindLong(1, pid); sp.bindString(2, pname);
                    sp.bindString(3, cats[c][0]); sp.bindLong(4, price); sp.executeInsert();
                    pid++;
                }
            }
            SQLiteStatement so = db.compileStatement("INSERT INTO orders VALUES (?,?,?,?,?,?)");
            for (int i = 1; i <= 2500; i++) {
                int prod = 1 + r.nextInt(120);
                int qty = 1 + r.nextInt(4);
                so.bindLong(1, i); so.bindLong(2, 1 + r.nextInt(300));
                so.bindLong(3, prod); so.bindString(4, datePlus(r, 2024, 700));
                so.bindLong(5, qty); so.bindLong(6, (long) prices[prod] * qty);
                so.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void genUpi(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS accounts");
        db.execSQL("CREATE TABLE accounts (account_id INTEGER PRIMARY KEY, holder_name TEXT, bank TEXT, city TEXT, balance INTEGER)");
        db.execSQL("CREATE TABLE transactions (txn_id INTEGER PRIMARY KEY, account_id INTEGER, txn_date TEXT, type TEXT, amount INTEGER, merchant TEXT)");
        Random r = new Random(7702);
        String[] banks = {"SBI", "HDFC Bank", "ICICI Bank", "Punjab National Bank", "Axis Bank", "Kotak"};
        String[] merchants = {"Swiggy", "Zomato", "Amazon", "Flipkart", "IRCTC", "BigBasket", "PhonePe Transfer", "Petrol Pump", "Electricity Bill", "Jio Recharge"};
        db.beginTransaction();
        try {
            SQLiteStatement s = db.compileStatement("INSERT INTO accounts VALUES (?,?,?,?,?)");
            for (int i = 1; i <= 250; i++) {
                s.bindLong(1, i); s.bindString(2, name(r));
                s.bindString(3, banks[r.nextInt(banks.length)]);
                s.bindString(4, CITIES[r.nextInt(CITIES.length)]);
                s.bindLong(5, 2000 + r.nextInt(248000)); s.executeInsert();
            }
            SQLiteStatement st = db.compileStatement("INSERT INTO transactions VALUES (?,?,?,?,?,?)");
            int tid = 1;
            // normal activity: 8-12 transactions per account
            for (int a = 1; a <= 250; a++) {
                int n = 8 + r.nextInt(5);
                for (int k = 0; k < n; k++) {
                    st.bindLong(1, tid++); st.bindLong(2, a);
                    st.bindString(3, datePlus(r, 2025, 300));
                    st.bindString(4, r.nextBoolean() ? "credit" : "debit");
                    st.bindLong(5, 100 + r.nextInt(20000));
                    st.bindString(6, merchants[r.nextInt(merchants.length)]);
                    st.executeInsert();
                }
            }
            // planted fraud: 3 accounts burst with large debits on one day each
            int[] fraudAccounts = {17, 103, 231};
            for (int a : fraudAccounts) {
                String day = datePlus(r, 2025, 300);
                for (int k = 0; k < 12; k++) {
                    st.bindLong(1, tid++); st.bindLong(2, a);
                    st.bindString(3, day);
                    st.bindString(4, "debit");
                    st.bindLong(5, 45000 + r.nextInt(40000));
                    st.bindString(6, "Unknown Merchant");
                    st.executeInsert();
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void genHr(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS employees");
        db.execSQL("DROP TABLE IF EXISTS departments");
        db.execSQL("CREATE TABLE departments (dept_id INTEGER PRIMARY KEY, dept_name TEXT)");
        db.execSQL("CREATE TABLE employees (emp_id INTEGER PRIMARY KEY, name TEXT, dept_id INTEGER, designation TEXT, salary INTEGER, join_date TEXT, city TEXT)");
        Random r = new Random(9103);
        String[] depts = {"Engineering", "Sales", "Marketing", "Operations", "Finance", "HR", "Support", "Admin"};
        int[] weights = {220, 200, 190, 180, 150, 120, 80, 60}; // sums to 1200
        String[] roles = {"Analyst", "Senior Analyst", "Manager", "Executive", "Assistant", "Team Lead"};
        int[][] sal = {{40000, 50000}, {65000, 45000}, {95000, 65000}, {45000, 40000}, {30000, 25000}, {80000, 50000}};
        db.beginTransaction();
        try {
            SQLiteStatement sd = db.compileStatement("INSERT INTO departments VALUES (?,?)");
            for (int i = 0; i < depts.length; i++) {
                sd.bindLong(1, i + 1); sd.bindString(2, depts[i]); sd.executeInsert();
            }
            SQLiteStatement s = db.compileStatement("INSERT INTO employees VALUES (?,?,?,?,?,?,?)");
            int eid = 1;
            for (int d = 0; d < depts.length; d++) {
                for (int k = 0; k < weights[d]; k++) {
                    int role = r.nextInt(roles.length);
                    s.bindLong(1, eid++); s.bindString(2, name(r));
                    s.bindLong(3, d + 1); s.bindString(4, roles[role]);
                    s.bindLong(5, sal[role][0] + r.nextInt(sal[role][1]));
                    s.bindString(6, datePlus(r, 2018, 2800));
                    s.bindString(7, CITIES[r.nextInt(CITIES.length)]);
                    s.executeInsert();
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static void genProduct(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS events");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("CREATE TABLE users (user_id INTEGER PRIMARY KEY, name TEXT, platform TEXT, signup_date TEXT, country TEXT)");
        db.execSQL("CREATE TABLE events (event_id INTEGER PRIMARY KEY, user_id INTEGER, event_date TEXT, event_type TEXT, session_minutes INTEGER)");
        Random r = new Random(3304);
        String[] platforms = {"Android", "iOS", "Web"};
        String[] types = {"login", "view", "purchase", "share"};
        db.beginTransaction();
        try {
            SQLiteStatement s = db.compileStatement("INSERT INTO users VALUES (?,?,?,?,?)");
            for (int i = 1; i <= 800; i++) {
                s.bindLong(1, i); s.bindString(2, name(r));
                s.bindString(3, platforms[r.nextInt(platforms.length)]);
                s.bindString(4, datePlus(r, 2024, 600));
                s.bindString(5, "India"); s.executeInsert();
            }
            SQLiteStatement se = db.compileStatement("INSERT INTO events VALUES (?,?,?,?,?)");
            int eid = 1;
            for (int u = 1; u <= 800; u++) {
                boolean power = u <= 100;
                int n = power ? 12 + r.nextInt(9) : 1 + r.nextInt(3);
                for (int k = 0; k < n; k++) {
                    se.bindLong(1, eid++); se.bindLong(2, u);
                    se.bindString(3, datePlus(r, 2025, 60));
                    se.bindString(4, types[r.nextInt(types.length)]);
                    se.bindLong(5, 1 + r.nextInt(120)); se.executeInsert();
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    // ---------------- schema + samples ----------------

    public static List<String> tablesOf(SQLiteDatabase db) {
        List<String> out = new ArrayList<String>();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' ORDER BY name", null);
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    public static List<String> columnsOf(SQLiteDatabase db, String table) {
        List<String> out = new ArrayList<String>();
        Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        while (c.moveToNext()) out.add(c.getString(1) + " " + c.getString(2));
        c.close();
        return out;
    }

    public static class QueryResult {
        public String[] columns = new String[0];
        public List<String[]> rows = new ArrayList<String[]>();
        public String error = null;
        /** Non-null when the result was cut at MAX_RESULT_ROWS (see runQuery). */
        public String note = null;
    }

    /** Hard cap on rows accumulated in memory by runQuery (runaway-query guard). */
    public static final int MAX_RESULT_ROWS = 5000;

    private static String cellText(Cursor c, int i) {
        int t = c.getType(i);
        if (t == Cursor.FIELD_TYPE_NULL) return "NULL";
        if (t == Cursor.FIELD_TYPE_FLOAT) {
            double v = c.getDouble(i);
            if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
            return String.valueOf(v);
        }
        return c.getString(i);
    }

    // ---------------- read-only validation ----------------

    static final String READONLY_MSG =
        "Only SELECT queries run here — this is a practice arena, the data stays read-only.";

    private static final String[] BLOCKED = {
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
        "REPLACE", "ATTACH", "DETACH", "PRAGMA", "VACUUM"
    };

    /** Returns the SQL with -- and block comments removed and every quoted
     *  literal/identifier replaced by a bare " ? " placeholder, so keyword
     *  scans never see words that live inside strings or comments. */
    static String stripCommentsAndLiterals(String sql) {
        StringBuilder sb = new StringBuilder();
        int i = 0, n = sql.length();
        while (i < n) {
            char ch = sql.charAt(i);
            if (ch == '-' && i + 1 < n && sql.charAt(i + 1) == '-') {
                while (i < n && sql.charAt(i) != '\n') i++;
                sb.append(' ');
            } else if (ch == '/' && i + 1 < n && sql.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) i++;
                i = Math.min(n, i + 2);
                sb.append(' ');
            } else if (ch == '\'' || ch == '"' || ch == '`') {
                char q = ch;
                i++;
                while (i < n) {
                    if (sql.charAt(i) == q) {
                        if (i + 1 < n && sql.charAt(i + 1) == q) { i += 2; continue; }
                        i++;
                        break;
                    }
                    i++;
                }
                sb.append(" ? ");
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static class Tok {
        String word;
        int depth; // parenthesis depth at the token's start
        int end;   // index just past the token in the analysed string
    }

    private static List<Tok> tokenize(String clean) {
        List<Tok> out = new ArrayList<Tok>();
        int depth = 0, i = 0, n = clean.length();
        while (i < n) {
            char ch = clean.charAt(i);
            if (ch == '(') { depth++; i++; continue; }
            if (ch == ')') { depth--; i++; continue; }
            if (Character.isLetter(ch) || ch == '_') {
                int start = i;
                while (i < n) {
                    char c = clean.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '_') i++;
                    else break;
                }
                Tok t = new Tok();
                t.word = clean.substring(start, i).toUpperCase(Locale.US);
                t.depth = depth;
                t.end = i;
                out.add(t);
                continue;
            }
            i++;
        }
        return out;
    }

    /** Read-only gate for the arena. Returns null when the query may run,
     *  otherwise the error message to show. Accepts a plain SELECT, or a
     *  WITH whose main statement (after the CTE definitions, at parenthesis
     *  depth 0) is SELECT. Any write/DDL keyword appearing as a real token
     *  outside string literals and comments rejects the query, which closes
     *  the WITH...DELETE/UPDATE/INSERT bypass: SQLite would happily run
     *  those through rawQuery. A blocked word used as a function call
     *  (REPLACE(name,'a','b')) is not a statement keyword and stays legal. */
    static String validateQuery(String sql) {
        if (sql == null) return READONLY_MSG;
        String t = stripCommentsAndLiterals(sql).trim();
        while (t.endsWith(";")) t = t.substring(0, t.length() - 1).trim();
        if (t.length() == 0) return READONLY_MSG;
        if (t.indexOf(';') >= 0)
            return "Run one query at a time — remove the extra statement after the semicolon.";
        List<Tok> toks = tokenize(t);
        if (toks.isEmpty()) return READONLY_MSG;
        String first = toks.get(0).word;
        if (!"SELECT".equals(first) && !"WITH".equals(first)) return READONLY_MSG;
        for (Tok tok : toks) {
            for (String b : BLOCKED) {
                if (!tok.word.equals(b)) continue;
                int j = tok.end;
                while (j < t.length() && Character.isWhitespace(t.charAt(j))) j++;
                if (j < t.length() && t.charAt(j) == '(') break; // function call, not a statement
                return "That query tries to change the data (" + b + ") — " + READONLY_MSG;
            }
        }
        if ("WITH".equals(first)) {
            String main = null;
            for (Tok tok : toks) {
                if (tok.depth != 0) continue;
                if ("SELECT".equals(tok.word) || "INSERT".equals(tok.word)
                        || "UPDATE".equals(tok.word) || "DELETE".equals(tok.word)
                        || "VALUES".equals(tok.word)) {
                    main = tok.word;
                    break;
                }
            }
            if (!"SELECT".equals(main)) return READONLY_MSG;
        }
        return null;
    }

    /** Runs a SELECT-only query (see validateQuery). Returns result or error
     *  message. Results are capped at MAX_RESULT_ROWS rows in memory; when
     *  the cap cuts a larger result, QueryResult.note says so. */
    public static QueryResult runQuery(SQLiteDatabase db, String sql) {
        QueryResult qr = new QueryResult();
        String err = validateQuery(sql);
        if (err != null) {
            qr.error = err;
            return qr;
        }
        try {
            Cursor c = db.rawQuery(sql.trim(), null);
            qr.columns = c.getColumnNames();
            while (c.moveToNext()) {
                if (qr.rows.size() >= MAX_RESULT_ROWS) {
                    qr.note = "Result cut at 5,000 rows — the full result is larger. "
                        + "Add a filter or a LIMIT to see it in parts.";
                    break;
                }
                String[] row = new String[qr.columns.length];
                for (int i = 0; i < row.length; i++) row[i] = cellText(c, i);
                qr.rows.add(row);
            }
            c.close();
        } catch (Exception e) {
            qr.error = e.getMessage();
        }
        return qr;
    }

    // ---------------- answer checking ----------------

    private static boolean cellEqual(String a, String b) {
        if (a == null) a = "NULL";
        if (b == null) b = "NULL";
        if (a.equals(b)) return true;
        try {
            double da = Double.parseDouble(a), dbv = Double.parseDouble(b);
            double tol = 0.01 + 1e-9 * Math.abs(dbv);
            return Math.abs(da - dbv) <= tol;
        } catch (Exception e) { return false; }
    }

    private static String rowKey(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (String cell : row) { sb.append(cell == null ? "NULL" : cell.trim()); sb.append('|'); }
        return sb.toString();
    }

    /** Compares two result sets. Column count and row multiset must match;
     *  row order is enforced only when orderSensitive is true. */
    public static boolean resultsMatch(QueryResult user, QueryResult expected, boolean orderSensitive) {
        if (user.error != null || expected.error != null) return false;
        if (user.columns.length != expected.columns.length) return false;
        if (user.rows.size() != expected.rows.size()) return false;
        if (orderSensitive) {
            for (int i = 0; i < user.rows.size(); i++) {
                String[] a = user.rows.get(i), b = expected.rows.get(i);
                for (int j = 0; j < a.length; j++) if (!cellEqual(a[j], b[j])) return false;
            }
            return true;
        }
        List<String> ua = new ArrayList<String>(), eb = new ArrayList<String>();
        for (String[] r : user.rows) ua.add(canonicalRow(r));
        for (String[] r : expected.rows) eb.add(canonicalRow(r));
        Collections.sort(ua); Collections.sort(eb);
        // canonical rows already normalise numbers via parse at build time below
        for (int i = 0; i < ua.size(); i++) {
            if (!rowsCanonEqual(ua.get(i), eb.get(i))) return false;
        }
        return true;
    }

    private static String canonicalRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (String cell : row) {
            String v = cell == null ? "NULL" : cell.trim();
            try {
                double d = Double.parseDouble(v);
                if (d == Math.floor(d) && Math.abs(d) < 1e15) v = String.valueOf((long) d);
                else v = String.format(Locale.US, "%.6f", d);
            } catch (Exception ignored) { }
            sb.append(v).append('|');
        }
        return sb.toString();
    }

    private static boolean rowsCanonEqual(String a, String b) {
        if (a.equals(b)) return true;
        // fall back to numeric tolerance per cell
        String[] pa = a.split("\\|", -1), pb = b.split("\\|", -1);
        if (pa.length != pb.length) return false;
        for (int i = 0; i < pa.length; i++) if (!cellEqual(pa[i], pb[i])) return false;
        return true;
    }

    // ---------------- CSV import ----------------

    public static class ImportSummary {
        public String table, error;
        public List<String> columns = new ArrayList<String>();
        public List<String> types = new ArrayList<String>();
        public long rows;
    }

    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else if (ch == '"') inQuotes = true;
            else if (ch == ',') { out.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out;
    }

    static String safeIdent(String raw, String fallback) {
        String s = raw.toLowerCase(Locale.US).replaceAll("[^a-z0-9_]+", "_")
            .replaceAll("^_+|_+$", "");
        if (s.length() == 0) s = fallback;
        if (Character.isDigit(s.charAt(0))) s = "t_" + s;
        return s;
    }

    /** Parses a whole CSV document character-wise. Unlike the old line-based
     *  reader, a quoted field may contain newlines and still parses as ONE
     *  field. Preserves the earlier proven behaviours: quoted commas, ""
     *  escaped quotes, trailing empty field kept, fields trimmed, CR/LF and
     *  CRLF row endings, a leading BOM stripped. Every returned row is one
     *  record; blank-line filtering is the caller's job (importCsv does it). */
    static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<List<String>>();
        if (content == null) return rows;
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        List<String> row = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false, dirty = false;
        int i = 0, n = content.length();
        while (i < n) {
            char ch = content.charAt(i);
            if (inQuotes) {
                dirty = true;
                if (ch == '"') {
                    if (i + 1 < n && content.charAt(i + 1) == '"') { cur.append('"'); i += 2; continue; }
                    inQuotes = false;
                    i++;
                    continue;
                }
                cur.append(ch);
                i++;
                continue;
            }
            if (ch == '"') { inQuotes = true; dirty = true; i++; continue; }
            if (ch == ',') {
                row.add(cur.toString().trim());
                cur.setLength(0);
                dirty = true;
                i++;
                continue;
            }
            if (ch == '\r' || ch == '\n') {
                if (ch == '\r' && i + 1 < n && content.charAt(i + 1) == '\n') i++;
                row.add(cur.toString().trim());
                cur.setLength(0);
                rows.add(row);
                row = new ArrayList<String>();
                dirty = false;
                i++;
                continue;
            }
            cur.append(ch);
            dirty = true;
            i++;
        }
        if (dirty || row.size() > 0) {
            row.add(cur.toString().trim());
            rows.add(row);
        }
        return rows;
    }

    /** Sanitizes header names and de-duplicates collisions with _2, _3, ...
     *  suffixes (Price / PRICE / "price " all sanitize to "price", which used
     *  to produce duplicate DDL columns and fail the whole import). */
    static List<String> columnNamesFor(List<String> header) {
        List<String> out = new ArrayList<String>();
        Set<String> used = new HashSet<String>();
        for (int i = 0; i < header.size(); i++) {
            String base = safeIdent(header.get(i), "col_" + (i + 1));
            String name = base;
            int suffix = 2;
            while (used.contains(name)) { name = base + "_" + suffix; suffix++; }
            used.add(name);
            out.add(name);
        }
        return out;
    }

    /** Imports a CSV stream as a new table in the mydata database. */
    public static ImportSummary importCsv(SQLiteDatabase db, InputStream in, String displayName) {
        ImportSummary sum = new ImportSummary();
        try {
            // read the whole stream up front: the parser works on the full
            // text so quoted multi-line fields survive as single fields.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int rd;
            while ((rd = in.read(buf)) != -1) bos.write(buf, 0, rd);
            String content = new String(bos.toByteArray(), "UTF-8");
            if (content.indexOf('\0') >= 0 || content.startsWith("%PDF")
                    || content.startsWith("PK\u0003\u0004")) {
                sum.error = "That file is not a CSV — it looks like a binary file "
                    + "(Excel, PDF or similar). Export it as a .csv file and try again.";
                return sum;
            }
            List<List<String>> allRows = parseCsv(content);
            if (allRows.isEmpty()) { sum.error = "That file looks empty."; return sum; }
            List<String> header = allRows.get(0);
            if (header.size() == 1 && header.get(0).length() == 0) {
                sum.error = "That file looks empty.";
                return sum;
            }
            int ncols = header.size();
            List<String> colNames = columnNamesFor(header);
            // read data rows (cap 20000 for phone safety), skipping blank lines
            List<String[]> data = new ArrayList<String[]>();
            for (int ri = 1; ri < allRows.size() && data.size() < 20000; ri++) {
                List<String> cells = allRows.get(ri);
                if (cells.size() == 1 && cells.get(0).length() == 0) continue;
                String[] row = new String[ncols];
                for (int i = 0; i < ncols; i++) row[i] = i < cells.size() ? cells.get(i) : "";
                data.add(row);
            }
            if (data.size() == 0) { sum.error = "No data rows found under the header."; return sum; }
            // type inference per column
            List<String> types = new ArrayList<String>();
            for (int c = 0; c < ncols; c++) {
                boolean isInt = true, isReal = true, seen = false;
                for (String[] row : data) {
                    String v = row[c];
                    if (v == null || v.length() == 0) continue;
                    seen = true;
                    try { Long.parseLong(v); } catch (Exception e) { isInt = false; }
                    try { Double.parseDouble(v); } catch (Exception e) { isReal = false; }
                    if (!isInt && !isReal) break;
                }
                types.add(!seen ? "TEXT" : isInt ? "INTEGER" : isReal ? "REAL" : "TEXT");
            }
            // unique table name from file name
            String base = displayName == null ? "my_table" : displayName.replaceAll("\\.[cC][sS][vV]$", "");
            base = safeIdent(base, "my_table");
            String table = base;
            int suffix = 2;
            while (tableExists(db, table)) { table = base + "_" + suffix; suffix++; }
            StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(table).append(" (");
            for (int i = 0; i < ncols; i++) {
                if (i > 0) ddl.append(", ");
                ddl.append(colNames.get(i)).append(' ').append(types.get(i));
            }
            ddl.append(")");
            db.execSQL(ddl.toString());
            StringBuilder ph = new StringBuilder();
            for (int i = 0; i < ncols; i++) { if (i > 0) ph.append(','); ph.append('?'); }
            SQLiteStatement st = db.compileStatement("INSERT INTO " + table + " VALUES (" + ph + ")");
            db.beginTransaction();
            try {
                for (String[] row : data) {
                    for (int i = 0; i < ncols; i++) {
                        String v = row[i];
                        if (v == null || v.length() == 0) st.bindNull(i + 1);
                        else if ("INTEGER".equals(types.get(i))) { try { st.bindLong(i + 1, Long.parseLong(v)); } catch (Exception e) { st.bindString(i + 1, v); } }
                        else if ("REAL".equals(types.get(i))) { try { st.bindDouble(i + 1, Double.parseDouble(v)); } catch (Exception e) { st.bindString(i + 1, v); } }
                        else st.bindString(i + 1, v);
                    }
                    st.executeInsert();
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            sum.table = table; sum.columns = colNames; sum.types = types; sum.rows = data.size();
            return sum;
        } catch (Exception e) {
            sum.error = "Import failed: " + e.getMessage();
            return sum;
        }
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
        boolean ok = c.moveToFirst() && c.getInt(0) > 0;
        c.close();
        return ok;
    }

    /** Builds the 4 template problems for one imported table. Numeric/text picks
     *  use the table's real columns; the WHERE threshold is the column's real average. */
    public static List<Problems.Problem> csvProblems(SQLiteDatabase db, String table,
                                                     List<String> cols, List<String> types) {
        List<Problems.Problem> out = new ArrayList<Problems.Problem>();
        if (cols.size() == 0) return out;
        String firstText = null, firstNum = null;
        for (int i = 0; i < cols.size(); i++) {
            if (firstText == null && "TEXT".equals(types.get(i))) firstText = cols.get(i);
            if (firstNum == null && ("INTEGER".equals(types.get(i)) || "REAL".equals(types.get(i)))) firstNum = cols.get(i);
        }
        String colList = "";
        for (int i = 0; i < Math.min(3, cols.size()); i++) { if (i > 0) colList += ", "; colList += cols.get(i); }
        out.add(new Problems.Problem("csv_" + table + "_1", "mydata",
            "First Look: " + table, "Easy", "Data Analyst", "Stage 1", "Selecting columns", 85,
            "Exploring a new table's columns is the first move on any dataset.",
            "Accenture, TCS, Infosys",
            "You just imported " + table + ". Show these columns for every row: " + colList + ".",
            "Name the columns after SELECT, then the table after FROM.",
            "SELECT " + colList + " FROM " + table,
            "Start every new dataset by looking at a few real columns before analysing.",
            false));
        if (firstNum != null) {
            double avg = 0;
            try {
                Cursor c = db.rawQuery("SELECT AVG(" + firstNum + ") FROM " + table, null);
                if (c.moveToFirst()) avg = c.getDouble(0);
                c.close();
            } catch (Exception ignored) { }
            long threshold = Math.round(avg);
            // Degenerate-template guard: if no row beats the threshold (e.g.
            // a constant column, where avg == every value), the reference
            // answer would be empty and an empty user result would "solve"
            // it for free — skip this template for such a table.
            long aboveCount = 0;
            try {
                Cursor cc = db.rawQuery("SELECT COUNT(*) FROM " + table
                    + " WHERE " + firstNum + " > " + threshold, null);
                if (cc.moveToFirst()) aboveCount = cc.getLong(0);
                cc.close();
            } catch (Exception ignored) { }
            if (aboveCount > 0) {
                out.add(new Problems.Problem("csv_" + table + "_2", "mydata",
                    "Above Average in " + table, "Medium", "Data Analyst", "Stage 1", "WHERE", 86,
                    "Above-average filters are asked on almost every dataset.",
                    "Meta, Amazon, Flipkart",
                    "In " + table + ", show all columns for rows where " + firstNum + " is greater than " + threshold + " (about its average).",
                    "WHERE " + firstNum + " > " + threshold,
                    "SELECT * FROM " + table + " WHERE " + firstNum + " > " + threshold,
                    "The threshold here is the column's real average, rounded — a classic first filter.",
                    false));
            }
            out.add(new Problems.Problem("csv_" + table + "_4", "mydata",
                "Top 5 by " + firstNum, "Medium", "Business Analyst", "Stage 1", "ORDER BY + LIMIT", 82,
                "Top-N questions appear in nearly every analyst round.",
                "Amazon, Walmart, Meta",
                "Show the 5 rows of " + table + " with the highest " + firstNum + ", highest first. All columns.",
                "ORDER BY " + firstNum + " DESC, then LIMIT 5.",
                "SELECT * FROM " + table + " ORDER BY " + firstNum + " DESC LIMIT 5",
                "ORDER BY + LIMIT is the standard leaderboard recipe.",
                true));
        }
        if (firstText != null) {
            out.add(new Problems.Problem("csv_" + table + "_3", "mydata",
                "Counts by " + firstText, "Medium", "MIS Executive", "Stage 2", "GROUP BY + COUNT", 87,
                "GROUP BY counts are the most repeated analyst pattern of all.",
                "TCS, Wipro, Deloitte",
                "For " + table + ", count rows per " + firstText + ": show " + firstText + " and the count.",
                "GROUP BY " + firstText + " with COUNT(*).",
                "SELECT " + firstText + ", COUNT(*) FROM " + table + " GROUP BY " + firstText,
                "Grouping by a text column turns a raw import into a summary report.",
                false));
        }
        return out;
    }
}
