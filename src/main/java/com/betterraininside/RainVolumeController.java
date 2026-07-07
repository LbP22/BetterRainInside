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
    private final RainLowPassEffect lowPassEffect;

    private int ticksSinceRefresh;
    private float cachedOpenness = 1.0f;
    private float targetMultiplier = 1.0f;
    private float smoothedMultiplier = 1.0f;
    private float targetLowpassGainHf = 1.0f;
    private float smoothedLowpassGainHf = 1.0f;

    public RainVolumeController(Config config, RainEnvironmentAnalyzer analyzer, RainLowPassEffect lowPassEffect) {
        this.config = config;
        this.analyzer = analyzer;
        this.lowPassEffect = lowPassEffect;
    }

    public void tick(Minecraft client) {
        if (!this.config.enabled) {
            this.targetMultiplier = 1.0f;
            this.targetLowpassGainHf = 1.0f;
            smoothTowardsTarget();
            this.lowPassEffect.clearActiveSources();
            return;
        }

        ClientLevel world = client.level;
        LocalPlayer player = client.player;

        if (world == null || player == null || !world.isRaining()) {
            this.targetMultiplier = 1.0f;
            this.targetLowpassGainHf = 1.0f;
            smoothTowardsTarget();
            this.lowPassEffect.clearActiveSources();
            return;
        }

        if (this.ticksSinceRefresh <= 0) {
            this.cachedOpenness = this.analyzer.analyzeOpenness(world, player, this.config);
            this.targetMultiplier = opennessToVolume(this.cachedOpenness);
            this.targetLowpassGainHf = opennessToLowpassGainHf(this.cachedOpenness);
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
        float shapedOpen = shapeOpenness(openness);
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

    private float opennessToLowpassGainHf(float openness) {
        if (!this.config.muffleEnabled) {
            return 1.0f;
        }

        // GAINHF of 1.0 leaves highs untouched (fully open); lower values cut them (muffled).
        float shapedOpen = shapeOpenness(openness);
        float minGainHf = 1.0f - Mth.clamp(this.config.indoorMuffleStrength, 0.0f, 1.0f);
        return Mth.lerp(shapedOpen, minGainHf, 1.0f);
    }

    private float shapeOpenness(float openness) {
        float clampedOpen = Mth.clamp(openness, 0.0f, 1.0f);
        return (float) Math.pow(clampedOpen, this.config.indoorResponseCurve);
    }

    private void smoothTowardsTarget() {
        float alpha = Mth.clamp(this.config.transitionSpeed, 0.01f, 1.0f);
        this.smoothedMultiplier = Mth.lerp(alpha, this.smoothedMultiplier, this.targetMultiplier);
        this.smoothedLowpassGainHf = Mth.lerp(alpha, this.smoothedLowpassGainHf, this.targetLowpassGainHf);
        this.lowPassEffect.setMuffle(this.smoothedLowpassGainHf);
    }

    static boolean isRainSound(ResourceLocation soundId) {
        return WEATHER_RAIN_ID.equals(soundId) || WEATHER_RAIN_ABOVE_ID.equals(soundId);
    }
}
