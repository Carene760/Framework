package com.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) // Disponible  lexcution pour la rflexion
@Target(ElementType.TYPE)           // Applicable uniquement aux classes
public @interface Controller {
}



