package net.weyne1.randomcrafts.core.generator.filter;

import net.minecraft.world.item.Items;
import net.weyne1.randomcrafts.core.item.CoreItem;

public class EnderEyeRuleFilter implements CandidateFilter {
    @Override
    public boolean isAllowed(CoreItem candidate, CoreItem output) {
        if (output.vanillaItem() == Items.ENDER_EYE) {
            return candidate.tier() != 14;
        }
        return true;
    }
}