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
import io.github.illuminationsreimagined.client.BiomeSettingsScreen;
import io.github.illuminationsreimagined.client.DimensionSettingsScreen;
import io.github.illuminationsreimagined.client.IlluminationsConfigScreen;
import io.github.illuminationsreimagined.config.IlluminationsConfig;
import io.github.illuminationsreimagined.config.SeasonalMode;
import io.github.illuminationsreimagined.config.SpawnRate;
import io.github.illuminationsreimagined.particle.AmbientParticle;
import io.github.illuminationsreimagined.particle.EyesParticle;
import io.github.illuminationsreimagined.particle.FireflyParticle;
import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.particle.ParticleTracker;
import io.github.illuminationsreimagined.particle.PoltergeistParticle;
import io.github.illuminationsreimagined.particle.PumpkinSpiritParticle;
import io.github.illuminationsreimagined.particle.Sprites;
import io.github.illuminationsreimagined.particle.WillOWispParticle;
import io.github.illuminationsreimagined.world.BiomeGroup;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end check of every ambient effect in a real client: spawning, block-driven effects, day/night fade-out
 * and tracker cleanup after leaving the world.
 */
public class AmbientEffectsClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        testConfigScreens(context);

        // Make every effect frequent so the test is fast and deterministic enough.
        context.runOnClient(client -> {
            IlluminationsConfig config = IlluminationsConfig.get();
            config.density = 1000;
            config.biomeGroup(BiomeGroup.PLAINS).plankton = SpawnRate.HIGH;
            config.biomeGroup(BiomeGroup.PLAINS).fireflies = SpawnRate.HIGH;
            config.biomeGroup(BiomeGroup.PLAINS).glowworms = SpawnRate.HIGH;
            config.eyesInTheDark.mode = SeasonalMode.ALWAYS;
            config.eyesInTheDark.rate = SpawnRate.HIGH;
            config.halloweenSpirits.mode = SeasonalMode.ALWAYS;
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            BlockPos p = context.computeOnClient(client -> client.player.blockPosition());
            int x = p.getX(), y = p.getY(), z = p.getZ();
            IlluminationsReimagined.LOGGER.info("[gametest] player at {}", p);

            world.getServer().runCommand("time set midnight");
            world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("gamerule advance_weather false");

            // Sealed, unlit stone room for glowworms and eyes (12-30 blocks away, so beyond the eyes' vanishing distance),
            // large because the spawner samples a wide area thinly, as the original did.
            fill(world, x + 12, y - 1, z - 12, x + 30, y + 8, z + 12, "stone hollow");
            // A canopy of leaves over open air, where glowworms must not appear.
            fill(world, x - 40, y + 6, z + 12, x - 24, y + 6, z + 30, "oak_leaves[persistent=true]");
            // Chorus flower on end stone.
            world.getServer().runCommand(String.format("setblock %d %d %d end_stone", x - 6, y - 1, z + 4));
            world.getServer().runCommand(String.format("setblock %d %d %d chorus_flower", x - 6, y, z + 4));
            // Covered (dark) water pool with a sea lantern at one end.
            fill(world, x - 20, y - 1, z - 8, x - 8, y + 6, z + 8, "stone hollow");
            fill(world, x - 19, y, z - 7, x - 9, y + 5, z + 7, "water");
            // A separate unlit pool for plankton (the lanterns light up the first one), big enough that the spawner's
            // wide sampling lands in it often.
            fill(world, x - 8, y - 1, z - 30, x + 8, y + 5, z - 16, "stone hollow");
            fill(world, x - 7, y, z - 29, x + 7, y + 4, z - 17, "water");
            // Many lanterns: the original spreads crystals ±15 blocks around a lantern and favours dim water, so few land in
            // this pool per lantern.
            for (int[] l : new int[][]{{-10, 2, 0}, {-12, 1, -4}, {-12, 1, 4}, {-15, 3, 0}, {-10, 1, -6}, {-10, 1, 6},
                    {-14, 1, -6}, {-14, 1, 6}, {-18, 2, -3}, {-18, 2, 3}, {-16, 4, 0}, {-11, 4, 0}}) {
                world.getServer().runCommand(String.format("setblock %d %d %d sea_lantern", x + l[0], y + l[1], z + l[2]));
            }
            // Halloween spirit sources: 5x5 patches of jack o'lanterns and skeleton skulls close to the player, so random
            // display ticks reach them often enough for a reliable test (each has a 1-in-100 chance per hit).
            fill(world, x + 3, y, z - 7, x + 7, y, z - 3, "jack_o_lantern");
            fill(world, x + 3, y, z + 3, x + 7, y, z + 7, "skeleton_skull");
            // Soul lantern.
            world.getServer().runCommand(String.format("setblock %d %d %d soul_lantern", x - 4, y, z - 6));
            // Face the chorus flower / pool.
            world.getServer().runCommand(String.format("tp @p %d %d %d 90 10", x, y, z));

            // Track the highest count seen for each effect over 20 seconds (rare, fast spirits may come and go).
            Map<ParticleKind, Integer> night = new EnumMap<>(ParticleKind.class);
            int[] maxStuck = {0};
            int[] spiritSamples = {0};
            for (int i = 0; i < 20; i++) {
                context.waitTicks(20);
                counts(context).forEach((kind, count) -> night.merge(kind, count, Math::max));
                int[] stuck = context.computeOnClient(c -> {
                    int inside = 0;
                    int total = 0;
                    for (ParticleKind kind : new ParticleKind[]{ParticleKind.WILL_O_WISP, ParticleKind.PUMPKIN_SPIRIT, ParticleKind.POLTERGEIST}) {
                        for (net.minecraft.world.phys.Vec3 pos : ParticleTracker.positions(kind)) {
                            BlockPos bp = BlockPos.containing(pos);
                            total++;
                            if (!c.level.getBlockState(bp).getCollisionShape(c.level, bp).isEmpty()) {
                                inside++;
                            }
                        }
                    }
                    return new int[]{inside, total};
                });
                maxStuck[0] = Math.max(maxStuck[0], stuck[0]);
                spiritSamples[0] += stuck[1];
            }
            IlluminationsReimagined.LOGGER.info("[gametest] counts after 400 night ticks: {}", night);
            IlluminationsReimagined.LOGGER.info("[gametest] spirits inside a block at once (max): {} of {} sampled", maxStuck[0], spiritSamples[0]);
            context.takeScreenshot("illuminations_night_west");
            world.getServer().runCommand(String.format("tp @p %d %d %d -90 0", x, y, z));
            context.waitTicks(5);
            context.takeScreenshot("illuminations_night_east");

            require(night.get(ParticleKind.FIREFLY) > 0, "fireflies spawn at night in plains");
            require(night.get(ParticleKind.GLOWWORM) > 0, "glowworms spawn under a ceiling");
            // Glowworms belong in caves, not under tree canopies: count any hanging from leaves.
            int onLeaves = context.computeOnClient(c -> (int) ParticleTracker.positions(ParticleKind.GLOWWORM).stream()
                    .filter(pos -> c.level.getBlockState(BlockPos.containing(pos.x, pos.y + 0.5, pos.z)).is(BlockTags.LEAVES)).count());
            IlluminationsReimagined.LOGGER.info("[gametest] glowworms hanging from leaves: {}", onLeaves);
            require(onLeaves == 0, "glowworms do not spawn under tree leaves");
            // Natural eyes are rare by design (as in the original), so only log them; check their behaviour directly.
            IlluminationsReimagined.LOGGER.info("[gametest] eyes spawned naturally: {}", night.get(ParticleKind.EYES));
            List<EyesParticle> eyes = context.computeOnClient(c -> {
                EyesParticle inDark = new EyesParticle(c.level, x + 17.5, y + 2.5, z + 0.5);
                EyesParticle nearPlayer = new EyesParticle(c.level, x + 2.5, y + 1.5, z + 0.5);
                ParticleTracker.spawn(inDark);
                ParticleTracker.spawn(nearPlayer);
                return List.of(inDark, nearPlayer);
            });
            context.waitTicks(20);
            List<Boolean> eyesAlive = context.computeOnClient(c -> eyes.stream().map(EyesParticle::isAlive).toList());
            require(eyesAlive.get(0), "eyes keep watching from total darkness");
            require(!eyesAlive.get(1), "eyes close when a player is close");
            require(night.get(ParticleKind.CHORUS_PETAL) > 0, "chorus flower sheds petals");
            require(night.get(ParticleKind.PRISMARINE_CRYSTAL) > 0, "sea lantern spawns crystals");
            require(night.get(ParticleKind.PLANKTON) > 0, "plankton spawns in dark water");
            boolean spiritArt = context.computeOnClient(c -> Sprites.isSkinAvailable(Sprites.PUMPKIN_SPIRIT_SKIN) && Sprites.isSkinAvailable(Sprites.POLTERGEIST_SKIN));
            int spirits = night.get(ParticleKind.PUMPKIN_SPIRIT) + night.get(ParticleKind.POLTERGEIST);
            if (spiritArt) {
                require(night.get(ParticleKind.PUMPKIN_SPIRIT) > 0, "jack o'lanterns release pumpkin spirits");
                require(night.get(ParticleKind.POLTERGEIST) > 0, "skeleton skulls release poltergeists");
            } else {
                require(spirits == 0, "spirits stay dormant while their textures are missing");
            }
            for (ParticleKind kind : ParticleKind.values()) {
                int cap = context.computeOnClient(c -> kind.cap(IlluminationsConfig.get()));
                require(night.get(kind) <= cap, kind + " respects its cap (" + night.get(kind) + " <= " + cap + ")");
            }

            // Breaking the chorus flower bursts petals.
            int petalsBefore = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL));
            world.getServer().runCommand(String.format("setblock %d %d %d air destroy", x - 6, y, z + 4));
            context.waitTicks(3);
            int petalsAfter = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL));
            IlluminationsReimagined.LOGGER.info("[gametest] petals before/after break: {} / {}", petalsBefore, petalsAfter);
            require(petalsAfter > petalsBefore, "breaking a chorus flower bursts petals");

            // Spirits and fireflies that hit the ground keep moving. Vanilla's collision latch used to freeze them for good
            // (spirits spun on the spot) the first time a downward move was fully blocked.
            List<AmbientParticle> fliers = context.computeOnClient(c -> {
                List<AmbientParticle> spawned = new ArrayList<>();
                // Natural fireflies already fill the cap here; make room for these few.
                int fireflyCap = IlluminationsConfig.get().fireflies.maxCount;
                IlluminationsConfig.get().fireflies.maxCount = fireflyCap + 10;
                for (int i = 0; i < 10; i++) {
                    double fx = x - 4 + i;
                    double fz = z + 14;
                    double ground = c.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(fx), (int) Math.floor(fz)) + 0.05;
                    AmbientParticle particle = switch (i) {
                        case 0, 1, 2 -> new WillOWispParticle(c.level, fx, ground, fz);
                        case 3 -> new PumpkinSpiritParticle(c.level, fx, ground, fz);
                        case 4 -> new PoltergeistParticle(c.level, fx, ground, fz);
                        default -> FireflyParticle.create(c.level, fx, ground, fz, 0x9AFF3C);
                    };
                    particle.setParticleSpeed(0.0, i < 5 ? -0.3 : -0.05, 0.0);
                    if (ParticleTracker.spawn(particle)) {
                        spawned.add(particle);
                    }
                }
                IlluminationsConfig.get().fireflies.maxCount = fireflyCap;
                return spawned;
            });
            int frozen = 0;
            int checked = 0;
            List<Vec3> previous = context.computeOnClient(c -> fliers.stream().map(AmbientParticle::currentPosition).toList());
            for (int round = 0; round < 4; round++) {
                context.waitTicks(10);
                List<Vec3> now = context.computeOnClient(c -> fliers.stream().map(AmbientParticle::currentPosition).toList());
                List<Boolean> alive = context.computeOnClient(c -> fliers.stream().map(AmbientParticle::isAlive).toList());
                for (int i = 0; i < fliers.size(); i++) {
                    if (alive.get(i)) {
                        checked++;
                        if (now.get(i).equals(previous.get(i))) {
                            frozen++;
                        }
                    }
                }
                previous = now;
            }
            IlluminationsReimagined.LOGGER.info("[gametest] grounded fliers frozen: {} of {} checks ({} spawned)", frozen, checked, fliers.size());
            require(fliers.size() >= 8 && checked > 0, "spirits and fireflies spawn for the grounding check");
            require(frozen == 0, "spirits and fireflies keep moving after touching the ground");

            // Daylight: fireflies fade out.
            world.getServer().runCommand("time set noon");
            context.waitTicks(200);
            int firefliesByDay = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.FIREFLY));
            IlluminationsReimagined.LOGGER.info("[gametest] fireflies 200 ticks after noon: {}", firefliesByDay);
            require(firefliesByDay == 0, "fireflies disappear during the day");

            // Switching the Overworld off stops new effects there (petals, crystals and lantern wisps spawn by day too),
            // and switching it back on brings them back.
            Identifier overworld = Level.OVERWORLD.identifier();
            // The earlier break test removed the chorus flower; put it back as a steady source.
            world.getServer().runCommand(String.format("setblock %d %d %d chorus_flower", x - 6, y, z + 4));
            context.runOnClient(c -> {
                IlluminationsConfig.get().setDimensionEnabled(overworld, false);
                ParticleTracker.clear();
            });
            context.waitTicks(200);
            int whileOff = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL)
                    + ParticleTracker.count(ParticleKind.PRISMARINE_CRYSTAL) + ParticleTracker.count(ParticleKind.WILL_O_WISP));
            context.runOnClient(c -> IlluminationsConfig.get().setDimensionEnabled(overworld, true));
            context.waitTicks(200);
            int whileOn = context.computeOnClient(c -> ParticleTracker.count(ParticleKind.CHORUS_PETAL)
                    + ParticleTracker.count(ParticleKind.PRISMARINE_CRYSTAL) + ParticleTracker.count(ParticleKind.WILL_O_WISP));
            IlluminationsReimagined.LOGGER.info("[gametest] block effects with the Overworld off / on: {} / {}", whileOff, whileOn);
            require(whileOff == 0, "no effects appear in a dimension that is switched off");
            require(whileOn > 0, "effects return when the dimension is switched back on");
        }

        // After leaving the world, the tracker must not keep stale particles.
        context.waitTicks(5);
        Map<ParticleKind, Integer> after = counts(context);
        IlluminationsReimagined.LOGGER.info("[gametest] counts after leaving world: {}", after);
        require(after.values().stream().allMatch(c -> c == 0), "tracker is empty after leaving the world");
    }

    /** Opens the settings screens, navigates to the biome screen and back, and checks that closing saves the file. */
    private static void testConfigScreens(ClientGameTestContext context) {
        context.setScreen(() -> new IlluminationsConfigScreen(null));
        context.waitForScreen(IlluminationsConfigScreen.class);
        context.takeScreenshot("illuminations_config_screen");
        // The biome button lives inside the scrolling options list, which the test helper cannot click into.
        context.runOnClient(client -> client.gui.setScreen(new BiomeSettingsScreen(client.gui.screen())));
        context.waitForScreen(BiomeSettingsScreen.class);
        context.takeScreenshot("illuminations_biome_screen");
        context.clickScreenButton("gui.done");
        context.waitForScreen(IlluminationsConfigScreen.class);
        context.runOnClient(client -> client.gui.setScreen(new DimensionSettingsScreen(client.gui.screen())));
        context.waitForScreen(DimensionSettingsScreen.class);
        context.takeScreenshot("illuminations_dimension_screen");
        context.clickScreenButton("gui.done");
        context.waitForScreen(IlluminationsConfigScreen.class);
        context.runOnClient(client -> IlluminationsConfig.get().density = 250);
        context.clickScreenButton("gui.done");
        context.waitFor(client -> !(client.gui.screen() instanceof IlluminationsConfigScreen));
        boolean saved = context.computeOnClient(client -> {
            try {
                return java.nio.file.Files.readString(IlluminationsConfig.path()).contains("\"density\": 250");
            } catch (java.io.IOException e) {
                return false;
            }
        });
        require(saved, "closing the config screen saves the config file");
    }

    private static void fill(TestSingleplayerContext world, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        world.getServer().runCommand(String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block));
    }

    private static Map<ParticleKind, Integer> counts(ClientGameTestContext context) {
        return context.computeOnClient(c -> {
            Map<ParticleKind, Integer> map = new EnumMap<>(ParticleKind.class);
            for (ParticleKind kind : ParticleKind.values()) {
                map.put(kind, ParticleTracker.count(kind));
            }
            return map;
        });
    }

    private static void require(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("Expected: " + what);
        }
        IlluminationsReimagined.LOGGER.info("[gametest] OK: {}", what);
    }
}
