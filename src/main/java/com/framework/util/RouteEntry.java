package com.framework.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Reprsente toutes les mthodes HTTP pour un chemin donn
 */
public class RouteEntry {
    private final String path;
    private final Map<String, MethodRoute> methods; // Mthode HTTP -> MethodRoute
    
    public RouteEntry(String path) {
        this.path = path;
        this.methods = new HashMap<>();
    }
    
    public void addMethod(String httpMethod, MethodRoute route) {
        methods.put(httpMethod.toUpperCase(), route);
    }
    
    public MethodRoute getMethod(String httpMethod) {
        return methods.get(httpMethod.toUpperCase());
    }
    
    public boolean hasMethod(String httpMethod) {
        return methods.containsKey(httpMethod.toUpperCase());
    }
    
    public String getPath() {
        return path;
    }
    
    public Map<String, MethodRoute> getAllMethods() {
        return new HashMap<>(methods);
    }
    
    @Override
    public String toString() {
        return path + " -> " + methods.keySet();
    }
}


