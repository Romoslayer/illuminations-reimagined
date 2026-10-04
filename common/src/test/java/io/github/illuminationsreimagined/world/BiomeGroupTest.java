/*
 * Illuminations Reimagined - an unofficial continuation of Illuminations
 * Copyright (C) 2026 Romoslayer
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

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import org.junit.jupiter.api.Test;

import static io.github.illuminationsreimagined.world.TestBiomes.biome;
import static io.github.illuminationsreimagined.world.TestBiomes.convention;
import static io.github.illuminationsreimagined.world.TestBiomes.modded;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Classification with the overlapping tags vanilla and the {@code c:} conventions really give these biomes. */
class BiomeGroupTest {
    @Test
    void meadowIsPlainsAlthoughVanillaTagsItAsMountain() {
        // Vanilla's is_mountain tag lists minecraft:meadow on both supported versions.
        assertEquals(BiomeGroup.PLAINS, BiomeGroup.classify(biome(Biomes.MEADOW, BiomeTags.IS_MOUNTAIN, BiomeTags.IS_OVERWORLD)));
    }

    @Test
    void plainsStayPlains() {
        assertEquals(BiomeGroup.PLAINS, BiomeGroup.classify(biome(Biomes.PLAINS, BiomeTags.IS_OVERWORLD, convention("is_plains"))));
        assertEquals(BiomeGroup.PLAINS, BiomeGroup.classify(biome(Biomes.SUNFLOWER_PLAINS, BiomeTags.IS_OVERWORLD)));
    }

    @Test
    void mountainsStayMountains() {
        assertEquals(BiomeGroup.MOUNTAINS, BiomeGroup.classify(biome(Biomes.JAGGED_PEAKS, BiomeTags.IS_MOUNTAIN, BiomeTags.IS_OVERWORLD)));
        assertEquals(BiomeGroup.MOUNTAINS, BiomeGroup.classify(biome(Biomes.CHERRY_GROVE, BiomeTags.IS_MOUNTAIN, BiomeTags.IS_OVERWORLD)));
        assertEquals(BiomeGroup.MOUNTAINS, BiomeGroup.classify(biome(Biomes.WINDSWEPT_HILLS, BiomeTags.IS_HILL, BiomeTags.IS_OVERWORLD)));
    }

    @Test
    void snowyMountainsStaySnowy() {
        assertEquals(BiomeGroup.SNOWY, BiomeGroup.classify(biome(Biomes.SNOWY_SLOPES, BiomeTags.IS_MOUNTAIN, BiomeTags.IS_OVERWORLD, convention("is_snowy"))));
        assertEquals(BiomeGroup.SNOWY, BiomeGroup.classify(biome(Biomes.GROVE, BiomeTags.IS_MOUNTAIN, BiomeTags.IS_FOREST, convention("is_snowy"))));
    }

    @Test
    void moddedMountainTaggedPlainsKeepsMountainPriority() {
        // Only vanilla's meadow is special-cased; modded biomes keep following their tags in the usual order.
        assertEquals(BiomeGroup.MOUNTAINS, BiomeGroup.classify(modded("alpine_meadow", BiomeTags.IS_MOUNTAIN, convention("is_plains"), BiomeTags.IS_OVERWORLD)));
        assertEquals(BiomeGroup.PLAINS, BiomeGroup.classify(modded("prairie", convention("is_plains"), BiomeTags.IS_OVERWORLD)));
    }

    @Test
    void otherGroupsUnchanged() {
        assertEquals(BiomeGroup.SWAMP, BiomeGroup.classify(biome(Biomes.SWAMP, BiomeTags.IS_OVERWORLD)));
        assertEquals(BiomeGroup.FOREST, BiomeGroup.classify(biome(Biomes.FOREST, BiomeTags.IS_FOREST, BiomeTags.IS_OVERWORLD)));
        assertEquals(BiomeGroup.SOUL_SAND_VALLEY, BiomeGroup.classify(biome(Biomes.SOUL_SAND_VALLEY, BiomeTags.IS_NETHER)));
        assertEquals(BiomeGroup.END, BiomeGroup.classify(biome(Biomes.THE_END, BiomeTags.IS_END)));
        assertEquals(BiomeGroup.OTHER, BiomeGroup.classify(modded("void")));
    }
}
