package com.framework.util;

import com.framework.annotation.Param;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class ParameterResolver {
    
    /**
     * Résout tous les paramètres : URL + GET/POST avec priorité
     */
    public static Map<String, Object> resolveAllParameters(
            HttpServletRequest request, 
            Method method, 
            String requestPath,
            String routePattern) {
        
        Map<String, Object> parameterValues = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        // 1. Combiner tous les paramètres disponibles
        Map<String, String> allParams = RouteMatcher.combineAllParameters(requestPath, routePattern, request);
        
        // Debug: afficher ce qui a été extrait
        RouteMatcher.debugUrlExtraction(requestPath, routePattern);
        
        System.out.println("📋 Tous les paramètres disponibles (URL + GET/POST):");
        allParams.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
        
        // 2. Pour chaque paramètre de la méthode, trouver la valeur
        for (Parameter param : parameters) {
            String paramName = getParameterName(param);
            Object paramValue = findParameterValue(param, paramName, allParams);
            parameterValues.put(paramName, paramValue);
        }
        
        return parameterValues;
    }
    
    /**
     * Trouve la valeur d'un paramètre selon la priorité
     */
    private static Object findParameterValue(
            Parameter param,
            String paramName,
            Map<String, String> allParams) {
        
        Class<?> type = param.getType();
        String paramAnnotationName = null;
        
        // Vérifier si l'annotation @Param est présente
        if (param.isAnnotationPresent(Param.class)) {
            paramAnnotationName = param.getAnnotation(Param.class).value();
            System.out.print("  🔍 " + paramName + " → cherche avec @Param('" + paramAnnotationName + "')");
            
            // PRIORITÉ 1: Chercher avec le nom spécifié dans @Param
            if (allParams.containsKey(paramAnnotationName)) {
                String value = allParams.get(paramAnnotationName);
                System.out.println(" → TROUVÉ = " + value);
                return convertValue(value, type);
            }
            System.out.println(" → NON TROUVÉ, essaie avec nom du paramètre");
        } else {
            System.out.print("  🔍 " + paramName + " → cherche avec nom");
        }
        
        // PRIORITÉ 2: Chercher avec le nom du paramètre
        if (allParams.containsKey(paramName)) {
            String value = allParams.get(paramName);
            System.out.println(" → TROUVÉ = " + value);
            return convertValue(value, type);
        }
        
        // PRIORITÉ 3: Aucune correspondance
        System.out.println(" → NON TROUVÉ");
        return getDefaultValue(type);
    }
    
    /**
     * Obtient le nom réel d'un paramètre
     */
    private static String getParameterName(Parameter param) {
        // Java garde les noms seulement si compilé avec -parameters
        if (param.isNamePresent()) {
            return param.getName();
        }
        // Fallback: utiliser arg0, arg1, etc. basé sur l'index
        // (Cette partie sera améliorée si nécessaire)
        return "arg" + Arrays.asList(param.getDeclaringExecutable().getParameters()).indexOf(param);
    }
    
    /**
     * Convertit une valeur String en type cible
     */
    private static Object convertValue(String stringValue, Class<?> type) {
        if (stringValue == null) {
            return getDefaultValue(type);
        }
        
        try {
            if (type.equals(String.class)) {
                return stringValue;
            } else if (type.equals(Integer.class) || type.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (type.equals(Long.class) || type.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (type.equals(Double.class) || type.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (type.equals(Float.class) || type.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                return Boolean.parseBoolean(stringValue);
            } else {
                return stringValue;
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Erreur de conversion: '" + stringValue + "' en " + type.getSimpleName());
            return getDefaultValue(type);
        }
    }
    
    /**
     * Convertit une Map de valeurs en tableau d'objets pour l'invocation de méthode
     */
    public static Object[] prepareArguments(Method method, Map<String, Object> parameterValues) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = getParameterName(param);
            args[i] = parameterValues.getOrDefault(paramName, getDefaultValue(param.getType()));
        }
        
        return args;
    }
    
    /**
     * Retourne la valeur par défaut pour un type
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type.equals(int.class) || type.equals(long.class) || 
            type.equals(double.class) || type.equals(float.class)) {
            return 0;
        } else if (type.equals(boolean.class)) {
            return false;
        } else if (type.equals(char.class)) {
            return '\0';
        } else {
            return null;
        }
    }
    
    /**
     * Affiche un résumé des arguments pour le débogage
     */
    public static void debugArguments(Method method, Object[] args) {
        System.out.println("🎯 Arguments pour " + method.getName() + ":");
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < args.length; i++) {
            Parameter param = parameters[i];
            String paramName = getParameterName(param);
            String paramInfo = paramName;
            
            if (param.isAnnotationPresent(Param.class)) {
                paramInfo += " @Param('" + param.getAnnotation(Param.class).value() + "')";
            }
            
            String typeName = param.getType().getSimpleName();
            String valueInfo = args[i] != null ? 
                args[i] + " (" + args[i].getClass().getSimpleName() + ")" : 
                "null";
                
            System.out.println("  [" + i + "] " + typeName + " " + paramInfo + " = " + valueInfo);
        }
    }
}