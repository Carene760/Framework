package com.framework;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) // Disponible à l’exécution pour la réflexion
@Target(ElementType.TYPE)           // Applicable uniquement aux classes
public @interface MonController {
}
