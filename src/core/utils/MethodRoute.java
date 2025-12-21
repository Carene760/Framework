// Dans src/core/utils/MethodRoute.java
package core.utils;

import java.lang.reflect.Method;

public class MethodRoute {
    private Class<?> controllerClass;
    private Method method;
    private String url;

    public MethodRoute(Class<?> controllerClass, Method method, String url) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.url = url;
    }

    public Object getControllerInstance() throws Exception {
        return controllerClass.getDeclaredConstructor().newInstance();
    }

    public Class<?> getControllerClass() { 
        return controllerClass; 
    }
    
    public Method getMethod() { 
        return method; 
    }
    
    public String getUrl() {
        return url;
    }
}