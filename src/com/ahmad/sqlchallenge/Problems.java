package com.ahmad.sqlchallenge;

import java.util.ArrayList;
import java.util.List;

/** Original built-in problem bank (Stage 1 + Stage 2) plus theory cards.
 *  Interview chance is an ESTIMATE from topic frequency, never a promise.
 *  "Asked at" is pattern-level: companies known to ask this TYPE of question. */
public class Problems {

    public static class Problem {
        public String id, dataset, title, difficulty, role, stage, topic;
        public int chance;
        public String chanceReason, companies, question, hint, solution, explanation;
        public boolean orderSensitive;
        Problem(String id, String ds, String title, String diff, String role, String stage,
                String topic, int chance, String reason, String companies, String q,
                String hint, String sol, String expl, boolean ord) {
            this.id = id; this.dataset = ds; this.title = title; this.difficulty = diff;
            this.role = role; this.stage = stage; this.topic = topic; this.chance = chance;
            this.chanceReason = reason; this.companies = companies; this.question = q;
            this.hint = hint; this.solution = sol; this.explanation = expl;
            this.orderSensitive = ord;
        }
    }

    public static List<Problem> all() {
        List<Problem> p = new ArrayList<Problem>();

        // ---------------- RETAIL ----------------
        p.add(new Problem("r1", "retail", "All Orders, Full View", "Easy", "Data Analyst",
            "Stage 1", "SELECT *", 90,
            "SELECT basics appear in almost every analyst SQL round.",
            "Meta, Amazon, Flipkart",
            "The store manager wants to eyeball the raw orders table. Show every column for every row in the orders table.",
            "You need the star form of SELECT on the orders table.",
            "SELECT * FROM orders",
            "SELECT * returns all columns. Fine for exploring a table; in reports, name the columns you need.",
            false));
        p.add(new Problem("r2", "retail", "Customer Directory", "Easy", "Business Analyst",
            "Stage 1", "Selecting columns", 88,
            "Picking exact columns is the most basic interview check.",
            "Accenture, TCS, Infosys",
            "The sales team needs a light customer directory: only the name and city of every customer.",
            "List just two columns from customers.",
            "SELECT name, city FROM customers",
            "Name the columns you want instead of *, so the result stays small and readable.",
            false));
        p.add(new Problem("r3", "retail", "Premium Products", "Easy", "Data Analyst",
            "Stage 1", "WHERE", 89,
            "WHERE filtering with a comparison is asked in nearly every round.",
            "Meta, Walmart, Amazon",
            "Find all products that cost more than 500. Show the product name and its price.",
            "Filter products with WHERE on the price column.",
            "SELECT name, price FROM products WHERE price > 500",
            "WHERE keeps only rows matching the condition. > 500 excludes products priced exactly 500.",
            false));
        p.add(new Problem("r4", "retail", "Newest Customers First", "Easy", "MIS Executive",
            "Stage 1", "ORDER BY", 85,
            "Sorting a report newest-first is daily MIS work and a common test.",
            "TCS, Deloitte, HCLTech",
            "For a welcome-mail list, show each customer's name and signup date, newest signup first.",
            "ORDER BY the signup date in descending order.",
            "SELECT name, signup_date FROM customers ORDER BY signup_date DESC",
            "ORDER BY sorts the result; DESC means largest/newest first. Row order matters here, so the checker compares in order.",
            true));
        p.add(new Problem("r5", "retail", "Cities We Serve", "Easy", "Operations Analyst",
            "Stage 1", "DISTINCT", 78,
            "DISTINCT comes up whenever duplicates pollute a list.",
            "Amazon, Flipkart, Meesho",
            "The logistics team wants the list of cities customers live in, each city exactly once. One column only.",
            "DISTINCT removes duplicate values from the result.",
            "SELECT DISTINCT city FROM customers",
            "DISTINCT collapses duplicate rows in the selected columns, giving one row per city.",
            false));
        p.add(new Problem("r6", "retail", "Top 5 Costliest Products", "Medium", "Business Analyst",
            "Stage 1", "ORDER BY + LIMIT", 82,
            "Top-N with ORDER BY + LIMIT is a classic interview pattern.",
            "Meta, Amazon, Walmart",
            "Show the name and price of the 5 most expensive products, costliest first.",
            "Sort by price descending, then keep only 5 rows.",
            "SELECT name, price FROM products ORDER BY price DESC LIMIT 5",
            "ORDER BY + LIMIT is the standard top-N recipe. Without the sort, LIMIT would return arbitrary rows.",
            true));
        p.add(new Problem("r7", "retail", "Total Revenue", "Easy", "Financial Analyst",
            "Stage 2", "SUM", 90,
            "A single SUM over a fact table is the first aggregate every analyst is tested on.",
            "Deloitte, Accenture, KPMG",
            "Finance wants one number: the total revenue from all orders. The orders table stores the billed amount per order.",
            "Aggregate the amount column of orders with SUM.",
            "SELECT SUM(amount) FROM orders",
            "SUM adds up a numeric column across all rows, producing a single summary row.",
            false));
        p.add(new Problem("r8", "retail", "Average Order Value", "Medium", "Business Analyst",
            "Stage 2", "AVG", 84,
            "AOV is a headline business metric and AVG questions are everywhere.",
            "Amazon, Flipkart, Nykaa",
            "What is the average value of one order? Return a single number.",
            "Use AVG on the amount column.",
            "SELECT AVG(amount) FROM orders",
            "AVG returns the mean of a numeric column. AOV = total revenue / number of orders, and AVG(amount) computes exactly that.",
            false));
        p.add(new Problem("r9", "retail", "Products per Category", "Medium", "Marketing Analyst",
            "Stage 2", "GROUP BY + COUNT", 87,
            "GROUP BY with COUNT is the single most repeated analyst pattern.",
            "Meta, Google, Amazon",
            "Show how many products the store carries in each category: category name and product count.",
            "Group products by category and count rows in each group.",
            "SELECT category, COUNT(*) FROM products GROUP BY category",
            "GROUP BY buckets rows by category; COUNT(*) counts rows inside each bucket.",
            false));
        p.add(new Problem("r10", "retail", "Star Products by Revenue", "Hard", "Sales Analyst",
            "Stage 2", "GROUP BY + HAVING", 68,
            "Filtering groups with HAVING separates mid-level candidates.",
            "Salesforce, Amazon, Walmart",
            "Find the star products: for each product_id, total revenue above 200000 only. Show product_id and its total revenue.",
            "Group orders by product_id, sum amount, then filter groups with HAVING (not WHERE).",
            "SELECT product_id, SUM(amount) FROM orders GROUP BY product_id HAVING SUM(amount) > 200000",
            "WHERE filters rows before grouping; HAVING filters the groups after aggregation. Revenue per product needs HAVING.",
            false));
        p.add(new Problem("r11", "retail", "Monthly Order Report", "Medium", "MIS Executive",
            "Stage 2", "GROUP BY on dates", 76,
            "Month-wise reporting is the core of MIS work.",
            "TCS, Wipro, Genpact",
            "Build the monthly MIS summary: for each month (YYYY-MM), how many orders were placed? Show month and order count.",
            "Use strftime to cut the month out of order_date, then group by it. On this phone the engine is SQLite, so strftime replaces MySQL's DATE_FORMAT.",
            "SELECT strftime('%Y-%m', order_date) AS month, COUNT(*) FROM orders GROUP BY month",
            "strftime('%Y-%m', ...) extracts the year-month from a date, and GROUP BY rolls orders into monthly buckets.",
            false));

        // ---------------- UPI / BANK ----------------
        p.add(new Problem("u1", "upi", "Only the Debits", "Easy", "Risk Analyst",
            "Stage 1", "WHERE on text", 86,
            "Filtering by a category value is asked in banking-domain rounds constantly.",
            "Paytm, PhonePe, Razorpay",
            "The audit team wants every debit transaction: all columns, but only rows where the type is debit.",
            "WHERE with a text value in single quotes.",
            "SELECT * FROM transactions WHERE type = 'debit'",
            "Text values in SQL go in single quotes. type = 'debit' keeps only debit rows.",
            false));
        p.add(new Problem("u2", "upi", "Richest Accounts First", "Easy", "Financial Analyst",
            "Stage 1", "WHERE + ORDER BY", 83,
            "Combining a threshold filter with a ranking is a standard screen.",
            "HDFC Bank, ICICI Bank, Deloitte",
            "List holder name and balance for accounts holding at least 100000, richest first.",
            "Filter on balance, then sort it descending.",
            "SELECT holder_name, balance FROM accounts WHERE balance >= 100000 ORDER BY balance DESC",
            ">= includes accounts at exactly 100000. ORDER BY balance DESC ranks richest first.",
            true));
        p.add(new Problem("u3", "upi", "Big-Ticket Transactions", "Medium", "Risk Analyst",
            "Stage 1", "WHERE", 81,
            "Threshold-based flagging is the first step of every fraud rule.",
            "Paytm, Razorpay, Stripe",
            "Flag every transaction above 50000 for manual review. Show txn_id, account_id and amount.",
            "A single numeric WHERE condition on amount.",
            "SELECT txn_id, account_id, amount FROM transactions WHERE amount > 50000",
            "A simple threshold rule is how real fraud monitoring starts: surface the outliers, then investigate.",
            false));
        p.add(new Problem("u4", "upi", "Debit Total per Account", "Medium", "Risk Analyst",
            "Stage 2", "SUM + GROUP BY", 80,
            "Per-entity totals are the bread and butter of risk reporting.",
            "PhonePe, Paytm, Amazon",
            "For each account, how much money went out in debits in total? Show account_id and the debit total.",
            "Filter to debits first, then group by account and sum.",
            "SELECT account_id, SUM(amount) FROM transactions WHERE type = 'debit' GROUP BY account_id",
            "WHERE runs before grouping, so only debit rows enter each account's bucket; SUM totals them.",
            false));
        p.add(new Problem("u5", "upi", "High-Frequency Accounts", "Hard", "Risk Analyst",
            "Stage 2", "HAVING on COUNT", 66,
            "Frequency outliers are a known fraud signal; HAVING questions test it.",
            "Stripe, Razorpay, Meta",
            "Fraud often means unusual frequency. Find accounts with more than 18 transactions: account_id and their transaction count.",
            "Group transactions by account, count, and keep groups with HAVING COUNT(*) > 18.",
            "SELECT account_id, COUNT(*) FROM transactions GROUP BY account_id HAVING COUNT(*) > 18",
            "HAVING filters aggregated groups. A count far above the average per account deserves a look.",
            false));
        p.add(new Problem("u6", "upi", "Credit vs Debit Average", "Medium", "Data Analyst",
            "Stage 2", "AVG + GROUP BY", 79,
            "Comparing averages across two groups is a recurring analysis ask.",
            "Google, Meta, Accenture",
            "Compare behaviour: what is the average transaction amount for credits and for debits? Show type and average amount.",
            "Group by the type column and average amount per group.",
            "SELECT type, AVG(amount) FROM transactions GROUP BY type",
            "GROUP BY type makes two buckets; AVG is computed separately inside each one.",
            false));

        // ---------------- HR ----------------
        p.add(new Problem("h1", "hr", "High Earners", "Easy", "HR Analyst",
            "Stage 1", "WHERE", 84,
            "Salary-band filtering is the staple HR analytics question.",
            "Accenture, Infosys, TCS",
            "List the name and salary of employees earning more than 80000.",
            "WHERE on the salary column of employees.",
            "SELECT name, salary FROM employees WHERE salary > 80000",
            "A plain numeric filter. HR bands (like > 80000) are how payroll slices are usually built.",
            false));
        p.add(new Problem("h2", "hr", "Roles in the Company", "Easy", "HR Analyst",
            "Stage 1", "DISTINCT", 77,
            "Listing unique job titles is a typical warm-up question.",
            "TCS, Wipro, HCLTech",
            "Show every designation that exists in the company, each exactly once, one column only.",
            "DISTINCT on the designation column.",
            "SELECT DISTINCT designation FROM employees",
            "DISTINCT removes repeats, leaving the unique set of designations.",
            false));
        p.add(new Problem("h3", "hr", "Top 10 Salaries", "Medium", "HR Analyst",
            "Stage 1", "ORDER BY + LIMIT", 82,
            "Top-N earners is a classic leaderboard pattern.",
            "Deloitte, KPMG, EY",
            "Show the name and salary of the 10 highest-paid employees, highest first.",
            "Sort salary descending and limit to 10.",
            "SELECT name, salary FROM employees ORDER BY salary DESC LIMIT 10",
            "ORDER BY salary DESC + LIMIT 10 gives the top-10 leaderboard. Order matters in this result.",
            true));
        p.add(new Problem("h4", "hr", "Average Salary per Department", "Medium", "HR Analyst",
            "Stage 2", "AVG + GROUP BY", 85,
            "Department-wise averages are asked in almost every HR analytics round.",
            "Google, Amazon, Accenture",
            "For each dept_id, show the average salary. Columns: dept_id and the average.",
            "Group employees by dept_id and average the salary.",
            "SELECT dept_id, AVG(salary) FROM employees GROUP BY dept_id",
            "Each department becomes a bucket; AVG(salary) is computed inside each bucket.",
            false));
        p.add(new Problem("h5", "hr", "Big Departments Only", "Hard", "Business Analyst",
            "Stage 2", "HAVING on COUNT", 64,
            "Headcount thresholds with HAVING show up in org-design questions.",
            "Infosys, TCS, Deloitte",
            "Which departments are big? Show dept_id and employee count, but only for departments with at least 160 employees.",
            "Group by dept_id, count employees, filter groups with HAVING.",
            "SELECT dept_id, COUNT(*) FROM employees GROUP BY dept_id HAVING COUNT(*) >= 160",
            "The count exists only after grouping, so the >= 160 condition belongs in HAVING, not WHERE.",
            false));
        p.add(new Problem("h6", "hr", "Patna Team, Well Paid", "Medium", "Operations Analyst",
            "Stage 1", "AND", 75,
            "Two-condition AND filters are a basic screening staple.",
            "Genpact, Wipro, Concentrix",
            "Find employees based in Patna who earn more than 60000. Show name, city and salary.",
            "Combine two conditions with AND in one WHERE.",
            "SELECT name, city, salary FROM employees WHERE city = 'Patna' AND salary > 60000",
            "AND requires both conditions to be true for a row to survive the filter.",
            false));

        // ---------------- PRODUCT APP ----------------
        p.add(new Problem("p1", "product", "What Do Users Do?", "Easy", "Product Analyst",
            "Stage 1", "DISTINCT", 76,
            "Knowing the event vocabulary is step one of product analytics.",
            "Meta, Google, Swiggy",
            "List every event type the app records, each exactly once, one column only.",
            "DISTINCT on event_type in the events table.",
            "SELECT DISTINCT event_type FROM events",
            "Events tables store one row per action; DISTINCT lists the action vocabulary (login, view, purchase, share...).",
            false));
        p.add(new Problem("p2", "product", "Purchase Events Only", "Easy", "Product Analyst",
            "Stage 1", "WHERE on text", 84,
            "Slicing one event type is the funnel's first query.",
            "Amazon, Zomato, Meta",
            "Show all columns for events where the event type is purchase.",
            "Filter events by event_type with a quoted text value.",
            "SELECT * FROM events WHERE event_type = 'purchase'",
            "Funnel analysis starts by isolating the money event; WHERE event_type = 'purchase' does that.",
            false));
        p.add(new Problem("p3", "product", "Events per Type", "Medium", "Product Analyst",
            "Stage 2", "GROUP BY + COUNT", 88,
            "Event-mix breakdowns are asked in nearly every product interview.",
            "Meta, Google, Flipkart",
            "Show how many events of each type the app recorded: event_type and its count.",
            "Group events by event_type and count.",
            "SELECT event_type, COUNT(*) FROM events GROUP BY event_type",
            "This is the event mix — the first chart in any product analytics deck.",
            false));
        p.add(new Problem("p4", "product", "Daily Active Users", "Hard", "Product Analyst",
            "Stage 2", "COUNT DISTINCT + GROUP BY", 69,
            "DAU with COUNT DISTINCT is a signature product-analyst question.",
            "Meta, Swiggy, CRED",
            "For each date, how many distinct users were active? Show event_date and the distinct user count.",
            "Group by event_date and use COUNT(DISTINCT user_id), not COUNT(*).",
            "SELECT event_date, COUNT(DISTINCT user_id) FROM events GROUP BY event_date",
            "One user can fire many events a day, so DAU needs COUNT(DISTINCT user_id), not a raw row count.",
            false));
        p.add(new Problem("p5", "product", "Longest Sessions", "Medium", "Data Analyst",
            "Stage 1", "ORDER BY + LIMIT", 73,
            "Top-N by a duration metric is a common engagement question.",
            "Netflix, YouTube, Spotify",
            "Show user_id and session_minutes for the 5 longest sessions, longest first.",
            "Sort by session_minutes descending, limit 5.",
            "SELECT user_id, session_minutes FROM events ORDER BY session_minutes DESC LIMIT 5",
            "Same top-N recipe as prices or salaries — here the ranking metric is engagement time.",
            true));
        p.add(new Problem("p6", "product", "Power Users", "Hard", "Product Analyst",
            "Stage 2", "HAVING on COUNT", 65,
            "Finding power users with HAVING is a known product round question.",
            "CRED, Zerodha, Meta",
            "Find power users: user_id and event count, only for users with more than 8 events.",
            "Group events by user_id and keep busy groups with HAVING.",
            "SELECT user_id, COUNT(*) FROM events GROUP BY user_id HAVING COUNT(*) > 8",
            "Heavy users drive most product value; HAVING COUNT(*) isolates them after grouping.",
            false));

        return p;
    }

    public static class Lesson {
        public String topic, body;
        Lesson(String t, String b) { topic = t; body = b; }
    }

    public static List<Lesson> lessons() {
        List<Lesson> l = new ArrayList<Lesson>();
        l.add(new Lesson("SELECT — reading data",
            "SELECT chooses the columns, FROM chooses the table.\n\nSyntax:\nSELECT col1, col2 FROM table_name;\nSELECT * FROM table_name;  -- all columns\n\nExample:\nSELECT name, city FROM customers;\n\nTip: name your columns in real reports; * is for exploring."));
        l.add(new Lesson("WHERE — filtering rows",
            "WHERE keeps only rows that match a condition.\n\nOperators: =  <>  >  <  >=  <=\nCombine with AND / OR / NOT. Text goes in single quotes.\n\nExample:\nSELECT * FROM products WHERE price > 500 AND category = 'Electronics';\n\nBETWEEN and IN are shortcuts:\nWHERE price BETWEEN 100 AND 500\nWHERE city IN ('Patna','Delhi')"));
        l.add(new Lesson("ORDER BY & LIMIT — ranking",
            "ORDER BY sorts the result (ASC = smallest first, DESC = largest first). LIMIT keeps the first N rows.\n\nExample (top-N pattern):\nSELECT name, price FROM products\nORDER BY price DESC LIMIT 5;\n\nWithout ORDER BY, LIMIT returns arbitrary rows."));
        l.add(new Lesson("DISTINCT — removing duplicates",
            "DISTINCT collapses duplicate values in the selected columns.\n\nExample:\nSELECT DISTINCT city FROM customers;\n\nUse it for 'list every X once' questions."));
        l.add(new Lesson("Aggregates — COUNT, SUM, AVG, MIN, MAX",
            "Aggregates turn many rows into one summary number.\n\nSELECT COUNT(*) FROM orders;\nSELECT SUM(amount) FROM orders;\nSELECT AVG(amount) FROM orders;\n\nCOUNT(*) counts rows; COUNT(column) skips NULLs."));
        l.add(new Lesson("GROUP BY & HAVING — summaries per group",
            "GROUP BY buckets rows; the aggregate is computed per bucket.\n\nSELECT category, COUNT(*) FROM products GROUP BY category;\n\nWHERE filters rows BEFORE grouping.\nHAVING filters groups AFTER grouping:\n... GROUP BY product_id HAVING SUM(amount) > 200000;"));
        return l;
    }
}
