package android.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Desktop stand-in for android.database.Cursor, backed by a JDBC
 * ResultSet. Column indexes are 0-based, matching Android. Cell type
 * reporting mirrors Android: the runtime type of the stored value
 * (NULL / INTEGER / FLOAT / STRING / BLOB), which Engine.cellText
 * relies on for its float formatting rule.
 */
public class Cursor {
    public static final int FIELD_TYPE_NULL = 0;
    public static final int FIELD_TYPE_INTEGER = 1;
    public static final int FIELD_TYPE_FLOAT = 2;
    public static final int FIELD_TYPE_STRING = 3;
    public static final int FIELD_TYPE_BLOB = 4;

    private final ResultSet rs;
    private final Statement stmt;
    private String[] columnNames;

    public Cursor(ResultSet rs, Statement stmt) {
        this.rs = rs;
        this.stmt = stmt;
    }

    public boolean moveToFirst() {
        try { return rs.next(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public boolean moveToNext() {
        try { return rs.next(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public int getInt(int i) {
        try { return rs.getInt(i + 1); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public long getLong(int i) {
        try { return rs.getLong(i + 1); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public double getDouble(int i) {
        try { return rs.getDouble(i + 1); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public String getString(int i) {
        try { return rs.getString(i + 1); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public int getType(int i) {
        try {
            Object o = rs.getObject(i + 1);
            if (o == null) return FIELD_TYPE_NULL;
            if (o instanceof Double || o instanceof Float) return FIELD_TYPE_FLOAT;
            if (o instanceof String) return FIELD_TYPE_STRING;
            if (o instanceof byte[]) return FIELD_TYPE_BLOB;
            return FIELD_TYPE_INTEGER;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public String[] getColumnNames() {
        if (columnNames == null) {
            try {
                ResultSetMetaData md = rs.getMetaData();
                columnNames = new String[md.getColumnCount()];
                for (int i = 0; i < columnNames.length; i++)
                    columnNames[i] = md.getColumnLabel(i + 1);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        return columnNames;
    }

    public void close() {
        try { rs.close(); } catch (Exception ignored) { }
        if (stmt != null) try { stmt.close(); } catch (Exception ignored) { }
    }
}
