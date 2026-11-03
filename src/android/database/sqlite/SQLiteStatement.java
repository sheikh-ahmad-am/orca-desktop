package android.database.sqlite;

import java.sql.PreparedStatement;

/**
 * Desktop stand-in for android.database.sqlite.SQLiteStatement over a
 * JDBC PreparedStatement. Bind indexes are 1-based, matching Android,
 * and bindings persist across executions, matching Android.
 */
public class SQLiteStatement {

    private final PreparedStatement ps;

    SQLiteStatement(PreparedStatement ps) {
        this.ps = ps;
    }

    public void bindLong(int index, long value) {
        try { ps.setLong(index, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public void bindDouble(int index, double value) {
        try { ps.setDouble(index, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public void bindString(int index, String value) {
        try { ps.setString(index, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public void bindNull(int index) {
        try { ps.setObject(index, null); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public long executeInsert() {
        try { ps.executeUpdate(); return 1; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public void close() {
        try { ps.close(); } catch (Exception ignored) { }
    }
}
