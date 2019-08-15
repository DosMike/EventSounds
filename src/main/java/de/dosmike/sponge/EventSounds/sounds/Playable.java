package de.dosmike.sponge.EventSounds.sounds;

import de.dosmike.sponge.EventSounds.config.PlaytimeManager;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.text.title.Title;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class Playable {

    public enum PlayTarget {
        /** play to noone */
        NONE,
        /** play to player triggering the sound */
        SOURCE,
        /** if event is pvp related, play to attacker and victim, otherwise equals SOURCE */
        BOTH,
        /** player to every player in the world of the triggering player */
        WORLD,
        /** play to everone currently on the server */
        ALL;

        public static PlayTarget getOrElse(String key, PlayTarget other) {
            for (PlayTarget target : values())
                if (target.name().equalsIgnoreCase(key))
                    return target;
            return other;
        }
    }

    private String[] message;
    private PlayTarget messageTarget, soundTarget;
    private SoundDefinition sound;
    private int priority;

    public PlayTarget getMessageTarget() {
        return messageTarget;
    }

    public PlayTarget getSoundTarget() {
        return soundTarget;
    }

    public SoundDefinition getSound() {
        return sound;
    }

    public int getPriority() {
        return priority;
    }


    private Text resolveMessage(String message, Player player, @Nullable Player victim) {
        message = message.replace("%player%", player.getName())
                .replace("%victim%", victim==null?"<ERROR>":victim.getName());
        return TextSerializers.FORMATTING_CODE.deserialize(message);
    }

    private void sendMessage(Player player, Player victim) {

        Text titleText = (message.length>0 && !message[0].isEmpty()) ? resolveMessage(message[0], player, victim) : Text.EMPTY;
        Text subtitleText = (message.length>1 && !message[1].isEmpty()) ? resolveMessage(message[1], player, victim) : Text.EMPTY;
        if (titleText.isEmpty() && subtitleText.isEmpty()) return;

        if (!messageTarget.equals(PlayTarget.NONE)) {
            player.sendTitle(Title.builder()
                    .title(titleText)
                    .subtitle(subtitleText)
                    .fadeIn(0).stay(60).fadeOut(20).build());
        }

        Set<Player> targets = new HashSet<>();
        if (messageTarget.equals(PlayTarget.BOTH) && victim != null) {
            targets.add(victim);
        } else if (messageTarget.equals(PlayTarget.WORLD)) {
            targets.addAll(player.getWorld().getPlayers());
        } else if (messageTarget.equals(PlayTarget.ALL)) {
            targets.addAll(Sponge.getServer().getOnlinePlayers());
        }
        targets.remove(player);

        Title title;
        if (targets.size()>0) {
            title = Title.builder()
                    .title(titleText)
                    .subtitle(subtitleText)
                    .fadeIn(0).stay(60).fadeOut(20).build();
            targets.forEach(target->target.sendTitle(title));
        }
    }
    private void playForPlayer(Player player) {
        player.playSound(sound.getSound(), sound.getCategory(), player.getPosition(), Double.MAX_VALUE, 1.0, 1.0);
    }
    private void playSound(Player player, Player victim) {
        Set<Player> players = new HashSet<>();
        if (soundTarget != PlayTarget.NONE) {
            players.add(player);
        }
        if (soundTarget == PlayTarget.BOTH && victim != null) {
            players.add(victim);
            playForPlayer(victim);
        } else if (soundTarget == PlayTarget.WORLD) {
            players.addAll(player.getWorld().getPlayers());
        } else if (soundTarget == PlayTarget.ALL) {
            players.addAll(Sponge.getServer().getOnlinePlayers());
        }
        players.forEach(this::playForPlayer);
    }

    /** for PvP related events */
    void play(@NotNull Player player, @Nullable Player victim) {
        PlaytimeManager.stopAndPlay(getSound(), player);
        sendMessage(player, victim);
        playSound(player, victim);
    }

    //Region builder
    public static class Builder {
        private String[] b_title, b_files;
        private int b_priority;
        private PlayTarget b_targetTitle, b_targetSound;
        protected Builder() {
        }
        public Builder fromConfigurationNode(ConfigurationNode node) {
            b_files = node.getNode("sounds").getChildrenList().stream().map(ConfigurationNode::getString).toArray(String[]::new);
            b_targetTitle = PlayTarget.getOrElse(node.getNode("titleTarget").getString("?"), PlayTarget.NONE);
            b_targetSound = PlayTarget.getOrElse(node.getNode("soundTarget").getString("?"), PlayTarget.ALL);

            List<String> tmp = node.getNode("title").getChildrenList().stream().map(ConfigurationNode::getString).collect(Collectors.toList());
            b_title = new String[]{"",""};
            if (tmp.size() > 0) b_title[0] = tmp.get(0);
            if (tmp.size() > 1) b_title[1] = tmp.get(1);

            b_priority = node.getNode("priority").getInt(-1);
            return Builder.this;
        }
        public Playable build(String event, String name) {
            Playable playable = new Playable();
            playable.message = b_title;
            playable.messageTarget = b_targetTitle;
            playable.soundTarget = b_targetSound;
            playable.priority = b_priority;
            playable.sound = EventSoundRegistry.registerSound(event, name, "master", b_files);
            return playable;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
    //endregion
}
