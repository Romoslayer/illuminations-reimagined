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
package io.github.illuminationsreimagined.spawn;

import io.github.illuminationsreimagined.config.SpawnRate;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Off" must mean off, even on the rare random roll of exactly zero. */
class SpawnRollTest {
    private static final float BASE = 0.0001F;

    /** A legacy random source whose first {@code nextFloat()} is exactly 0, a valid (if rare) output. */
    private static RandomSource zeroFirst() {
        BigInteger modulus = BigInteger.ONE.shiftLeft(48);
        BigInteger multiplier = BigInteger.valueOf(0x5DEECE66DL);
        // Choose the internal state so the next state is 0, then undo the scrambling setSeed applies.
        long state = BigInteger.valueOf(-11).multiply(multiplier.modInverse(modulus)).mod(modulus).longValue();
        RandomSource random = RandomSource.create(state ^ multiplier.longValue());
        return random;
    }

    @Test
    void zeroSourceReallyRollsZero() {
        assertEquals(0.0F, zeroFirst().nextFloat());
    }

    @Test
    void zeroDensityNeverSpawns() {
        assertFalse(AmbientSpawner.roll(zeroFirst(), BASE, SpawnRate.HIGH, 0.0F));
    }

    @Test
    void disabledRateNeverSpawns() {
        assertFalse(AmbientSpawner.roll(zeroFirst(), BASE, SpawnRate.DISABLED, 10.0F));
    }

    @Test
    void positiveChancePassesALowRoll() {
        assertTrue(AmbientSpawner.roll(zeroFirst(), BASE, SpawnRate.LOW, 1.0F));
    }

    @Test
    void comparisonIsStrict() {
        float chance = BASE * SpawnRate.MEDIUM.multiplier;
        assertFalse(AmbientSpawner.passes(chance, chance));
        assertFalse(AmbientSpawner.passes(0.5F, chance));
        assertTrue(AmbientSpawner.passes(Math.nextDown(chance), chance));
        assertFalse(AmbientSpawner.passes(0.0F, 0.0F));
        assertFalse(AmbientSpawner.passes(0.0F, -1.0F));
    }

    @Test
    void eyesFollowTheirOwnRateAndIgnoreDensity() {
        // Eyes ignore density (as in the original) but a disabled frequency must still never pass.
        assertFalse(AmbientSpawner.passes(0.0F, AmbientSpawner.eyesChance(SpawnRate.DISABLED)));
        assertTrue(AmbientSpawner.passes(0.0F, AmbientSpawner.eyesChance(SpawnRate.LOW)));
        assertEquals(0.0001F * 2.5F, AmbientSpawner.eyesChance(SpawnRate.HIGH));
    }
}
