/*
 * Illuminations Reimagined asset generator.
 *
 * Every texture shipped by Illuminations Reimagined is produced by this program from scratch: procedural glows and
 * hand-authored pixel grids defined below. No artwork from the original Illuminations (All Rights Reserved,
 * Ladysnake) or from any other project is read, traced or modified. Regenerating the assets is deterministic:
 *
 *     java tools/assetgen/GenerateAssets.java
 *
 * Output: common/src/main/resources/assets/illuminations_reimagined/...
 *
 * This file and the images it produces are covered by LICENSE-ASSETS.md.
 */

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GenerateAssets {
    private static final File ROOT = new File("common/src/main/resources/assets/illuminations_reimagined");
    private static final File PARTICLES = new File(ROOT, "textures/particle");

    public static void main(String[] args) throws IOException {
        PARTICLES.mkdirs();

        // ---- Fireflies: a soft tintable halo plus a small white core, drawn as two layers. ----
        write("firefly_glow", radialGlow(16, 7.5, 2.2, 6));
        write("firefly_core", radialGlow(16, 2.6, 1.0, 4));

        // ---- Glowworm: a faint silk thread with a glowing droplet at its end. ----
        write("glowworm", glowworm());

        // ---- Plankton: a tiny soft speck. ----
        write("plankton", radialGlow(8, 3.5, 1.0, 4));

        // ---- Wisp ember: small soft spark. ----
        write("wisp_ember", radialGlow(8, 3.6, 1.4, 5));

        // ---- Will o' wisp: four flame frames. ----
        for (int i = 0; i < 4; i++) {
            write("wisp_" + i, wispFrame(i));
        }

        // ---- Eyes in the dark: closed -> fully open. ----
        for (int i = 0; i < EYE_FRAMES.length; i++) {
            write("eyes_" + i, paint(EYE_FRAMES[i], EYE_PALETTE));
        }

        // ---- Chorus petals: three petal silhouettes, near-white so the particle can tint them. ----
        for (int i = 0; i < PETALS.length; i++) {
            write("chorus_petal_" + i, paint(PETALS[i], PETAL_PALETTE));
        }

        // ---- Prismarine crystals: three faceted shards, near-white so the particle can tint them. ----
        for (int i = 0; i < CRYSTALS.length; i++) {
            write("prismarine_crystal_" + i, paint(CRYSTALS[i], CRYSTAL_PALETTE));
        }

        // ---- Mod icon. ----
        ImageIO.write(icon(128), "png", new File(ROOT, "icon.png"));
        System.out.println("Assets written to " + ROOT.getPath());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Procedural shapes
    // ------------------------------------------------------------------------------------------------------------

    /**
     * White radial glow with alpha falling off from the centre. {@code steps} quantises the alpha so the result keeps
     * a pixel-art look at Minecraft's scale instead of a smooth airbrushed gradient.
     */
    static BufferedImage radialGlow(int size, double radius, double falloffPower, int steps) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double c = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - c, y - c) / radius;
                if (d >= 1.0) continue;
                double a = Math.pow(1.0 - d, falloffPower);
                a = Math.floor(a * steps + 0.35) / steps;
                if (a <= 0) continue;
                img.setRGB(x, y, argb(a, 255, 255, 255));
            }
        }
        return img;
    }

    static BufferedImage glowworm() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Thread: a single dim pixel column from the top edge.
        for (int y = 0; y < 8; y++) {
            img.setRGB(7, y, argb(0.18 + y * 0.02, 255, 255, 255));
        }
        // Droplet body: slightly elongated vertical glow centred lower in the frame.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = (x - 7.0) / 3.2;
                double dy = (y - 10.5) / 4.2;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d >= 1.0) continue;
                double a = Math.ceil(Math.pow(1.0 - d, 1.4) * 5) / 5.0;
                int existing = img.getRGB(x, y) >>> 24;
                img.setRGB(x, y, argb(Math.max(a, existing / 255.0), 255, 255, 255));
            }
        }
        return img;
    }

    /** A teardrop flame whose tip sways a pixel or two between frames. Grayscale; the particle tints it. */
    static BufferedImage wispFrame(int frame) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        double[] tipSway = {0.0, 0.8, 0.0, -0.8};
        double[] stretch = {1.0, 1.08, 0.95, 1.05};
        Random noise = new Random(9127L + frame);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // Body: circle at the bottom, tapering to a point toward the top.
                double t = (11.0 - y) / (9.0 * stretch[frame]);     // 0 at body centre, 1 at the tip
                double cx = 7.5 + (t > 0 ? tipSway[frame] * t * t * 2.0 : 0.0);
                double halfWidth = t <= 0 ? 4.2 * Math.sqrt(Math.max(0, 1 - (t * 2.2) * (t * 2.2))) : 4.2 * Math.pow(1 - t, 1.3);
                if (halfWidth <= 0) continue;
                double dx = Math.abs(x - cx) / halfWidth;
                if (dx >= 1.0) continue;
                double edge = 1.0 - dx;
                double heat = Math.max(0, 1.0 - Math.max(0, t)) * edge;   // brighter toward the core
                double a = Math.min(1.0, 0.35 + heat * 0.9 + noise.nextDouble() * 0.08);
                a = Math.ceil(a * 6) / 6.0;
                int shade = (int) (190 + 65 * Math.min(1.0, heat * 1.4));
                img.setRGB(x, y, argb(a * edge * 1.15 > 1 ? 1 : a * Math.min(1.0, edge * 1.6), shade, shade, shade));
            }
        }
        return img;
    }

    static BufferedImage icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int px = size / 32; // draw on a 32x32 grid, scaled up crisply
        Random rnd = new Random(20261002L);
        // Night sky gradient.
        for (int gy = 0; gy < 32; gy++) {
            double t = gy / 31.0;
            int r = (int) lerp(14, 32, t), g = (int) lerp(18, 44, t), b = (int) lerp(48, 70, t);
            for (int gx = 0; gx < 32; gx++) fill(img, gx, gy, px, 0xFF000000 | r << 16 | g << 8 | b);
        }
        // Stars.
        for (int i = 0; i < 14; i++) {
            int gx = rnd.nextInt(32), gy = rnd.nextInt(14);
            fill(img, gx, gy, px, 0xFF8890B8);
        }
        // Rolling hill silhouette and grass blades.
        for (int gx = 0; gx < 32; gx++) {
            int top = 24 + (int) Math.round(2.0 * Math.sin(gx / 5.0) + Math.sin(gx / 2.3));
            for (int gy = top; gy < 32; gy++) fill(img, gx, gy, px, gy == top ? 0xFF1F4A2E : 0xFF14301F);
            if (rnd.nextInt(3) == 0) fill(img, gx, top - 1, px, 0xFF1F4A2E);
        }
        // Fireflies: tinted halos with white cores.
        int[][] flies = {{9, 15}, {17, 10}, {23, 17}, {13, 21}, {26, 9}};
        int[] colors = {0xC8F25C, 0xE4F270, 0x9BE04A, 0xC8F25C, 0xF2D66A};
        for (int i = 0; i < flies.length; i++) {
            int gx = flies[i][0], gy = flies[i][1], col = colors[i];
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    double d = Math.hypot(dx, dy);
                    if (d > 2.3) continue;
                    double a = d < 0.5 ? 1.0 : d < 1.5 ? 0.55 : 0.2;
                    blend(img, gx + dx, gy + dy, px, col, a);
                }
            }
            fill(img, gx, gy, px, 0xFFFFFFF0);
        }
        return img;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hand-authored pixel grids. Each character maps to a palette entry; '.' is transparent.
    // ------------------------------------------------------------------------------------------------------------

    /** Amber eyes with vertical slit pupils. Frames: closed -> opening -> almost open -> open. */
    static final String[][] EYE_FRAMES = {
            {
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "..aaaa....aaaa..",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
            },
            {
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "..abba....abba..",
                    "..bbkb....bkbb..",
                    "..abba....abba..",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
            },
            {
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "...aa......aa...",
                    "..abbb....bbba..",
                    ".abckbb..bbkcba.",
                    "..abbb....bbba..",
                    "...aa......aa...",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
            },
            {
                    "................",
                    "................",
                    "................",
                    "................",
                    "...aaa....aaa...",
                    "..abbbb..bbbba..",
                    ".abcckb..bkccba.",
                    ".abcckb..bkccba.",
                    ".abbbkb..bkbbba.",
                    "..abbbb..bbbba..",
                    "...aaa....aaa...",
                    "................",
                    "................",
                    "................",
                    "................",
                    "................",
            },
    };
    static final Object[][] EYE_PALETTE = {
            {'a', 0.55, 0xB8641E}, // dim outer glow
            {'b', 1.0, 0xF2A23A},  // amber iris
            {'c', 1.0, 0xFFE7A0},  // bright highlight
            {'k', 1.0, 0x1A0A00},  // slit pupil
    };

    static final String[][] PETALS = {
            {
                    "........",
                    "...ab...",
                    "..abbb..",
                    "..bccb..",
                    ".abccba.",
                    "..bccb..",
                    "...bb...",
                    "........",
            },
            {
                    "........",
                    "....ab..",
                    "...abcb.",
                    "..abccb.",
                    ".abccb..",
                    ".bccb...",
                    "..bb....",
                    "........",
            },
            {
                    "........",
                    "........",
                    ".abbbba.",
                    "abccccba",
                    ".abccba.",
                    "...bb...",
                    "........",
                    "........",
            },
    };
    static final Object[][] PETAL_PALETTE = {
            {'a', 0.6, 0xC9B6D8},
            {'b', 1.0, 0xE6DAF0},
            {'c', 1.0, 0xFFFFFF},
    };

    static final String[][] CRYSTALS = {
            {
                    "........",
                    "...a....",
                    "..abb...",
                    "..bcb...",
                    ".abccb..",
                    "..bcb...",
                    "...b....",
                    "........",
            },
            {
                    "........",
                    "........",
                    "..abbb..",
                    ".abccb..",
                    ".bccb...",
                    ".bbb....",
                    "........",
                    "........",
            },
            {
                    "........",
                    ".....a..",
                    "....ab..",
                    "...bcb..",
                    "..bcb...",
                    ".bcb....",
                    ".bb.....",
                    "........",
            },
    };
    static final Object[][] CRYSTAL_PALETTE = {
            {'a', 0.7, 0xDDEEEE},
            {'b', 0.9, 0xEAF6F4},
            {'c', 1.0, 0xFFFFFF},
    };

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    static BufferedImage paint(String[] rows, Object[][] palette) {
        int h = rows.length, w = rows[0].length();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            if (rows[y].length() != w) throw new IllegalArgumentException("Ragged pixel grid at row " + y);
            for (int x = 0; x < w; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') continue;
                Object[] entry = lookup(palette, ch);
                int rgb = (Integer) entry[2];
                img.setRGB(x, y, argb((Double) entry[1], rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF));
            }
        }
        return img;
    }

    static Object[] lookup(Object[][] palette, char ch) {
        for (Object[] e : palette) if ((Character) e[0] == ch) return e;
        throw new IllegalArgumentException("Unknown palette key '" + ch + "'");
    }

    static int argb(double a, int r, int g, int b) {
        int ai = (int) Math.round(Math.max(0, Math.min(1, a)) * 255);
        return ai << 24 | r << 16 | g << 8 | b;
    }

    static void fill(BufferedImage img, int gx, int gy, int px, int argb) {
        if (gx < 0 || gy < 0 || gx * px >= img.getWidth() || gy * px >= img.getHeight()) return;
        for (int y = 0; y < px; y++) for (int x = 0; x < px; x++) img.setRGB(gx * px + x, gy * px + y, argb);
    }

    static void blend(BufferedImage img, int gx, int gy, int px, int rgb, double a) {
        if (gx < 0 || gy < 0 || gx * px >= img.getWidth() || gy * px >= img.getHeight()) return;
        int base = img.getRGB(gx * px, gy * px);
        int r = (int) lerp(base >> 16 & 0xFF, rgb >> 16 & 0xFF, a);
        int g = (int) lerp(base >> 8 & 0xFF, rgb >> 8 & 0xFF, a);
        int b = (int) lerp(base & 0xFF, rgb & 0xFF, a);
        fill(img, gx, gy, px, 0xFF000000 | r << 16 | g << 8 | b);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static void write(String name, BufferedImage img) throws IOException {
        ImageIO.write(img, "png", new File(PARTICLES, name + ".png"));
    }
}
