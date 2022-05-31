package me.ultimate.tvhook.utils;

import me.ultimate.tvhook.Main;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private Map<String, Object> configMap;
    private boolean exists = false;


    public Configuration(String fileName) {
        this(new File(Main.DATA_FOLDER, fileName));
    }

    public Configuration(File file) {
        try {
            exists = file.exists();
            if (!exists) {
                Main.getLogger().warn("Configuration file not found: {}", file.getAbsolutePath());
                configMap = new HashMap<>();
                return;
            }

            Yaml yaml = new Yaml();
            configMap = yaml.load(new FileInputStream(file));
        } catch (Exception e) {
            Main.getLogger().warn("Failed to load/parse configuration file: {}", file.getAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    public Object get(String path) {
        String[] keys = path.split("\\.");

        Map<String, Object> map = configMap;
        Object obj = null;
        for (String key : keys) {
            if (map.containsKey(key)) {
                if (map.get(key) instanceof Map) {
                    map = (Map<String, Object>) map.get(key);
                } else {
                    obj = map.get(key);
                }
            }
        }

        if (obj == null) Main.getLogger().warn("Unknown configuration key: {}", path);
        return obj;
    }

    public String getString(String path) {
        Object obj = get(path);
        return (obj instanceof String) ? (String) obj : null;
    }

    public int getInt(String path, int def) {
        Object obj = get(path);

        if (obj instanceof Double) return (int) Math.round((double) obj);
        return (obj instanceof Integer) ? (int) obj : def;
    }

    public double getDouble(String path, double def) {
        Object obj = get(path);

        if (obj instanceof Integer) return (int) obj;
        return (obj instanceof Double) ? (double) obj : def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object obj = get(path);
        return (obj instanceof Boolean) ? (boolean) obj : def;
    }

    // Java
    public int getColor(String path, int def) {
        int uc = Utils.getColor(getString(path));
        return uc > 0 ? uc : getInt(path, def);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getStringList(String path) {
        Object obj = get(path);

        if (obj instanceof ArrayList) return (ArrayList<String>) obj;
        return new ArrayList<>();
    }

    public boolean exists() {
        return exists;
    }
}
