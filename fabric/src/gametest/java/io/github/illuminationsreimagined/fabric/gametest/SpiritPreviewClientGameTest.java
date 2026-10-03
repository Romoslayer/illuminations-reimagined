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
package io.github.illuminationsreimagined.fabric.gametest;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PoltergeistParticle;
import io.github.illuminationsreimagined.particle.PumpkinSpiritParticle;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;

/**
 * Opt-in visual check of the 3D spirits and their trails (run with {@code -PspiritPreview}). Spawns spirits a few
 * blocks in front of the camera at night on a flat world and saves a short sequence of screenshots.
 */
public class SpiritPreviewClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("illuminations_reimagined.spiritPreview")) {
            return;
        }
        Path out = Path.of(System.getProperty("illuminations_reimagined.galleryDir", "gallery")).resolve("spirit-preview");
        context.runOnClient(client -> {
            IlluminationsConfig.resetToDefaults();
            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition());
            for (String command : new String[]{"time set midnight", "gamerule advance_time false", "gamemode spectator @a"}) {
                world.getServer().runCommand(command);
            }
            world.getServer().runCommand(String.format("tp @a %d %d %d -90 -10", p.getX(), p.getY() + 2, p.getZ()));
            context.waitTicks(20);
            context.runOnClient(client -> {
                for (int i = 0; i < 3; i++) {
                    ParticleTracker.spawn(new WillOWispParticle(client.level, p.getX() + 6 + i, p.getY() + 3, p.getZ() - 2 + i * 2));
                }
                ParticleTracker.spawn(new PumpkinSpiritParticle(client.level, p.getX() + 6, p.getY() + 2, p.getZ() + 4));
                ParticleTracker.spawn(new PoltergeistParticle(client.level, p.getX() + 6, p.getY() + 2, p.getZ() - 4));
            });
            for (int shot = 0; shot < 6; shot++) {
                context.waitTicks(shot == 0 ? 3 : 15);
                String counts = context.computeOnClient(client -> ParticleTracker.describe());
                IlluminationsReimagined.LOGGER.info("[spirit-preview] shot {}: {}", shot, counts);
                context.takeScreenshot(TestScreenshotOptions.of("spirits_" + shot).withSize(1920, 1080).disableCounterPrefix().withDestinationDir(out));
            }
        }
    }
}
