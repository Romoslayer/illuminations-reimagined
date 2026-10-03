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

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/**
 * The 3D head shared by will o' wisps, pumpkin spirits and poltergeists: a 6x6x6 cube inside a slightly larger
 * see-through 6x7x6 shell, textured from a 32x32 skin. The geometry follows the original Illuminations wisp model
 * (GPL-3.0-or-later); each spirit supplies its own newly made skin.
 *
 * <p>Skin layout (32x32): the inner cube uses the 24x12 area at (0, 0); the outer shell uses the 24x13 area at
 * (0, 16). Each area is the standard Minecraft box unwrap: top and bottom faces in the first row, then the four
 * side faces.</p>
 */
public final class SpiritModel {
    private static ModelPart root;

    private SpiritModel() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                        .texOffs(0, 16).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** The baked model. It is plain geometry (no texture), so it is built once and reused. */
    public static ModelPart root() {
        if (root == null) {
            root = createLayer().bakeRoot();
        }
        return root;
    }
}
