package net.weyne1.randomcrafts.core.generator.filter;

import net.minecraft.world.item.*;
import java.util.Set;

public class ToolAndArmorFilter implements ItemFilter {
    private static final Set<Class<? extends Item>> TOOLS_AND_ARMOR = Set.of(
            ArmorItem.class, DiggerItem.class, SwordItem.class, ShieldItem.class, FlintAndSteelItem.class,
            ElytraItem.class, BowItem.class, CrossbowItem.class, TridentItem.class, ShearsItem.class,
            SpyglassItem.class, ProjectileWeaponItem.class, FishingRodItem.class, BrushItem.class
    );

    @Override
    public boolean isAllowed(Item item) {
        boolean isToolOrArmor = TOOLS_AND_ARMOR.stream().anyMatch(clazz -> clazz.isInstance(item))
                || item instanceof BoatItem;
        return !isToolOrArmor;
    }
}