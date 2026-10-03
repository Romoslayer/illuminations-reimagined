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
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.illuminationsreimagined.IlluminationsReimagined;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;

/**
 * Draws the light-giving particles (fireflies, glowworms, plankton, eyes, prismarine crystals and spirit trail sparks)
 * with the glowing entity render type the spirits' heads use, so shader packs give them bloom like other glowing things.
 * Each particle still builds its own quads exactly as a vanilla particle would, so without shaders nothing changes.
 */
public class GlowParticleGroup extends ParticleGroup<SingleQuadParticle> {
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType(IlluminationsReimagined.MOD_ID + ":glowing", "IRG");

    private final QuadParticleRenderState quads = new QuadParticleRenderState();

    public GlowParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        this.quads.clear();
        for (SingleQuadParticle particle : this.particles) {
            if (frustum.isVisible(particle.getBoundingBox().inflate(0.5))) {
                particle.extract(this.quads, camera, partialTickTime);
            }
        }
        return new State(this.quads);
    }

    private record State(QuadParticleRenderState quads) implements ParticleGroupRenderState {
        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (this.quads.isEmpty()) {
                return;
            }
            RenderType renderType = RenderTypes.entityTranslucentEmissive(TextureAtlas.LOCATION_PARTICLES);
            for (SingleQuadParticle.Layer layer : this.quads.layers()) {
                collector.submitCustomGeometry(new PoseStack(), renderType,
                        (pose, buffer) -> this.quads.buildLayer(layer, new EntityVertices(buffer)));
            }
        }
    }

    /**
     * Passes particle vertices on to an entity buffer, adding the overlay and normal that the entity vertex format
     * needs and particle vertices do not have.
     */
    private record EntityVertices(VertexConsumer buffer) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.buffer.addVertex(x, y, z);
            this.buffer.setOverlay(OverlayTexture.NO_OVERLAY);
            this.buffer.setNormal(0.0F, 1.0F, 0.0F);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            this.buffer.setColor(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            this.buffer.setColor(argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.buffer.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.buffer.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.buffer.setUv2(u, v);
            return this;
        }

        // Only part of the interface on Minecraft 26.3, so no @Override (it must also compile for 26.2).
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.buffer.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.buffer.setLineWidth(width);
            return this;
        }
    }
}
