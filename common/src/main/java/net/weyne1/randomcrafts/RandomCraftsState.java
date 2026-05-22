package net.weyne1.randomcrafts;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomCraftsState extends SavedData {
    public boolean applied = false;
    public long usedSeed = 0;

    public RandomCraftsState() { }

    public static final Codec<RandomCraftsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("Applied").orElse(false).forGetter(s -> s.applied),
            Codec.LONG.fieldOf("UsedSeed").orElse(0L).forGetter(s -> s.usedSeed)
    ).apply(instance, (applied, usedSeed) -> {
        RandomCraftsState state = new RandomCraftsState();
        state.applied = applied;
        state.usedSeed = usedSeed;
        return state;
    }));

    public static final SavedDataType<RandomCraftsState> TYPE = new SavedDataType<>(
            "randomcrafts",
            RandomCraftsState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    public static RandomCraftsState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}