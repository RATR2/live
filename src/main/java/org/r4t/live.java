package org.r4t;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class live implements ModInitializer {
    public static final String MOD_ID = "live";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String TEAM_NAME = "live_team";

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
            Scoreboard scoreboard = server.getScoreboard();
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team != null) {
                scoreboard.removePlayerFromTeam(handler.player.getEntityName(), team);
            }
        });
    }

    private boolean toggleLiveStatus(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        Scoreboard scoreboard = server.getScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);

        if (team == null) {
            team = scoreboard.addTeam(TEAM_NAME);
            team.setPrefix(Text.literal("⬤ ").formatted(Formatting.RED));
            team.setColor(Formatting.WHITE);
        }

        boolean isCurrentlyLive = player.getScoreboardTeam() != null &&
                player.getScoreboardTeam().getName().equals(TEAM_NAME);

        if (!isCurrentlyLive) {
            scoreboard.addPlayerToTeam(player.getEntityName(), team);
            return true;
        } else {
            scoreboard.removePlayerFromTeam(player.getEntityName(), team);
            return false;
        }
    }
}