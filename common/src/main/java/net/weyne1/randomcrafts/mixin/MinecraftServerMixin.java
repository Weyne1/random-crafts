package net.weyne1.randomcrafts.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.weyne1.randomcrafts.RandomCraftsGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "onGameRuleChanged", at = @At("HEAD"))
    private <T> void onRandomCraftsRuleChanged(GameRule<T> gameRule, T value, CallbackInfo ci) {
        RandomCraftsGameRules.handleRuleChange((MinecraftServer) (Object) this, gameRule, value);
    }
}