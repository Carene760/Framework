package com.framework.util;

import java.util.*;
import java.util.regex.*;

public class RouteMatcher {
    
    /**
     * Vérifie si une route correspond à un pattern avec paramètres
     * Ex: /user/123 correspond à /user/{id}
     */
    public static boolean matches(String requestPath, String routePattern) {
        // Si c'est une route exacte
        if (!routePattern.contains("{") && !routePattern.contains("}")) {
            return requestPath.equals(routePattern);
        }
        
        // Convertir le pattern en regex
        // Ex: /user/{id} -> /user/([^/]+)
        String regex = routePattern.replaceAll("\\{[^/]+\\}", "([^/]+)");
        regex = "^" + regex + "$";
        
        return requestPath.matches(regex);
    }
    
    /**
     * Extrait les paramètres d'une URL selon un pattern
     * Ex: /user/123 avec pattern /user/{id} -> {"id": "123"}
     */
    public static Map<String, String> extractParameters(String requestPath, String routePattern) {
        Map<String, String> params = new HashMap<>();
        
        if (!routePattern.contains("{")) {
            return params;
        }
        
        // Extraire les noms des paramètres du pattern
        List<String> paramNames = new ArrayList<>();
        Pattern paramPattern = Pattern.compile("\\{([^/]+)\\}");
        Matcher matcher = paramPattern.matcher(routePattern);
        
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        
        // Convertir le pattern en regex
        String regex = routePattern.replaceAll("\\{[^/]+\\}", "([^/]+)");
        regex = "^" + regex + "$";
        
        // Extraire les valeurs
        Pattern valuePattern = Pattern.compile(regex);
        Matcher valueMatcher = valuePattern.matcher(requestPath);
        
        if (valueMatcher.find()) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), valueMatcher.group(i + 1));
            }
        }
        
        return params;
    }
    
    /**
     * Vérifie si un pattern contient des paramètres
     */
    public static boolean isParameterizedRoute(String route) {
        return route.contains("{") && route.contains("}");
    }
}