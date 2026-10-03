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
import io.github.illuminationsreimagined.config.SeasonalMode;
import io.github.illuminationsreimagined.particle.ChorusPetalParticle;
import io.github.illuminationsreimagined.particle.EyesParticle;
import io.github.illuminationsreimagined.particle.FireflyParticle;
import io.github.illuminationsreimagined.particle.GlowwormParticle;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PlanktonParticle;
import io.github.illuminationsreimagined.particle.PoltergeistParticle;
import io.github.illuminationsreimagined.particle.PrismarineCrystalParticle;
import io.github.illuminationsreimagined.particle.PumpkinSpiritParticle;
import io.github.illuminationsreimagined.particle.Sprites;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import io.github.illuminationsreimagined.world.BiomeGroup;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import java.nio.file.Path;

/**
 * Opt-in single showcase screenshot of every effect (run with {@code -Pshowcase}): a small night-time scene in front of
 * a stone-brick wall with all effects spawned in view, saved to {@code fabric/build/gallery/showcase.png}.
 */
public class ShowcaseClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("illuminations_reimagined.showcase")) {
            return;
        }
        Path out = Path.of(System.getProperty("illuminations_reimagined.galleryDir", "gallery"));
        context.runOnClient(client -> {
            IlluminationsConfig.resetToDefaults();
            IlluminationsConfig config = IlluminationsConfig.get();
            config.enabled = false; // nothing spawns on its own; the scene is placed by hand below
            config.fireflies.autumnColors = SeasonalMode.DISABLED; // year-round green
            config.eyesInTheDark.mode = SeasonalMode.ALWAYS; // keep the eyes open whatever the date
            if (!client.gui.hud.isHidden()) {
                client.gui.hud.toggle();
            }
        });
        boolean skins = context.computeOnClient(client -> Sprites.isSkinAvailable(Sprites.WISP_SKIN)
                && Sprites.isSkinAvailable(Sprites.PUMPKIN_SPIRIT_SKIN) && Sprites.isSkinAvailable(Sprites.POLTERGEIST_SKIN));
        if (!skins) {
            IlluminationsReimagined.LOGGER.warn("[showcase] spirit skins are missing; spirits will render without textures");
        }

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition());
            int x = p.getX();
            int y = p.getY();
            int z = p.getZ();
            for (String command : new String[]{"time set midnight", "gamerule advance_time false", "gamerule advance_weather false",
                    "weather clear", "gamemode spectator @a"}) {
                server.runCommand(command);
            }
            // Backdrop wall with an overhang for glowworms.
            fill(server, x + 10, y, z - 9, x + 10, y + 7, z + 9, "stone_bricks");
            fill(server, x + 8, y + 4, z - 3, x + 9, y + 4, z + 1, "stone_bricks");
            // Water tank with a sea lantern (plankton, prismarine crystals).
            fill(server, x + 6, y, z - 7, x + 8, y + 3, z - 4, "glass");
            fill(server, x + 7, y + 1, z - 6, x + 7, y + 3, z - 5, "water");
            server.runCommand(String.format("setblock %d %d %d sea_lantern", x + 7, y, z - 6));
            // Chorus flower on end stone.
            server.runCommand(String.format("setblock %d %d %d end_stone", x + 8, y - 1, z + 3));
            server.runCommand(String.format("setblock %d %d %d chorus_plant", x + 8, y, z + 3));
            server.runCommand(String.format("setblock %d %d %d chorus_flower", x + 8, y + 1, z + 3));
            // A 5-block tunnel into a stone mass behind the wall: deep enough that no light reaches the far end, where the
            // eyes watch from the dark.
            fill(server, x + 11, y, z - 3, x + 17, y + 3, z + 1, "stone");
            fill(server, x + 10, y + 1, z - 1, x + 16, y + 2, z - 1, "air");
            server.runCommand(String.format("tp @a %d %d %d facing %d %d %d", x + 2, y + 2, z, x + 10, y + 2, z));
            context.waitTicks(40);
            world.getConnection().waitForChunksRender();

            context.runOnClient(client -> {
                ClientLevel level = client.level;
                RandomSource r = level.getRandom();
                int green = IlluminationsConfig.get().biomeGroup(BiomeGroup.SWAMP).fireflyColorRgb();
                for (int i = 0; i < 14; i++) {
                    ParticleTracker.spawn(FireflyParticle.create(level, x + 3 + r.nextDouble() * 6, y + 1 + r.nextDouble() * 4, z - 8 + r.nextDouble() * 16, green));
                }
                for (int i = 0; i < 8; i++) {
                    ParticleTracker.spawn(new GlowwormParticle(level, x + 8.15 + r.nextDouble() * 1.7, y + 4, z - 2.85 + r.nextDouble() * 4.7));
                }
                for (int i = 0; i < 25; i++) {
                    ParticleTracker.spawn(new PlanktonParticle(level, x + 7.1 + r.nextDouble() * 0.8, y + 1.1 + r.nextDouble() * 2.7, z - 5.9 + r.nextDouble() * 1.8));
                }
                for (int i = 0; i < 20; i++) {
                    ParticleTracker.spawn(new PrismarineCrystalParticle(level, x + 7.1 + r.nextDouble() * 0.8, y + 1.5 + r.nextDouble() * 2.3, z - 5.9 + r.nextDouble() * 1.8));
                }
                ParticleTracker.spawn(new EyesParticle(level, x + 16.5, y + 1.6, z - 0.5));
            });
            context.waitTicks(40);

            context.runOnClient(client -> {
                ClientLevel level = client.level;
                RandomSource r = level.getRandom();
                ParticleTracker.spawn(new WillOWispParticle(level, x + 7, y + 4.5, z - 3));
                ParticleTracker.spawn(new WillOWispParticle(level, x + 7, y + 5, z + 2.5));
                ParticleTracker.spawn(new PumpkinSpiritParticle(level, x + 6, y + 3, z + 2));
                ParticleTracker.spawn(new PoltergeistParticle(level, x + 6, y + 3.5, z - 1.5));
                for (int i = 0; i < 20; i++) {
                    ParticleTracker.spawn(new ChorusPetalParticle(level, x + 8.5, y + 1.6, z + 3.5,
                            r.nextGaussian() * 0.05, r.nextGaussian() * 0.04 + 0.04, r.nextGaussian() * 0.05, true));
                }
            });
            context.waitTicks(6);
            String counts = context.computeOnClient(client -> ParticleTracker.describe());
            int eyeLight = context.computeOnClient(client -> client.level.getMaxLocalRawBrightness(new BlockPos(x + 16, y + 1, z - 1)));
            IlluminationsReimagined.LOGGER.info("[showcase] light at the eyes: {}", eyeLight);
            IlluminationsReimagined.LOGGER.info("[showcase] {}", counts);
            context.takeScreenshot(TestScreenshotOptions.of("showcase").withSize(1920, 1080).disableCounterPrefix().withDestinationDir(out));
        }
    }

    private static void fill(TestServerContext server, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        server.runCommand(String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block));
    }
}
