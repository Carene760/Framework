package com.framework.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionParam {
    String value() default ""; // Nom de l'attribut de session
    boolean required() default true;
}



