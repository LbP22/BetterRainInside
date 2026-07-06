package com.betterraininside;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RainSoundMod.MOD_ID)
public final class RainSoundMod {
    public static final String MOD_ID = "betterraininside";
    public static final Logger LOGGER = LoggerFactory.getLogger("BetterRainInside");

    private static RainVolumeController volumeController;

    public RainSoundMod() {
        Config config = ConfigManager.loadOrCreate();
        RainEnvironmentAnalyzer analyzer = new RainEnvironmentAnalyzer();
        volumeController = new RainVolumeController(config, analyzer);

        LOGGER.info("BetterRainInside initialized");
    }

    public static RainVolumeController getVolumeController() {
        return volumeController;
    }

    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (volumeController == null) {
                return;
            }
            volumeController.tick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void onPlaySound(PlaySoundEvent event) {
            if (volumeController == null) {
                return;
            }

            SoundInstance sound = event.getSound();
            if (sound == null) {
                return;
            }

            float multiplier = volumeController.applyRainVolume(sound.getLocation(), 1.0f);
            if (Math.abs(multiplier - 1.0f) > 0.0001f) {
                event.setSound(new VolumeWrappedSoundInstance(sound, multiplier));
            }
        }
    }

    private record VolumeWrappedSoundInstance(SoundInstance delegate, float volumeMultiplier) implements SoundInstance {
        @Override
        public ResourceLocation getLocation() {
            return delegate.getLocation();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return delegate.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return delegate.getSound();
        }

        @Override
        public SoundSource getSource() {
            return delegate.getSource();
        }

        @Override
        public boolean isLooping() {
            return delegate.isLooping();
        }

        @Override
        public boolean isRelative() {
            return delegate.isRelative();
        }

        @Override
        public int getDelay() {
            return delegate.getDelay();
        }

        @Override
        public float getVolume() {
            return delegate.getVolume() * volumeMultiplier;
        }

        @Override
        public float getPitch() {
            return delegate.getPitch();
        }

        @Override
        public double getX() {
            return delegate.getX();
        }

        @Override
        public double getY() {
            return delegate.getY();
        }

        @Override
        public double getZ() {
            return delegate.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return delegate.getAttenuation();
        }

        @Override
        public boolean canPlaySound() {
            return delegate.canPlaySound();
        }

        @Override
        public boolean canStartSilent() {
            return delegate.canStartSilent();
        }
    }
}
