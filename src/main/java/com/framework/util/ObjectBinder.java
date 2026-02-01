package com.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.*;

public class ObjectBinder {
    
    /**
     * Crée et remplit un objet à partir des paramètres de la requête
     */
    public static Object bindObject(Class<?> clazz, HttpServletRequest request, String prefix) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            populateObject(instance, request, prefix);
            return instance;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création de " + clazz.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Remplit un objet existant avec les paramètres de la requête
     */
    private static void populateObject(Object obj, HttpServletRequest request, String prefix) {
        Class<?> clazz = obj.getClass();
        
        // Parcourir tous les paramètres de la requête qui correspondent au préfixe
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String fullParamName = paramNames.nextElement();
            
            // Vérifier si ce paramètre correspond à notre préfixe
            if (prefix != null && !prefix.isEmpty()) {
                // Format: "adresse.rue" ou "emp[0].nom"
                if (fullParamName.startsWith(prefix + ".") || 
                    fullParamName.startsWith(prefix + "[")) {
                    
                    // Extraire le nom de la propriété après le préfixe
                    String propertyPath = extractPropertyPath(fullParamName, prefix);
                    setPropertyByPath(obj, propertyPath, request.getParameter(fullParamName));
                }
            } else {
                // Pas de préfixe, traiter les paramètres directs
                setPropertyByName(obj, fullParamName, request.getParameter(fullParamName));
            }
        }
    }
    
    /**
     * Extrait le chemin de propriété d'un nom de paramètre complet
     */
    private static String extractPropertyPath(String fullParamName, String prefix) {
        if (fullParamName.startsWith(prefix + ".")) {
            return fullParamName.substring(prefix.length() + 1);
        } else if (fullParamName.startsWith(prefix + "[")) {
            // Pour les tableaux/listes: "emp[0].nom" → "[0].nom"
            return fullParamName.substring(prefix.length());
        }
        return fullParamName;
    }
    
    /**
     * Définit une propriété en utilisant un chemin (ex: "adresse.rue")
     */
    private static void setPropertyByPath(Object obj, String propertyPath, String value) {
        String[] parts = propertyPath.split("\\.");
        Object current = obj;
        
        try {
            // Naviguer à travers les objets imbriqués
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                
                // Gérer les indices de tableau/liste: "[0]" ou "adresse"
                if (part.startsWith("[") && part.endsWith("]")) {
                    // C'est un tableau/liste
                    int index = Integer.parseInt(part.substring(1, part.length() - 1));
                    
                    if (current instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) current;
                        while (list.size() <= index) {
                            // Créer l'objet pour cet index
                            // Note: on ne connaît pas le type, donc on utilise Object
                            list.add(new HashMap<String, Object>());
                        }
                        current = list.get(index);
                    } else if (current.getClass().isArray()) {
                        current = Array.get(current, index);
                    }
                } else {
                    // C'est une propriété d'objet
                    current = getProperty(current, part);
                    if (current == null) {
                        // Créer l'objet imbriqué si nécessaire
                        current = createNestedObject(obj, parts, i);
                        setProperty(current, part, obj);
                    }
                }
            }
            
            // Définir la valeur finale
            String finalProperty = parts[parts.length - 1];
            setPropertyValue(current, finalProperty, value);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du setPropertyByPath '" + propertyPath + "': " + e.getMessage());
        }
    }
    
    /**
     * Obtient la valeur d'une propriété
     */
    private static Object getProperty(Object obj, String propertyName) {
        try {
            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (NoSuchMethodException e) {
            // Essayer avec "is" pour les boolean
            try {
                String getterName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
                Method getter = obj.getClass().getMethod(getterName);
                return getter.invoke(obj);
            } catch (Exception e2) {
                // Essayer d'accéder directement au champ
                try {
                    Field field = obj.getClass().getDeclaredField(propertyName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (Exception e3) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Définit la valeur d'une propriété
     */
    private static void setProperty(Object target, String propertyName, Object value) {
        try {
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method setter = target.getClass().getMethod(setterName, value.getClass());
            setter.invoke(target, value);
        } catch (Exception e) {
            try {
                // Essayer d'accéder directement au champ
                Field field = target.getClass().getDeclaredField(propertyName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e2) {
                System.err.println("❌ Impossible de setter " + propertyName + " sur " + 
                                 target.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Crée un objet imbriqué
     */
    private static Object createNestedObject(Object parentObj, String[] pathParts, int currentIndex) {
        try {
            // Déterminer le type de la propriété
            Class<?> parentClass = parentObj.getClass();
            String propertyName = pathParts[currentIndex];
            
            // Chercher le type via le setter ou le champ
            Class<?> propertyType = null;
            
            // Chercher le setter
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            for (Method method : parentClass.getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    propertyType = method.getParameterTypes()[0];
                    break;
                }
            }
            
            // Sinon chercher le champ
            if (propertyType == null) {
                try {
                    Field field = parentClass.getDeclaredField(propertyName);
                    propertyType = field.getType();
                } catch (NoSuchFieldException e) {
                    // Utiliser Object par défaut
                    propertyType = Object.class;
                }
            }
            
            // Créer l'instance
            Object nestedObj = propertyType.getDeclaredConstructor().newInstance();
            
            // Définir la propriété sur le parent
            setProperty(parentObj, propertyName, nestedObj);
            
            return nestedObj;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur création objet imbriqué: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Définit la valeur finale d'une propriété
     */
    private static void setPropertyValue(Object obj, String propertyName, String stringValue) {
        if (stringValue == null || stringValue.trim().isEmpty()) {
            return;
        }
        
        try {
            // Déterminer le type de la propriété
            Class<?> propertyType = null;
            
            // Chercher le setter
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            for (Method method : obj.getClass().getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    propertyType = method.getParameterTypes()[0];
                    break;
                }
            }
            
            if (propertyType == null) {
                // Chercher le champ
                try {
                    Field field = obj.getClass().getDeclaredField(propertyName);
                    propertyType = field.getType();
                } catch (NoSuchFieldException e) {
                    System.err.println("❌ Propriété non trouvée: " + propertyName);
                    return;
                }
            }
            
            // Convertir la valeur
            Object value = convertStringToType(stringValue, propertyType);
            
            // Définir la valeur
            setProperty(obj, propertyName, value);
            
            System.out.println("    ✅ " + propertyName + " = " + value + " (" + propertyType.getSimpleName() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur setPropertyValue '" + propertyName + "': " + e.getMessage());
        }
    }
    
    /**
     * Définit une propriété par nom simple (sans chemin)
     */
    private static void setPropertyByName(Object obj, String propertyName, String value) {
        setPropertyValue(obj, propertyName, value);
    }
    
    /**
     * Convertit une chaîne en type cible
     */
    private static Object convertStringToType(String stringValue, Class<?> targetType) {
        if (stringValue == null) {
            return null;
        }
        
        try {
            if (targetType.equals(String.class)) {
                return stringValue;
            } else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return Boolean.parseBoolean(stringValue);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else {
                // Pour les autres types, retourner la chaîne
                return stringValue;
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Conversion échouée: '" + stringValue + "' en " + targetType.getSimpleName());
            if (targetType.equals(int.class) || targetType.equals(double.class) || 
                targetType.equals(long.class) || targetType.equals(float.class)) {
                return 0;
            } else if (targetType.equals(boolean.class)) {
                return false;
            }
            return null;
        }
    }
    
    /**
     * Crée un tableau d'objets (version simplifiée)
     */
    public static Object bindArray(Class<?> componentType, HttpServletRequest request, String prefix) {
        try {
            // Compter les éléments
            int maxIndex = -1;
            Enumeration<String> paramNames = request.getParameterNames();
            
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (paramName.startsWith(prefix + "[")) {
                    // Extraire l'index: "emp[0].nom" → 0
                    int start = paramName.indexOf('[') + 1;
                    int end = paramName.indexOf(']');
                    if (start > 0 && end > start) {
                        try {
                            int index = Integer.parseInt(paramName.substring(start, end));
                            if (index > maxIndex) {
                                maxIndex = index;
                            }
                        } catch (NumberFormatException e) {
                            // Ignorer
                        }
                    }
                }
            }
            
            if (maxIndex == -1) {
                return Array.newInstance(componentType, 0);
            }
            
            int count = maxIndex + 1;
            Object array = Array.newInstance(componentType, count);
            
            // Créer chaque élément
            for (int i = 0; i < count; i++) {
                Object element = bindObject(componentType, request, prefix + "[" + i + "]");
                Array.set(array, i, element);
            }
            
            System.out.println("    📦 Tableau créé: " + componentType.getSimpleName() + "[" + count + "]");
            return array;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur bindArray: " + e.getMessage());
            return Array.newInstance(componentType, 0);
        }
    }
    
    /**
     * Crée une liste d'objets (version simplifiée)
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> bindList(Class<T> elementType, HttpServletRequest request, String prefix) {
        List<T> list = new ArrayList<>();
        
        try {
            // Compter les éléments comme pour le tableau
            int maxIndex = -1;
            Enumeration<String> paramNames = request.getParameterNames();
            
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (paramName.startsWith(prefix + "[")) {
                    int start = paramName.indexOf('[') + 1;
                    int end = paramName.indexOf(']');
                    if (start > 0 && end > start) {
                        try {
                            int index = Integer.parseInt(paramName.substring(start, end));
                            if (index > maxIndex) {
                                maxIndex = index;
                            }
                        } catch (NumberFormatException e) {
                            // Ignorer
                        }
                    }
                }
            }
            
            if (maxIndex == -1) {
                return list;
            }
            
            // Créer chaque élément
            for (int i = 0; i <= maxIndex; i++) {
                T element = (T) bindObject(elementType, request, prefix + "[" + i + "]");
                if (element != null) {
                    list.add(element);
                }
            }
            
            System.out.println("    📦 Liste créée: " + elementType.getSimpleName() + 
                             " (" + list.size() + " éléments)");
            return list;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur bindList: " + e.getMessage());
            return list;
        }
    }
}