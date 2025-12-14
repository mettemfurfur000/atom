package org.shotrush.atom.content.mobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RegisterSystem(
    id = "mob_scale",
    priority = 2,
    toggleable = true,
    description = "Scales mob sizes for better realism"
)
public class MobScale implements Listener {
    private final Plugin plugin;
    private static final Map<EntityType, Double> MOB_SCALES = new HashMap<>();
    
    static {

        MOB_SCALES.put(EntityType.COW, 1.0);           
        MOB_SCALES.put(EntityType.PIG, 0.85);            
        MOB_SCALES.put(EntityType.SHEEP, 1.0);         
        MOB_SCALES.put(EntityType.CHICKEN, 0.7);       
        MOB_SCALES.put(EntityType.RABBIT, 0.8);         
        MOB_SCALES.put(EntityType.HORSE, 1.0);         
        MOB_SCALES.put(EntityType.DONKEY, 0.9);         
        MOB_SCALES.put(EntityType.MULE, 0.95);          
        MOB_SCALES.put(EntityType.LLAMA, 1.05);          
        MOB_SCALES.put(EntityType.CAT, 0.5);           
        MOB_SCALES.put(EntityType.WOLF, 0.75);           
        MOB_SCALES.put(EntityType.PARROT, 0.6);         
        MOB_SCALES.put(EntityType.FOX, 0.65);           
        MOB_SCALES.put(EntityType.PANDA, 1.0);          
        MOB_SCALES.put(EntityType.POLAR_BEAR, 1.2);     
        MOB_SCALES.put(EntityType.GOAT, 0.85);           
        MOB_SCALES.put(EntityType.AXOLOTL, 0.14);       
        MOB_SCALES.put(EntityType.FROG, 0.06);           
        
        MOB_SCALES.put(EntityType.ZOMBIE, 1.0);         
        MOB_SCALES.put(EntityType.SKELETON, 1.0);       
        MOB_SCALES.put(EntityType.CREEPER, 0.94);        
        MOB_SCALES.put(EntityType.SPIDER, 0.5);         
        MOB_SCALES.put(EntityType.CAVE_SPIDER, 0.4);   
        MOB_SCALES.put(EntityType.ENDERMAN, 1.67);      
        MOB_SCALES.put(EntityType.WITCH, 1.0);          
        MOB_SCALES.put(EntityType.BLAZE, 1.0);         
        MOB_SCALES.put(EntityType.SILVERFISH, 0.3);    
        MOB_SCALES.put(EntityType.ENDERMITE, 0.4);     
        MOB_SCALES.put(EntityType.PHANTOM, 0.56);        
        MOB_SCALES.put(EntityType.DROWNED, 1.0);        
        MOB_SCALES.put(EntityType.HUSK, 1.0);           
        MOB_SCALES.put(EntityType.STRAY, 1.0);          
        MOB_SCALES.put(EntityType.VINDICATOR, 1.0);     
        MOB_SCALES.put(EntityType.EVOKER, 1.0);         
        MOB_SCALES.put(EntityType.PILLAGER, 1.0);       
        MOB_SCALES.put(EntityType.RAVAGER, 1.22);        
        MOB_SCALES.put(EntityType.VEX, 0.33);            
        MOB_SCALES.put(EntityType.GUARDIAN, 1.0);       
        MOB_SCALES.put(EntityType.ELDER_GUARDIAN, 1.0); 
        MOB_SCALES.put(EntityType.SHULKER, 0.56);       
        MOB_SCALES.put(EntityType.HOGLIN, 0.78);         
        MOB_SCALES.put(EntityType.PIGLIN, 1.0);         
        MOB_SCALES.put(EntityType.PIGLIN_BRUTE, 1.0);   
        MOB_SCALES.put(EntityType.ZOMBIFIED_PIGLIN, 1.0); 
        MOB_SCALES.put(EntityType.ZOGLIN, 0.78);         
        MOB_SCALES.put(EntityType.WITHER_SKELETON, 1.33);
        
        MOB_SCALES.put(EntityType.COD, 0.5);           
        MOB_SCALES.put(EntityType.SALMON, 0.6);         
        MOB_SCALES.put(EntityType.TROPICAL_FISH, 0.4);  
        MOB_SCALES.put(EntityType.PUFFERFISH, 0.5);     
        MOB_SCALES.put(EntityType.SQUID, 0.7);          
        MOB_SCALES.put(EntityType.GLOW_SQUID, 0.7);     
        MOB_SCALES.put(EntityType.DOLPHIN, 0.9);        
        MOB_SCALES.put(EntityType.TURTLE, 0.7);
        
        MOB_SCALES.put(EntityType.VILLAGER, 1.0);       
        MOB_SCALES.put(EntityType.WANDERING_TRADER, 1.0); 
        MOB_SCALES.put(EntityType.IRON_GOLEM, 1.0);     
        MOB_SCALES.put(EntityType.SNOW_GOLEM, 1.0);
        
        MOB_SCALES.put(EntityType.WITHER, 1.8);         
        MOB_SCALES.put(EntityType.ENDER_DRAGON, 2.0);   
        MOB_SCALES.put(EntityType.WARDEN, 1.5);         

        MOB_SCALES.put(EntityType.SLIME, 0.5);          
        MOB_SCALES.put(EntityType.MAGMA_CUBE, 0.5);     

        MOB_SCALES.put(EntityType.BAT, 0.4);           
        MOB_SCALES.put(EntityType.BEE, 0.5);            
        MOB_SCALES.put(EntityType.STRIDER, 0.94);        
        MOB_SCALES.put(EntityType.ALLAY, 0.28);          
    }
    
    public MobScale(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof LivingEntity livingEntity)) return;

        EntityType type = entity.getType();
        
        
        Double scale = MOB_SCALES.get(type);
        if (scale == null) return;
        
        
        if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            int size = slime.getSize();
            
            scale = scale * (size / 2.0);
        }
        
        
        if (livingEntity.getAttribute(Attribute.SCALE) != null) {
            Objects.requireNonNull(livingEntity.getAttribute(Attribute.SCALE)).setBaseValue(scale);
        }
    }
}
