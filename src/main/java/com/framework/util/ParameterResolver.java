package com.framework.util;

import com.framework.annotation.Param;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class ParameterResolver {
    
    /**
     * Résout les paramètres avec support de @Param et priorité
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
        
        for (Parameter param : parameters) {
            String paramName = getParameterName(param);
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
        if (paramAnnotationName != null) {
            if (allParams.containsKey(paramAnnotationName)) {
                String value = allParams.get(paramAnnotationName);
                System.out.println("    ✅ " + paramName + " → via @Param('" + paramAnnotationName + "') = " + value);
                return convertValue(value, type);
            }
            System.out.println("    ⚠️ " + paramName + " → @Param('" + paramAnnotationName + "') non trouvé");
        }
        
        // PRIORITÉ 2: Le nom du paramètre existe dans les paramètres
        if (allParams.containsKey(paramName)) {
            String value = allParams.get(paramName);
            System.out.println("    ✅ " + paramName + " → via nom = " + value);
            return convertValue(value, type);
        }
        
        // PRIORITÉ 3: Aucun paramètre trouvé
        System.out.println("    ❌ " + paramName + " → non trouvé");
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
        // Fallback: utiliser arg0, arg1, etc.
        return "arg" + param.hashCode(); // Simple fallback
    }
    
    /**
     * Convertit une valeur String en type cible
     */
    public static Object convertValue(String stringValue, Class<?> type) {
        if (stringValue == null) {
            return null;
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
            System.err.println("Erreur de conversion pour " + type.getSimpleName() + ": '" + stringValue + "'");
            return null;
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
    public static Object getDefaultValue(Class<?> type) {
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
}