package com.framework.util;

import java.lang.reflect.Method;

public class MethodRoute {
    private Object controllerInstance;
    private Method method;
    private String pathPattern; // Pattern original avec {param}
    private boolean hasParameters;
    
    public MethodRoute(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = null; // Sera défini plus tard
        this.hasParameters = false;
    }
    
    public MethodRoute(Object controllerInstance, Method method, String pathPattern, boolean hasParameters) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = pathPattern;
        this.hasParameters = hasParameters;
    }

    public Object getControllerInstance() { return controllerInstance; }
    public Method getMethod() { return method; }
    public String getPathPattern() { return pathPattern; }
    public boolean hasParameters() { return hasParameters; }
}