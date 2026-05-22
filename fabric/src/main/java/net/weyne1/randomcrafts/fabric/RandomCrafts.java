package net.weyne1.randomcrafts.fabric;

import net.fabricmc.api.ModInitializer;

public final class RandomCrafts implements ModInitializer {
    @Override
    public void onInitialize() {
        net.weyne1.randomcrafts.RandomCrafts.init();
        net.weyne1.randomcrafts.RandomCraftsGameRules.init();
    }
}
