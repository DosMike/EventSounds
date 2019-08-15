package de.dosmike.sponge.EventSounds.events;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;

/**
 * This event is emitted every time EventSounds finished skimming through all plugins assets for sounds.
 * With this event you can call SoundCollector.forPlugin(myPluginContainer) to store a map of named {@link SoundType}s.
 * The map reflects the names you've used in your /assets/PLUGIN_ID/eventsounds.json.<br>
 * Since the method SoundCollector.forPlugin(...) generates the map dynamically it is recommended to retrieve it once
 * and store it in a static field somewhere in your plugin.
 */
public class SoundsCollectedEvent implements Event {

    public SoundsCollectedEvent() {
    }

    @Override
    public Cause getCause() {
        return Sponge.getCauseStackManager().getCurrentCause();
    }

    @Override
    public Object getSource() {
        return getCause().root();
    }

    @Override
    public EventContext getContext() {
        return Sponge.getCauseStackManager().getCurrentContext();
    }
}
