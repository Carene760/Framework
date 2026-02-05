package com.framework.util;

import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonSerializer {

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        serialize(obj, sb);
        return sb.toString();
    }

    private static void serialize(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
            return;
        }

        if (obj instanceof String) {
            sb.append('"').append(escape((String) obj)).append('"');
            return;
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
            return;
        }

        if (obj instanceof Date) {
            sb.append('"').append(ISO.format((Date) obj)).append('"');
            return;
        }

        if (obj.getClass().isArray()) {
            sb.append('[');
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                serialize(Array.get(obj, i), sb);
            }
            sb.append(']');
            return;
        }

        if (obj instanceof Collection) {
            sb.append('[');
            boolean first = true;
            for (Object o : (Collection<?>) obj) {
                if (!first) sb.append(','); first = false;
                serialize(o, sb);
            }
            sb.append(']');
            return;
        }

        if (obj instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?,?> e : ((Map<?,?>) obj).entrySet()) {
                if (!first) sb.append(','); first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
                serialize(e.getValue(), sb);
            }
            sb.append('}');
            return;
        }

        // Bean-like object: use getters
        sb.append('{');
        boolean first = true;
        Method[] methods = obj.getClass().getMethods();
        for (Method m : methods) {
            if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0) {
                String name = m.getName();
                String prop = null;
                if (name.startsWith("get") && name.length() > 3) {
                    prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2 && (m.getReturnType().equals(boolean.class) || m.getReturnType().equals(Boolean.class))) {
                    prop = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                if (prop != null && !"class".equals(prop)) {
                    try {
                        Object value = m.invoke(obj);
                        if (!first) sb.append(','); first = false;
                        sb.append('"').append(escape(prop)).append('"').append(':');
                        serialize(value, sb);
                    } catch (Exception ignored) {}
                }
            }
        }
        sb.append('}');
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 32 || c > 126) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else out.append(c);
            }
        }
        return out.toString();
    }
}



