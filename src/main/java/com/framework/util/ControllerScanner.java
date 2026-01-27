package com.framework.util;

import com.framework.annotation.Controller;
import com.framework.annotation.Url;

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

                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url route = method.getAnnotation(Url.class);
                        String routePath = route.value();
                        
                        // Afficher les informations sur les paramètres
                        if (method.getParameterCount() > 0) {
                            System.out.println("    📝 Méthode avec " + method.getParameterCount() + " paramètre(s):");
                            for (java.lang.reflect.Parameter param : method.getParameters()) {
                                String paramInfo = param.getType().getSimpleName() + " " + param.getName();
                                if (param.isAnnotationPresent(com.framework.annotation.Param.class)) {
                                    paramInfo += " (@Param: " + param.getAnnotation(com.framework.annotation.Param.class).value() + ")";
                                }
                                System.out.println("      - " + paramInfo);
                            }
                        }
                        
                        // Vérifier si c'est une route paramétrée
                        if (RouteMatcher.isParameterizedRoute(routePath)) {
                            MethodRoute methodRoute = new MethodRoute(
                                controllerInstance, 
                                method, 
                                routePath, 
                                true
                            );
                            parameterizedRoutes.add(methodRoute);
                            System.out.println("    ↳ Route paramétrée : " + routePath + 
                                               " → " + clazz.getSimpleName() + "." + method.getName() + "()");
                        } else {
                            // Route normale
                            routes.put(routePath, new MethodRoute(controllerInstance, method));
                            System.out.println("    ↳ Route : " + routePath + 
                                               " → " + clazz.getSimpleName() + "." + method.getName() + "()");
                        }
                    }
                }
            }
        }
        System.out.println("==============================================\n");
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