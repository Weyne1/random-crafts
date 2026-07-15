package net.weyne1.randomcrafts.core.generator;

import net.minecraft.world.item.Item;
import net.weyne1.randomcrafts.core.generator.filter.*;
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
    private final List<ItemFilter> itemFilters = new ArrayList<>();
    private final List<CandidateFilter> candidateFilters = List.of(
            new SelfReferenceFilter(),
            new EnderEyeRuleFilter()
    );

    public RecipeGenerator(RecipeGraph graph, long seed, GenerationSettings settings) {
        this.graph = graph;
        this.random = new Random(seed);
        this.settings = settings;
        this.allPossibleIngredients = graph.getCoreItemIdMap().values().stream()
                .sorted(Comparator.comparing(CoreItem::id))
                .toList();

        if (settings.excludeTools()) itemFilters.add(new ToolAndArmorFilter());
        if (settings.excludeFunctional()) itemFilters.add(new FunctionalBlockFilter());
        if (settings.excludeColors()) itemFilters.add(new ColorVariantFilter());
    }

    public CoreRecipe generateRandomRecipe(CoreRecipe original) {
        if (original == null) return null;

        CoreItem output = graph.getCoreItemById(original.output().id());
        if (output == null) output = original.output();

        List<String> uniqueIngredientIds = original.inputs().stream()
                .map(CoreItem::id)
                .distinct()
                .sorted()
                .toList();

        List<CoreItem> newInputsList;
        String fingerprint;
        int attempts = 0;

        do {
            Map<String, CoreItem> replacementMap = createReplacementMap(uniqueIngredientIds, output, attempts);

            newInputsList = original.inputs().stream()
                    .map(ing -> replacementMap.get(ing.id()))
                    .toList();

            fingerprint = getRecipeFingerprint(newInputsList);
            attempts++;

        } while (usedFingerprints.contains(fingerprint) && attempts < 10);

        if (usedFingerprints.contains(fingerprint)) {
            LOGGER.warn("Could not find unique recipe for {} after {} attempts.", output.id(), attempts);
            return original;
        }

        usedFingerprints.add(fingerprint);
        return new CoreRecipe(original.id(), output, newInputsList, original.outputCount(),
                original.isShapeless(), original.patternLayout(), original.category());
    }

    private Map<String, CoreItem> createReplacementMap(List<String> uniqueIds, CoreItem output, int bonusSpread) {
        Map<String, CoreItem> map = new HashMap<>();

        for (String oldId : uniqueIds) {
            List<CoreItem> candidates = findCandidatesFor(output, bonusSpread);

            CoreItem chosen = candidates.isEmpty()
                    ? graph.getCoreItemById(oldId)
                    : candidates.get(random.nextInt(candidates.size()));

            map.put(oldId, chosen);
        }
        return map;
    }

    private List<CoreItem> findCandidatesFor(CoreItem output, int bonusSpread) {
        int outTier = Math.max(0, output.tier());
        int minT, maxT;

        if (settings.useTiers()) {
            if (outTier <= 2) {
                minT = 0;
                maxT = 2;
            } else {
                minT = Math.max(0, outTier - (settings.tierSpread() + bonusSpread));
                maxT = outTier - 1;
            }
        } else {
            minT = 0;
            maxT = 100;
        }

        for (int fallback = 0; fallback <= 10; fallback++) {
            int currentMin = Math.max(0, minT - fallback);
            int currentMax = Math.max(currentMin, maxT);

            List<CoreItem> found = allPossibleIngredients.stream()
                    .filter(i -> {
                        int iTier = i.tier();
                        return iTier >= currentMin && iTier <= currentMax;
                    })
                    .filter(i -> isAllowedAsIngredient(i.vanillaItem()))
                    .filter(i -> isAllowedCandidate(i, output))
                    .toList();

            if (!found.isEmpty()) return found;

            if (currentMin == 0) break;
        }

        return Collections.emptyList();
    }

    private boolean isAllowedAsIngredient(Item item) {
        return itemFilters.stream().allMatch(filter -> filter.isAllowed(item));
    }

    private boolean isAllowedCandidate(CoreItem candidate, CoreItem output) {
        return candidateFilters.stream().allMatch(filter -> filter.isAllowed(candidate, output));
    }

    private String getRecipeFingerprint(List<CoreItem> inputs) {
        return inputs.stream()
                .map(CoreItem::id)
                .sorted()
                .collect(Collectors.joining(","));
    }
}