package com.framework.util;

import java.lang.reflect.Method;

public class MethodRoute {
    private Object controllerInstance;
    private Method method;

    public MethodRoute(Object controllerInstance, Method method) {
        this.controllerInstance = controllerInstance;
        this.method = method;
    }

    public Object getControllerInstance() { return controllerInstance; }
    public Method getMethod() { return method; }
}
