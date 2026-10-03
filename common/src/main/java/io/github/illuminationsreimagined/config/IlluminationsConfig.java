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
package io.github.illuminationsreimagined.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.illuminationsreimagined.IlluminationsReimagined;
import io.github.illuminationsreimagined.platform.Services;
import io.github.illuminationsreimagined.world.BiomeGroup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Client configuration, stored as human-editable JSON in {@code config/illuminations_reimagined.json}.
 *
 * <p>Replaces the original {@code .properties} config, which mixed ambient settings with donor-cosmetic,
 * auto-updater, greeting-screen and donation-toast flags. Unknown or invalid values are corrected on load
 * instead of crashing the game.</p>
 */
public final class IlluminationsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int CURRENT_VERSION = 1;
    private static IlluminationsConfig instance = new IlluminationsConfig();

    public int configVersion = CURRENT_VERSION;

    // ---- global ----
    /** Master switch for all ambient effects. */
    public boolean enabled = true;
    /** Global density multiplier in percent (0 – 1000). */
    public int density = 100;
    // ---- per-effect ----
    public Fireflies fireflies = new Fireflies();
    public Glowworms glowworms = new Glowworms();
    public Plankton plankton = new Plankton();
    public Eyes eyesInTheDark = new Eyes();
    public WillOWisps willOWisps = new WillOWisps();
    public ChorusPetals chorusPetals = new ChorusPetals();
    public PrismarineCrystals prismarineCrystals = new PrismarineCrystals();
    public HalloweenSpirits halloweenSpirits = new HalloweenSpirits();

    /** Per-biome-group overrides, keyed by {@link BiomeGroup#key()}. */
    public Map<String, BiomeGroupSettings> biomeGroups = defaultBiomeGroups();
    /**
     * Dimensions, by ID (for example {@code minecraft:the_end} or a modded one), where no effects appear at all. Empty by
     * default, so effects appear wherever their biome or block rules allow, as in the original mod.
     */
    public List<String> disabledDimensions = new ArrayList<>();
    private transient Set<Identifier> disabledDimensionIds = Set.of();

    public static final class Fireflies {
        public int maxCount = 160;
        /** Spawn during the day as well as at night. */
        public boolean spawnAlways = false;
        /** Allow fireflies below the surface (caves under eligible biomes). */
        public boolean spawnUnderground = false;
        /** Brightness of the white core, in percent (0 – 100). */
        public int coreBrightness = 100;
        /** Random hue for every firefly instead of the biome colour. */
        public boolean rainbow = false;
        /** Orange "autumn" tint during October. Not in the original mod, so off by default. */
        public SeasonalMode autumnColors = SeasonalMode.DISABLED;
        /** Fireflies drift toward nearby light sources they can see. */
        public boolean lightAttraction = true;
    }

    public static final class Glowworms {
        public int maxCount = 200;
    }

    public static final class Plankton {
        /** High because the original had no cap and keeps around a thousand specks alive near oceans at night. */
        public int maxCount = 1000;
    }

    public static final class Eyes {
        public SeasonalMode mode = SeasonalMode.SEASONAL;
        public SpawnRate rate = SpawnRate.MEDIUM;
        public int maxCount = 12;
    }

    public static final class WillOWisps {
        /** Ambient wisps in Soul Sand Valleys. */
        public SpawnRate soulSandValleyRate = SpawnRate.MEDIUM;
        /** Wisps occasionally drifting out of soul lanterns. */
        public boolean fromSoulLanterns = true;
        public int maxCount = 16;
    }

    public static final class ChorusPetals {
        /** Multiplier for petals around chorus flowers (0 disables, max 10). */
        public int multiplier = 1;
        public boolean burstOnBreak = true;
        public int maxCount = 400;
    }

    public static final class PrismarineCrystals {
        public boolean enabled = true;
        public int maxCount = 150;
    }

    public static final class HalloweenSpirits {
        /** When pumpkin spirits and poltergeists appear (only at night). */
        public SeasonalMode mode = SeasonalMode.SEASONAL;
        /** Pumpkin spirits slip out of jack o'lanterns. */
        public boolean fromJackOLanterns = true;
        /** Poltergeists rise from skeleton skulls. */
        public boolean fromSkulls = true;
        /** Poltergeists sometimes escape from undead that die at night. */
        public boolean fromUndeadDeaths = true;
        public int maxCount = 16;
    }

    public static final class BiomeGroupSettings {
        public SpawnRate fireflies;
        public SpawnRate glowworms;
        public SpawnRate plankton;
        /** Firefly colour as {@code #RRGGBB}. */
        public String fireflyColor;

        private transient int parsedColor;

        public BiomeGroupSettings() {
        }

        BiomeGroupSettings(BiomeGroup group) {
            this.fireflies = group.defaultFireflies;
            this.glowworms = group.defaultGlowworms;
            this.plankton = group.defaultPlankton;
            this.fireflyColor = formatColor(group.defaultFireflyColor);
            this.parsedColor = group.defaultFireflyColor;
        }

        public int fireflyColorRgb() {
            return this.parsedColor;
        }

        private void sanitize(BiomeGroup group) {
            if (this.fireflies == null) this.fireflies = group.defaultFireflies;
            if (this.glowworms == null) this.glowworms = group.defaultGlowworms;
            if (this.plankton == null) this.plankton = group.defaultPlankton;
            Integer color = parseColor(this.fireflyColor);
            this.parsedColor = color != null ? color : group.defaultFireflyColor;
            this.fireflyColor = formatColor(this.parsedColor);
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    public static IlluminationsConfig get() {
        return instance;
    }

    /** False when the user switched effects off for this level's dimension. */
    public boolean isDimensionEnabled(Level level) {
        return !this.disabledDimensionIds.contains(level.dimension().identifier());
    }

    public boolean isDimensionEnabled(Identifier dimension) {
        return !this.disabledDimensionIds.contains(dimension);
    }

    public void setDimensionEnabled(Identifier dimension, boolean enabled) {
        String id = dimension.toString();
        this.disabledDimensions.remove(id);
        if (!enabled) {
            this.disabledDimensions.add(id);
        }
        this.disabledDimensionIds = parseDimensions(this.disabledDimensions);
    }

    public BiomeGroupSettings biomeGroup(BiomeGroup group) {
        return this.biomeGroups.get(group.key());
    }

    public float densityFactor() {
        return this.density / 100.0F;
    }

    public static Path path() {
        return Services.PLATFORM.getConfigDir().resolve(IlluminationsReimagined.MOD_ID + ".json");
    }

    /** Replaces the current settings with defaults (used by the config screen's reset button). */
    public static void resetToDefaults() {
        instance = new IlluminationsConfig();
        instance.sanitize();
        instance.save();
    }

    public static void load() {
        Path path = path();
        IlluminationsConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, IlluminationsConfig.class);
            } catch (IOException | JsonParseException e) {
                IlluminationsReimagined.LOGGER.error("Could not read {}; keeping a backup and using defaults", path, e);
                try {
                    Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException copyError) {
                    IlluminationsReimagined.LOGGER.warn("Could not back up broken config", copyError);
                }
            }
        }
        instance = loaded != null ? loaded : new IlluminationsConfig();
        instance.sanitize();
        instance.save();
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            IlluminationsReimagined.LOGGER.error("Could not save {}", path, e);
        }
    }

    /** Clamps every value into its valid range and fills anything missing with defaults. */
    public void sanitize() {
        this.configVersion = CURRENT_VERSION;
        this.density = Mth.clamp(this.density, 0, 1000);

        if (this.fireflies == null) this.fireflies = new Fireflies();
        if (this.glowworms == null) this.glowworms = new Glowworms();
        if (this.plankton == null) this.plankton = new Plankton();
        if (this.eyesInTheDark == null) this.eyesInTheDark = new Eyes();
        if (this.willOWisps == null) this.willOWisps = new WillOWisps();
        if (this.chorusPetals == null) this.chorusPetals = new ChorusPetals();
        if (this.prismarineCrystals == null) this.prismarineCrystals = new PrismarineCrystals();
        if (this.halloweenSpirits == null) this.halloweenSpirits = new HalloweenSpirits();

        this.fireflies.maxCount = Mth.clamp(this.fireflies.maxCount, 0, 2000);
        this.fireflies.coreBrightness = Mth.clamp(this.fireflies.coreBrightness, 0, 100);
        if (this.fireflies.autumnColors == null) this.fireflies.autumnColors = SeasonalMode.DISABLED;
        this.glowworms.maxCount = Mth.clamp(this.glowworms.maxCount, 0, 2000);
        this.plankton.maxCount = Mth.clamp(this.plankton.maxCount, 0, 2000);
        if (this.eyesInTheDark.mode == null) this.eyesInTheDark.mode = SeasonalMode.SEASONAL;
        if (this.eyesInTheDark.rate == null) this.eyesInTheDark.rate = SpawnRate.MEDIUM;
        this.eyesInTheDark.maxCount = Mth.clamp(this.eyesInTheDark.maxCount, 0, 200);
        if (this.willOWisps.soulSandValleyRate == null) this.willOWisps.soulSandValleyRate = SpawnRate.MEDIUM;
        this.willOWisps.maxCount = Mth.clamp(this.willOWisps.maxCount, 0, 200);
        this.chorusPetals.multiplier = Mth.clamp(this.chorusPetals.multiplier, 0, 10);
        this.chorusPetals.maxCount = Mth.clamp(this.chorusPetals.maxCount, 0, 4000);
        this.prismarineCrystals.maxCount = Mth.clamp(this.prismarineCrystals.maxCount, 0, 2000);
        if (this.halloweenSpirits.mode == null) this.halloweenSpirits.mode = SeasonalMode.SEASONAL;
        this.halloweenSpirits.maxCount = Mth.clamp(this.halloweenSpirits.maxCount, 0, 200);

        Map<String, BiomeGroupSettings> groups = new LinkedHashMap<>();
        for (BiomeGroup group : BiomeGroup.values()) {
            BiomeGroupSettings settings = this.biomeGroups != null ? this.biomeGroups.get(group.key()) : null;
            if (settings == null) {
                settings = new BiomeGroupSettings(group);
            }
            settings.sanitize(group);
            groups.put(group.key(), settings);
        }
        this.biomeGroups = groups;
        // Keep valid, distinct dimension IDs in their normal form ("the_end" becomes "minecraft:the_end").
        this.disabledDimensionIds = parseDimensions(this.disabledDimensions);
        List<String> dimensions = new ArrayList<>();
        this.disabledDimensionIds.forEach(id -> dimensions.add(id.toString()));
        this.disabledDimensions = dimensions;
    }

    /** The valid IDs in the list, in order and without duplicates; anything unparseable is dropped. */
    private static Set<Identifier> parseDimensions(List<String> ids) {
        Set<Identifier> parsed = new LinkedHashSet<>();
        if (ids != null) {
            for (String id : ids) {
                Identifier identifier = id == null ? null : Identifier.tryParse(id.trim());
                if (identifier != null) {
                    parsed.add(identifier);
                }
            }
        }
        return parsed;
    }

    private static Map<String, BiomeGroupSettings> defaultBiomeGroups() {
        Map<String, BiomeGroupSettings> groups = new LinkedHashMap<>();
        for (BiomeGroup group : BiomeGroup.values()) {
            groups.put(group.key(), new BiomeGroupSettings(group));
        }
        return groups;
    }

    static String formatColor(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    static Integer parseColor(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
