package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class AdminRolePreset {
    private final Set<LoadoutItemType> items = new LinkedHashSet<>();
    private final Set<String> disabledPerks = new LinkedHashSet<>();
    private boolean enabled;

    public Set<LoadoutItemType> getItems() {
        return items;
    }

    public Set<String> getDisabledPerks() {
        return disabledPerks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void replaceDisabledPerks(Collection<String> values) {
        disabledPerks.clear();
        if (values != null) {
            for (String perkId : values) {
                if (perkId != null && !perkId.isBlank()) {
                    disabledPerks.add(perkId);
                }
            }
        }
    }
}

