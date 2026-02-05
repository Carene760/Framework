package com.framework.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionAttributes {
    // Annotation pour injecter Map<String, Object> de toute la session
}



