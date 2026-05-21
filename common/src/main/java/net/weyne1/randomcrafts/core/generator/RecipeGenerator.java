package net.weyne1.randomcrafts.core.generator;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import net.weyne1.randomcrafts.core.graph.RecipeGraph;

import java.util.*;
import java.util.stream.Collectors;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RecipeGenerator {
    private final RecipeGraph graph;
    private final Random random;
    private final GenerationSettings settings;
    private final Set<String> usedFingerprints = new HashSet<>();
    private final List<CoreItem> allPossibleIngredients;

    public RecipeGenerator(RecipeGraph graph, long seed, GenerationSettings settings) {
        this.graph = graph;
        this.random = new Random(seed);
        this.settings = settings;
        this.allPossibleIngredients = graph.getCoreItemIdMap().values().stream()
                .sorted(Comparator.comparing(CoreItem::id))
                .toList();
    }

    private String getRecipeFingerprint(List<CoreItem> inputs) {
        return inputs.stream()
                .map(CoreItem::id)
                .sorted()
                .collect(Collectors.joining(","));
    }

    public CoreRecipe generateRandomRecipe(CoreRecipe original) {
        if (original == null) return null;

        CoreItem output = graph.getCoreItemById(original.output().id());
        if (output == null) output = original.output();

        // 1. Подготовка данных
        List<String> uniqueIngredientIds = original.inputs().stream()
                .map(CoreItem::id).distinct().sorted().toList();

        boolean isEnderEye = output.vanillaItem() == Items.ENDER_EYE;
        List<CoreItem> newInputsList;
        String fingerprint;
        int attempts = 0;

        // 2. Основной цикл генерации
        do {
            Map<String, CoreItem> replacementMap = createReplacementMap(uniqueIngredientIds, output, attempts, isEnderEye);

            newInputsList = original.inputs().stream()
                    .map(ing -> replacementMap.get(ing.id()))
                    .toList();

            fingerprint = getRecipeFingerprint(newInputsList);
            attempts++;

        } while (usedFingerprints.contains(fingerprint) && attempts < 10);

        // 3. Финальная проверка
        if (usedFingerprints.contains(fingerprint)) {
            LOGGER.warn("Could not find unique recipe for {} after {} attempts.", output.id(), attempts);
            return original;
        }

        usedFingerprints.add(fingerprint);
        return new CoreRecipe(original.id(), output, newInputsList, original.outputCount(),
                original.isShapeless(), original.patternLayout(), original.category());
    }

    /**
     * Создает карту замен: Старый ID -> Новый случайный предмет
     */
    private Map<String, CoreItem> createReplacementMap(List<String> uniqueIds, CoreItem output, int bonusSpread, boolean isEnderEye) {
        Map<String, CoreItem> map = new HashMap<>();

        for (String oldId : uniqueIds) {
            List<CoreItem> candidates = findCandidatesFor(output, bonusSpread, isEnderEye);

            CoreItem chosen = candidates.isEmpty()
                    ? graph.getCoreItemById(oldId) // Fallback на оригинал
                    : candidates.get(random.nextInt(candidates.size()));

            map.put(oldId, chosen);
        }
        return map;
    }

    /**
     * Ищет подходящие предметы с учетом тиров, фильтров и адаптивного поиска вниз
     */
    private List<CoreItem> findCandidatesFor(CoreItem output, int bonusSpread, boolean isEnderEye) {
        int outTier = Math.max(0, output.tier());
        int minT, maxT;

        if (settings.useTiers()) {
            // Если тир низкий (0, 1, 2), разрешаем брать всё из этого начального диапазона
            if (outTier <= 2) {
                minT = 0;
                maxT = 2;
            } else {
                // Для высоких тиров ищем ингредиенты СТРОГО дешевле выхода, но не ниже 0
                minT = Math.max(0, outTier - (settings.tierSpread() + bonusSpread));
                maxT = outTier - 1;
            }
        } else {
            // Если тиры отключены — берем любой предмет с тирами от 0 до 100
            minT = 0;
            maxT = 100;
        }

        // Адаптивный поиск (спуск по тирам вниз, если в заданном диапазоне пусто)
        // fallback до 10 шагов позволит найти хоть что-то, даже если это будет тир 0
        for (int fallback = 0; fallback <= 10; fallback++) {
            int currentMin = Math.max(0, minT - fallback);

            // Чтобы не застрять, если maxT оказался меньше текущего min из-за сдвига
            int currentMax = Math.max(currentMin, maxT);

            List<CoreItem> found = allPossibleIngredients.stream()
                    .filter(i -> {
                        int iTier = i.tier();
                        return iTier >= currentMin && iTier <= currentMax;
                    })
                    .filter(i -> !i.id().equals(output.id())) // Защита от рекурсии "предмет из самого себя"
                    .filter(i -> isAllowedAsIngredient(i.vanillaItem())) // Проверка черного списка/правил
                    .filter(i -> !isEnderEye || i.tier() != 14) // Спец. условие для Ока Края (не даем энд-ресурсы)
                    .toList();

            if (!found.isEmpty()) return found;

            if (currentMin == 0) break;
        }

        return Collections.emptyList();
    }

    /**
     * Центральный фильтр. Проверяет предмет на основе активных GameRules.
     */
    private boolean isAllowedAsIngredient(Item item) {
        if (settings.excludeTools() && isToolOrArmor(item)) return false;

        if (settings.excludeFunctional() && isFunctionalBlock(item)) return false;

        return !settings.excludeColors() || !isColorVariant(item);
    }

    // --- Группы запрещенных классов ---

    private static final Set<Class<? extends Item>> TOOLS_AND_ARMOR = Set.of(
            BowItem.class, CrossbowItem.class, TridentItem.class, SpyglassItem.class, FlintAndSteelItem.class,
            ProjectileWeaponItem.class, FishingRodItem.class, BrushItem.class, ShieldItem.class, ShearsItem.class
    );

    private static final Set<Class<? extends Block>> FUNCTIONAL_CLASSES = Set.of(
            AbstractFurnaceBlock.class, AnvilBlock.class, CraftingTableBlock.class,
            EnchantingTableBlock.class, CrafterBlock.class, BaseEntityBlock.class, BedBlock.class
    );

    private static final Set<Block> SPECIFIC_FUNCTIONAL_BLOCKS = Set.of(
            Blocks.FLETCHING_TABLE, Blocks.SMITHING_TABLE, Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE,
            Blocks.LECTERN, Blocks.STONECUTTER, Blocks.BARREL
    );

    // --- Вспомогательные проверки ---


    private boolean isToolOrArmor(Item item) {
        if (TOOLS_AND_ARMOR.stream().anyMatch(clazz -> clazz.isInstance(item))) return true;
        if (item instanceof BoatItem || item == Items.ELYTRA) return true;

        // Броня: через компонент EQUIPPABLE
        ItemStack stack = item.getDefaultInstance();
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST ||
                    slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) return true;
        }

        // Инструменты/оружие: через компонент TOOL
        return stack.has(DataComponents.TOOL);
    }

    private boolean isFunctionalBlock(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();

            if (SPECIFIC_FUNCTIONAL_BLOCKS.contains(block)) {
                return true;
            }

            Class<? extends Block> blockClass = block.getClass();
            for (Class<? extends Block> functionalClass : FUNCTIONAL_CLASSES) {
                if (functionalClass.isAssignableFrom(blockClass)) {
                    return true;
                }
            }
        }

        return item instanceof BedItem;
    }

    private boolean isColorVariant(Item item) {
        // Красители разрешены
        if (item instanceof DyeItem) return false;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();

        // Список всех цветов для поиска в ID
        boolean hasColorName = path.matches(".*(orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black).*");
        if (!hasColorName) return false;

        // 1. Баннеры, кровати, седла гастов
        if (item instanceof BannerItem
                || item instanceof BedItem
                || item.getDefaultInstance().is(ItemTags.HARNESSES))
            return true;

        // 2. Блоки
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState state = block.defaultBlockState();

            return block instanceof StainedGlassBlock ||
                    block instanceof StainedGlassPaneBlock ||
                    block instanceof ConcretePowderBlock ||
                    block instanceof CarpetBlock ||
                    block instanceof CandleBlock ||
                    state.is(BlockTags.WOOL) ||
                    state.is(BlockTags.TERRACOTTA) ||
                    path.contains("concrete") ||
                    path.contains("shulker_box");
        }

        return false;
    }
}