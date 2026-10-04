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

import io.github.illuminationsreimagined.config.IlluminationsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks live ambient particles per {@link ParticleKind} so every effect can be capped.
 *
 * <p>Vanilla clears particles without calling {@code remove()} (world change, resource reload, the particle group
 * being full), so counting on add/remove would drift. Instead every tracked particle stamps the tracker's tick
 * counter each time it ticks. Particles that stop ticking for any reason are pruned automatically, which keeps the
 * counts correct through disconnects, dimension changes and reloads.</p>
 */
public final class ParticleTracker {
    /** A particle not ticked for this many tracker ticks is considered gone. */
    private static final int STALE_AFTER = 3;

    private static final Map<ParticleKind, List<AmbientParticle>> LIVE = new EnumMap<>(ParticleKind.class);
    private static long tick;
    private static boolean effectsEnabled = true;
    private static long effectsCheckedAt = -1;

    static {
        for (ParticleKind kind : ParticleKind.values()) {
            LIVE.put(kind, new ArrayList<>());
        }
    }

    private ParticleTracker() {
    }

    static long currentTick() {
        return tick;
    }

    /**
     * Whether effects are allowed in the level (the master switch and the dimension's switch). Read from the config once
     * per tick, so live particles vanish on the first tick after either switch goes off, including the first tick after
     * the game is unpaused.
     */
    static boolean effectsEnabled(ClientLevel level) {
        if (effectsCheckedAt != tick) {
            effectsCheckedAt = tick;
            IlluminationsConfig config = IlluminationsConfig.get();
            effectsEnabled = config.enabled && config.isDimensionEnabled(level);
        }
        return effectsEnabled;
    }

    /** Called once per client tick, only while the particle engine itself is ticking. */
    public static void tick() {
        tick++;
        for (List<AmbientParticle> list : LIVE.values()) {
            if (!list.isEmpty()) {
                list.removeIf(p -> !p.isAlive() || tick - p.lastTrackedTick() > STALE_AFTER);
            }
        }
    }

    public static boolean hasRoom(ParticleKind kind) {
        return LIVE.get(kind).size() < kind.cap(IlluminationsConfig.get());
    }

    public static int count(ParticleKind kind) {
        return LIVE.get(kind).size();
    }

    /** Adds the particle to the engine and starts tracking it, if the cap allows. */
    public static boolean spawn(AmbientParticle particle) {
        ParticleKind kind = particle.kind();
        if (!hasRoom(kind)) {
            return false;
        }
        particle.markTracked(tick);
        LIVE.get(kind).add(particle);
        Minecraft.getInstance().particleEngine.add(particle);
        return true;
    }

    /** Positions of the live particles of one kind (used by tests and debugging tools). */
    public static List<net.minecraft.world.phys.Vec3> positions(ParticleKind kind) {
        List<net.minecraft.world.phys.Vec3> out = new ArrayList<>();
        for (AmbientParticle p : LIVE.get(kind)) {
            if (p.isAlive()) {
                out.add(p.currentPosition());
            }
        }
        return out;
    }

    public static String describe() {
        StringBuilder sb = new StringBuilder();
        LIVE.forEach((kind, list) -> sb.append(kind).append('=').append(list.size()).append(' '));
        return sb.toString().trim();
    }

    public static void clear() {
        LIVE.values().forEach(List::clear);
        effectsCheckedAt = -1;
    }
}
