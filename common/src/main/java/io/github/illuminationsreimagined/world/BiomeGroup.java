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
package io.github.illuminationsreimagined.world;

import io.github.illuminationsreimagined.config.SpawnRate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Locale;
import java.util.Set;

/**
 * Coarse biome classification used for spawn rules and firefly colours.
 *
 * <p>The original mod used {@code Biome.Category} plus a hard-coded list of pre-1.18 biome IDs. The category
 * enum no longer exists, and modded biomes fell through or crashed. Classification now uses vanilla biome tags and
 * the {@code c:} convention tags shared by Fabric and NeoForge, with explicit vanilla keys as a fallback, so modded
 * biomes that tag themselves correctly get sensible behaviour automatically.</p>
 */
public enum BiomeGroup {
    // Defaults are the original mod's (DefaultConfig.BIOME_SETTINGS), mapped from its biome categories.
    // name               firefly            glowworm           plankton           firefly colour
    SWAMP(SpawnRate.HIGH, SpawnRate.HIGH, SpawnRate.DISABLED, 0x009F00),
    FOREST(SpawnRate.MEDIUM, SpawnRate.MEDIUM, SpawnRate.DISABLED, 0xBFFF00),
    JUNGLE(SpawnRate.LOW, SpawnRate.LOW, SpawnRate.DISABLED, 0x00FF21),
    TAIGA(SpawnRate.LOW, SpawnRate.LOW, SpawnRate.DISABLED, 0xBFFF00),
    PLAINS(SpawnRate.LOW, SpawnRate.LOW, SpawnRate.DISABLED, 0xBFFF00),
    SAVANNA(SpawnRate.LOW, SpawnRate.LOW, SpawnRate.DISABLED, 0xBFFF00),
    RIVER(SpawnRate.MEDIUM, SpawnRate.MEDIUM, SpawnRate.DISABLED, 0xBFFF00),
    LUSH_CAVES(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xEB8931),
    CAVES(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00),
    OCEAN(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.HIGH, 0xBFFF00),
    WARM_OCEAN(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.HIGH, 0xBFFF00),
    BEACH(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00),
    SNOWY(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0x00BFFF),
    DESERT(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xFFA755),
    BADLANDS(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00),
    MOUNTAINS(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00),
    MUSHROOM(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xFF7F8F),
    OTHER_OVERWORLD(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00),
    SOUL_SAND_VALLEY(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0x00FFFF),
    NETHER(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xFF8000),
    END(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0x8000FF),
    OTHER(SpawnRate.DISABLED, SpawnRate.DISABLED, SpawnRate.DISABLED, 0xBFFF00);

    public final SpawnRate defaultFireflies;
    public final SpawnRate defaultGlowworms;
    public final SpawnRate defaultPlankton;
    public final int defaultFireflyColor;

    BiomeGroup(SpawnRate fireflies, SpawnRate glowworms, SpawnRate plankton, int fireflyColor) {
        this.defaultFireflies = fireflies;
        this.defaultGlowworms = glowworms;
        this.defaultPlankton = plankton;
        this.defaultFireflyColor = fireflyColor;
    }

    /** Stable lower-case key used in the config file. */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    private static final TagKey<Biome> C_IS_SWAMP = conventionTag("is_swamp");
    private static final TagKey<Biome> C_IS_SNOWY = conventionTag("is_snowy");
    private static final TagKey<Biome> C_IS_ICY = conventionTag("is_icy");
    private static final TagKey<Biome> C_IS_DESERT = conventionTag("is_desert");
    private static final TagKey<Biome> C_IS_PLAINS = conventionTag("is_plains");
    private static final TagKey<Biome> C_IS_MUSHROOM = conventionTag("is_mushroom");
    private static final TagKey<Biome> C_IS_CAVE = conventionTag("is_cave");
    private static final TagKey<Biome> C_IS_UNDERGROUND = conventionTag("is_underground");

    private static final Set<ResourceKey<Biome>> WARM_OCEANS = Set.of(Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);
    private static final Set<ResourceKey<Biome>> PLAINS_KEYS = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS);
    private static final Set<ResourceKey<Biome>> SWAMP_KEYS = Set.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);

    /** Classifies a biome. The order matters: more specific groups are tested first. */
    public static BiomeGroup classify(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_NETHER)) {
            return biome.is(Biomes.SOUL_SAND_VALLEY) ? SOUL_SAND_VALLEY : NETHER;
        }
        if (biome.is(BiomeTags.IS_END)) {
            return END;
        }
        if (biome.is(Biomes.LUSH_CAVES)) {
            return LUSH_CAVES;
        }
        if (biome.is(C_IS_CAVE) || biome.is(C_IS_UNDERGROUND) || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.DEEP_DARK)) {
            return CAVES;
        }
        if (biome.is(C_IS_SWAMP) || SWAMP_KEYS.stream().anyMatch(biome::is)) {
            return SWAMP;
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) {
            return WARM_OCEANS.stream().anyMatch(biome::is) ? WARM_OCEAN : OCEAN;
        }
        if (biome.is(BiomeTags.IS_RIVER)) {
            return RIVER;
        }
        if (biome.is(BiomeTags.IS_BEACH)) {
            return BEACH;
        }
        if (biome.is(C_IS_SNOWY) || biome.is(C_IS_ICY)) {
            return SNOWY;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return JUNGLE;
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return TAIGA;
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return FOREST;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return SAVANNA;
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return BADLANDS;
        }
        if (biome.is(C_IS_DESERT) || biome.is(Biomes.DESERT)) {
            return DESERT;
        }
        // Vanilla tags meadows as mountains; they belong with plains ("Plains and Meadows" in the settings).
        if (biome.is(Biomes.MEADOW)) {
            return PLAINS;
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) {
            return MOUNTAINS;
        }
        if (biome.is(C_IS_PLAINS) || PLAINS_KEYS.stream().anyMatch(biome::is)) {
            return PLAINS;
        }
        if (biome.is(C_IS_MUSHROOM) || biome.is(Biomes.MUSHROOM_FIELDS)) {
            return MUSHROOM;
        }
        if (biome.is(BiomeTags.IS_OVERWORLD)) {
            return OTHER_OVERWORLD;
        }
        return OTHER;
    }

    private static TagKey<Biome> conventionTag(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", path));
    }
}
