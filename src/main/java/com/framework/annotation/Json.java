package com.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Json {
    // Optionnel: peut spcifier un nom de vue alternatif
    String value() default "";
}


