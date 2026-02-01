package com.framework.util;

import com.framework.annotation.Param;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ParameterResolver {
    
    /**
     * Résout tous les paramètres avec support spécial pour Map
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
            
            // CAS SPÉCIAL: Si le paramètre est une Map
            if (isMapParameter(param)) {
                System.out.println("  🗺️  Paramètre Map détecté: " + paramName);
                Object paramValue = handleMapParameter(request, param);
                parameterValues.put(paramName, paramValue);
            } 
            // CAS SPÉCIAL: Si le paramètre est un Model (pour les vues)
            else if (isModelParameter(param)) {
                System.out.println("  🎨 Paramètre Model détecté: " + paramName);
                Object paramValue = handleModelParameter(request);
                parameterValues.put(paramName, paramValue);
            }
            // CAS NORMAL: Paramètre simple
            else {
                Object paramValue = findParameterValue(param, paramName, allParams);
                parameterValues.put(paramName, paramValue);
            }
        }
        
        return parameterValues;
    }
    
    /**
     * Vérifie si un paramètre est de type Map
     */
    private static boolean isMapParameter(Parameter param) {
        Class<?> type = param.getType();
        return Map.class.isAssignableFrom(type);
    }
    
    /**
     * Vérifie si un paramètre est de type Model (pour compatibilité future)
     */
    private static boolean isModelParameter(Parameter param) {
        // À implémenter si on ajoute une classe Model
        return false;
    }
    
    /**
     * Gère un paramètre de type Map
     * Récupère tous les paramètres de la requête et les met dans la Map
     */
    private static Object handleMapParameter(HttpServletRequest request, Parameter param) {
        Map<String, Object> paramMap = new HashMap<>();
        
        // Récupérer tous les paramètres de la requête
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String[] values = request.getParameterValues(name);
            
            // Cas spécial pour les checkbox: si plusieurs valeurs
            if (values != null && values.length > 1) {
                paramMap.put(name, Arrays.asList(values));
                System.out.println("    📦 " + name + " = " + Arrays.toString(values) + " (liste)");
            } 
            // Cas normal: une seule valeur
            else if (values != null && values.length == 1) {
                paramMap.put(name, values[0]);
                System.out.println("    📦 " + name + " = " + values[0]);
            }
        }
        
        // Ajouter aussi les attributs de la requête
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = attrNames.nextElement();
            paramMap.put(name, request.getAttribute(name));
        }
        
        // Retourner la Map typée selon le paramètre
        return convertMapToTypedMap(paramMap, param);
    }
    
    /**
     * Convertit une Map simple en Map typée selon la déclaration
     */
    @SuppressWarnings("unchecked")
    private static Object convertMapToTypedMap(Map<String, Object> paramMap, Parameter param) {
        Class<?> type = param.getType();
        
        // Si c'est une Map<String, String>
        if (type.equals(Map.class)) {
            // On retourne la Map telle quelle (brute)
            return paramMap;
        }
        
        // Sinon on essaie de créer une instance du type spécifié
        try {
            Map<String, Object> typedMap = (Map<String, Object>) type.getDeclaredConstructor().newInstance();
            typedMap.putAll(paramMap);
            return typedMap;
        } catch (Exception e) {
            System.err.println("❌ Impossible de créer une Map typée: " + type.getName());
            return paramMap;
        }
    }
    
    /**
     * Gère un paramètre Model (pour compatibilité future)
     */
    private static Object handleModelParameter(HttpServletRequest request) {
        // À implémenter quand on aura une classe Model
        return null;
    }
    
    // [Les autres méthodes restent inchangées...]
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
    
    private static String getParameterName(Parameter param) {
        // 1. Si le nom est disponible (avec -parameters)
        if (param.isNamePresent()) {
            return param.getName();
        }
        
        // 2. Si annotation @Param présente
        if (param.isAnnotationPresent(Param.class)) {
            return param.getAnnotation(Param.class).value();
        }
        
        // 3. Pour les Map, nom générique
        if (Map.class.isAssignableFrom(param.getType())) {
            return "formData";
        }
        
        // 4. Fallback: arg0, arg1, etc.
        return "arg" + Arrays.asList(param.getDeclaringExecutable().getParameters()).indexOf(param);
    }
    
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
            String valueInfo;
            
            if (args[i] instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) args[i];
                valueInfo = "Map avec " + map.size() + " entrées";
                if (!map.isEmpty()) {
                    valueInfo += ": " + map.keySet();
                }
            } else {
                valueInfo = args[i] != null ? 
                    args[i] + " (" + args[i].getClass().getSimpleName() + ")" : 
                    "null";
            }
                
            System.out.println("  [" + i + "] " + typeName + " " + paramInfo + " = " + valueInfo);
        }
    }
}