package com.framework.util;

import com.framework.annotation.Param;
import com.framework.annotation.FileUpload;
import com.framework.model.UploadedFile;
import jakarta.servlet.http.HttpServletRequest;
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
     * Résout tous les paramètres avec support spécial pour Map
     */
    public static Map<String, Object> resolveAllParameters(
        HttpServletRequest request, 
        Method method, 
        String requestPath,
        String routePattern) {
    
        Map<String, Object> parameterValues = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        // Combiner tous les paramètres disponibles
        Map<String, String> allParams = RouteMatcher.combineAllParameters(requestPath, routePattern, request);
        
        System.out.println("📋 Tous les paramètres disponibles (URL + GET/POST):");
        allParams.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
        
        // Pour chaque paramètre de la méthode, trouver la valeur
        for (Parameter param : parameters) {
            String paramName = getParameterName(param);
            
            // CAS SPÉCIAL: Si le paramètre est une Map
            if (isMapParameter(param)) {
                System.out.println("  🗺️  Paramètre Map détecté: " + paramName);
                Object paramValue = handleMapParameter(request, param);
                parameterValues.put(paramName, paramValue);
            } 
            // CAS NORMAL: Autres paramètres
            else {
                // PASSER LA REQUEST POUR LES OBJETS
                Object paramValue = findParameterValue(param, paramName, allParams, request);
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
    
    /**
     * Trouve la valeur d'un paramètre avec support des objets complexes
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
                System.err.println("❌ Erreur upload fichier: " + e.getMessage());
            }
        }
        
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
        
        // PRIORITÉ 2: Si c'est un objet complexe (POJO) → utiliser le binder interne
        if (isComplexObject(type)) {
            System.out.println(" → CRÉATION D'OBJET " + type.getSimpleName());
            // Utiliser notre binding interne qui supporte les noms avec ou sans préfixe
            return bindComplexObject(type, request, paramName);
        }
        
        // PRIORITÉ 3: Si c'est un tableau d'objets
        if (type.isArray() && isComplexObject(type.getComponentType())) {
            System.out.println(" → CRÉATION DE TABLEAU " + type.getComponentType().getSimpleName() + "[]");
            return ObjectBinder.bindArray(type.getComponentType(), request, paramName);
        }
        
        // PRIORITÉ 4: Si c'est une liste d'objets
        if (type.equals(List.class) || type.equals(ArrayList.class)) {
            // Obtenir le type générique
            Type genericType = param.getParameterizedType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    Class<?> elementType = (Class<?>) typeArgs[0];
                    if (isComplexObject(elementType)) {
                        System.out.println(" → CRÉATION DE LISTE " + elementType.getSimpleName());
                        return ObjectBinder.bindList(elementType, request, paramName);
                    }
                }
            }
        }
        
        // PRIORITÉ 5: Chercher avec le nom du paramètre (types simples)
        if (allParams.containsKey(paramName)) {
            String value = allParams.get(paramName);
            System.out.println(" → TROUVÉ = " + value);
            return convertValue(value, type);
        }
        
        // PRIORITÉ 6: Aucune correspondance
        System.out.println(" → NON TROUVÉ");
        return getDefaultValue(type);
    }

    /**
     * Crée et remplit un objet complexe
     */
    private static Object bindComplexObject(Class<?> type, HttpServletRequest request, String prefix) {
        try {
            System.out.println("    🏗️  Construction de l'objet: " + type.getSimpleName());
            
            // Créer l'instance
            Object instance = type.getDeclaredConstructor().newInstance();
            
            // Remplir avec les paramètres de la requête
            populateComplexObject(instance, request, prefix);
            
            return instance;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de " + type.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Lit un seul fichier uploadé pour le champ `fieldName`.
     */
    private static UploadedFile handleSingleFileUpload(HttpServletRequest request, String fieldName) throws IOException, ServletException {
        try {
            Part part = request.getPart(fieldName);
            if (part == null) {
                // essayer à chercher parmi les parts si le champ n'existe pas exact
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

            System.out.println("    ✅ Fichier uploadé: " + uploaded);
            return uploaded;
        } catch (IllegalStateException e) {
            System.err.println("❌ Taille fichier trop grande: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lit plusieurs fichiers uploadés pour le champ `fieldName`.
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
            System.out.println("    ✅ Fichier uploadé: " + uploaded);
        }

        return list.toArray(new UploadedFile[0]);
    }

    /**
     * Remplit un objet complexe avec les paramètres
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
                        System.out.println("      📝 " + propertyName + " = " + paramValue);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur setter " + method.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Obtient un paramètre pour un objet
     */
    private static Object getParameterForObject(HttpServletRequest request, String paramName, 
                                            Class<?> type, String propertyName) {
        String stringValue = request.getParameter(paramName);

        // Fallback: si la requête n'utilise pas le préfixe (ex: "nom" au lieu de "emp.nom"),
        // essayer aussi avec le nom de la propriété seule.
        if ((stringValue == null || stringValue.trim().isEmpty()) && propertyName != null) {
            stringValue = request.getParameter(propertyName);
        }

        if (stringValue == null || stringValue.trim().isEmpty()) {
            // Vérifier si c'est un objet imbriqué — essayer avec les deux préfixes possibles
            if (isComplexObject(type)) {
                System.out.println("      💭 Objet imbriqué détecté: " + type.getSimpleName());
                // PRIORITÉ 1: essayer avec le nom de la propriété seul (adresse.rue, departement.code)
                Object nested = bindComplexObject(type, request, propertyName);
                if (nested != null) {
                    System.out.println("      ✅ Objet trouvé avec préfixe: " + propertyName);
                    return nested;
                }
                // PRIORITÉ 2: essayer avec le chemin complet (emp.adresse.rue)
                System.out.println("      💭 Essai avec chemin complet: " + paramName);
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
            System.err.println("❌ Conversion échouée pour " + paramName + ": '" + stringValue + "'");
        }
        
        return null;
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