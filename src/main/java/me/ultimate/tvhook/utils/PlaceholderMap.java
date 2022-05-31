package me.ultimate.tvhook.utils;

import java.util.HashMap;

public class PlaceholderMap {
    private final HashMap<String, String> PLACEHOLDERS = new HashMap<>();


    public PlaceholderMap() {
        add("newline", "\n");
    }

    public static PlaceholderMap builder() {
        return new PlaceholderMap();
    }

    public PlaceholderMap add(String key, String value) {
        PLACEHOLDERS.put(key.toLowerCase(), value);
        return this;
    }

    public boolean contains(String key) {
        return PLACEHOLDERS.containsKey(key.toLowerCase().trim());
    }

    public String get(String key) {
        return PLACEHOLDERS.get(key.toLowerCase().trim());
    }

    public HashMap<String, String> getAll() {
        return PLACEHOLDERS;
    }

    public String apply(String text) {
        if (text == null) return "";
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '{' || (i > 1 && text.charAt(i - 1) == '\\')) continue;

            int end = text.indexOf('}', i);
            if (end == -1) continue;

            String inner = text.substring(i + 1, end);
            String function = inner.contains("(") ? inner.substring(0, inner.indexOf('(')) : null;
            String key = inner.contains("(") ? inner.substring(inner.indexOf('(') + 1, inner.length() - 1) : inner;
            key = key.toLowerCase();

            if (function != null && contains(key)) {
                function = function.toLowerCase();
                String[] args = key.split(",");

                if ("upper".equals(function)) text = text.replace("{" + inner + "}", PLACEHOLDERS.get(key).toUpperCase());
                else if ("lower".equals(function)) text = text.replace("{" + inner + "}", PLACEHOLDERS.get(key).toLowerCase());
                else if ("if".equals(function) && args.length == 3) {
                    if ("true".equalsIgnoreCase(args[0]) || (contains(args[0]) && "true".equalsIgnoreCase(PLACEHOLDERS.get(args[0]).trim())))
                        text = text.replace("{" + inner + "}",contains(args[1]) ? get(args[1]) : args[1].trim());
                    else
                        text = text.replace("{" + inner + "}", contains(args[2]) ? get(args[2]) : args[2].trim());
                }
            }

            if (contains(key)) text = text.replace("{" + inner + "}", PLACEHOLDERS.get(key));
        }

        return text;
    }
}
