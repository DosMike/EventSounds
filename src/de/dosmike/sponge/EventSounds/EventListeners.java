package de.dosmike.sponge.EventSounds;

import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Optional;

public class EventListeners {

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        EventSoundRegistry.triggerJoin(event.getTargetEntity());
    }
    @Listener
    public void onChat(MessageChannelEvent.Chat event) {
        Player from = event.getCause().first(Player.class).orElse(null);
        if (from == null) return;
        String message = event.getRawMessage().toPlain();
        EventSounds.l("Chat: %s %s", from.getName(), message);
        EventSoundRegistry.triggerChat(from, message);
    }
    @Listener
    public void onDamageEntity(DamageEntityEvent event) {

        if (event.isCancelled()) return;
        if (!(event.getTargetEntity() instanceof Living)) return;
        Living target = (Living)event.getTargetEntity();
        Optional<EntityDamageSource> source = event.getCause().first(EntityDamageSource.class);
        Entity attacker = null;
        if (!source.isPresent()) { //

        } else {
            Entity test = source.get().getSource();
            if (test instanceof Living) {
                attacker = test;
            } else { //resolve indirect damage source
                if (test.getCreator().isPresent()) {
                    Optional<Entity> perhaps = target.getWorld().getEntity(test.getCreator().get()); //Sponge.getServer().getPlayer(attacker.getCreator().get());
                    if (perhaps.isPresent() && (perhaps.get() instanceof Living))
                        attacker = perhaps.get();
                }
            }
        }
        if (!(target instanceof Player) || !event.willCauseDeath()) return;

        if (attacker instanceof Player) {
            EventSoundRegistry.triggerQuake((Player)attacker, (Player)target); //attacker kills target
        } else if (attacker instanceof Living) {
            EventSoundRegistry.triggerDeath((Player)target, attacker);
        } else if (source.isPresent()) {
            EventSoundRegistry.triggerDeath((Player)target, source.get().getType());
        }
    }

    @Listener
    public void onPlayerDeath(RespawnPlayerEvent event) {
        EventSoundRegistry.resetPlayer(event.getOriginalPlayer());
    }
    @Listener
    public void onPlayerPart(ClientConnectionEvent.Disconnect event) {
        EventSoundRegistry.resetPlayer(event.getTargetEntity());
    }

}
