package net.weyne1.randomcrafts.quilt;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

import net.weyne1.randomcrafts.RandomCrafts;

public final class RandomCraftsQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        // Run our common setup.
        RandomCrafts.init();
    }
}
