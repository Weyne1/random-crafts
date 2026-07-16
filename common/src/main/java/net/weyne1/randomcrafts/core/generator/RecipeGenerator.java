package net.weyne1.randomcrafts.core.generator;

import net.weyne1.randomcrafts.core.generator.filter.*;
import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import net.weyne1.randomcrafts.core.graph.RecipeGraph;

import java.util.*;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RecipeGenerator {
    private final RecipeGraph graph;
    private final Random random;
    private final GenerationSettings settings;
    private final Set<String> usedFingerprints = new HashSet<>();
    private final Map<Integer, List<CoreItem>> ingredientsByTier;

    private final List<CandidateFilter> candidateFilters = List.of(
            new SelfReferenceFilter(),
            new EnderEyeRuleFilter()
    );

    public RecipeGenerator(RecipeGraph graph, long seed, GenerationSettings settings) {
        this.graph = graph;
        this.random = new Random(seed);
        this.settings = settings;

        List<ItemFilter> itemFilters = new ArrayList<>();
        if (settings.excludeTools()) itemFilters.add(new ToolAndArmorFilter());
        if (settings.excludeFunctional()) itemFilters.add(new FunctionalBlockFilter());
        if (settings.excludeColors()) itemFilters.add(new ColorVariantFilter());

        this.ingredientsByTier = new HashMap<>();
        for (CoreItem item : graph.getCoreItemIdMap().values()) {
            boolean allowed = true;
            for (ItemFilter filter : itemFilters) {
                if (!filter.isAllowed(item.vanillaItem())) {
                    allowed = false;
                    break;
                }
            }
            if (allowed) {
                this.ingredientsByTier
                        .computeIfAbsent(item.tier(), k -> new ArrayList<>())
                        .add(item);
            }
        }
    }

    public CoreRecipe generateRandomRecipe(CoreRecipe original) {
        if (original == null) return null;

        CoreItem output = graph.getCoreItemById(original.output().id());
        if (output == null) output = original.output();

        List<CoreItem> originalInputs = original.inputs();
        Set<String> uniqueIdsSet = new HashSet<>(originalInputs.size());
        for (CoreItem ing : originalInputs) {
            uniqueIdsSet.add(ing.id());
        }
        List<String> uniqueIngredientIds = new ArrayList<>(uniqueIdsSet);

        List<CoreItem> newInputsList;
        String fingerprint;
        int attempts = 0;

        do {
            Map<String, CoreItem> replacementMap = createReplacementMap(uniqueIngredientIds, output, attempts);

            newInputsList = new ArrayList<>(originalInputs.size());
            for (CoreItem ing : originalInputs) {
                newInputsList.add(replacementMap.get(ing.id()));
            }

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
        Map<String, CoreItem> map = new HashMap<>(uniqueIds.size());
        List<CoreItem> candidates = findCandidatesFor(output, bonusSpread);

        for (String oldId : uniqueIds) {
            CoreItem chosen = candidates.isEmpty() ? graph.getCoreItemById(oldId) : candidates.get(random.nextInt(candidates.size()));

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

            List<CoreItem> found = new ArrayList<>();

            for (int t = currentMin; t <= currentMax; t++) {
                List<CoreItem> tierItems = ingredientsByTier.get(t);
                if (tierItems == null) continue;

                for (CoreItem item : tierItems) {
                    if (isAllowedCandidate(item, output)) {
                        found.add(item);
                    }
                }
            }

            if (!found.isEmpty()) return found;

            if (currentMin == 0) break;
        }

        return Collections.emptyList();
    }

    private boolean isAllowedCandidate(CoreItem candidate, CoreItem output) {
        for (CandidateFilter filter : candidateFilters) {
            if (!filter.isAllowed(candidate, output)) {
                return false;
            }
        }
        return true;
    }

    private String getRecipeFingerprint(List<CoreItem> inputs) {
        int size = inputs.size();
        if (size == 0) return "";

        String[] ids = new String[size];
        for (int i = 0; i < size; i++) {
            ids[i] = inputs.get(i).id();
        }
        Arrays.sort(ids);

        return String.join(",", ids);
    }
}