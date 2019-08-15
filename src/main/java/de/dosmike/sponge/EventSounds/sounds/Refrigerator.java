package de.dosmike.sponge.EventSounds.sounds;

import org.spongepowered.api.entity.living.player.Player;

import java.util.*;

public class Refrigerator {

    private long globalCooldown = 0;
    private long individualCooldown = 0;

    private long globalExpire = 0;
    private Map<UUID, Long> individualExpire = new HashMap<>();

    public Refrigerator(long globalCooldown, long individualCooldown) {
        this.globalCooldown = globalCooldown;
        this.individualCooldown = individualCooldown;
    }

    /** Checks whether a cooldown is currently active globally or for this player.
     * If no cooldown is active the global cooldown and player related cooldown are set.
     * @return true if no cooldown is currently active */
    public synchronized boolean test(Player player) {
        cleanMap();
        return (System.currentTimeMillis() >= globalExpire && !individualExpire.containsKey(player.getUniqueId()));
    }
    /** A special test method designed to use this Refrigerator not as cooldown, but time limiter.
     * The global value is still used as cooldown!
     * @return true if no cooldown is set or a cooldown is active for the player */
    public synchronized boolean testNotOr0(Player player) {
        cleanMap();
        return (System.currentTimeMillis() >= globalExpire ||
                individualCooldown == 0 || individualExpire.containsKey(player.getUniqueId()));
    }
    public synchronized void put(Player player) {
        cleanMap();
        globalExpire = System.currentTimeMillis()+globalCooldown;
        individualExpire.put(player.getUniqueId(), System.currentTimeMillis()+individualCooldown);
    }
    public synchronized void reset(Player player) {
        individualExpire.remove(player.getUniqueId());
    }

    private void cleanMap() {
        Set<UUID> staleKeys = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : individualExpire.entrySet()) {
            if (System.currentTimeMillis() >= entry.getValue())
                staleKeys.add(entry.getKey());
        }
        for (UUID stale : staleKeys)
            individualExpire.remove(stale);
    }
}
