package de.dosmike.sponge.EventSounds.sounds;

import ninja.leaping.configurate.ConfigurationNode;

public class SoundOptions {

    private static final String OPTION_COOLDOWN_CLIENT = "cooldown_client";
    private static final String OPTION_COOLDOWN_GLOBAL = "cooldown_global";
    private static final String OPTION_PLAYER_VS_MOBS = "pvm";
    private static final String OPTION_TRIGGER_DELAY = "delay";

    private int delayMs;
    private double cooldownGlobal, cooldownClient;
    private boolean pvm;

    private SoundOptions() {}
    private static final SoundOptions DEFAULT = new SoundOptions();
    static {
        DEFAULT.cooldownClient = 0.0;
        DEFAULT.cooldownGlobal = 0.0;
        DEFAULT.delayMs = 0;
        DEFAULT.pvm = false;
    }

    public static SoundOptions parseConfigurationNode(ConfigurationNode node) {
        SoundOptions result = new SoundOptions();
        result.cooldownClient = node.getNode(OPTION_COOLDOWN_CLIENT).getDouble(0.0);
        result.cooldownGlobal = node.getNode(OPTION_COOLDOWN_GLOBAL).getDouble(0.0);
        result.delayMs = (int)(node.getNode(OPTION_TRIGGER_DELAY).getDouble(0.0)*1000);
        result.pvm = node.getNode(OPTION_PLAYER_VS_MOBS).getBoolean(false);
        return result;
    }
    public static SoundOptions getDefault() {
        return DEFAULT;
    }

    public int getDelayMs() {
        return delayMs;
    }
    public double getCooldownGlobal() {
        return cooldownGlobal;
    }
    public double getCooldownClient() {
        return cooldownClient;
    }
    public boolean getPvmEnabled() {
        return pvm;
    }
}
