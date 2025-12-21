package core;

import java.lang.reflect.Method;

public class Route {
    private Class<?> controllerClass;
    private Method method;
    
    public Route(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }
    
    // GETTERS OBLIGATOIRES
    public Class<?> getControllerClass() {
        return controllerClass;
    }
    
    public Method getMethod() {
        return method;
    }
}