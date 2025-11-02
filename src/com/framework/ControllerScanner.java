package com.framework;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ControllerScanner {

    private final String packageName;

    public ControllerScanner(String packageName) {
        this.packageName = packageName;
    }

    public void afficherLesControllers() throws Exception {
        List<Class<?>> classes = getClassesInPackageRecursively(packageName);

        System.out.println("\n=== Liste des contrôleurs trouvés (via réflexion) ===");
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(MonController.class)) { 
                System.out.println("→ " + clazz.getSimpleName() + " (" + clazz.getName() + ")");
            }
        }
        System.out.println("=====================================================\n");
    }

    private List<Class<?>> getClassesInPackageRecursively(String packageName) throws Exception {
        String path = packageName.replace('.', '/');
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(path);
        if (packageUrl == null) {
            throw new IllegalArgumentException("Package introuvable : " + packageName);
        }

        File directory = new File(packageUrl.toURI());
        List<Class<?>> classes = new ArrayList<>();
        scanDirectory(directory, packageName, classes);
        return classes;
    }

    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // Sous-package → récursivité
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                classes.add(Class.forName(className));
            }
        }
    }
}
