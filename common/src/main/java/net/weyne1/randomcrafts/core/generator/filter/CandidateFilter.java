package net.weyne1.randomcrafts.core.generator.filter;

import net.weyne1.randomcrafts.core.item.CoreItem;

public interface CandidateFilter {
    boolean isAllowed(CoreItem candidate, CoreItem output);
}