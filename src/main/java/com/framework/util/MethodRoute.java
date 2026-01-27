package com.framework.util;

import java.lang.reflect.Method;

public class MethodRoute {
    private Object controllerInstance;
    private Method method;
    private String pathPattern;
    private boolean hasParameters;
    private java.lang.reflect.Parameter[] methodParameters;
    
    public MethodRoute(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = null;
        this.hasParameters = false;
        this.methodParameters = method.getParameters();
    }
    
    public MethodRoute(Object controllerInstance, Method method, String pathPattern, boolean hasParameters) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = pathPattern;
        this.hasParameters = hasParameters;
        this.methodParameters = method.getParameters();
    }

    public Object getControllerInstance() { return controllerInstance; }
    public Method getMethod() { return method; }
    public String getPathPattern() { return pathPattern; }
    public boolean hasParameters() { return hasParameters; }
    public java.lang.reflect.Parameter[] getMethodParameters() { return methodParameters; }
    
    public void displayParametersInfo() {
        System.out.println("📋 Paramètres de la méthode " + method.getName() + ":");
        for (int i = 0; i < methodParameters.length; i++) {
            java.lang.reflect.Parameter param = methodParameters[i];
            System.out.println("  - " + param.getType().getSimpleName() + " " + param.getName());
        }
    }
}