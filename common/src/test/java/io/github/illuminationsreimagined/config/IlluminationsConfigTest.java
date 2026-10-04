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
package io.github.illuminationsreimagined.config;

import io.github.illuminationsreimagined.particle.ParticleKind;
import io.github.illuminationsreimagined.world.BiomeGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IlluminationsConfigTest {
    @TempDir
    Path dir;

    private Path write(String json) throws IOException {
        Path file = this.dir.resolve("illuminations_reimagined.json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void capsUpToTheirMaximumSurviveSanitizing() {
        IlluminationsConfig config = new IlluminationsConfig();
        config.eyesInTheDark.maxCount = IlluminationsConfig.MAX_EYES;
        config.willOWisps.maxCount = IlluminationsConfig.MAX_WISPS;
        config.halloweenSpirits.maxCount = IlluminationsConfig.MAX_HALLOWEEN_SPIRITS;
        config.chorusPetals.maxCount = IlluminationsConfig.MAX_CHORUS_PETALS;
        config.sanitize();
        assertEquals(200, config.eyesInTheDark.maxCount);
        assertEquals(200, config.willOWisps.maxCount);
        assertEquals(200, config.halloweenSpirits.maxCount);
        assertEquals(4000, config.chorusPetals.maxCount);
        config.eyesInTheDark.maxCount = 201;
        config.density = -5;
        config.sanitize();
        assertEquals(200, config.eyesInTheDark.maxCount);
        assertEquals(0, config.density);
    }

    @Test
    void halloweenCapAppliesToEachSpiritType() {
        IlluminationsConfig config = new IlluminationsConfig();
        config.halloweenSpirits.maxCount = 16;
        config.willOWisps.maxCount = 16;
        assertEquals(16, ParticleKind.PUMPKIN_SPIRIT.cap(config));
        assertEquals(16, ParticleKind.POLTERGEIST.cap(config));
        // 300 trail sparks for every spirit that can be alive: 16 wisps + 16 pumpkin spirits + 16 poltergeists.
        assertEquals(48 * 300, ParticleKind.WISP_EMBER.cap(config));
    }

    @Test
    void invalidValuesAreRepaired() throws IOException {
        Path file = write("""
                {"configVersion": 1, "density": 5000, "eyesInTheDark": {"mode": "SOMETIMES", "rate": null, "maxCount": -3},
                 "fireflies": null, "biomeGroups": {"plains": {"fireflies": "LOUD", "fireflyColor": "#12345"}},
                 "disabledDimensions": ["minecraft:the_end", "the_end", "Not An Id!", null]}
                """);
        IlluminationsConfig.load(file);
        IlluminationsConfig config = IlluminationsConfig.get();
        assertEquals(1000, config.density);
        assertEquals(SeasonalMode.SEASONAL, config.eyesInTheDark.mode);
        assertEquals(SpawnRate.MEDIUM, config.eyesInTheDark.rate);
        assertEquals(0, config.eyesInTheDark.maxCount);
        assertNotNull(config.fireflies);
        assertEquals(BiomeGroup.PLAINS.defaultFireflies, config.biomeGroup(BiomeGroup.PLAINS).fireflies);
        assertEquals(BiomeGroup.PLAINS.defaultFireflyColor, config.biomeGroup(BiomeGroup.PLAINS).fireflyColorRgb());
        assertEquals(java.util.List.of("minecraft:the_end"), config.disabledDimensions);
        // The current version is normalised and written back.
        assertTrue(Files.readString(file).contains("\"density\": 1000"));
    }

    @Test
    void newerConfigIsUsedButNeverOverwritten() throws IOException {
        String json = "{\"configVersion\": 99, \"density\": 300, \"futureSetting\": true}";
        Path file = write(json);
        IlluminationsConfig.load(file);
        IlluminationsConfig config = IlluminationsConfig.get();
        assertEquals(300, config.density, "settings this version understands still apply");
        assertEquals(json, Files.readString(file), "startup leaves the newer file alone");
        // What the settings screen does when it closes.
        config.density = 400;
        config.sanitize();
        config.save(file);
        assertEquals(json, Files.readString(file), "closing the settings screen leaves the newer file alone");
        IlluminationsConfig.resetToDefaults(file);
        assertEquals(json, Files.readString(file), "resetting leaves the newer file alone");
        assertEquals(100, IlluminationsConfig.get().density);
    }

    @Test
    void malformedConfigIsBackedUpBeforeDefaultsAreWritten() throws IOException {
        String broken = "{\"density\": 300,";
        Path file = write(broken);
        IlluminationsConfig.load(file);
        assertEquals(100, IlluminationsConfig.get().density);
        assertEquals(broken, Files.readString(file.resolveSibling(file.getFileName() + ".broken")));
        assertTrue(Files.readString(file).contains("\"density\": 100"));
    }

    @Test
    void failedSaveKeepsTheExistingFile() throws IOException {
        Path file = write("{\"configVersion\": 1, \"density\": 300}");
        String before = Files.readString(file);
        // A directory where the temporary file should go makes the write fail.
        Files.createDirectory(file.resolveSibling(file.getFileName() + ".tmp"));
        IlluminationsConfig config = new IlluminationsConfig();
        config.sanitize();
        config.save(file);
        assertEquals(before, Files.readString(file));
        assertFalse(Files.isRegularFile(file.resolveSibling(file.getFileName() + ".tmp")));
    }
}
