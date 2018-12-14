package de.dosmike.sponge.EventSounds.events;

import de.dosmike.sponge.EventSounds.sounds.Playable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.entity.living.humanoid.player.TargetPlayerEvent;

import java.util.Optional;

/**
 * This event is emitted just before the sound is actually played in case you want
 * to tie certain sounds to additional requirements.
 */
public class PlaySoundEvent implements Cancellable, TargetPlayerEvent {

    private boolean cancel=false;

    private Player source, victim;
    private Playable playable;

    public PlaySoundEvent(Player src, Player vic, Playable sound) {
        this.source = src;
        this.victim = vic;
        this.playable = sound;
    }

    public Playable getPlayable() {
        return playable;
    }

    /**
     * @return the player that initially triggered the sound
     */
    @Override
    public Player getTargetEntity() {
        return source;
    }

    /** This will only return a value in PvP related events
     * @return the player that was killed */
    public Optional<Player> getVictimEntity() {
        return Optional.ofNullable(victim);
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel=cancel;
    }

    @Override
    public Cause getCause() {
        return Sponge.getCauseStackManager().getCurrentCause();
    }
}
