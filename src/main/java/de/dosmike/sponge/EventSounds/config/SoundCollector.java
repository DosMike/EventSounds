package de.dosmike.sponge.EventSounds.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.dosmike.sponge.EventSounds.EventSounds;
import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** gather sounds from other plugins */
public class SoundCollector {

    private static Gson gson = new Gson();

    /** @return maping of sound names to file list for registration */
    static Map<String, Set<String>> readIndex(PluginContainer plugin) {
        Map<String, Set<String>> entries = new HashMap<>();
        if (!plugin.getInstance().isPresent()) {
//            if (EventSounds.logVerbose()) EventSounds.l("Could not read sounds from %s", plugin.getName());
        } else {
            Optional<Asset> asset = plugin.getAsset( "eventsounds.json");
            if (asset.isPresent()) {
                try {
                    entries.putAll(gson.fromJson(asset.get().readString(), new TypeToken<Map<String, Set<String>>>() {}.getType()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (entries.size()>0)
                EventSounds.l("Found %d sounds for %s", entries.size(), plugin.getName());
        }
        return entries;
    }

    static Map<String, Set<String>> pluginSounds = new HashMap<>();
    static Map<String, Set<String>> externalFiles = new HashMap<>();
    public static void collect() {
        externalFiles.clear();
        pluginSounds.clear();
        Map<String, Map<String, Set<String>>> sounds = new HashMap<>(); //map pluginIDs to sound->files<>

        Sponge.getPluginManager().getPlugins().forEach(p->
                sounds.put(p.getId(), readIndex(p))
        );

        //validate files paths and squish plugin id + sound name
        sounds.forEach((id, entries)->{
            Set<String> soundNamesForPlugin = new HashSet<>();
            entries.forEach((n,files)->{
                Set<String> found = new HashSet<>();

                for (String f : files) {
                    Sponge.getPluginManager().getPlugin(id).get().getAsset(f).ifPresent(a->{
                        found.add(f);
                    });
                }
                String squish = String.format("%s.%s", id, n);
                if (found.size() != files.size()) {
                    Set<String> tmp = new HashSet<>(files);
                    tmp.removeAll(found);
                    EventSounds.w("Missing sounds for %s: %s", id, String.join(", ", tmp));
                } else {
//                    if (EventSounds.logVerbose()) EventSounds.l(" > %d files for %s", found.size(), squish);
                    externalFiles.put(squish, found);
                    soundNamesForPlugin.add(squish);
                }
            });
            if (!soundNamesForPlugin.isEmpty())
                pluginSounds.put(id, soundNamesForPlugin);
        });
    }

    /** retrieve the sounds from your plugin. the key is the name as specified in eventsounds.json.
     * @return A mapping of sound names to SoundTypes */
    public static Map<String, SoundType> getForPlugin(PluginContainer plugin) {
        Map<String, SoundType> map = new HashMap<>();
        pluginSounds.getOrDefault(plugin.getId(), new HashSet<>())
                .stream().map(s->{
                    int i = s.lastIndexOf('.');
                    return new String[]{s.substring(i+1), "es.plugin."+s};
                })
                .forEach(s->map.put(s[0], SoundType.of(s[1])));
        return map;
    }

    /** register SoundTypes to SoundDefinitionRegistry */
    public static void registerSound() {
        externalFiles.forEach((name,files)->{
            String[] f = files.toArray(new String[0]);
            EventSoundRegistry.registerSound(EventSoundRegistry.EVENT_PLUGIN, name, "master", f);
        });
    }

    /** copy all files to the resource-pack */
    static void copyToResourcePack(ZipOutputStream zos) {
        pluginSounds.forEach((id, names)->{
            Set<String> assets = new HashSet<>();
            for (String name : names) {
                assets.addAll(externalFiles.getOrDefault(name, new HashSet<>()));
            }
            for (String name : assets) {
                Sponge.getPluginManager().getPlugin(id).get().getAsset(name).ifPresent(a->{
                    try {
                        ZipEntry outEntry;
                        String path = "assets/minecraft/sounds/custom/es/plugin/" + id + "/" + name.replace('\\', '/').toLowerCase();
                        EventSounds.l("Zipping %s:%s to %s", id, name, path);
                        outEntry = new ZipEntry(path);
                        zos.putNextEntry(outEntry);
                        zos.write(a.readBytes());
                        zos.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

}
