package com.redeemed;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Players who are Redeemed Gem holders. Bliss gems are kept off these players. */
public final class RedeemedStore {
    private final RedeemedGemPlugin pl;
    private final File file;
    private final Set<UUID> players = new HashSet<>();
    /** Gems taken on death, waiting to be given back on respawn: player -> {stage, disguise or ""}. */
    private final Map<UUID, String[]> pending = new HashMap<>();

    public RedeemedStore(RedeemedGemPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "redeemed.yml");
        if (file.exists()) {
            for (String s : YamlConfiguration.loadConfiguration(file).getStringList("players")) {
                try { players.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) { }
            }
            var sec = YamlConfiguration.loadConfiguration(file).getConfigurationSection("pending");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    try {
                        pending.put(UUID.fromString(k), new String[]{sec.getString(k + ".stage", "DORMANT"), sec.getString(k + ".disguise", "")});
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> l = new ArrayList<>();
        for (UUID u : players) l.add(u.toString());
        y.set("players", l);
        for (Map.Entry<UUID, String[]> en : pending.entrySet()) {
            y.set("pending." + en.getKey() + ".stage", en.getValue()[0]);
            y.set("pending." + en.getKey() + ".disguise", en.getValue()[1]);
        }
        try { y.save(file); } catch (IOException e) { pl.getLogger().warning("Could not save redeemed.yml: " + e.getMessage()); }
    }

    public boolean has(UUID id) { return players.contains(id); }
    public void add(UUID id) { if (players.add(id)) save(); }
    public void remove(UUID id) {
        boolean a = players.remove(id), b = pending.remove(id) != null;
        if (a || b) save();
    }

    public void setPending(UUID id, String stage, String disguise) {
        pending.put(id, new String[]{stage, disguise == null ? "" : disguise});
        save();
    }
    /** {stage, disguise or ""}, or null if nothing is waiting. */
    public String[] pendingOf(UUID id) { return pending.get(id); }
    public void clearPending(UUID id) { if (pending.remove(id) != null) save(); }
    public Set<UUID> all() { return Collections.unmodifiableSet(players); }
}
