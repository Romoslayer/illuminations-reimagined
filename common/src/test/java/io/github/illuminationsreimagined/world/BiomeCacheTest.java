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

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.github.illuminationsreimagined.world.TestBiomes.biome;
import static io.github.illuminationsreimagined.world.TestBiomes.convention;
import static io.github.illuminationsreimagined.world.TestBiomes.retag;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The biome-group cache must follow tag updates from the server without waiting for a level change. */
class BiomeCacheTest {
    @AfterEach
    void clear() {
        WorldConditions.clearCaches();
    }

    @Test
    void classificationIsCachedBetweenTagUpdates() {
        Holder.Reference<Biome> plains = biome(Biomes.PLAINS, BiomeTags.IS_OVERWORLD);
        assertEquals(BiomeGroup.PLAINS, WorldConditions.biomeGroup(plains));
        // Without a tag update the cached answer stands (no reclassification per sample).
        retag(plains, BiomeTags.IS_OVERWORLD, convention("is_swamp"));
        assertEquals(BiomeGroup.PLAINS, WorldConditions.biomeGroup(plains));
    }

    @Test
    void tagUpdateReclassifies() {
        Holder.Reference<Biome> plains = biome(Biomes.PLAINS, BiomeTags.IS_OVERWORLD);
        assertEquals(BiomeGroup.PLAINS, WorldConditions.biomeGroup(plains));
        retag(plains, BiomeTags.IS_OVERWORLD, convention("is_swamp"));
        WorldConditions.onTagsUpdated();
        assertEquals(BiomeGroup.SWAMP, WorldConditions.biomeGroup(plains));
        retag(plains, BiomeTags.IS_OVERWORLD);
        WorldConditions.onTagsUpdated();
        assertEquals(BiomeGroup.PLAINS, WorldConditions.biomeGroup(plains));
    }
}
