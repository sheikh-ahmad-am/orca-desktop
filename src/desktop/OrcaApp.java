package desktop;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.ahmad.sqlchallenge.Engine;
import com.ahmad.sqlchallenge.Problems;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Orca — desktop edition. A faithful port of the Android app
 * (com.ahmad.sqlchallenge v1.2): same Engine, same problem bank, same
 * scoring and streak rules, same texts. Only the shell differs
 * (Swing window instead of an Android Activity) and progress lives in
 * a properties file instead of SharedPreferences. Fully offline.
 */
public class OrcaApp {

    // palette (same values as the APK)
    static final Color BG = new Color(11, 18, 32);
    static final Color CARD = new Color(20, 32, 54);
    static final Color CARD2 = new Color(16, 42, 46);
    static final Color STROKE = new Color(52, 84, 120);
    static final Color ACCENT = new Color(46, 230, 168);
    static final Color GOLD = new Color(255, 196, 64);
    static final Color TEXT = new Color(235, 244, 255);
    static final Color MUTED = new Color(150, 170, 200);
    static final Color RED = new Color(255, 107, 107);
    static final Color EDITOR_BG = new Color(8, 13, 24);

    private final JFrame frame;
    private final JScrollPane scroll;
    private final Context ctx;
    private final Progress prefs;
    private Set<String> revealed = new HashSet<String>();
    private String currentScreen = "home";

    // ---------- data location ----------

    /** Per-user local data: %LOCALAPPDATA%\Orca on Windows
     *  (datasets, imported tables and progress live here — not in the exe). */
    static File dataDir() {
        String override = System.getProperty("orca.datadir");
        if (override != null && override.length() > 0) return new File(override);
        String lad = System.getenv("LOCALAPPDATA");
        if (lad != null && lad.length() > 0) return new File(lad, "Orca");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.US);
        if (os.contains("win"))
            return new File(System.getProperty("user.home"), "AppData\\Local\\Orca");
        return new File(System.getProperty("user.home"), ".orca");
    }

    public OrcaApp() {
        File dir = dataDir();
        ctx = new Context(dir);
        prefs = new Progress(new File(dir, "progress.properties"));
        revealed = revealedSet();

        frame = new JFrame("Orca");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1120, 780);
        frame.setMinimumSize(new Dimension(760, 560));
        try {
            InputStream in = OrcaApp.class.getResourceAsStream("/icon-orca.png");
            if (in != null) {
                frame.setIconImage(ImageIO.read(in));
                in.close();
            }
        } catch (Exception ignored) { }
        scroll = new JScrollPane();
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        frame.setLayout(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);
        showHome();
    }

    // ---------- helpers ----------

    JPanel page() {
        JPanel l = new JPanel();
        l.setLayout(new BoxLayout(l, BoxLayout.Y_AXIS));
        l.setBackground(BG);
        l.setBorder(BorderFactory.createEmptyBorder(16, 16, 24, 16));
        return l;
    }

    void setPage(JPanel inner) {
        scroll.setViewportView(inner);
        scroll.getVerticalScrollBar().setValue(0);
    }

    JLabel tv(String text, int size, Color color, boolean bold) {
        return tv(text, size, color, bold, false);
    }

    JLabel tv(String text, int size, Color color, boolean bold, boolean mono) {
        JLabel t = new JLabel("<html>" + escape(text).replace("\n", "<br>") + "</html>");
        t.setFont(new Font(mono ? Font.MONOSPACED : Font.SANS_SERIF,
            bold ? Font.BOLD : Font.PLAIN, size));
        t.setForeground(color);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    JPanel card(Color bgColor) {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(bgColor);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(STROKE, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return c;
    }

    void addCard(JPanel page, JPanel card) {
        page.add(card);
        page.add(Box.createVerticalStrut(12));
    }

    JButton btn(String label, boolean primary) {
        JButton b = new JButton(label);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        b.setForeground(primary ? new Color(6, 26, 20) : TEXT);
        b.setBackground(primary ? ACCENT : CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primary ? ACCENT : STROKE, 1, true),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JPanel headerRow(String title, final String backTo) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton back = new JButton("← Back");
        back.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        back.setForeground(TEXT);
        back.setBackground(CARD);
        back.setFocusPainted(false);
        back.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(STROKE, 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        back.addActionListener(e -> go(backTo));
        row.add(back);
        JLabel t = new JLabel(title);
        t.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        t.setForeground(TEXT);
        row.add(t);
        return row;
    }

    void clickable(JPanel c, final Runnable action) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
    }

    void go(String screen) {
        if ("home".equals(screen)) showHome();
        else if ("datasets".equals(screen)) showDatasets();
        else if ("problems".equals(screen)) showProblems(prefs.getString("dataset", "retail"));
        else if ("mydata".equals(screen)) showMyData();
        else if ("learn".equals(screen)) showLearn();
        else if ("guide".equals(screen)) showGuide();
    }

    Set<String> solvedSet() {
        return new HashSet<String>(prefs.getStringSet("solved", new HashSet<String>()));
    }

    /** Persists the solution-viewed set (XP forfeit) — same fix as APK v1.1:
     *  saved on every change, reloaded at startup, so a restart cannot
     *  quietly restore forfeited XP. */
    void persistRevealed() {
        prefs.edit().putStringSet("revealed", new HashSet<String>(revealed)).apply();
    }

    Set<String> revealedSet() {
        return new HashSet<String>(prefs.getStringSet("revealed", new HashSet<String>()));
    }

    void toast(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Orca", JOptionPane.INFORMATION_MESSAGE);
    }

    void runBg(final Runnable work, final Runnable done) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { work.run(); return null; }
            @Override protected void done() { if (done != null) done.run(); }
        }.execute();
    }

    // ---------- HOME ----------
    void showHome() {
        currentScreen = "home";
        final JPanel l = page();
        l.add(tv("Orca", 24, TEXT, true));
        l.add(tv("Practice SQL on real tables, earn XP, get interview-ready. Fully offline.", 13, MUTED, false));
        l.add(Box.createVerticalStrut(10));

        int xp = prefs.getInt("xp", 0);
        int level = xp / 100 + 1;
        int solved = solvedSet().size();
        int streak = displayedStreak();
        String dsName = Engine.datasetById(prefs.getString("dataset", "retail")).name;

        JPanel xpTile = card(CARD2);
        xpTile.add(tv("TOTAL XP", 11, ACCENT, true));
        xpTile.add(tv(xp + " XP  •  Level " + level, 22, TEXT, true));
        xpTile.add(tv("Every solved problem = +10 XP. Level up every 100 XP.", 12, MUTED, false));
        addCard(l, xpTile);

        JPanel rowTiles = new JPanel(new GridLayout(1, 2, 10, 0));
        rowTiles.setBackground(BG);
        rowTiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowTiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        JPanel streakTile = card(CARD);
        streakTile.add(tv("DAILY STREAK", 11, GOLD, true));
        streakTile.add(tv(streak + (streak == 1 ? " day" : " days"), 18, TEXT, true));
        JPanel solvedTile = card(CARD);
        solvedTile.add(tv("PROBLEMS SOLVED", 11, ACCENT, true));
        solvedTile.add(tv(String.valueOf(solved), 18, TEXT, true));
        rowTiles.add(streakTile);
        rowTiles.add(solvedTile);
        l.add(rowTiles);
        l.add(Box.createVerticalStrut(12));

        final JPanel dsTile = card(CARD);
        dsTile.add(tv("CURRENT DATASET", 11, MUTED, true));
        dsTile.add(tv(dsName, 17, TEXT, true));
        dsTile.add(tv("Click this tile to switch datasets. All built-in data is SYNTHETIC practice data.", 12, MUTED, false));
        clickable(dsTile, () -> showDatasets());
        addCard(l, dsTile);

        JButton bProblems = btn("Play — Problems", true);
        bProblems.addActionListener(e -> showProblems(prefs.getString("dataset", "retail")));
        l.add(bProblems); l.add(Box.createVerticalStrut(8));
        JButton bDs = btn("Datasets", false);
        bDs.addActionListener(e -> showDatasets());
        l.add(bDs); l.add(Box.createVerticalStrut(8));
        JButton bMy = btn("My Data — import a CSV", false);
        bMy.addActionListener(e -> showMyData());
        l.add(bMy); l.add(Box.createVerticalStrut(8));
        JButton bLearn = btn("Learn — theory cards", false);
        bLearn.addActionListener(e -> showLearn());
        l.add(bLearn); l.add(Box.createVerticalStrut(8));
        JButton bGuide = btn("Guide — how to use this app", false);
        bGuide.addActionListener(e -> showGuide());
        l.add(bGuide); l.add(Box.createVerticalStrut(8));
        int mistakes = prefs.getInt("mistakes", 0);
        if (mistakes > 0) l.add(tv("Mistakes so far: " + mistakes + " — every wrong try is noted here. Retry any problem until it clicks.", 12, MUTED, false));
        setPage(l);
    }

    // ---------- DATASETS ----------
    void showDatasets() {
        currentScreen = "datasets";
        final JPanel l = page();
        l.add(headerRow("Datasets", "home"));
        l.add(tv("Built-in datasets are generated on your computer with a fixed seed, so the numbers are the same every run. All are labelled SYNTHETIC — practice data, not any company's real data.", 12, MUTED, false));
        l.add(Box.createVerticalStrut(8));
        final String current = prefs.getString("dataset", "retail");
        for (final Engine.DatasetInfo d : Engine.datasets()) {
            if ("mydata".equals(d.id)) continue;
            final JPanel c = card(CARD);
            c.add(tv(d.name + (d.id.equals(current) ? "  •  ACTIVE" : ""), 16, TEXT, true));
            c.add(tv("SYNTHETIC", 10, GOLD, true));
            c.add(tv(d.blurb, 13, MUTED, false));
            final JLabel meta = tv("Tables: " + join(d.tables) + "  •  preparing counts…", 12, MUTED, false);
            c.add(meta);
            clickable(c, () -> {
                prefs.edit().putString("dataset", d.id).apply();
                showProblems(d.id);
            });
            addCard(l, c);
            runBg(() -> {
                SQLiteDatabase db = Engine.openDataset(ctx, d.id);
                final StringBuilder sb = new StringBuilder();
                sb.append("Tables: ");
                for (int i = 0; i < d.tables.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(d.tables[i]).append(" (").append(Engine.rowCount(db, d.tables[i])).append(")");
                }
                db.close();
                SwingUtilities.invokeLater(() -> meta.setText("<html>" + escape(sb.toString()) + "</html>"));
            }, null);
        }
        JButton bMy = btn("My Data — your own CSV tables", false);
        bMy.addActionListener(e -> showMyData());
        l.add(bMy);
        setPage(l);
    }

    String join(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) { if (i > 0) sb.append(", "); sb.append(arr[i]); }
        return sb.toString();
    }

    String join(List<String> arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) { if (i > 0) sb.append(", "); sb.append(arr.get(i)); }
        return sb.toString();
    }

    // ---------- PROBLEMS LIST ----------
    void showProblems(final String datasetId) {
        currentScreen = "problems";
        final JPanel l = page();
        l.add(headerRow("Problems", "home"));
        final Engine.DatasetInfo d = Engine.datasetById(datasetId);
        l.add(tv(d.name, 16, ACCENT, true));
        final JLabel loading = tv("Preparing dataset on this computer (first time takes a few seconds)…", 13, MUTED, false);
        l.add(loading);
        final JPanel listHolder = new JPanel();
        listHolder.setLayout(new BoxLayout(listHolder, BoxLayout.Y_AXIS));
        listHolder.setBackground(BG);
        listHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.add(listHolder);
        setPage(l);
        runBg(() -> {
            final List<Problems.Problem> probs = new ArrayList<Problems.Problem>();
            SQLiteDatabase db = Engine.openDataset(ctx, datasetId);
            if ("mydata".equals(datasetId)) {
                for (String t : Engine.tablesOf(db)) {
                    List<String> cols = new ArrayList<String>();
                    List<String> types = new ArrayList<String>();
                    for (String ct : Engine.columnsOf(db, t)) {
                        String[] parts = ct.split(" ", 2);
                        cols.add(parts[0]); types.add(parts.length > 1 ? parts[1] : "TEXT");
                    }
                    probs.addAll(Engine.csvProblems(db, t, cols, types));
                }
            } else {
                for (Problems.Problem p : Problems.all())
                    if (p.dataset.equals(datasetId)) probs.add(p);
            }
            db.close();
            final Set<String> solved = solvedSet();
            SwingUtilities.invokeLater(() -> {
                loading.setText("<html>" + probs.size() + " problems • interview chance is an estimate • click one to play</html>");
                if (probs.size() == 0) {
                    listHolder.add(tv("No problems yet for this dataset. Import a CSV from My Data first.", 13, MUTED, false));
                }
                for (final Problems.Problem p : probs) {
                    JPanel c = card(CARD);
                    c.add(tv(p.title + (solved.contains(p.id) ? "   ✓ SOLVED" : ""), 15, TEXT, true));
                    c.add(tv(p.difficulty + "  •  " + p.role + "  •  " + p.stage + " — " + p.topic, 12, MUTED, false));
                    c.add(tv("Interview chance (estimate): " + p.chance + "%", 12, GOLD, true));
                    clickable(c, () -> showProblem(p));
                    addCard(listHolder, c);
                }
                listHolder.revalidate();
            });
        }, null);
    }

    // ---------- PROBLEM SCREEN ----------
    void showProblem(final Problems.Problem p) {
        currentScreen = "problem";
        final JPanel l = page();
        l.add(headerRow("Problem", "problems"));
        l.add(tv(p.title, 19, TEXT, true));
        l.add(tv(p.difficulty + "  •  " + p.role + "  •  " + p.stage + " — " + p.topic, 12, MUTED, false));
        l.add(tv("Interview chance (estimate): " + p.chance + "% — " + p.chanceReason, 12, GOLD, false));
        l.add(tv("This type asked at: " + p.companies + " (question types, not this exact problem)", 12, MUTED, false));
        l.add(Box.createVerticalStrut(8));

        JPanel qCard = card(CARD2);
        qCard.add(tv("BUSINESS QUESTION", 11, ACCENT, true));
        qCard.add(tv(p.question, 15, TEXT, false));
        addCard(l, qCard);

        JPanel schemaCard = card(CARD);
        schemaCard.add(tv("SCHEMA + SAMPLE PEEK", 11, ACCENT, true));
        final JTextArea schemaText = new JTextArea("Loading tables…");
        schemaText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        schemaText.setForeground(MUTED);
        schemaText.setBackground(CARD);
        schemaText.setEditable(false);
        schemaText.setLineWrap(false);
        schemaCard.add(schemaText);
        addCard(l, schemaCard);

        final JLabel hintText = tv("", 13, GOLD, false);
        hintText.setVisible(false);
        JButton bHint = btn("Show hint", false);
        bHint.addActionListener(e -> {
            hintText.setText("<html>Hint: " + escape(p.hint) + "</html>");
            hintText.setVisible(true);
            l.revalidate();
        });
        l.add(bHint); l.add(Box.createVerticalStrut(4));
        l.add(hintText); l.add(Box.createVerticalStrut(8));

        final PlaceholderArea editor = new PlaceholderArea("Write your SQL here…");
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setForeground(TEXT);
        editor.setCaretColor(TEXT);
        editor.setBackground(EDITOR_BG);
        editor.setLineWrap(false);
        editor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(STROKE, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        editor.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane editorScroll = new JScrollPane(editor);
        editorScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        editorScroll.setPreferredSize(new Dimension(200, 120));
        editorScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        l.add(editorScroll);
        l.add(Box.createVerticalStrut(8));

        final JLabel verdict = tv("", 15, TEXT, true);
        final JPanel resultHolder = new JPanel();
        resultHolder.setLayout(new BoxLayout(resultHolder, BoxLayout.Y_AXIS));
        resultHolder.setBackground(BG);
        resultHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        final JLabel explText = tv("", 13, MUTED, false);
        explText.setVisible(false);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setBackground(BG);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JButton bRun = btn("Run", false);
        JButton bSubmit = btn("Submit / Check", true);
        btnRow.add(bRun);
        btnRow.add(bSubmit);
        l.add(btnRow);
        l.add(Box.createVerticalStrut(6));
        l.add(verdict);
        l.add(resultHolder);

        JButton bSolution = btn("Show solution (no XP after viewing)", false);
        bSolution.addActionListener(e -> {
            revealed.add(p.id);
            persistRevealed();
            explText.setText("<html>Solution:<br>" + escape(p.solution) + "<br><br>Why it works: " + escape(p.explanation) + "</html>");
            explText.setVisible(true);
            l.revalidate();
        });
        l.add(Box.createVerticalStrut(8));
        l.add(bSolution);
        l.add(explText);
        setPage(l);

        // schema + sample load
        runBg(() -> {
            SQLiteDatabase db = Engine.openDataset(ctx, p.dataset);
            final StringBuilder sb = new StringBuilder();
            List<String> tables = Engine.tablesOf(db);
            List<String> show = new ArrayList<String>();
            if ("mydata".equals(p.dataset)) {
                String sol = p.solution.toUpperCase(Locale.US);
                for (String t : tables) if (sol.contains(" " + t.toUpperCase(Locale.US))) show.add(t);
                if (show.isEmpty() && tables.size() > 0) show.add(tables.get(0));
            } else show.addAll(tables);
            for (String t : show) {
                sb.append(t).append(" (").append(Engine.rowCount(db, t)).append(" rows)\n");
                for (String col : Engine.columnsOf(db, t)) sb.append("   • ").append(col).append("\n");
                Engine.QueryResult sample = Engine.runQuery(db, "SELECT * FROM " + t + " LIMIT 3");
                if (sample.error == null && sample.rows.size() > 0) {
                    sb.append("   sample: ");
                    for (int i = 0; i < Math.min(3, sample.rows.size()); i++) {
                        if (i > 0) sb.append("  |  ");
                        String[] row = sample.rows.get(i);
                        for (int j = 0; j < row.length; j++) { if (j > 0) sb.append(", "); sb.append(row[j]); }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            db.close();
            SwingUtilities.invokeLater(() -> schemaText.setText(sb.toString().trim()));
        }, null);

        bRun.addActionListener(e -> {
            final String sql = editor.getText();
            verdict.setText("<html>Running…</html>");
            verdict.setForeground(MUTED);
            runBg(() -> {
                SQLiteDatabase db = Engine.openDataset(ctx, p.dataset);
                final Engine.QueryResult res = Engine.runQuery(db, sql);
                db.close();
                SwingUtilities.invokeLater(() -> {
                    resultHolder.removeAll();
                    if (res.error != null) {
                        verdict.setText("<html>SQL error: " + escape(res.error) + "</html>");
                        verdict.setForeground(RED);
                    } else {
                        verdict.setText("<html>Your query ran: " + res.rows.size() + " rows. Submit when it looks right."
                            + (res.note != null ? "<br>" + escape(res.note) : "") + "</html>");
                        verdict.setForeground(MUTED);
                        resultHolder.add(resultTable(res, 30));
                    }
                    resultHolder.revalidate();
                    l.revalidate();
                });
            }, null);
        });

        bSubmit.addActionListener(e -> {
            final String sql = editor.getText();
            verdict.setText("<html>Checking…</html>");
            verdict.setForeground(MUTED);
            runBg(() -> {
                SQLiteDatabase db = Engine.openDataset(ctx, p.dataset);
                final Engine.QueryResult mine = Engine.runQuery(db, sql);
                final Engine.QueryResult expected = Engine.runQuery(db, p.solution);
                db.close();
                final boolean ok = mine.error == null && Engine.resultsMatch(mine, expected, p.orderSensitive);
                SwingUtilities.invokeLater(() -> {
                    resultHolder.removeAll();
                    if (mine.error != null) {
                        verdict.setText("<html>Not quite — SQL error: " + escape(mine.error) + "</html>");
                        verdict.setForeground(RED);
                        noteMistake();
                        resultHolder.revalidate();
                        return;
                    }
                    if (ok) {
                        Set<String> solved = solvedSet();
                        boolean firstTime = !solved.contains(p.id);
                        if (firstTime) {
                            solved.add(p.id);
                            Progress.Editor ed = prefs.edit();
                            ed.putStringSet("solved", solved);
                            if (!revealed.contains(p.id)) ed.putInt("xp", prefs.getInt("xp", 0) + 10);
                            ed.apply();
                        }
                        // Any accepted solve counts the day toward the streak —
                        // first-time or re-solve alike (XP is still first-time
                        // only). updateStreak no-ops when today is already counted.
                        updateStreak();
                        verdict.setText(revealed.contains(p.id) && firstTime
                            ? "<html>Accepted ✓ — correct, but no XP because the solution was viewed.</html>"
                            : firstTime ? "<html>Accepted ✓  +10 XP</html>" : "<html>Accepted ✓ (already solved earlier)</html>");
                        verdict.setForeground(ACCENT);
                        explText.setText("<html>Why it works: " + escape(p.explanation) + "</html>");
                        explText.setVisible(true);
                    } else {
                        noteMistake();
                        verdict.setText("<html>Not quite. Expected " + expected.rows.size() + " rows × " +
                            expected.columns.length + " cols • Yours " + mine.rows.size() + " rows × " +
                            mine.columns.length + " cols. Check the hint and try again.</html>");
                        verdict.setForeground(RED);
                        resultHolder.add(tv("Your result (first rows):", 12, MUTED, true));
                        resultHolder.add(resultTable(mine, 10));
                    }
                    resultHolder.revalidate();
                    l.revalidate();
                });
            }, null);
        });
    }

    void noteMistake() {
        prefs.edit().putInt("mistakes", prefs.getInt("mistakes", 0) + 1).apply();
    }

    String dayString(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    /** The streak Home may show: if the last solve day is neither today nor
     *  yesterday, the streak is broken and displays 0 until a solve today
     *  starts a new one (same rule as APK v1.1+). */
    int displayedStreak() {
        String last = prefs.getString("lastDay", "");
        if (dayString(0).equals(last) || dayString(-1).equals(last))
            return prefs.getInt("streak", 0);
        return 0;
    }

    void updateStreak() {
        String today = dayString(0);
        String last = prefs.getString("lastDay", "");
        if (today.equals(last)) return;
        int streak = dayString(-1).equals(last) ? prefs.getInt("streak", 0) + 1 : 1;
        prefs.edit().putInt("streak", streak).putString("lastDay", today).apply();
    }

    JPanel resultTable(Engine.QueryResult res, int maxRows) {
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBackground(BG);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        int n = Math.min(maxRows, res.rows.size());
        String[][] data = new String[n][];
        for (int i = 0; i < n; i++) data[i] = res.rows.get(i);
        DefaultTableModel model = new DefaultTableModel(data, res.columns) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(EDITOR_BG);
        table.setGridColor(STROKE);
        table.getTableHeader().setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setBackground(CARD);
        table.setRowHeight(20);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane ts = new JScrollPane(table);
        ts.setAlignmentX(Component.LEFT_ALIGNMENT);
        ts.setPreferredSize(new Dimension(200, Math.min(360, 28 + n * 20)));
        ts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        wrap.add(ts);
        if (res.rows.size() > n) wrap.add(tv("… " + res.rows.size() + " rows total, showing first " + n, 11, MUTED, false));
        return wrap;
    }

    // ---------- MY DATA ----------
    void showMyData() {
        currentScreen = "mydata";
        final JPanel l = page();
        l.add(headerRow("My Data", "home"));
        l.add(tv("Import your own CSV file and the app turns it into a real table, then generates practice problems from its columns. Files stay on this computer.", 13, MUTED, false));
        l.add(Box.createVerticalStrut(8));
        JButton bImport = btn("Import a CSV file", true);
        bImport.addActionListener(e -> importCsvFlow());
        l.add(bImport);
        l.add(Box.createVerticalStrut(8));
        final JPanel holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));
        holder.setBackground(BG);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.add(holder);
        setPage(l);
        runBg(() -> {
            SQLiteDatabase db = Engine.openDataset(ctx, "mydata");
            final List<String> lines = new ArrayList<String>();
            for (String t : Engine.tablesOf(db)) {
                lines.add(t + " — " + Engine.rowCount(db, t) + " rows");
                lines.add("   " + join(Engine.columnsOf(db, t)));
            }
            db.close();
            SwingUtilities.invokeLater(() -> {
                if (lines.isEmpty()) {
                    holder.add(tv("No imported tables yet.", 13, MUTED, false));
                } else {
                    holder.add(tv("YOUR IMPORTED TABLES", 11, ACCENT, true));
                    for (String s : lines) holder.add(tv(s, 13, TEXT, false));
                    holder.add(Box.createVerticalStrut(8));
                    JButton bPlay = btn("Practice on my data", true);
                    bPlay.addActionListener(ev -> {
                        prefs.edit().putString("dataset", "mydata").apply();
                        showProblems("mydata");
                    });
                    holder.add(bPlay);
                }
                holder.revalidate();
            });
        }, null);
    }

    void importCsvFlow() {
        JFileChooser fc = new JFileChooser(new File(System.getProperty("user.home")));
        fc.setDialogTitle("Import a CSV file");
        fc.setFileFilter(new FileNameExtensionFilter("CSV / text files (*.csv, *.txt)", "csv", "txt"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        final File file = fc.getSelectedFile();
        final String fname = file.getName();
        // Same plain-language guard as the APK: a non-CSV pick gets a clear
        // message, and Engine.importCsv sniffs binary content as a backstop.
        String lower = fname.toLowerCase(Locale.US);
        if (!lower.endsWith(".csv") && !lower.endsWith(".txt")) {
            toast("That file is not a CSV — pick a .csv file exported from your spreadsheet app.");
            return;
        }
        runBg(() -> {
            Engine.ImportSummary sum;
            try {
                InputStream in = new FileInputStream(file);
                SQLiteDatabase db = Engine.openDataset(ctx, "mydata");
                sum = Engine.importCsv(db, in, fname);
                db.close();
                in.close();
            } catch (Exception ex) {
                sum = new Engine.ImportSummary();
                sum.error = "Could not open that file: " + ex.getMessage();
            }
            final Engine.ImportSummary s2 = sum;
            SwingUtilities.invokeLater(() -> {
                if (s2.error != null) {
                    toast(s2.error);
                } else {
                    toast("Imported " + s2.table + ": " + s2.rows + " rows, " + s2.columns.size() + " columns");
                    prefs.edit().putString("dataset", "mydata").apply();
                }
                showMyData();
            });
        }, null);
    }

    // ---------- LEARN ----------
    void showLearn() {
        currentScreen = "learn";
        final JPanel l = page();
        l.add(headerRow("Learn", "home"));
        l.add(tv("Short theory cards for Stage 1 and Stage 2. Click a card to open it. Note: the built-in engine is SQLite, so a few functions differ from MySQL — the cards flag those spots.", 12, MUTED, false));
        l.add(Box.createVerticalStrut(8));
        for (final Problems.Lesson lesson : Problems.lessons()) {
            JPanel c = card(CARD);
            c.add(tv(lesson.topic, 15, TEXT, true));
            final JLabel body = tv(lesson.body, 13, MUTED, false, true);
            body.setVisible(false);
            c.add(body);
            clickable(c, () -> {
                body.setVisible(!body.isVisible());
                l.revalidate();
            });
            addCard(l, c);
        }
        setPage(l);
    }

    // ---------- GUIDE ----------
    void showGuide() {
        currentScreen = "guide";
        JPanel l = page();
        l.add(headerRow("Guide", "home"));
        String[][] sections = {
            {"1. Pick a dataset", "Home → Datasets. Start with Retail & E-Commerce, or pick the dataset made for your target role (UPI/Bank for Risk, Product App for Product Analyst, HR for HR Analyst). Built-in data is synthetic practice data, generated the same way every time."},
            {"2. Solve problems", "Play — Problems lists this dataset's problems with difficulty, role tag and an interview-chance estimate. Open one, read the business question, check the schema card and sample rows, then write SQL in the editor."},
            {"3. Run vs Submit", "Run just shows your result (or the SQL error) with no scoring — use it while drafting. Submit/Check compares your full result against the solution's result and gives the verdict: Accepted (+10 XP) or Not quite, with expected vs your row counts. Wrong tries are counted as mistakes; you can retry as often as you like."},
            {"4. Stuck?", "Use the hint first. The solution button explains the answer, but viewing it before solving means no XP for that problem — it is for learning, not scoring."},
            {"5. Bring your own data", "My Data → Import a CSV. The app infers the column types, creates a real table, and generates practice problems (select, filter, group-by, top-5) from your own columns."},
            {"6. Keep the streak", "Solving on consecutive days builds your streak, shown on Home next to your XP and level (100 XP per level). Theory cards live under Learn."},
        };
        for (String[] s : sections) {
            JPanel c = card(CARD);
            c.add(tv(s[0], 15, ACCENT, true));
            c.add(tv(s[1], 13, TEXT, false));
            addCard(l, c);
        }
        setPage(l);
    }

    // ---------- editor with placeholder ----------

    static class PlaceholderArea extends JTextArea {
        private final String placeholder;
        PlaceholderArea(String placeholder) { this.placeholder = placeholder; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                g.setColor(MUTED);
                g.setFont(getFont());
                g.drawString(placeholder, getInsets().left + 2, getInsets().top + g.getFontMetrics().getAscent());
            }
        }
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OrcaApp app = new OrcaApp();
            app.frame.setLocationRelativeTo(null);
            app.frame.setVisible(true);
            // Hidden smoke mode used by the build's UI verification:
            // walks Home → Problems → first problem, then exits 0.
            for (String a : args) {
                if ("--selftest-ui".equals(a)) {
                    javax.swing.Timer t = new javax.swing.Timer(2500, e -> {
                        app.showProblems("retail");
                        javax.swing.Timer t2 = new javax.swing.Timer(4000, e2 -> {
                            for (Problems.Problem p : Problems.all()) {
                                if (p.dataset.equals("retail")) { app.showProblem(p); break; }
                            }
                            javax.swing.Timer t3 = new javax.swing.Timer(4000, e3 -> {
                                System.out.println("UI SELFTEST OK");
                                System.exit(0);
                            });
                            t3.setRepeats(false);
                            t3.start();
                        });
                        t2.setRepeats(false);
                        t2.start();
                    });
                    t.setRepeats(false);
                    t.start();
                }
            }
        });
    }
}
