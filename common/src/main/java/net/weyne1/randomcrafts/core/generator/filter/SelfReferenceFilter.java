package net.weyne1.randomcrafts.core.generator.filter;

import net.weyne1.randomcrafts.core.item.CoreItem;

public class SelfReferenceFilter implements CandidateFilter {
    @Override
    public boolean isAllowed(CoreItem candidate, CoreItem output) {
        return !candidate.id().equals(output.id());
    }
}