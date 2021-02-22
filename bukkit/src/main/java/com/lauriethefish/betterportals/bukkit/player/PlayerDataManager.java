package com.lauriethefish.betterportals.bukkit.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.net.requests.GetSelectionRequest;
import com.lauriethefish.betterportals.bukkit.player.selection.IPlayerSelectionManager;
import com.lauriethefish.betterportals.bukkit.player.selection.IPortalSelection;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.requests.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
public class PlayerDataManager implements IPlayerDataManager, Listener   {
    private final Logger logger;
    private final PlayerDataFactory playerDataFactory;
    private final Map<Player, IPlayerData> players = new HashMap<>();

    private final Map<UUID, TeleportRequest> pendingTeleportOnJoin = new HashMap<>();
    private final Map<UUID, GetSelectionRequest.ExternalSelectionInfo> pendingSelectionOnJoin = new HashMap<>();
    /**
     * Used to retain selections throughout logouts.
     */
    private final Map<UUID, IPlayerSelectionManager> loggedOutPlayerSelections = new HashMap<>();

    @Inject
    public PlayerDataManager(JavaPlugin pl, Logger logger, PlayerDataFactory playerDataFactory) {
        this.logger = logger;
        this.playerDataFactory = playerDataFactory;

        addExistingPlayers();
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    private void addExistingPlayers() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            players.put(player, playerDataFactory.create(player));
        }
    }

    @Override
    public @NotNull Collection<IPlayerData> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    @Override
    public @Nullable IPlayerData getPlayerData(@NotNull Player player) {
        return players.get(player);
    }
    @Override
    public void onPluginDisable() {
        players.values().forEach(IPlayerData::onPluginDisable);
    }

    @Override
    public void setTeleportOnJoin(TeleportRequest request) {
        pendingTeleportOnJoin.put(request.getPlayerId(), request);
    }

    @Override
    public void setExternalSelectionOnLogin(UUID uniqueId, GetSelectionRequest.ExternalSelectionInfo selection) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player != null) {
            logger.fine("Directly setting external selection for player with ID %s", uniqueId);
            players.get(player).getSelection().setExternalSelection(selection);
        }   else    { // If the player is not online yet, add it to this map so that it will be set when they log in
            logger.fine("Setting external selection to pending for player with ID %s", uniqueId);
            pendingSelectionOnJoin.put(uniqueId, selection);
        }
    }

    @Override
    public @Nullable IPortalSelection getDestinationSelectionWhenLoggedOut(UUID uniqueId) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player != null) {
            return players.get(player).getSelection().getDestSelection();
        }   else    {
            return loggedOutPlayerSelections.get(uniqueId).getDestSelection();
        }
    }

    // Add/remove players upon joining and leaving
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        logger.fine("Registering player data on join for player: %s", playerId);
        IPlayerData playerData = playerDataFactory.create(event.getPlayer());
        players.put(event.getPlayer(), playerData);

        TeleportRequest teleportOnJoin = pendingTeleportOnJoin.remove(playerId);
        if(teleportOnJoin != null) {
            processTeleportOnJoin(event.getPlayer(), teleportOnJoin);
        }

        IPlayerSelectionManager selectionManager = loggedOutPlayerSelections.get(playerId);
        if(selectionManager != null) {
            logger.fine("Restoring selection on join");
            playerData.setSelection(selectionManager);
        }
        playerData.getSelection().setExternalSelection(pendingSelectionOnJoin.get(playerId));
    }

    private void processTeleportOnJoin(@NotNull Player player, @NotNull TeleportRequest request) {
        World world = Bukkit.getWorld(request.getDestWorldId());
        if(world == null) {
            world = Bukkit.getWorld(request.getDestWorldName());
        }

        Location destinationPosition = new Location(
                world,
                request.getDestX(),
                request.getDestY(),
                request.getDestZ(),
                request.getDestYaw(),
                request.getDestPitch()
        );

        Vector destinationVelocity = new Vector(
                request.getDestVelX(),
                request.getDestVelY(),
                request.getDestVelZ()
        );

        player.teleport(destinationPosition);
        player.setVelocity(destinationVelocity);

        player.setFlying(request.isFlying());
        player.setGliding(request.isGliding());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        logger.fine("Saving selection on leave");
        loggedOutPlayerSelections.put(event.getPlayer().getUniqueId(), players.get(event.getPlayer()).getSelection());

        logger.fine("Unregistering player data on leave for player: %s", event.getPlayer().getUniqueId());
        if(players.remove(event.getPlayer()) == null) { // Remove the registered data, printing a warning if there wasn't one
            logger.warning("Player left who had unregistered player data. This shouldn't happen");
        }
    }
}
