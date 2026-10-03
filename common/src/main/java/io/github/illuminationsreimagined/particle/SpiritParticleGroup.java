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

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders spirits as small 3D heads through the vanilla particle pipeline (the same approach vanilla uses for the
 * elder guardian particle). Registered with each loader's particle-group API, so no engine internals are patched.
 */
public class SpiritParticleGroup extends ParticleGroup<WanderingSpiritParticle> {
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(IlluminationsReimagined.MOD_ID + ":spirits", "IRS");

    public SpiritParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        Vec3 cam = camera.position();
        List<Entry> entries = new ArrayList<>(this.particles.size());
        for (WanderingSpiritParticle particle : this.particles) {
            if (!frustum.isVisible(particle.getBoundingBox().inflate(0.5))) {
                continue;
            }
            Vec3 pos = particle.renderPosition(partialTickTime);
            PoseStack poseStack = new PoseStack();
            poseStack.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            // Face the direction of travel, like the original model did.
            poseStack.rotateAround(new Quaternionf().rotationY(Mth.DEG_TO_RAD * (particle.renderYaw(partialTickTime) - 180.0F)), 0.0F, 0.0F, 0.0F);
            poseStack.rotateAround(new Quaternionf().rotationX(Mth.DEG_TO_RAD * particle.renderPitch(partialTickTime)), 0.0F, 0.0F, 0.0F);
            // Flip X and Y (a 180-degree turn about Z, as vanilla does for mob models) so the skin is not mirrored.
            poseStack.scale(-0.5F, -0.5F, 0.5F);
            poseStack.translate(0.0F, -1.0F, 0.0F);
            int color = ARGB.colorFromFloat(particle.renderAlpha(), 1.0F, 1.0F, 1.0F);
            entries.add(new Entry(poseStack, RenderTypes.entityTranslucentEmissive(particle.skin()), color));
        }
        return new State(entries);
    }

    private record Entry(PoseStack poseStack, RenderType renderType, int color) {
    }

    private record State(List<Entry> entries) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            ModelPart model = SpiritModel.root();
            for (Entry entry : this.entries) {
                collector.submitCustomGeometry(entry.poseStack(), entry.renderType(),
                        (pose, buffer) -> model.render(entry.poseStack(), buffer, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, entry.color()));
            }
        }
    }

    static float lerpDegrees(float partialTick, float from, float to) {
        return from + Mth.wrapDegrees(to - from) * partialTick;
    }
}
