package com.betterraininside;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RainVolumeController {
    private static final ResourceLocation WEATHER_RAIN_ID = BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.WEATHER_RAIN);
    private static final ResourceLocation WEATHER_RAIN_ABOVE_ID = BuiltInRegistries.SOUND_EVENT.getKey(SoundEvents.WEATHER_RAIN_ABOVE);

    private final Config config;
    private final RainEnvironmentAnalyzer analyzer;

    private int ticksSinceRefresh;
    private float cachedOpenness = 1.0f;
    private float targetMultiplier = 1.0f;
    private float smoothedMultiplier = 1.0f;

    public RainVolumeController(Config config, RainEnvironmentAnalyzer analyzer) {
        this.config = config;
        this.analyzer = analyzer;
    }

    public void tick(Minecraft client) {
        if (!this.config.enabled) {
            this.targetMultiplier = 1.0f;
            smoothTowardsTarget();
            return;
        }

        ClientLevel world = client.level;
        LocalPlayer player = client.player;

        if (world == null || player == null || !world.isRaining()) {
            this.targetMultiplier = 1.0f;
            smoothTowardsTarget();
            return;
        }

        if (this.ticksSinceRefresh <= 0) {
            this.cachedOpenness = this.analyzer.analyzeOpenness(world, player, this.config);
            this.targetMultiplier = opennessToVolume(this.cachedOpenness);
            this.ticksSinceRefresh = this.config.updateIntervalTicks;
        } else {
            this.ticksSinceRefresh--;
        }

        // Interpolation prevents pops when crossing doorways or overhangs.
        smoothTowardsTarget();
    }

    public float applyRainVolume(ResourceLocation soundId, float originalVolume) {
        if (!this.config.enabled || !isRainSound(soundId)) {
            return originalVolume;
        }

        float multiplier = Math.max(this.config.minimumVolume, this.smoothedMultiplier);
        return originalVolume * multiplier;
    }

    private float opennessToVolume(float openness) {
        float clampedOpen = Mth.clamp(openness, 0.0f, 1.0f);
        float shapedOpen = (float) Math.pow(clampedOpen, this.config.indoorResponseCurve);
        float min = this.config.minimumVolume;
        float indoorMax = Math.max(min, this.config.maximumIndoorVolume);

        // Two-stage mapping gives deeper dampening in caves while still supporting
        // gentle partial-cover transitions near windows and overhangs.
        if (shapedOpen <= 0.25f) {
            float t = shapedOpen / 0.25f;
            return Mth.lerp(t, min, indoorMax);
        }

        float t = (shapedOpen - 0.25f) / 0.75f;
        return Mth.lerp(t, indoorMax, 1.0f);
    }

    private void smoothTowardsTarget() {
        float alpha = Mth.clamp(this.config.transitionSpeed, 0.01f, 1.0f);
        this.smoothedMultiplier = Mth.lerp(alpha, this.smoothedMultiplier, this.targetMultiplier);
    }

    private static boolean isRainSound(ResourceLocation soundId) {
        return WEATHER_RAIN_ID.equals(soundId) || WEATHER_RAIN_ABOVE_ID.equals(soundId);
    }
}
