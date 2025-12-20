package org.shotrush.atom.content.foragingage;

import net.kyori.adventure.text.format.TextColor;
import org.shotrush.atom.core.age.Age;
import org.shotrush.atom.core.age.AgeProvider;
import org.shotrush.atom.core.age.annotation.AutoRegisterAge;

@AutoRegisterAge(order = 0)
public class ForagingAge implements AgeProvider {
    
    @Override
    public Age createAge() {
        return Age.builder()
                .id("foraging_age")
                .displayName("Foraging")
                .year(50000)
                .isBC(true)
                .order(0)
                .titleColor(TextColor.color(34, 139, 34))
                .description("Gathering resources from nature")
                .build();
    }
}
