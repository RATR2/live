package org.r4t;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class live implements ModInitializer {
    public static final String MOD_ID = "live";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<UUID> livePlayers = new HashSet<>();
    private static final Text LIVE_PREFIX = Text.literal("⬤ ").formatted(Formatting.RED);

    @Override
    public void onInitialize() {
        LOGGER.info("Live Mod initialized!");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("live")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
                            boolean isLive = toggleLiveStatus(player);
                            Text message = isLive
                                    ? Text.literal("You are now live!").formatted(Formatting.GREEN)
                                    : Text.literal("You are no longer live.").formatted(Formatting.RED);
                            player.sendMessage(Text.literal("[LIVE] ").formatted(Formatting.DARK_GRAY).append(message), false);
                            return 1;
                        }
                        return 0;
                    }));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            livePlayers.remove(handler.player.getUuid());
        });
    }
    private boolean toggleLiveStatus(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean isNowLive = !livePlayers.contains(uuid);
        if (isNowLive) {
            livePlayers.add(uuid);
        } else {
            livePlayers.remove(uuid);
        }
        updatePlayerIdentity(player, isNowLive);
        return isNowLive;
    }
    private void updatePlayerIdentity(ServerPlayerEntity player, boolean isLive) {
        Text tabName = isLive
                ? Text.empty().append(LIVE_PREFIX).append(player.getName().copy().formatted(Formatting.WHITE))
                : null;
        player.setCustomName(tabName);
        player.setCustomNameVisible(isLive);
        player.getCommandSource().withEntity(player);
        PlayerListS2CPacket tabPacket = new PlayerListS2CPacket(
                PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME,
                player
        );

        player.getServer().getPlayerManager().sendToAll(tabPacket);
    }
}