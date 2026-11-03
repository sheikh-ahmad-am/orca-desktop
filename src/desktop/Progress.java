package desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Desktop replacement for Android SharedPreferences ("progress" store).
 * Same keys and semantics as the APK: xp, solved set, revealed set,
 * mistakes, streak, lastDay, dataset. Persisted to progress.properties
 * in the app's per-user data directory, so progress survives restarts
 * and lives outside the exe.
 */
public class Progress {

    private final File file;
    private final Properties props = new Properties();

    public Progress(File file) {
        this.file = file;
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                try { props.load(in); } finally { in.close(); }
            } catch (Exception ignored) { }
        }
    }

    public synchronized int getInt(String key, int def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }

    public synchronized String getString(String key, String def) {
        return props.getProperty(key, def);
    }

    public synchronized Set<String> getStringSet(String key, Set<String> def) {
        String v = props.getProperty(key);
        if (v == null) return new HashSet<String>(def);
        Set<String> out = new HashSet<String>();
        if (v.length() > 0) for (String s : v.split(",")) out.add(s);
        return out;
    }

    public Editor edit() {
        return new Editor();
    }

    public class Editor {
        private final Map<String, String> staged = new HashMap<String, String>();

        public Editor putInt(String key, int value) {
            staged.put(key, String.valueOf(value));
            return this;
        }

        public Editor putString(String key, String value) {
            staged.put(key, value);
            return this;
        }

        public Editor putStringSet(String key, Set<String> values) {
            StringBuilder sb = new StringBuilder();
            for (String s : values) {
                if (sb.length() > 0) sb.append(',');
                sb.append(s);
            }
            staged.put(key, sb.toString());
            return this;
        }

        /** Mirrors SharedPreferences.Editor.apply(): merge + persist now. */
        public void apply() {
            synchronized (Progress.this) {
                for (Map.Entry<String, String> e : staged.entrySet())
                    props.setProperty(e.getKey(), e.getValue());
                saveLocked();
            }
        }
    }

    private void saveLocked() {
        try {
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file);
            try { props.store(out, "Orca progress"); } finally { out.close(); }
        } catch (Exception ignored) { }
    }
}
