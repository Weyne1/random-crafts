package net.weyne1.randomcrafts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class RandomCraftsState extends SavedData {
    public boolean applied = false;
    public long usedSeed = 0;

    public RandomCraftsState() { }

    public static RandomCraftsState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RandomCraftsState::load, RandomCraftsState::new, "randomcrafts");
    }

    public static RandomCraftsState load(CompoundTag nbt) {
        RandomCraftsState state = new RandomCraftsState();
        state.applied = nbt.getBoolean("Applied");
        state.usedSeed = nbt.getLong("UsedSeed");
        return state;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("Applied", applied);
        nbt.putLong("UsedSeed", usedSeed);
        return nbt;
    }
}