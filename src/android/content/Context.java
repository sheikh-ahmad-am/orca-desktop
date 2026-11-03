package android.content;

import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/**
 * Desktop stand-in for android.content.Context — only the surface that
 * Engine.java uses. Databases live as plain files inside the app's
 * per-user data directory, exactly like on-device internal storage.
 */
public class Context {
    public static final int MODE_PRIVATE = 0;

    private final File dataDir;

    public Context(File dataDir) {
        this.dataDir = dataDir;
        if (!dataDir.exists()) dataDir.mkdirs();
    }

    public File getDataDir() {
        return dataDir;
    }

    public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                                               SQLiteDatabase.CursorFactory factory) {
        return new SQLiteDatabase(new File(dataDir, name));
    }
}
