package de.dosmike.sponge.EventSounds.config;

import de.dosmike.sponge.EventSounds.EventSounds;
import de.dosmike.sponge.EventSounds.events.SoundsCollectedEvent;
import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.Optional;

public class ConfigLoader {

    public static ResourcePacker load(ConfigurationLoader<CommentedConfigurationNode> config) throws IOException, ConfigurationException {

        EventSoundRegistry.resetAll();

        CommentedConfigurationNode root = config.load(ConfigurationOptions.defaults());
        ResourcePacker packer = null;
        if (!root.getNode("packer").isVirtual()) {
            packer = new ResourcePacker(
                    getOptionalString(root.getNode("packer"), "template").orElse(null),
                    getOptionalString(root.getNode("packer"), "ftpserver").orElse(null),
                    getOptionalString(root.getNode("packer"), "ftplogin").orElse(null),
                    getOptionalString(root.getNode("packer"), "ftppasswd").orElse(null));
            EventSounds.setForceDownload(
                    getOptionalString(root.getNode("packer"), "forceDownload").orElse("true").equalsIgnoreCase("true")
            );
        }
        for (String event : EventSoundRegistry.getStandardEvents()) {
            ConfigurationNode cfgEvent = root.getNode(event);
            if (!cfgEvent.isVirtual()) {
                EventSoundRegistry.parseConfigurationNode(event, cfgEvent);
            }
        }

        SoundCollector.registerSound();

        try(CauseStackManager.StackFrame ignored = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getEventManager().post(new SoundsCollectedEvent());
        }

        PlaytimeManager.readFileLengths();

        return packer;
    }

    private static Optional<String> getOptionalString(ConfigurationNode node, String key) {
        if (node.getNode(key).isVirtual()) {
            return Optional.empty();
        }
        return Optional.of(node.getNode(key).getString());
    }

}
