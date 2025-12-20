package org.shotrush.atom.core.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterSystem {
    
    
    String id() default "";
    
    
    int priority() default 10;
    
    
    String[] dependencies() default {};
    
    
    boolean toggleable() default true;
    
    
    String description() default "";
    
    
    boolean enabledByDefault() default true;
    
    
    String[] provides() default {};
    
    
    String[] requires() default {};
}
