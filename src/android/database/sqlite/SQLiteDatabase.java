package android.database.sqlite;

import android.database.Cursor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Desktop stand-in for android.database.sqlite.SQLiteDatabase, backed by
 * a real SQLite database file through JDBC (Xerial sqlite-jdbc). Only the
 * API surface Engine.java uses is implemented, with Android semantics:
 * failures surface as unchecked exceptions, like Android's
 * android.database.SQLException (a RuntimeException).
 */
public class SQLiteDatabase {

    public interface CursorFactory { }

    private final Connection conn;
    private boolean txnSuccessful = false;

    static {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (Exception e) { throw new RuntimeException("SQLite JDBC driver missing", e); }
    }

    public SQLiteDatabase(File file) {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void execSQL(String sql) {
        try {
            Statement st = conn.createStatement();
            try { st.execute(sql); } finally { st.close(); }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            if (selectionArgs != null) {
                for (int i = 0; i < selectionArgs.length; i++)
                    ps.setString(i + 1, selectionArgs[i]);
            }
            ResultSet rs = ps.executeQuery();
            return new Cursor(rs, ps);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public SQLiteStatement compileStatement(String sql) {
        try {
            return new SQLiteStatement(conn.prepareStatement(sql));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void beginTransaction() {
        try { conn.setAutoCommit(false); txnSuccessful = false; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public void setTransactionSuccessful() {
        txnSuccessful = true;
    }

    public void endTransaction() {
        try {
            if (txnSuccessful) conn.commit(); else conn.rollback();
            conn.setAutoCommit(true);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void close() {
        try { conn.close(); } catch (Exception ignored) { }
    }
}
