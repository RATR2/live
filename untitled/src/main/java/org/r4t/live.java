package org.r4t;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class live implements ModInitializer {
    public static final String MOD_ID = "live";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Set<UUID> livePlayers = new HashSet<>();
    private static final String LIVE_INDICATOR = "§c⬤ §r";

    @Override
    public void onInitialize() {
        LOGGER.info("Live Mod initialized!");

        // Register the /live command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("live")
                    .executes(context -> {
                        var source = context.getSource();

                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            boolean isLive = toggleLiveStatus(player);

                            if (isLive) {
                                player.sendMessage(Text.literal("§a[LIVE] §7You are now live!"), false);
                            } else {
                                player.sendMessage(Text.literal("§c[LIVE] §7You are no longer live."), false);
                            }

                            return 1;
                        }

                        source.sendError(Text.literal("This command can only be used by players."));
                        return 0;
                    }));
        });

        // Clean up when players disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            livePlayers.remove(handler.player.getUuid());
        });
    }

    private static boolean toggleLiveStatus(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        boolean isNowLive;

        if (livePlayers.contains(playerId)) {
            livePlayers.remove(playerId);
            isNowLive = false;
        } else {
            livePlayers.add(playerId);
            isNowLive = true;
        }

        updatePlayerTablistName(player, isNowLive);
        return isNowLive;
    }

    private static void updatePlayerTablistName(ServerPlayerEntity player, boolean isLive) {
        String playerName = player.getName().getString();

        if (isLive) {
            player.playerListName = Text.literal(LIVE_INDICATOR + playerName);
        } else {
            player.playerListName = null;
        }
    }
}