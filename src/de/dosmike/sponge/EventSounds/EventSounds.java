package de.dosmike.sponge.EventSounds;

import com.google.inject.Inject;
import de.dosmike.sponge.EventSounds.config.ConfigLoader;
import de.dosmike.sponge.EventSounds.config.ResourcePacker;
import de.dosmike.sponge.EventSounds.config.SoundCollector;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.util.Optional;

@Plugin(id="eventsounds", name="Event Sounds", version="0.1", authors={"DosMike"})
public class EventSounds {

	private SpongeExecutorService executor;
	public static SpongeExecutorService getExecutor() {
		return instance.executor;
	}

	private static EventSounds instance;
	public static EventSounds getInstance() {
		return instance;
	}

	private static ResourcePacker packer = null;
	static Optional<ResourcePacker> getResourcePacker() {
		return Optional.ofNullable(packer);
	}

	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		instance = this;
		SoundCollector.collect();

		try {
			loadConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CommandRegistra.RegisterCommands();
		executor = Sponge.getScheduler().createAsyncExecutor(this);
		Sponge.getEventManager().registerListeners(this, new EventListeners());
	}
	
	@Listener
	public void onReload(GameReloadEvent event) {
		loadConfig();
	}

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> config;
	private void loadConfig() {
		try {
			packer = ConfigLoader.load(config);
		} catch (Exception e) {
			if (e.getMessage()!=null)
				w(e.getMessage());
			else
				w(e.getClass().getSimpleName());
			throw new RuntimeException("Failed to load config", e);
		}
	}
}
