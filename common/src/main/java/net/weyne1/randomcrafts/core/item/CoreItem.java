package net.weyne1.randomcrafts.core.item;

import net.minecraft.world.item.Item;

public class CoreItem {
    private final String id;
    private final String name;
    private final Item vanillaItem;
    private int tier;

    public CoreItem(String id, String name, int tier, Item vanillaItem) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.vanillaItem = vanillaItem;
    }

    public String id() { return id; }
    public String name() { return name; }
    public int tier() { return tier; }
    public Item vanillaItem() { return vanillaItem; }

    public void setTier(int tier) {
        this.tier = tier;
    }

    @Override
    public String toString() {
        return name + " (Tier " + tier + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoreItem coreItem)) return false;
        return id.equals(coreItem.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}