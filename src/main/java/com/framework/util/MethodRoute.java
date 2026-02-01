package com.framework.util;

import java.lang.reflect.Method;

public class MethodRoute {
    private Object controllerInstance;
    private Method method;
    private String pathPattern;
    private boolean hasParameters;
    private String httpMethod; 
    
    public MethodRoute(Object controllerInstance, Method method, String httpMethod) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = null;
        this.hasParameters = false;
        this.httpMethod = httpMethod != null ? httpMethod : "GET";
    }
    
    public MethodRoute(Object controllerInstance, Method method, String pathPattern, 
                      boolean hasParameters, String httpMethod) {
        this.controllerInstance = controllerInstance;
        this.method = method;
        this.pathPattern = pathPattern;
        this.hasParameters = hasParameters;
        this.httpMethod = httpMethod != null ? httpMethod : "GET";
    }

    public Object getControllerInstance() { return controllerInstance; }
    public Method getMethod() { return method; }
    public String getPathPattern() { return pathPattern; }
    public boolean hasParameters() { return hasParameters; }
    public String getHttpMethod() { return httpMethod; }
    
    public boolean matchesHttpMethod(String requestMethod) {
        return httpMethod.equalsIgnoreCase(requestMethod);
    }
    
    @Override
    public String toString() {
        String path = (pathPattern != null && !pathPattern.isEmpty()) ? pathPattern : "exact";
        return httpMethod + " " + path + 
            " -> " + method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}