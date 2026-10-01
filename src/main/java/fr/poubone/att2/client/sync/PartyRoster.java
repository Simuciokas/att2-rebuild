package fr.poubone.att2.client.sync;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PartyRoster {
    private PartyRoster() {
    }

    public static List<String> listedIds(UUID self, Iterable<UUID> tabIds) {
        Set<String> ids = new LinkedHashSet<>();
        if (self != null) {
            ids.add(self.toString());
        }

        if (tabIds != null) {
            for (UUID id : tabIds) {
                if (id != null) {
                    ids.add(id.toString());
                }
            }
        }

        return new ArrayList<>(ids);
    }
}
