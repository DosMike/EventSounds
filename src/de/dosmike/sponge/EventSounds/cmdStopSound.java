package de.dosmike.sponge.EventSounds;

import org.spongepowered.api.GameDictionary;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class cmdStopSound {

    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .description(Text.of("Usage: /stopsound [player] [source] [sound]"))
                .extendedDescription(Text.of("This command was changed! Each argument is now optional and has it's own additional permission: ", Text.NEW_LINE, " Player: minecraft.command.stopsound.player", Text.NEW_LINE, " Source: minecraft.command.stopsound.source", Text.NEW_LINE, " Sound: minecraft.command.stopsound.sound"))
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.requiringPermission(
                                        GenericArguments.player(Text.of("player"))
                                        ,"minecraft.command.stopsound.player")
                        ),
                        GenericArguments.optional(
                                GenericArguments.requiringPermission(
                                        GenericArguments.catalogedElement(Text.of("source"), SoundCategory.class)
                                        ,"minecraft.command.stopsound.source")
                        ),
                        GenericArguments.optional(
                                GenericArguments.requiringPermission(
                                        GenericArguments.string(Text.of("sound"))
                                        ,"minecraft.command.stopsound.sound")
                        )
                )
                .executor(((src, args) -> {
                    Collection<Player> targets = args.getAll("player");
                    if (targets.isEmpty() && src instanceof Player) {
                        targets.add((Player)src);
                    }
                    if (targets.isEmpty()) {
                        if (src instanceof ConsoleSource)
                            throw new CommandException(Text.of("Console requires target"));
                        else
                            throw new CommandException(Text.of("No player found"));
                    }

                    Optional<SoundCategory> category = args.getOne("source");
                    Optional<SoundType> sound = args.<String>getOne("sound").map(SoundType::of);

                    if (category.isPresent() && sound.isPresent()) {
                        for (Player player : targets)
                            player.stopSounds(sound.get(), category.get());
                        Sponge.getServer().getBroadcastChannel().send(
                                Text.of(String.format("Stopped sound '%s' with source '%s' for %s"
                                                    , sound.get().getId().toLowerCase()
                                                    , category.get().getName().toLowerCase()
                                                    , String.join(", ", targets.stream().map(Player::getName).collect(Collectors.toSet())))));
                    } else if (category.isPresent()) {
                        for (Player player : targets)
                            player.stopSounds(category.get());
                        Sponge.getServer().getBroadcastChannel().send(
                                Text.of(String.format("Stopped source '%s' for %s"
                                        , category.get().getName().toLowerCase()
                                        , String.join(", ", targets.stream().map(Player::getName).collect(Collectors.toSet())))));
                    } else if (sound.isPresent()) {
                        for (Player player : targets)
                            player.stopSounds(sound.get());
                        Sponge.getServer().getBroadcastChannel().send(
                                Text.of(String.format("Stopped sound '%s' for %s"
                                        , sound.get().getId().toLowerCase()
                                        , String.join(", ", targets.stream().map(Player::getName).collect(Collectors.toSet())))));
                    } else {
                        for (Player player : targets)
                            player.stopSounds();
                        Sponge.getServer().getBroadcastChannel().send(
                                Text.of(String.format("Stopped all sounds for %s"
                                        , String.join(", ", targets.stream().map(Player::getName).collect(Collectors.toSet())))));
                    }

                    return CommandResult.builder().affectedEntities(targets.size()).build();
                }))
                .permission("minecraft.command.stopsound")
                .build();
    }

}
