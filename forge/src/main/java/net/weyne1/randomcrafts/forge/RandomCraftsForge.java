package net.weyne1.randomcrafts.forge;

import net.minecraftforge.fml.common.Mod;

import net.weyne1.randomcrafts.RandomCrafts;

@Mod(RandomCrafts.MOD_ID)
public final class RandomCraftsForge {
    public RandomCraftsForge() {
        RandomCrafts.init();
    }
}
