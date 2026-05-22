package net.weyne1.randomcrafts.neoforge;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.weyne1.randomcrafts.RandomCrafts;
import net.weyne1.randomcrafts.RandomCraftsGameRules;

@Mod(RandomCrafts.MOD_ID)
public class RandomCraftsNeoForge {

    public RandomCraftsNeoForge(IEventBus modEventBus) {
        RandomCrafts.init();
        modEventBus.addListener(this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.GAME_RULE)) {
            RandomCraftsGameRules.init();
        }
    }
}