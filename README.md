# Illuminations Reimagined

**An unofficial modern continuation of [Illuminations](https://github.com/Ladysnake/Illuminations) by Ladysnake,
updated for modern Minecraft on Fabric and NeoForge, with bug fixes, rewritten systems and all-new artwork.**

> ⚠️ This project is **not** affiliated with, maintained by, or endorsed by Ladysnake or the original
> Illuminations developers. Please report issues here, not to them.

Illuminations Reimagined is a **client-side** mod that adds ambient lights to make dark places feel alive.
It works on any server, including vanilla servers, and nothing needs to be installed server-side.

## Effects

| Effect | Where / when |
|---|---|
| **Fireflies** | At night, a few blocks above the ground in forests, swamps, rivers, jungles, taigas, plains and savannas. Colour depends on the biome; orange "autumn" fireflies in October. |
| **Glowworms** | Clinging to cave ceilings; more in lush caves. They drop if their ceiling is broken. |
| **Plankton** | Tiny glowing specks in dark ocean water. |
| **Chorus petals** | Drifting from chorus flowers; a burst of petals when a flower breaks. |
| **Prismarine crystals** | Floating in the water around sea lanterns. |
| **Will o' wisps** | Rising from soul sand in Soul Sand Valleys and drifting out of soul lanterns. |
| **Eyes in the dark** | In October (configurable), watching from pitch-black spots. They vanish if you get close or bring light. |

## Supported versions

| Minecraft | Fabric Loader | Fabric API | NeoForge |
|---|---|---|---|
| 26.3 | ≥ 0.19.5 | 0.161.0+26.3 | 26.3.0.43-beta |
| 26.2 | ≥ 0.19.5 | 0.161.0+26.2 | 26.2.0.88 |

Java 25 is required, as for Minecraft 26.x itself.

## Configuration

The config file is `config/illuminations_reimagined.json`. It is created on first launch and is safe to
hand-edit; invalid values are corrected on load. Options include:

* A master switch, global **density** (0–1000 %), **spawn radius** and per-tick **sample budget**.
* A **maximum live count** for every effect, so particle numbers can never run away.
* Fireflies: spawn always (day too), spawn underground, core brightness, rainbow, autumn colours, light attraction.
* Per-biome-group firefly, glowworm and plankton rates (`DISABLED` / `LOW` / `MEDIUM` / `HIGH`) and firefly colour.
* Eyes in the dark: `SEASONAL` (October), `ALWAYS` or `DISABLED`, and a spawn rate.

Biomes are classified with vanilla and `c:` convention biome tags, so modded biomes that tag themselves
correctly are supported automatically.

## What changed from the original

**Removed:** every donor cosmetic (Twilight and Ghostly auras, the Frost, Solar, Bloodfiend, Dreadlich, Mooncult,
Chorus and Prismarine crowns, horns, halos, tiaras, wreaths, pride-heart and other pets, Jacko, and so on), the
cosmetics dashboard and its UUID-based network lookups, the self-updater and its bundled uninstaller, the
donation, update and greeting screens, and the override of Minecraft's own particle shader.

**Rewritten and fixed** (details in [`CHANGELOG.md`](CHANGELOG.md) and [`docs/SOURCE_AUDIT.md`](docs/SOURCE_AUDIT.md)):

* Fireflies no longer **teleport** onto light sources, **freeze** mid-air, or **dive** toward y = 0.
  Light attraction only follows lights the firefly can actually see.
* Spawning runs once per tick with a fixed budget, **never touches unloaded chunks**, caches biome lookups,
  and caps every effect, instead of piggy-backing on ~1,300 vanilla display-tick calls per tick.
* Particles are **not registered** in the particle-type registry, which avoids registry-sync trouble on servers.
* Counts stay correct through world changes, disconnects and resource reloads.
* Glowworms and plankton no longer allocate a new `Random` every tick. Glowworms respect the modern world height
  and spawn in modern noise caves.
* Biome detection no longer depends on the removed `Biome.Category`, which crashed with modded biomes.
* Eyes no longer flood the Nether and End, and no longer run game logic inside rendering code.
* Chorus petals use one robust hook instead of a fragile chain of mixins.
* Will o' wisps render through the normal particle pipeline instead of a private immediate-mode buffer.

## Building

```bash
./gradlew build            # Minecraft 26.3 (default)
./gradlew build -Pmc=26.2  # Minecraft 26.2
```

Jars are written to `fabric/build/libs/` and `neoforge/build/libs/`. Version-specific dependency versions live
in `versions/<mc>.properties`.

Textures are generated, not hand-edited: `java tools/assetgen/GenerateAssets.java`.

## Licensing

* **Code:** GPL-3.0-or-later. This is a modified version of Illuminations, © 2021 Ladysnake, GPL-3.0-or-later;
  modifications © 2026 Romoslayer. See [`LICENSE`](LICENSE) and [`NOTICE.md`](NOTICE.md).
* **Artwork:** all textures and the icon are **newly created** for this project (see
  [`docs/ASSETS.md`](docs/ASSETS.md) and [`LICENSE-ASSETS.md`](LICENSE-ASSETS.md)). None of the original
  Illuminations artwork (All Rights Reserved, Ladysnake) is included.
* **Source:** the complete corresponding source code is available in this repository.

## Credits

* **doctor4t and the Ladysnake contributors** created the original
  [Illuminations](https://github.com/Ladysnake/Illuminations) and its ideas: fireflies, glowworms, plankton, chorus
  petals, eyes in the dark and will o' wisps.
* Build layout based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template) (CC0).
