package com.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.*;

public class RouteMatcher {
    
    /**
     * Vrifie si une route correspond  un pattern avec paramtres
     */
    public static boolean matches(String requestPath, String routePattern) {
        if (!routePattern.contains("{") && !routePattern.contains("}")) {
            return requestPath.equals(routePattern);
        }
        
        String regex = convertPatternToRegex(routePattern);
        return requestPath.matches(regex);
    }
    
    /**
     * Convertit un pattern comme /etudiant/{id} en regex /etudiant/([^/]+)
     */
    private static String convertPatternToRegex(String pattern) {
        String regex = pattern.replaceAll("\\{[^/]+\\}", "([^/]+)");
        return "^" + regex + "$";
    }
    
    /**
     * Extrait les valeurs des paramtres d'URL et les retourne dans une Map
     * Ex: /etudiant/25 avec pattern /etudiant/{id}  {"id": "25"}
     */
    public static Map<String, String> extractUrlParameters(String requestPath, String routePattern) {
        Map<String, String> urlParams = new HashMap<>();
        
        if (!routePattern.contains("{")) {
            return urlParams;
        }
        
        // Extraire les noms des paramtres du pattern
        List<String> paramNames = new ArrayList<>();
        Pattern paramPattern = Pattern.compile("\\{([^/]+)\\}");
        Matcher nameMatcher = paramPattern.matcher(routePattern);
        
        while (nameMatcher.find()) {
            paramNames.add(nameMatcher.group(1));
        }
        
        // Convertir le pattern en regex et extraire les valeurs
        String regex = convertPatternToRegex(routePattern);
        Pattern valuePattern = Pattern.compile(regex);
        Matcher valueMatcher = valuePattern.matcher(requestPath);
        
        if (valueMatcher.find()) {
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                String paramValue = valueMatcher.group(i + 1);
                urlParams.put(paramName, paramValue);
            }
        }
        
        return urlParams;
    }
    
    /**
     * Combine tous les paramtres : URL (prioritaire) + GET/POST
     */
    public static Map<String, String> combineAllParameters(
            String requestPath, String routePattern, HttpServletRequest request) {
        
        Map<String, String> allParams = new HashMap<>();
        
        // 1. D'abord les paramtres GET/POST (priorit basse)
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            allParams.put(name, request.getParameter(name));
        }
        
        // 2. Ensuite les paramtres d'URL (priorit haute - crasent les GET/POST)
        Map<String, String> urlParams = extractUrlParameters(requestPath, routePattern);
        allParams.putAll(urlParams); // Les paramtres d'URL crasent les GET/POST
        
        return allParams;
    }
    
    /**
     * Vrifie si un pattern contient des paramtres
     */
    public static boolean isParameterizedRoute(String route) {
        return route.contains("{") && route.contains("}");
    }
    
    /**
     * Affiche les paramtres extraits pour le dbogage
     */
    public static void debugUrlExtraction(String requestPath, String routePattern) {
        Map<String, String> urlParams = extractUrlParameters(requestPath, routePattern);
        if (!urlParams.isEmpty()) {
            System.out.println(" Paramtres extraits de l'URL:");
            urlParams.forEach((k, v) -> System.out.println("  - {" + k + "} = " + v));
        }
    }
}


