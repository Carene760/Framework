// Dans src/core/utils/AnnotationScanner.java
package core.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import core.annotations.GetMapping;
import core.annotations.PostMapping;
import core.annotations.MyURL;

public class AnnotationScanner {
    
    public static void scanControllers(String packageName) {
        try {
            List<Class<?>> controllers = getClasses(packageName);
            
            for (Class<?> controller : controllers) {
                System.out.println("Scanning controller: " + controller.getName());
                
                Method[] methods = controller.getDeclaredMethods();
                for (Method method : methods) {
                    // ====================================================
                    // MODIFICATION SPRINT 7: COMPATIBILITÉ AVEC ANNOTATIONS HTTP
                    // On garde le format Object[] mais on ajoute la méthode HTTP
                    // Format: Object[] = [Class<?>, Method, String httpMethod] 
                    // ====================================================
                    
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        // ====================================================
                        // NOUVEAU: Détection de l'annotation @GetMapping
                        // ====================================================
                        GetMapping annotation = method.getAnnotation(GetMapping.class);
                        String url = annotation.value();
                        
                        // ====================================================
                        // MODIFICATION: Ajout de la méthode HTTP "GET" en 3ème élément
                        // ====================================================
                        Object[] routeInfo = {controller, method, "GET"};
                        core.Router.routes.put(url, routeInfo);
                        
                        System.out.println("Route GET ajoutée : " + url + " -> " + 
                                         controller.getSimpleName() + "." + method.getName());
                                         
                    } else if (method.isAnnotationPresent(PostMapping.class)) {
                        // ====================================================
                        // NOUVEAU: Détection de l'annotation @PostMapping
                        // ====================================================
                        PostMapping annotation = method.getAnnotation(PostMapping.class);
                        String url = annotation.value();
                        
                        // ====================================================
                        // MODIFICATION: Ajout de la méthode HTTP "POST" en 3ème élément
                        // ====================================================
                        Object[] routeInfo = {controller, method, "POST"};
                        core.Router.routes.put(url, routeInfo);
                        
                        System.out.println("Route POST ajoutée : " + url + " -> " + 
                                         controller.getSimpleName() + "." + method.getName());
                                         
                    } else if (method.isAnnotationPresent(MyURL.class)) {
                        // ====================================================
                        // EXISTANT: Détection de l'annotation @MyURL (code original)
                        // MODIFICATION: Ajout de "ANY" comme méthode HTTP par défaut
                        // ====================================================
                        MyURL annotation = method.getAnnotation(MyURL.class);
                        String url = annotation.value();
                        
                        // ====================================================
                        // MODIFICATION: Ajout de la méthode HTTP "ANY" en 3ème élément
                        // "ANY" signifie que toutes les méthodes HTTP sont acceptées
                        // ====================================================
                        Object[] routeInfo = {controller, method, "ANY"};
                        core.Router.routes.put(url, routeInfo);
                        
                        System.out.println("Route ANY ajoutée : " + url + " -> " + 
                                         controller.getSimpleName() + "." + method.getName());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ====================================================
    // MÉTHODE EXISTANTE: Pas de modification nécessaire
    // ====================================================
    private static List<Class<?>> getClasses(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);
        
        if (resource == null) {
            throw new Exception("Package non trouvé: " + packageName);
        }
        
        File directory = new File(resource.getFile());
        List<Class<?>> classes = new ArrayList<>();
        
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + 
                                      file.getName().substring(0, file.getName().length() - 6);
                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        System.err.println("Classe non trouvée: " + className);
                    }
                }
            }
        }
        return classes;
    }
}