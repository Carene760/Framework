package com.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface HttpMethod {
    String value() default "GET"; // GET, POST, PUT, DELETE, etc.
}


