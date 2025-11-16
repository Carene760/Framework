package com.framework.util;

import com.framework.annotation.MonController;
import com.framework.annotation.MesRoutes;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class ControllerScanner {

    private final String packageName;
    private final Map<String, MethodRoute> routes = new HashMap<>();

    public ControllerScanner(String packageName) {
        this.packageName = packageName;
    }

    public Map<String, MethodRoute> getRoutes() {
        return routes;
    }

    public void afficherLesControllersEtRoutes() throws Exception {
        List<Class<?>> classes = getClassesInPackageRecursively(packageName);

        System.out.println("\n=== Liste des contrôleurs et routes trouvés ===");
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(MonController.class)) {
                System.out.println("🧩 Contrôleur : " + clazz.getName());

                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(MesRoutes.class)) {
                        MesRoutes route = method.getAnnotation(MesRoutes.class);
                        routes.put(route.value(), new MethodRoute(controllerInstance, method));
                        System.out.println("    ↳ Route : " + route.value() + 
                                           " → " + clazz.getSimpleName() + "." + method.getName() + "()");
                    }
                }
            }
        }
        System.out.println("==============================================\n");
    }

    private List<Class<?>> getClassesInPackageRecursively(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            System.out.println("⚠️ Aucun répertoire trouvé pour " + path);
            return Collections.emptyList();
        }
    
        // Si c’est un chemin dans un dossier normal
        if (resource.getProtocol().equals("file")) {
            File directory = new File(resource.toURI());
            List<Class<?>> classes = new ArrayList<>();
            scanDirectory(directory, packageName, classes);
            return classes;
        }
    
        // Si c’est dans un JAR (comme dans ton .war déployé)
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
