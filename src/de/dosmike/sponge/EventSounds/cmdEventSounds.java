package de.dosmike.sponge.EventSounds;

import de.dosmike.sponge.EventSounds.config.ResourcePacker;
import de.dosmike.sponge.EventSounds.config.SoundCollector;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class cmdEventSounds {

	/** state from -2 to 1 */
	private static Text getQuadStateText(String text, int state) {
		if (state < -1 ) {
			return Text.of(TextColors.DARK_GRAY, "\u2718 ", text); //don't do
		} else if (state == -1) {
			return Text.of(TextColors.WHITE, "\u2751 ", text); //not yet
		} else if (state == 0) {
			return Text.of(TextColors.YELLOW, "\u25BA ", text, "..."); //running
		} else {
			return Text.of(TextColors.GREEN, "\u2714 ", text); //completed
		}
	}
	private static void printUpdate(CommandSource viewer, int stage, int maxStage) {
		String[] stages = new String[]{"Searching for other Plugins", "Reloading configuration", "Rebuilding Resource-Pack", "Uploading Pack to Server"};
		PaginationList.Builder builder = PaginationList.builder().title(Text.of("EventSounds"));
		builder.padding(Text.of("="));
		List<Text> lines = new LinkedList<>();

		for (int i = 0; i < stages.length; i++) {
			int state = (i > maxStage) ? -2 : ( Integer.compare(stage, i) );
			lines.add(getQuadStateText(stages[i], state));
		}
		lines.add(Text.EMPTY);
		//dynamic last line
		if (stage > maxStage) {
			if (stage == stages.length) {
				lines.add(Text.of(TextColors.GREEN, "Done! ", TextColors.GOLD, "You may have to restart the server"));
			} else {
				lines.add(Text.of(TextColors.GREEN, "The reload was complete!"));
			}
		} else {
			lines.add(Text.of(TextColors.GRAY, "Please wait for the task to finish"));
		}

		builder.contents(lines);
		builder.sendTo(viewer);
	}
	public static AtomicBoolean rebuilding = new AtomicBoolean(false);


	public static CommandSpec getCommandSpec() {

		CommandSpec cmdReload = CommandSpec.builder()
				.permission("es.command.reload")
				.description(Text.of("Reload configuration, (-b)uild pack and (-u) upload it via ftp"))
				.extendedDescription(Text.of("Reload the configuration and rebuild the resource-pack", Text.NEW_LINE, "The flag -b (es.command.rebuild) will also rebuild the resource-pack", Text.NEW_LINE, "The flag -u (es.command.upload) will upload the pack via ftp if possible (implies -b)"))
				.arguments(
						GenericArguments.flags().permissionFlag(
								"es.command.upload",
								"u"
						).permissionFlag(
								"es.command.rebuild",
								"b"
						).buildWith(GenericArguments.none())
				)
				.executor((src,args)->{
					if (rebuilding.getAndSet(true))
						throw new CommandException(Text.of("The command is already running..."));
					boolean upload = args.hasAny("u");
					boolean rebuild = upload || args.hasAny("b");
					int maxStage = upload?3:(rebuild?2:1);

					try {
						printUpdate(src, 0, maxStage);
						SoundCollector.collect();
						printUpdate(src, 1, maxStage);
						EventSounds.getInstance().onReload(null);
						printUpdate(src, 2, maxStage);
					} catch (Exception e) {
//						e.printStackTrace();
						rebuilding.set(false);
						throw new CommandException(Text.of(e.getMessage()!=null?e.getMessage():e.getClass().getSimpleName()));
					}

					EventSounds.getExecutor().execute(()->{
						try {
							ResourcePacker packer = EventSounds.getResourcePacker().orElse(null);
							if (packer != null) {
								if (rebuild) {
									packer.repack();
									printUpdate(src, 3, maxStage);
								}
								if (upload) {
									packer.upload();
									printUpdate(src, 4, maxStage);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							src.sendMessage(Text.of(TextColors.RED, "Failed to Repack: ", e.getMessage()));
						} finally {
							rebuilding.set(false);
						}

					});
					return CommandResult.success();
				})
				.build();

		return CommandSpec.builder()
			.description(Text.of("Management command for EventSounds"))
			.extendedDescription(Text.of("/es prompts Resource Pack install."))
			.arguments(
					GenericArguments.none()
			)
			.executor(((src, args) -> {
				Optional<ResourcePack> pack = Sponge.getServer().getDefaultResourcePack();
				if (src instanceof Player) {
					Player player = (Player)src;
					if (pack.isPresent()) {
						player.sendMessage(Text.of(TextColors.DARK_GREEN, "Sending the Resource Pack your way..."));
						EventSounds.getExecutor().schedule(()->
							player.sendResourcePack(pack.get())
						, 50, TimeUnit.MILLISECONDS); //wait one tick, so the chat message gets sent first
					} else {
						player.sendMessage(Text.of(TextColors.RED, "There is no Resource Pack defined in server.properties"));
					}
				} else {
					if (pack.isPresent()) {
						src.sendMessage(Text.of("Resource Pack at "+pack.get().getUri().toString()));
					} else {
						src.sendMessage(Text.of("There is no Resource Pack defined in server.properties"));
					}
				}
				return CommandResult.success();
			}))
			.child(cmdReload, "reload")
//			.child(cmdPlay, "play")
			.build();
	}
	
}
