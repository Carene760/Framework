package core.utils;

import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * SPRINT 6: Gestionnaire de conversion des paramètres
 * Version complète avec support des dates
 */
public class ParametersHandler {
    
    public static Object convertToType(String value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        
        if (targetType == String.class) {
            return value != null ? value : "";
        } else if (targetType == int.class || targetType == Integer.class) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return targetType == int.class ? 0 : null;
            }
        } else if (targetType == long.class || targetType == Long.class) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return targetType == long.class ? 0L : null;
            }
        } else if (targetType == double.class || targetType == Double.class) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return targetType == double.class ? 0.0 : null;
            }
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == float.class || targetType == Float.class) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return targetType == float.class ? 0.0f : null;
            }
        } else if (targetType == Date.class) {
            return parseDate(value);
        } else if (targetType == LocalDate.class) {
            return parseLocalDate(value);
        } else if (targetType == LocalDateTime.class) {
            return parseLocalDateTime(value);
        }
        
        return value;
    }

    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String[] dateFormats = {
            "yyyy-MM-dd",
            "dd/MM/yyyy", 
            "MM/dd/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "dd-MM-yyyy",
            "yyyy/MM/dd"
        };
        
        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException e) {
                // Continue to next format
            }
        }
        
        return null;
    }

    private static LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e1) {
            String[] patterns = {"dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy"};
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDate.parse(value, formatter);
                } catch (DateTimeParseException e2) {
                    // Continue to next pattern
                }
            }
        }
        
        return null;
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e1) {
            String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss", 
                "MM/dd/yyyy HH:mm:ss"
            };
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException e2) {
                    // Continue to next pattern
                }
            }
        }
        
        return null;
    }
private static Object getDefaultValue(Class<?> type) {
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == double.class) return 0.0;
    if (type == float.class) return 0.0f;
    if (type == boolean.class) return false;
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == char.class) return '\u0000';
    if (type == String.class) return ""; 
    return null;
}
}