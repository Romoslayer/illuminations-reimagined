/*
 * Illuminations Reimagined - an unofficial continuation of Illuminations
 * Copyright (C) 2026 Romoslayer
 * Portions derived from Illuminations, Copyright (C) 2021 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.illuminationsreimagined.neoforge.devpreview;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PoltergeistParticle;
import io.github.illuminationsreimagined.particle.PumpkinSpiritParticle;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.File;

/**
 * Dev-only NeoForge counterpart of the Fabric spirit preview test ({@code -PspiritPreview} on
 * {@code :neoforge:runClient}, with {@code -PquickPlayWorld}). Once in the world it spawns spirits in front of the
 * camera at night, saves six screenshots and quits. Lives in its own source set, so it is never in the release jar.
 */
@EventBusSubscriber(modid = IlluminationsReimagined.MOD_ID, value = Dist.CLIENT)
public final class SpiritPreview {
    private static final boolean ENABLED = Boolean.getBoolean("illuminations_reimagined.spiritPreview");
    private static final int SETUP_TICK = 60;
    private static final int SPAWN_TICK = SETUP_TICK + 40;
    private static final int SHOTS = 6;

    private static int ticksInWorld;
    private static BlockPos origin;

    private SpiritPreview() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        IntegratedServer server = client.getSingleplayerServer();
        if (client.level == null || client.player == null || server == null) {
            return;
        }
        ticksInWorld++;
        if (ticksInWorld == SETUP_TICK) {
            IlluminationsConfig.resetToDefaults();
            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
            origin = client.player.blockPosition().above(2);
            String[] commands = {"time set midnight", "gamerule advance_time false", "gamemode spectator @a",
                    String.format("tp @a %d %d %d -90 -10", origin.getX(), origin.getY(), origin.getZ())};
            server.execute(() -> {
                for (String command : commands) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                }
            });
        } else if (ticksInWorld == SPAWN_TICK) {
            BlockPos p = origin;
            for (int i = 0; i < 3; i++) {
                ParticleTracker.spawn(new WillOWispParticle(client.level, p.getX() + 6 + i, p.getY() + 1, p.getZ() - 2 + i * 2));
            }
            ParticleTracker.spawn(new PumpkinSpiritParticle(client.level, p.getX() + 6, p.getY(), p.getZ() + 4));
            ParticleTracker.spawn(new PoltergeistParticle(client.level, p.getX() + 6, p.getY(), p.getZ() - 4));
        } else if (ticksInWorld > SPAWN_TICK) {
            int sinceSpawn = ticksInWorld - SPAWN_TICK;
            int shot = (sinceSpawn - 3) / 15;
            if ((sinceSpawn - 3) % 15 == 0 && shot < SHOTS) {
                IlluminationsReimagined.LOGGER.info("[spirit-preview] shot {}: {}", shot, ParticleTracker.describe());
                // Screenshot.grab saves into <dir>/screenshots and only creates that last folder itself.
                File dir = new File(System.getProperty("illuminations_reimagined.galleryDir", client.gameDirectory.getPath()), "neoforge-spirit-preview");
                dir.mkdirs();
                Screenshot.grab(dir, "spirits_" + shot + ".png", client.gameRenderer.mainRenderTarget(), 1,
                        message -> IlluminationsReimagined.LOGGER.info("[spirit-preview] {}", message.getString()));
            } else if (shot >= SHOTS && (sinceSpawn - 3) % 15 == 10) {
                client.stop();
            }
        }
    }
}
