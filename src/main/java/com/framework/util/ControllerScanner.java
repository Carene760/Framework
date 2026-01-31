package com.framework.util;

import com.framework.annotation.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class ControllerScanner {

    private final String packageName;
    private final Map<String, MethodRoute> routes = new HashMap<>();
    private final List<MethodRoute> parameterizedRoutes = new ArrayList<>();

    public ControllerScanner(String packageName) {
        this.packageName = packageName;
    }

    public Map<String, MethodRoute> getRoutes() {
        return routes;
    }
    
    public List<MethodRoute> getParameterizedRoutes() {
        return parameterizedRoutes;
    }

    public void afficherLesControllersEtRoutes() throws Exception {
        List<Class<?>> classes = getClassesInPackageRecursively(packageName);

        System.out.println("\n=== Liste des contrôleurs et routes trouvés ===");
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                System.out.println("🧩 Contrôleur : " + clazz.getName());

                // Récupérer le préfixe de chemin depuis @RequestMapping sur la classe
                String classPathPrefix = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    classPathPrefix = clazz.getAnnotation(RequestMapping.class).value();
                    System.out.println("  📁 Préfixe de chemin: " + classPathPrefix);
                }

                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                // Scanner toutes les méthodes
                for (Method method : clazz.getDeclaredMethods()) {
                    processMethod(method, controllerInstance, classPathPrefix);
                }
            }
        }
        System.out.println("==============================================\n");
    }
    
    private void processMethod(Method method, Object controllerInstance, String classPathPrefix) {
        String routePath = "";
        String httpMethod = "GET";
        
        // Détecter l'annotation de mapping
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            routePath = mapping.value();
            httpMethod = "GET";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            routePath = mapping.value();
            httpMethod = "POST";
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            routePath = mapping.value();
            httpMethod = mapping.method();
        } else if (method.isAnnotationPresent(Url.class)) {
            // Support rétro-compatible avec @Url
            Url mapping = method.getAnnotation(Url.class);
            routePath = mapping.value();
            httpMethod = mapping.method();
        } else {
            // Pas une méthode de route
            return;
        }
        
        // CORRECTION : Combiner le préfixe de classe et le chemin de la méthode
        String fullPath = combinePaths(classPathPrefix, routePath);
        
        // Normaliser le chemin (supprimer les doubles slash)
        fullPath = normalizePath(fullPath);
        
        System.out.println("  🛣️  Chemin combiné: '" + classPathPrefix + "' + '" + routePath + "' = '" + fullPath + "'");
        
        // Vérifier si c'est une route paramétrée
        if (RouteMatcher.isParameterizedRoute(fullPath)) {
            MethodRoute methodRoute = new MethodRoute(
                controllerInstance, 
                method, 
                fullPath, 
                true,
                httpMethod
            );
            parameterizedRoutes.add(methodRoute);
            System.out.println("    ↳ " + httpMethod + " " + fullPath + 
                            " → " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()");
        } else {
            // Route normale
            MethodRoute methodRoute = new MethodRoute(controllerInstance, method, httpMethod);
            routes.put(fullPath, methodRoute);
            System.out.println("    ↳ " + httpMethod + " " + fullPath + 
                            " → " + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()");
        }
    }

    private String combinePaths(String prefix, String path) {
        if (prefix == null || prefix.isEmpty()) {
            return path;
        }
        if (path == null || path.isEmpty()) {
            return prefix;
        }
        
        // Supprimer les / en double et s'assurer d'un seul / entre les parties
        prefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        path = path.startsWith("/") ? path : "/" + path;
        
        return prefix + path;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // S'assurer que le chemin commence par /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        // Supprimer les doubles slash
        path = path.replaceAll("/{2,}", "/");
        
        // S'assurer que le chemin ne se termine pas par / (sauf pour la racine)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        return path;
    }

    // [Les méthodes restantes inchangées...]
    private List<Class<?>> getClassesInPackageRecursively(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            System.out.println("⚠️ Aucun répertoire trouvé pour " + path);
            return Collections.emptyList();
        }
    
        if (resource.getProtocol().equals("file")) {
            File directory = new File(resource.toURI());
            List<Class<?>> classes = new ArrayList<>();
            scanDirectory(directory, packageName, classes);
            return classes;
        }
    
        else if (resource.getProtocol().equals("jar")) {
            List<Class<?>> classes = new ArrayList<>();
            String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath)) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                        String className = entryName.replace('/', '.').replace(".class", "");
                        classes.add(Class.forName(className));
                    }
                }
            }
            return classes;
        }
    
        return Collections.emptyList();
    }
    
    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                classes.add(Class.forName(className));
            }
        }
    }
}