package com.framework.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.*;
import jakarta.servlet.http.HttpServletRequest;

import com.framework.annotation.Param;

public class RouteMatcher {
    
    /**
     * Vérifie si une route correspond à un pattern avec paramètres
     */
    public static boolean matches(String requestPath, String routePattern) {
        if (!routePattern.contains("{") && !routePattern.contains("}")) {
            return requestPath.equals(routePattern);
        }
        
        String regex = routePattern.replaceAll("\\{[^/]+\\}", "([^/]+)");
        regex = "^" + regex + "$";
        
        return requestPath.matches(regex);
    }
    
    /**
     * Extrait les paramètres d'une URL selon un pattern
     * et les ajoute aux paramètres existants
     */
    public static Map<String, String> extractUrlParameters(String requestPath, String routePattern, 
                                                          HttpServletRequest request) {
        Map<String, String> urlParams = new HashMap<>();
        
        if (!routePattern.contains("{")) {
            return urlParams;
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
                String paramName = paramNames.get(i);
                String paramValue = valueMatcher.group(i + 1);
                urlParams.put(paramName, paramValue);
                
                // Ajouter aussi aux paramètres de la requête pour compatibilité
                // avec request.getParameter()
                if (request != null) {
                    request.setAttribute("urlParam_" + paramName, paramValue);
                }
            }
        }
        
        return urlParams;
    }
    
    /**
     * Combine les paramètres d'URL avec les paramètres GET/POST
     * Priorité: paramètres d'URL > paramètres GET/POST
     */
    public static Map<String, String> combineParameters(
            String requestPath, String routePattern, HttpServletRequest request) {
        
        Map<String, String> allParams = new HashMap<>();
        
        // 1. D'abord les paramètres GET/POST
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            allParams.put(name, request.getParameter(name));
        }
        
        // 2. Ensuite les paramètres d'URL (écrasent les paramètres GET/POST si même nom)
        Map<String, String> urlParams = extractUrlParameters(requestPath, routePattern, request);
        allParams.putAll(urlParams);
        
        return allParams;
    }
    
    /**
     * Vérifie si un pattern contient des paramètres
     */
    public static boolean isParameterizedRoute(String route) {
        return route.contains("{") && route.contains("}");
    }

        /**
     * Nouvelle méthode qui combine paramètres GET/POST et paramètres d'URL
     */
    public static Map<String, Object> resolveParametersWithUrl(
            HttpServletRequest request, 
            Method method, 
            String requestPath,
            String routePattern) {
        
        Map<String, Object> parameterValues = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        // Combiner tous les paramètres (URL + GET/POST)
        Map<String, String> allParams = RouteMatcher.combineParameters(requestPath, routePattern, request);
        
        System.out.println("📋 Tous les paramètres disponibles:");
        allParams.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            String paramAnnotationName = null;
            
            // Vérifier si l'annotation @Param est présente
            if (param.isAnnotationPresent(Param.class)) {
                paramAnnotationName = param.getAnnotation(Param.class).value();
            }
            
            // Chercher la valeur selon la priorité
            Object paramValue = findParameterValueWithPriority(param, paramAnnotationName, paramName, allParams);
            
            parameterValues.put(paramName, paramValue);
        }
        
        return parameterValues;
    }
    
    private static Object findParameterValueWithPriority(
            Parameter param,
            String paramAnnotationName,
            String paramName,
            Map<String, String> allParams) {
        
        Class<?> type = param.getType();
        
        // PRIORITÉ 1: @Param existe ET se trouve dans les paramètres
        if (paramAnnotationName != null && allParams.containsKey(paramAnnotationName)) {
            String value = allParams.get(paramAnnotationName);
            System.out.println("    ✅ " + paramName + " → via @Param('" + paramAnnotationName + "') = " + value);
            return ParameterResolver.convertValue(value, type);
        }
        
        // PRIORITÉ 2: Le nom du paramètre existe dans les paramètres
        if (allParams.containsKey(paramName)) {
            String value = allParams.get(paramName);
            System.out.println("    ✅ " + paramName + " → via nom = " + value);
            return ParameterResolver.convertValue(value, type);
        }
        
        // PRIORITÉ 3: Aucun paramètre trouvé
        System.out.println("    ❌ " + paramName + " → non trouvé");
        return ParameterResolver.getDefaultValue(type);
    }
}