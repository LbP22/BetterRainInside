package com.betterraininside;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.util.Mth;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Applies a real OpenAL low-pass filter to the rain ambience so indoor rain sounds muffled,
 * not just quieter. Minecraft's public sound API has no filter/DSP hook, so this reaches into
 * two private sound-engine fields via reflection: the raw OpenAL source handle behind a
 * {@link Channel}, and the {@link SoundEngine}'s dedicated audio-thread executor (all OpenAL
 * calls must happen on that thread or they silently no-op). Everything else uses NeoForge's
 * public {@code PlaySoundSourceEvent}/{@code PlayStreamingSourceEvent}. If either reflective
 * lookup fails, or the audio device doesn't support the EFX extension, the effect disables
 * itself permanently and rain falls back to the existing volume-only behavior.
 */
public final class RainLowPassEffect {
    private static final Logger LOGGER = RainSoundMod.LOGGER;

    private Field channelSourceField;
    private boolean reflectionUnavailable;

    private Executor soundExecutor;
    private Integer filterId;
    private boolean effectUnsupported;

    private final Set<Integer> activeSources = new HashSet<>();

    // Called from PlaySoundSourceEvent/PlayStreamingSourceEvent, already on the sound engine thread.
    public void onSoundSourceStarted(SoundEngine engine, SoundInstance sound, Channel channel) {
        if (this.effectUnsupported || this.reflectionUnavailable) {
            return;
        }
        if (!RainVolumeController.isRainSound(sound.getLocation())) {
            return;
        }

        if (this.soundExecutor == null) {
            this.soundExecutor = captureEngineExecutor(engine);
            if (this.soundExecutor == null) {
                this.reflectionUnavailable = true;
                return;
            }
        }

        Integer rawSource = readChannelSource(channel);
        if (rawSource == null) {
            this.reflectionUnavailable = true;
            return;
        }

        if (this.filterId == null && !createLowpassFilter()) {
            this.effectUnsupported = true;
            return;
        }

        AL10.alSourcei(rawSource, EXTEfx.AL_DIRECT_FILTER, this.filterId);
        this.activeSources.add(rawSource);
    }

    // Called every tick from the main thread with a smoothed 0..1 "openness" of the filter (1 = untouched highs).
    public void setMuffle(float gainHf) {
        if (this.effectUnsupported || this.reflectionUnavailable || this.soundExecutor == null || this.filterId == null) {
            return;
        }

        float clamped = Mth.clamp(gainHf, EXTEfx.AL_LOWPASS_MIN_GAINHF, EXTEfx.AL_LOWPASS_MAX_GAINHF);
        int filter = this.filterId;
        this.soundExecutor.execute(() -> {
            EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, clamped);
            this.activeSources.removeIf(source -> {
                if (!AL10.alIsSource(source)) {
                    return true;
                }
                AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
                return false;
            });
        });
    }

    // Called when rain stops so stale OpenAL source ids aren't kept around (they can be recycled by later sounds).
    public void clearActiveSources() {
        if (this.soundExecutor == null || this.activeSources.isEmpty()) {
            return;
        }
        Set<Integer> sources = new HashSet<>(this.activeSources);
        this.activeSources.clear();
        this.soundExecutor.execute(() -> {
            for (int source : sources) {
                if (AL10.alIsSource(source)) {
                    AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
                }
            }
        });
    }

    private boolean createLowpassFilter() {
        AL10.alGetError();
        int filter = EXTEfx.alGenFilters();
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            LOGGER.warn("BetterRainInside: OpenAL EFX low-pass filter is not supported on this system; indoor rain will only get quieter, not muffled.");
            return false;
        }

        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, EXTEfx.AL_LOWPASS_DEFAULT_GAIN);
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, EXTEfx.AL_LOWPASS_DEFAULT_GAINHF);
        this.filterId = filter;
        return true;
    }

    private Executor captureEngineExecutor(SoundEngine engine) {
        try {
            Field field = SoundEngine.class.getDeclaredField("executor");
            field.setAccessible(true);
            Object value = field.get(engine);
            if (value instanceof Executor executor) {
                return executor;
            }
            LOGGER.warn("BetterRainInside: sound engine executor field had an unexpected type; indoor rain will only get quieter, not muffled.");
            return null;
        } catch (ReflectiveOperationException | SecurityException exception) {
            LOGGER.warn("BetterRainInside: could not access the sound engine executor; indoor rain will only get quieter, not muffled.", exception);
            return null;
        }
    }

    private Integer readChannelSource(Channel channel) {
        try {
            if (this.channelSourceField == null) {
                Field field = Channel.class.getDeclaredField("source");
                field.setAccessible(true);
                this.channelSourceField = field;
            }
            return this.channelSourceField.getInt(channel);
        } catch (ReflectiveOperationException | SecurityException exception) {
            LOGGER.warn("BetterRainInside: could not access the raw audio source handle; indoor rain will only get quieter, not muffled.", exception);
            return null;
        }
    }
}
