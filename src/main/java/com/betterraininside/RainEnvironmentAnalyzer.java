package com.betterraininside;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RainEnvironmentAnalyzer {
    private static final Vec3[] WALL_DIRECTIONS = {
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
        new Vec3(0, 0, 1),
        new Vec3(0, 0, -1),
        new Vec3(1, 0, 1).normalize(),
        new Vec3(1, 0, -1).normalize(),
        new Vec3(-1, 0, 1).normalize(),
        new Vec3(-1, 0, -1).normalize()
    };

    public float analyzeOpenness(ClientLevel world, LocalPlayer player, Config config) {
        if (!world.dimensionType().hasSkyLight()) {
            return 0.0f;
        }

        float roofCoverage = computeRoofCoverage(world, player, config);
        float wallCoverage = computeWallCoverage(world, player, config.wallProbeDistance);

        float enclosure = (roofCoverage * config.roofCoverageWeight) + (wallCoverage * config.wallCoverageWeight);

        // Make roofed spaces feel more "indoors" even when nearby walls are sparse.
        if (roofCoverage > 0.6f && wallCoverage > 0.1f) {
            float roofFactor = (roofCoverage - 0.6f) / 0.4f;
            enclosure += config.roofedSpaceBoost * Mth.clamp(roofFactor, 0.0f, 1.0f);
        }

        // Under leaf canopy the roof can be strong while lateral enclosure remains weak.
        if (roofCoverage > 0.7f && wallCoverage < 0.25f) {
            enclosure *= (1.0f - config.treeCanopyRelief);
        }

        // Caves and deep enclosed spaces should be much quieter than partial cover.
        if (roofCoverage > 0.85f && wallCoverage > 0.55f) {
            enclosure = Math.max(enclosure, 0.9f);
        }

        enclosure = Mth.clamp(enclosure * config.detectionSensitivity, 0.0f, 1.0f);

        float shaped = (float) Math.pow(Mth.clamp(enclosure, 0.0f, 1.0f), config.enclosureCurve);
        float openness = 1.0f - shaped;
        return Mth.clamp(openness, 0.0f, 1.0f);
    }

    private float computeRoofCoverage(ClientLevel world, LocalPlayer player, Config config) {
        final int radius = config.sampleRadius;
        final int baseX = Mth.floor(player.getX());
        final int baseY = Mth.floor(player.getEyeY());
        final int baseZ = Mth.floor(player.getZ());

        float coveredWeight = 0.0f;
        float totalWeight = 0.0f;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int sampleX = baseX + dx;
                int sampleZ = baseZ + dz;

                if (!world.hasChunk(sampleX >> 4, sampleZ >> 4)) {
                    continue;
                }

                float distSq = (dx * dx) + (dz * dz);
                float weight = 1.0f / (1.0f + (distSq * 0.55f));
                totalWeight += weight;

                // Ignore leaves so tree canopies don't look like solid building roofs.
                int topNoLeaves = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sampleX, sampleZ);
                if (topNoLeaves > baseY + 1) {
                    coveredWeight += weight;
                }
            }
        }

        if (totalWeight <= 0.0001f) {
            return 1.0f;
        }

        return Mth.clamp(coveredWeight / totalWeight, 0.0f, 1.0f);
    }

    private float computeWallCoverage(ClientLevel world, LocalPlayer player, int wallProbeDistance) {
        Vec3 start = player.getEyePosition();
        int blocked = 0;

        for (Vec3 dir : WALL_DIRECTIONS) {
            Vec3 end = start.add(dir.scale(wallProbeDistance));
            HitResult hit = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (hit.getType() != HitResult.Type.MISS) {
                blocked++;
            }
        }

        return (float) blocked / (float) WALL_DIRECTIONS.length;
    }
}
