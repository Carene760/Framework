package com.framework.util;

import java.lang.reflect.Method;
import java.util.Map;

public class MethodRoute {
    private Object controllerInstance;
    private Method method;
    private String pathPattern;
    private boolean hasUrlParameters;
    private java.lang.reflect.Parameter[] methodParameters;
    private Map<String, String> urlParameters; // Paramètres extraits de l'URL
    
    public MethodRoute(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = null;
        this.hasUrlParameters = false;
        this.methodParameters = method.getParameters();
        this.urlParameters = null;
    }
    
    public MethodRoute(Object controllerInstance, Method method, String pathPattern, 
                      boolean hasUrlParameters, Map<String, String> urlParameters) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = pathPattern;
        this.hasUrlParameters = hasUrlParameters;
        this.methodParameters = method.getParameters();
        this.urlParameters = urlParameters;
    }

    public Object getControllerInstance() { return controllerInstance; }
    public Method getMethod() { return method; }
    public String getPathPattern() { return pathPattern; }
    public boolean hasUrlParameters() { return hasUrlParameters; }
    public java.lang.reflect.Parameter[] getMethodParameters() { return methodParameters; }
    public Map<String, String> getUrlParameters() { return urlParameters; }
    public void setUrlParameters(Map<String, String> urlParameters) { this.urlParameters = urlParameters; }
    
    public void displayInfo() {
        System.out.println("📋 Méthode: " + method.getName());
        System.out.println("🔗 Pattern: " + (pathPattern != null ? pathPattern : "exact"));
        
        if (urlParameters != null && !urlParameters.isEmpty()) {
            System.out.println("📌 Paramètres d'URL extraits:");
            urlParameters.forEach((k, v) -> System.out.println("  - " + k + " = " + v));
        }
        
        if (methodParameters.length > 0) {
            System.out.println("🎯 Paramètres de méthode:");
            for (int i = 0; i < methodParameters.length; i++) {
                java.lang.reflect.Parameter param = methodParameters[i];
                String info = param.getType().getSimpleName() + " " + param.getName();
                if (param.isAnnotationPresent(com.framework.annotation.Param.class)) {
                    info += " (@Param: \"" + param.getAnnotation(com.framework.annotation.Param.class).value() + "\")";
                }
                System.out.println("  - " + info);
            }
        }
    }
}