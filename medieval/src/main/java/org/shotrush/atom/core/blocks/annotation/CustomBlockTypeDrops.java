package org.shotrush.atom.core.blocks.annotation;

import org.bukkit.Material;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomBlockTypeDrops {
    
    Drop[] drops();
    
    
    boolean replaceMainDrop() default false;
    
    
    String[] ages() default {};
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Drop {
        
        Material material() default Material.AIR;
        
        
        double chance() default 1.0;
        
        
        int min() default 1;
        
        
        int max() default 1;
        
        
        String customItemId() default "";
    }
}
