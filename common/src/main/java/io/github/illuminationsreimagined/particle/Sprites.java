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
package io.github.illuminationsreimagined.particle;

import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Sprite lookup for the mod's particles.
 *
 * <p>Our textures live in {@code assets/illuminations_reimagined/textures/particle/}, which vanilla's particle
 * atlas definition stitches automatically. Particles are created directly rather than through registered
 * {@code ParticleType}s, so sprites are fetched from the atlas by ID. Particles are cleared on every resource reload,
 * so no particle ever keeps a sprite from a previous atlas.</p>
 */
public final class Sprites {
    public static final Identifier FIREFLY_GLOW = id("firefly_glow");
    public static final Identifier FIREFLY_CORE = id("firefly_core");
    public static final Identifier GLOWWORM = id("glowworm");
    public static final Identifier PLANKTON = id("plankton");
    public static final Identifier[] EYES = {id("eyes_0"), id("eyes_1"), id("eyes_2"), id("eyes_3")};
    public static final Identifier[] CHORUS_PETAL = {id("chorus_petal_0"), id("chorus_petal_1"), id("chorus_petal_2")};
    public static final Identifier[] PRISMARINE_CRYSTAL = {id("prismarine_crystal_0"), id("prismarine_crystal_1"), id("prismarine_crystal_2")};
    public static final Identifier[] WISP = {id("wisp_0"), id("wisp_1"), id("wisp_2"), id("wisp_3")};
    public static final Identifier WISP_EMBER = id("wisp_ember");
    public static final Identifier[] PUMPKIN_SPIRIT = {id("pumpkin_spirit_0"), id("pumpkin_spirit_1"), id("pumpkin_spirit_2"), id("pumpkin_spirit_3")};
    public static final Identifier[] POLTERGEIST = {id("poltergeist_0"), id("poltergeist_1"), id("poltergeist_2"), id("poltergeist_3")};

    private Sprites() {
    }

    public static TextureAtlasSprite get(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(id);
    }

    /**
     * Whether a texture actually exists in the atlas. Effects whose artwork has not been added yet stay dormant
     * instead of rendering the missing-texture checkerboard.
     */
    public static boolean isAvailable(Identifier id) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
        return atlas.getSprite(id) != atlas.missingSprite();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(IlluminationsReimagined.MOD_ID, path);
    }
}
