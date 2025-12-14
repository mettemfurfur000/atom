package org.shotrush.atom.core.blocks.annotation;

import org.bukkit.entity.EntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomEntityDrops {
    
    EntityType[] entities() default {};
    
    
    EntityCategory[] categories() default {};
    
    
    Drop[] drops();
    
    
    boolean replaceVanillaDrops() default true;
    
    
    String[] ages() default {};
    
    
    enum EntityCategory {
        ANIMALS,
        MONSTERS,
        WATER_MOBS,
        AMBIENT
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Drop {
        
        String customItemId();
        
        
        int minAmount() default 1;
        
        
        int maxAmount() default 1;
        
        
        boolean randomAmount() default true;
    }
}
