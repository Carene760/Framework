package com.framework.util;

import com.framework.annotation.Param;
import com.framework.annotation.FileUpload;
import com.framework.annotation.SessionAttributes;
import com.framework.annotation.SessionParam;
import com.framework.model.UploadedFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import jakarta.servlet.ServletException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ParameterResolver {
    
    /**
     * Rsout tous les paramtres avec support spcial pour Map
     */
    public static Map<String, Object> resolveAllParameters(
        HttpServletRequest request, 
        Method method, 
        String requestPath,
        String routePattern) {
    
        Map<String, Object> parameterValues = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        // Combiner tous les paramtres disponibles
        Map<String, String> allParams = RouteMatcher.combineAllParameters(requestPath, routePattern, request);
        
        System.out.println(" Tous les paramtres disponibles (URL + GET/POST):");
        allParams.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
        
        // Pour chaque paramtre de la mthode, trouver la valeur
        for (Parameter param : parameters) {
            String paramName = getParameterName(param);
            
            // CAS SPCIAL: @SessionAttributes - injecter toute la session
            if (param.isAnnotationPresent(SessionAttributes.class)) {
                System.out.println("  [SESSION] SessionAttributes detected: " + paramName);
                Object paramValue = handleSessionAttributes(request);
                parameterValues.put(paramName, paramValue);
            }
            // CAS SPCIAL: @SessionParam - injecter un attribut de session
            else if (param.isAnnotationPresent(SessionParam.class)) {
                SessionParam annotation = param.getAnnotation(SessionParam.class);
                String sessionKey = annotation.value().isEmpty() ? paramName : annotation.value();
                System.out.println("  [SESSION] SessionParam detected: " + sessionKey);
                Object paramValue = handleSessionParam(request, sessionKey, annotation.required());
                parameterValues.put(paramName, paramValue);
            }
            // CAS SPCIAL: Si le paramtre est une Map
            else if (isMapParameter(param)) {
                System.out.println("    Paramtre Map dtect: " + paramName);
                Object paramValue = handleMapParameter(request, param);
                parameterValues.put(paramName, paramValue);
            } 
            // CAS NORMAL: Autres paramtres
            else {
                // PASSER LA REQUEST POUR LES OBJETS
                Object paramValue = findParameterValue(param, paramName, allParams, request);
                parameterValues.put(paramName, paramValue);
            }
        }
        
        return parameterValues;
    }
    
    /**
     * Vrifie si un paramtre est de type Map
     */
    private static boolean isMapParameter(Parameter param) {
        Class<?> type = param.getType();
        return Map.class.isAssignableFrom(type);
    }
    
    /**
     * Vrifie si un paramtre est de type Model (pour compatibilit future)
     */
    private static boolean isModelParameter(Parameter param) {
        //  implmenter si on ajoute une classe Model
        return false;
    }
    
    /**
     * Gre un paramtre de type Map
     * Rcupre tous les paramtres de la requte et les met dans la Map
     */
    private static Object handleMapParameter(HttpServletRequest request, Parameter param) {
        Map<String, Object> paramMap = new HashMap<>();
        
        // Rcuprer tous les paramtres de la requte
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String[] values = request.getParameterValues(name);
            
            // Cas spcial pour les checkbox: si plusieurs valeurs
            if (values != null && values.length > 1) {
                paramMap.put(name, Arrays.asList(values));
                System.out.println("     " + name + " = " + Arrays.toString(values) + " (liste)");
            } 
            // Cas normal: une seule valeur
            else if (values != null && values.length == 1) {
                paramMap.put(name, values[0]);
                System.out.println("     " + name + " = " + values[0]);
            }
        }
        
        // Ajouter aussi les attributs de la requte
        Enumeration<String> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = attrNames.nextElement();
            paramMap.put(name, request.getAttribute(name));
        }
        
        // Retourner la Map type selon le paramtre
        return convertMapToTypedMap(paramMap, param);
    }
    
    /**
     * Convertit une Map simple en Map type selon la dclaration
     */
    @SuppressWarnings("unchecked")
    private static Object convertMapToTypedMap(Map<String, Object> paramMap, Parameter param) {
        Class<?> type = param.getType();
        
        // Si c'est une Map<String, String>
        if (type.equals(Map.class)) {
            // On retourne la Map telle quelle (brute)
            return paramMap;
        }
        
        // Sinon on essaie de crer une instance du type spcifi
        try {
            Map<String, Object> typedMap = (Map<String, Object>) type.getDeclaredConstructor().newInstance();
            typedMap.putAll(paramMap);
            return typedMap;
        } catch (Exception e) {
            System.err.println(" Impossible de crer une Map type: " + type.getName());
            return paramMap;
        }
    }
    
    /**
     * Gre un paramtre Model (pour compatibilit future)
     */
    private static Object handleModelParameter(HttpServletRequest request) {
        //  implmenter quand on aura une classe Model
        return null;
    }
    
    /**
     * Gre @SessionAttributes - retourne Map<String, Object> de toute la session
     */
    private static Map<String, Object> handleSessionAttributes(HttpServletRequest request) {
        Map<String, Object> sessionMap = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                sessionMap.put(name, session.getAttribute(name));
                System.out.println("    [SESSION] " + name + " = " + session.getAttribute(name));
            }
        }
        
        return sessionMap;
    }
    
    /**
     * Gre @SessionParam - retourne un attribut spcifique de la session
     */
    private static Object handleSessionParam(HttpServletRequest request, String sessionKey, boolean required) {
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            Object value = session.getAttribute(sessionKey);
            if (value != null) {
                System.out.println("    [SESSION] " + sessionKey + " = " + value);
                return value;
            }
        }
        
        if (required) {
            System.err.println("[ERROR] Required session attribute not found: " + sessionKey);
        }
        
        return null;
    }
    
    /**
     * Trouve la valeur d'un paramtre avec support des objets complexes
     */
    private static Object findParameterValue(
        Parameter param,
        String paramName,
        Map<String, String> allParams,
        HttpServletRequest request) {
    
        Class<?> type = param.getType();
        String paramAnnotationName = null;
        // Support for file upload parameters
        if (param.isAnnotationPresent(FileUpload.class) || UploadedFile.class.equals(type) ||
            (type.isArray() && UploadedFile.class.equals(type.getComponentType()))) {
            FileUpload fu = param.getAnnotation(FileUpload.class);
            String fieldName = (fu != null && !fu.value().isEmpty()) ? fu.value() : paramName;
            try {
                if (UploadedFile.class.equals(type)) {
                    return handleSingleFileUpload(request, fieldName);
                } else if (type.isArray() && UploadedFile.class.equals(type.getComponentType())) {
                    return handleMultipleFileUpload(request, fieldName);
                }
            } catch (Exception e) {
                System.err.println(" Erreur upload fichier: " + e.getMessage());
            }
        }
        
        // Vrifier si l'annotation @Param est prsente
        if (param.isAnnotationPresent(Param.class)) {
            paramAnnotationName = param.getAnnotation(Param.class).value();
            System.out.print("   " + paramName + "  cherche avec @Param('" + paramAnnotationName + "')");
            
            // PRIORIT 1: Chercher avec le nom spcifi dans @Param
            if (allParams.containsKey(paramAnnotationName)) {
                String value = allParams.get(paramAnnotationName);
                System.out.println("  TROUV = " + value);
                return convertValue(value, type);
            }
            System.out.println("  NON TROUV, essaie avec nom du paramtre");
        } else {
            System.out.print("   " + paramName + "  cherche avec nom");
        }
        
        // PRIORIT 2: Si c'est un objet complexe (POJO)  utiliser le binder interne
        if (isComplexObject(type)) {
            System.out.println("  CRATION D'OBJET " + type.getSimpleName());
            // Utiliser notre binding interne qui supporte les noms avec ou sans prfixe
            return bindComplexObject(type, request, paramName);
        }
        
        // PRIORIT 3: Si c'est un tableau d'objets
        if (type.isArray() && isComplexObject(type.getComponentType())) {
            System.out.println("  CRATION DE TABLEAU " + type.getComponentType().getSimpleName() + "[]");
            return ObjectBinder.bindArray(type.getComponentType(), request, paramName);
        }
        
        // PRIORIT 4: Si c'est une liste d'objets
        if (type.equals(List.class) || type.equals(ArrayList.class)) {
            // Obtenir le type gnrique
            Type genericType = param.getParameterizedType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    Class<?> elementType = (Class<?>) typeArgs[0];
                    if (isComplexObject(elementType)) {
                        System.out.println("  CRATION DE LISTE " + elementType.getSimpleName());
                        return ObjectBinder.bindList(elementType, request, paramName);
                    }
                }
            }
        }
        
        // PRIORIT 5: Chercher avec le nom du paramtre (types simples)
        if (allParams.containsKey(paramName)) {
            String value = allParams.get(paramName);
            System.out.println("  TROUV = " + value);
            return convertValue(value, type);
        }
        
        // PRIORIT 6: Aucune correspondance
        System.out.println("  NON TROUV");
        return getDefaultValue(type);
    }

    /**
     * Cre et remplit un objet complexe
     */
    private static Object bindComplexObject(Class<?> type, HttpServletRequest request, String prefix) {
        try {
            System.out.println("      Construction de l'objet: " + type.getSimpleName());
            
            // Crer l'instance
            Object instance = type.getDeclaredConstructor().newInstance();
            
            // Remplir avec les paramtres de la requte
            populateComplexObject(instance, request, prefix);
            
            return instance;
        } catch (Exception e) {
            System.err.println(" Erreur lors de la cration de " + type.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Lit un seul fichier upload pour le champ `fieldName`.
     */
    private static UploadedFile handleSingleFileUpload(HttpServletRequest request, String fieldName) throws IOException, ServletException {
        try {
            Part part = request.getPart(fieldName);
            if (part == null) {
                // essayer  chercher parmi les parts si le champ n'existe pas exact
                for (Part p : request.getParts()) {
                    if (p.getName().equals(fieldName)) { part = p; break; }
                }
            }
            if (part == null) return null;

            UploadedFile uploaded = new UploadedFile();
            String fileName = part.getSubmittedFileName();
            uploaded.setFileName(fileName);
            uploaded.setContentType(part.getContentType());
            uploaded.setSize(part.getSize());

            try (InputStream in = part.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                uploaded.setContent(baos.toByteArray());
            }

            System.out.println("     Fichier upload: " + uploaded);
            return uploaded;
        } catch (IllegalStateException e) {
            System.err.println(" Taille fichier trop grande: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lit plusieurs fichiers uploads pour le champ `fieldName`.
     */
    private static UploadedFile[] handleMultipleFileUpload(HttpServletRequest request, String fieldName) throws IOException, ServletException {
        List<UploadedFile> list = new ArrayList<>();
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
            if (!part.getName().equals(fieldName)) continue;
            if (part.getSubmittedFileName() == null) continue;

            UploadedFile uploaded = new UploadedFile();
            uploaded.setFileName(part.getSubmittedFileName());
            uploaded.setContentType(part.getContentType());
            uploaded.setSize(part.getSize());

            try (InputStream in = part.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                uploaded.setContent(baos.toByteArray());
            }
            list.add(uploaded);
            System.out.println("     Fichier upload: " + uploaded);
        }

        return list.toArray(new UploadedFile[0]);
    }

    /**
     * Remplit un objet complexe avec les paramtres
     */
    private static void populateComplexObject(Object obj, HttpServletRequest request, String prefix) {
        Class<?> clazz = obj.getClass();
        
        // Parcourir les setters
        for (Method method : clazz.getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                String propertyName = method.getName().substring(3);
                propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                
                String paramName = (prefix != null && !prefix.isEmpty()) ? 
                    prefix + "." + propertyName : propertyName;
                
                Class<?> paramType = method.getParameterTypes()[0];
                Object paramValue = getParameterForObject(request, paramName, paramType, propertyName);
                
                if (paramValue != null) {
                    try {
                        method.invoke(obj, paramValue);
                        System.out.println("       " + propertyName + " = " + paramValue);
                    } catch (Exception e) {
                        System.err.println(" Erreur setter " + method.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Obtient un paramtre pour un objet
     */
    private static Object getParameterForObject(HttpServletRequest request, String paramName, 
                                            Class<?> type, String propertyName) {
        String stringValue = request.getParameter(paramName);

        // Fallback: si la requte n'utilise pas le prfixe (ex: "nom" au lieu de "emp.nom"),
        // essayer aussi avec le nom de la proprit seule.
        if ((stringValue == null || stringValue.trim().isEmpty()) && propertyName != null) {
            stringValue = request.getParameter(propertyName);
        }

        if (stringValue == null || stringValue.trim().isEmpty()) {
            // Vrifier si c'est un objet imbriqu  essayer avec les deux prfixes possibles
            if (isComplexObject(type)) {
                System.out.println("       Objet imbriqu dtect: " + type.getSimpleName());
                // PRIORIT 1: essayer avec le nom de la proprit seul (adresse.rue, departement.code)
                Object nested = bindComplexObject(type, request, propertyName);
                if (nested != null) {
                    System.out.println("       Objet trouv avec prfixe: " + propertyName);
                    return nested;
                }
                // PRIORIT 2: essayer avec le chemin complet (emp.adresse.rue)
                System.out.println("       Essai avec chemin complet: " + paramName);
                return bindComplexObject(type, request, paramName);
            }
            return null;
        }
        
        // Conversion pour les types simples
        try {
            if (type.equals(String.class)) {
                return stringValue;
            } else if (type.equals(Integer.class) || type.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (type.equals(Double.class) || type.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                return Boolean.parseBoolean(stringValue);
            } else if (type.equals(Long.class) || type.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (type.equals(Float.class) || type.equals(float.class)) {
                return Float.parseFloat(stringValue);
            }
        } catch (NumberFormatException e) {
            System.err.println(" Conversion choue pour " + paramName + ": '" + stringValue + "'");
        }
        
        return null;
    }
    
    private static String getParameterName(Parameter param) {
        // 1. Si le nom est disponible (avec -parameters)
        if (param.isNamePresent()) {
            return param.getName();
        }
        
        // 2. Si annotation @Param prsente
        if (param.isAnnotationPresent(Param.class)) {
            return param.getAnnotation(Param.class).value();
        }
        
        // 3. Pour les Map, nom gnrique
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
            System.err.println(" Erreur de conversion: '" + stringValue + "' en " + type.getSimpleName());
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

    private static boolean isComplexObject(Class<?> type) {
        // Exclure les types simples, les tableaux, les collections, les Maps
        return !type.isPrimitive() && 
            !type.isArray() && 
            !type.equals(String.class) &&
            !Number.class.isAssignableFrom(type) &&
            !type.equals(Boolean.class) &&
            !Map.class.isAssignableFrom(type) &&
            !List.class.isAssignableFrom(type) &&
            !Set.class.isAssignableFrom(type) &&
            !type.getName().startsWith("java.") &&
            !type.getName().startsWith("javax.");
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
        System.out.println(" Arguments pour " + method.getName() + ":");
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
                valueInfo = "Map avec " + map.size() + " entres";
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


