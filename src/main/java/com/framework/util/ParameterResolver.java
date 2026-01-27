package com.framework.util;

import com.framework.annotation.Param;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class ParameterResolver {
    
    /**
     * Récupère les valeurs des paramètres pour une méthode donnée
     * @param request La requête HTTP
     * @param method La méthode du contrôleur
     * @return Map des valeurs des paramètres
     */
    public static Map<String, Object> resolveParameters(HttpServletRequest request, Method method) {
        Map<String, Object> parameterValues = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = getParameterName(param, i);
            Object paramValue = getParameterValue(request, param, paramName);
            
            parameterValues.put(param.getName(), paramValue);
        }
        
        return parameterValues;
    }
    
    /**
     * Détermine le nom du paramètre
     */
    private static String getParameterName(Parameter param, int index) {
        // 1. Vérifier si l'annotation @Param est présente
        if (param.isAnnotationPresent(Param.class)) {
            return param.getAnnotation(Param.class).value();
        }
        
        // 2. Sinon, utiliser le nom du paramètre
        // Note: nécessite -parameters lors de la compilation pour avoir les noms
        if (param.isNamePresent()) {
            return param.getName();
        }
        
        // 3. Fallback: utiliser un nom générique
        return "arg" + index;
    }
    
    /**
     * Récupère la valeur d'un paramètre depuis la requête
     */
    private static Object getParameterValue(HttpServletRequest request, Parameter param, String paramName) {
        String stringValue = request.getParameter(paramName);
        
        if (stringValue == null || stringValue.trim().isEmpty()) {
            return null;
        }
        
        // Convertir selon le type du paramètre
        Class<?> type = param.getType();
        
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
                // Pour les autres types, retourner la chaîne
                return stringValue;
            }
        } catch (NumberFormatException e) {
            // En cas d'erreur de conversion, retourner null
            System.err.println("Erreur de conversion pour le paramètre " + paramName + 
                             ": '" + stringValue + "' en " + type.getSimpleName());
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
            String paramName = param.getName();
            args[i] = parameterValues.getOrDefault(paramName, null);
        }
        
        return args;
    }
}