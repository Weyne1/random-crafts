package net.weyne1.randomcrafts.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.weyne1.randomcrafts.RandomCrafts;
import net.weyne1.randomcrafts.RandomCraftsGameRules;
import net.weyne1.randomcrafts.core.world.RandomCraftsWorldEvents;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

@Mod(RandomCrafts.MOD_ID)
public final class RandomCraftsNeoForge {
    public RandomCraftsNeoForge() {
        RandomCrafts.init();

        RandomCraftsGameRules.init(new RandomCraftsGameRules.Registrar() {
            @Override
            public GameRules.Key<GameRules.BooleanValue> registerBoolean(String name, boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> callback) {
                GameRules.Type<GameRules.BooleanValue> type = createType(GameRules.BooleanValue.class, defaultValue, callback);
                return registerGamerule(name, type);
            }

            @Override
            public GameRules.Key<GameRules.IntegerValue> registerInteger(String name, int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> callback) {
                BiConsumer<MinecraftServer, GameRules.IntegerValue> wrappedCallback = (server, value) -> {
                    int current = value.get();
                    int clamped = Math.max(min, Math.min(max, current));
                    if (current != clamped) {
                        value.set(clamped, null);
                    }
                    callback.accept(server, value);
                };

                GameRules.Type<GameRules.IntegerValue> type = createType(GameRules.IntegerValue.class, defaultValue, wrappedCallback);
                return registerGamerule(name, type);
            }
        });

        NeoForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> GameRules.Type<T> createType(Class<T> valueClass, Object defaultValue, BiConsumer<MinecraftServer, T> callback) {
        try {
            Class<?> paramType = defaultValue.getClass();
            if (paramType == Boolean.class) {
                paramType = boolean.class;
            } else if (paramType == Integer.class) {
                paramType = int.class;
            }
            Method method = valueClass.getDeclaredMethod("create", paramType, BiConsumer.class);
            method.setAccessible(true);
            return (GameRules.Type<T>) method.invoke(null, defaultValue, callback);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GameRules.Type for " + valueClass.getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> GameRules.Key<T> registerGamerule(String name, GameRules.Type<T> type) {
        try {
            Method method = GameRules.class.getDeclaredMethod("register", String.class, GameRules.Category.class, GameRules.Type.class);
            method.setAccessible(true);
            return (GameRules.Key<T>) method.invoke(null, name, GameRules.Category.MISC, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register gamerule: " + name, e);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RandomCraftsWorldEvents.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        RandomCraftsWorldEvents.handleServerStarted(event.getServer());
    }
}