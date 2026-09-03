// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial Viewer Subscription & Interest Management Engine.
 * Tracks which players are actively viewing/subscribing to state updates for specific machine positions.
 */
public class SubscriptionManager {

    // Map long packed block position -> Set of Player UUIDs
    private final Map<Long, Set<UUID>> positionSubscribers = new ConcurrentHashMap<>();
    // Map Player UUID -> Set of long packed block positions subscribed by player
    private final Map<UUID, Set<Long>> playerSubscriptions = new ConcurrentHashMap<>();

    /**
     * Subscribes a player to a specific machine block position.
     */
    public void subscribe(Player player, long blockPos) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        positionSubscribers.computeIfAbsent(blockPos, k -> ConcurrentHashMap.newKeySet()).add(uuid);
        playerSubscriptions.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(blockPos);
    }

    /**
     * Unsubscribes a player from a specific machine block position.
     */
    public void unsubscribe(Player player, long blockPos) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        Set<UUID> subscribers = positionSubscribers.get(blockPos);
        if (subscribers != null) {
            subscribers.remove(uuid);
            if (subscribers.isEmpty()) {
                positionSubscribers.remove(blockPos);
            }
        }

        Set<Long> positions = playerSubscriptions.get(uuid);
        if (positions != null) {
            positions.remove(blockPos);
            if (positions.isEmpty()) {
                playerSubscriptions.remove(uuid);
            }
        }
    }

    /**
     * Unsubscribes a player from all block positions (e.g. on disconnect or inventory close).
     */
    public void unsubscribeAll(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        Set<Long> positions = playerSubscriptions.remove(uuid);
        if (positions != null) {
            for (Long pos : positions) {
                Set<UUID> subscribers = positionSubscribers.get(pos);
                if (subscribers != null) {
                    subscribers.remove(uuid);
                    if (subscribers.isEmpty()) {
                        positionSubscribers.remove(pos);
                    }
                }
            }
        }
    }

    /**
     * Unsubscribes a player from all block positions except the target position.
     */
    public void updateActiveSubscription(Player player, long activeBlockPos) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        Set<Long> positions = playerSubscriptions.get(uuid);
        if (positions != null) {
            for (Long pos : new java.util.HashSet<>(positions)) {
                if (pos != activeBlockPos) {
                    unsubscribe(player, pos);
                }
            }
        }
        subscribe(player, activeBlockPos);
    }

    /**
     * Gets all subscriber UUIDs for a given block position.
     */
    public Set<UUID> getSubscribers(long blockPos) {
        Set<UUID> subscribers = positionSubscribers.get(blockPos);
        return subscribers != null ? Collections.unmodifiableSet(subscribers) : Collections.emptySet();
    }

    /**
     * Checks if a player is subscribed to a block position.
     */
    public boolean isSubscribed(Player player, long blockPos) {
        if (player == null) return false;
        Set<UUID> subscribers = positionSubscribers.get(blockPos);
        return subscribers != null && subscribers.contains(player.getUniqueId());
    }

    /**
     * Clears all subscriptions.
     */
    public void clear() {
        positionSubscribers.clear();
        playerSubscriptions.clear();
    }
}
