package de.dosmike.sponge.EventSounds;

import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

public class cmdChatSounds {

    public static CommandSpec getCommandSpec() {
        return CommandSpec.builder()
                .permission("es.command.soundlist")
                .description(Text.of("List of registered chat sounds"))
                .extendedDescription(Text.of("List all chat sounds currently registered - You may not be allowed to play all"))
                .arguments(
                        GenericArguments.none()
                )
                .executor((src,args)->{
                    src.sendMessage(Text.of("Currently known chat sounds:", Text.NEW_LINE, String.join(", ", EventSoundRegistry.listSaySounds())));
                    return CommandResult.success();
                })
                .build();
    }

}
