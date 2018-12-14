package de.dosmike.sponge.EventSounds;

import org.spongepowered.api.Sponge;

public class CommandRegistra {
	public static void RegisterCommands() {
		Sponge.getCommandManager().register(EventSounds.getInstance(), cmdEventSounds.getCommandSpec(), "eventsounds", "es");
		Sponge.getCommandManager().register(EventSounds.getInstance(), cmdChatSounds.getCommandSpec(), "soundlist", "chatsounds");
		Sponge.getCommandManager().register(EventSounds.getInstance(), cmdStopSound.getCommandSpec(), "stopsound");
	}
}
