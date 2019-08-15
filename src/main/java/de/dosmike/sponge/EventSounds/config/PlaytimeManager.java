package de.dosmike.sponge.EventSounds.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itwookie.utils.Expiring;
import de.dosmike.sponge.EventSounds.EventSounds;
import de.dosmike.sponge.EventSounds.sounds.EventSoundRegistry;
import de.dosmike.sponge.EventSounds.sounds.SoundDefinition;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class tracks when sounds are played, how long sounds last and
 * is supposed to stop them, before EventSounds plays the next sound.
 * This help to prevent overlap of sounds, longer than the event cooldown.
 */
public class PlaytimeManager {

    private static Map<String, Long> soundLengths = new HashMap<>();
    private static Map<UUID, Set<Expiring<SoundDefinition>>> currentlyPlaying = new HashMap<>();

    /**
     * Mojangs files have a samplerate of 48kHz. This can be used to estimate the sound length without
     * having to actually read the files, or ship them (since the server does not actually know them).
     * Calculating the time in seconds would be byteSize/playbackRate, but since the time in ms is wanted
     * we use a 1000th of the playbackRate.
     */
    private static final int playbackRate = 48;

    public static void readFileLengths() {
        soundLengths.clear();
        currentlyPlaying.clear();

        JsonObject soundJson;
        JsonObject fileMapJson;

        try {
            JsonParser parser = new JsonParser();
            soundJson = parser.parse(Sponge.getAssetManager().getAsset(EventSounds.getInstance(), "soundindex_1-12.json").get().readString()).getAsJsonObject();
            fileMapJson = parser.parse(Sponge.getAssetManager().getAsset(EventSounds.getInstance(), "filemapping_1-12.json").get().readString()).getAsJsonObject().getAsJsonObject("objects");
        } catch (IOException e) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : soundJson.entrySet()) {
            String id = entry.getKey();
            JsonObject definition = entry.getValue().getAsJsonObject();
            Set<String> soundFiles = new HashSet<>();
            for (JsonElement element : definition.getAsJsonArray("sounds")) {
                if (element.isJsonObject()) {
                    if (!(element.getAsJsonObject().has("type") && element.getAsJsonObject().get("type").getAsString().matches("event")))
                        soundFiles.add(element.getAsJsonObject().get("name").getAsString());
                } else if (element.isJsonPrimitive()) {
                    soundFiles.add(element.getAsString());
                } else
                    throw new RuntimeException("cached sounds.json seems to be corrupted!");
            }
            //get longest length
            long length = 0;
            for (String file : soundFiles) {
                try {
                    length = Math.max(length, fileMapJson.getAsJsonObject(String.format("minecraft/sounds/%s.ogg", file)).get("size").getAsLong());
                } catch (NullPointerException e) {
                    EventSounds.w("NullPointerException at %s", file);
                }
            }
            long timeMs = length/playbackRate;

//            EventSounds.l("Sound %s is %d ms long", id, timeMs);
            if (timeMs > 0)
                soundLengths.put(id, timeMs);
            else {
                if (EventSounds.logVerbose()) EventSounds.w("Could not read length for sound %s", id);
            }
        }

        EventSoundRegistry.getSoundDefinitions().forEach(def->{
            if (EventSounds.logVerbose()) EventSounds.l("File: %s %s", def.isExternal()?"Plugin":"Config", String.join(", ", def.getFiles()));
            long timeMs = -1;
            if (!def.isExternal()) {

                for (String file : def.getFiles()) {
                    try {
                        File f = new File(".", "assets/eventsounds/" + file);
                        int bytelength = (int) f.length();
                        byte[] filebytes = new byte[bytelength];
                        new FileInputStream(f).read(filebytes);
                        timeMs = Math.max(timeMs, getOggLength(filebytes));
                    } catch (IOException e) {
                        EventSounds.w("Could not read %s", file);
                    }
                }

            } else {

                String[] p = def.getRegistryName().split("\\.");
                String id = p[p.length-2];
                for (String file : def.getFiles()) {
                    try {
                        byte[] filebytes = Sponge.getPluginManager().getPlugin(id).get().getAsset(file).get().readBytes();
                        timeMs = Math.max(timeMs, getOggLength(filebytes));
                    } catch (IOException e) {
                        EventSounds.w("Could not read %s:%s", id, file);
                    }
                }

            }

            if (timeMs > 0)
                soundLengths.put(def.getRegistryName(), timeMs);
            else {
                if (EventSounds.logVerbose()) EventSounds.w("Could not read length for sound %s", def.getRegistryName());
            }
        });
    }

    private static long getOggLength(byte[] file) {
        int rate = -1;
        long length = -1;
        for (int i = 0; i<file.length-15; i++) {
            if (file[i] == (byte)'v' && new String(file,i,6,StandardCharsets.US_ASCII).equals("vorbis")) {
                ByteBuffer buffer = ByteBuffer.wrap(file, i+11, 4);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                rate = buffer.getInt();
                break;
            }
        }
        for (int i = file.length-1-8-2-4; i>=0; i--) {
            if (file[i] == (byte)'O' && new String(file,i,4,StandardCharsets.US_ASCII).equals("OggS")) {
                ByteBuffer buffer = ByteBuffer.wrap(file, i+6, 8);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                length = buffer.getLong();
                break;
            }
        }
        if (rate<=0 || length<=0)
            return 0;
        return length*1000 / rate;
    }

    /** returns the sound length in ms or 0 if not available */
    public static long getSoundLength(SoundType sound) {
        String id = sound.getId();
        if (id.contains(":")) id = id.substring(id.indexOf(":")+1);
        return soundLengths.getOrDefault(id, 0L);
    }

    /** stops sounds for the specified player if there's still one running in the same category */
    public static void stopAndPlay(SoundDefinition sound, Player player) {
        cleanRegistry();

        Set<Expiring<SoundDefinition>> playing = currentlyPlaying.getOrDefault(player.getUniqueId(), new HashSet<>());
        playing.removeIf(s->{ //stop sounds from the same event if still running
//            boolean res = s.getAnyways().getCategory().equals(sound.getCategory());
            String event1 = s.getAnyways().getRegistryName().split("\\.")[1];
            String event2 = sound.getRegistryName().split("\\.")[1];
            boolean res = event1.equals(event2);
            if (res) player.stopSounds(sound.getSound());
            return res;
        });
        playing.add(Expiring.expireIn(sound, getSoundLength(sound.getSound())));
        currentlyPlaying.put(player.getUniqueId(), playing);
    }

    public static void cleanRegistry() {
        Set<UUID> staleKeys = new HashSet<>();
        for (Map.Entry<UUID, Set<Expiring<SoundDefinition>>> entry : currentlyPlaying.entrySet()) {
            Set<Expiring<SoundDefinition>> sounds = entry.getValue();
            sounds.removeIf(Expiring::isExpired);
            if (sounds.isEmpty())
                staleKeys.add(entry.getKey());
        }
        for (UUID stale : staleKeys)
            currentlyPlaying.remove(stale);
    }

}
