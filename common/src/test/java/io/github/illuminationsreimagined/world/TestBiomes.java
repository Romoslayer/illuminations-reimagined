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
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Method;
import java.util.List;

/** Stand-alone biome holders with chosen tags, the way a registry binds them after (re)loading tags. */
final class TestBiomes {
    private static final HolderOwner<Biome> OWNER = new HolderOwner<>() {
    };

    private TestBiomes() {
    }

    @SafeVarargs
    static Holder.Reference<Biome> biome(ResourceKey<Biome> key, TagKey<Biome>... tags) {
        Holder.Reference<Biome> holder = Holder.Reference.createStandAlone(OWNER, key);
        retag(holder, tags);
        return holder;
    }

    @SafeVarargs
    static Holder.Reference<Biome> modded(String path, TagKey<Biome>... tags) {
        return biome(ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("testmod", path)), tags);
    }

    /** Replaces the holder's tags, as a tag sync from the server does. */
    @SafeVarargs
    static void retag(Holder.Reference<Biome> holder, TagKey<Biome>... tags) {
        try {
            Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", java.util.Collection.class);
            bindTags.setAccessible(true);
            bindTags.invoke(holder, List.of(tags));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Holder.Reference.bindTags is unavailable", e);
        }
    }

    static TagKey<Biome> convention(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", path));
    }
}
