package de.dosmike.sponge.EventSounds.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.dosmike.sponge.EventSounds.EventSounds;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;

import java.util.Arrays;
import java.util.Objects;

/** reflects a sound entry with the most important Json properties for sounds.json */
public class SoundDefinition {
    private String name;
    private SoundCategory category;
    private String[] files;
    private boolean external;

    SoundDefinition(String registryName, String category, String... files) {
        this.name = registryName.toLowerCase();
        try {
            String tmp = category.indexOf(':')<0?"minecraft:"+category:category;
            this.category = Sponge.getRegistry().getType(SoundCategory.class, tmp).get();
        } catch (Exception e) {
            throw new RuntimeException("Sound Category "+category+" not found!");
        }
        this.files = files;
        this.external = (registryName.split("\\.").length>3);
    }

    public SoundType getSound() {
        return SoundType.of(name);
    }
    public SoundCategory getCategory() {
        return category;
    }
    public String getRegistryName() {
        return name;
    }
    /** @return the name as set in the configuration */
    public String getName() {
        int i = name.lastIndexOf('.');
        if (i<0) EventSounds.w("regName: "+name);
        return name.substring(Math.max(0, i+1));
    }
    /** @return true if this sound was loaded from a plugin jar */
    public boolean isExternal() {
        return external;
    }

    public String[] getFiles() {
        return files;
    }

    public JsonObject toJson() {
        JsonArray jfiles = new JsonArray();
        if (!external)
            for (String s : files)
                jfiles.add("custom/es/server/"+(s.endsWith(".ogg")?s.substring(0, s.length()-4):s).toLowerCase());
        else {
            String pluginID = name.split("\\.")[2];
            for (String s : files)
                jfiles.add("custom/es/plugin/" + pluginID + "/" + (s.endsWith(".ogg") ? s.substring(0, s.length() - 4) : s).toLowerCase());
        }
        JsonObject jentry = new JsonObject();
        jentry.add("category", new JsonPrimitive(category.getName().toLowerCase()));
        jentry.add("sounds", jfiles);
        return jentry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundDefinition that = (SoundDefinition) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(category, that.category) &&
                Arrays.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, category);
        result = 31 * result + Arrays.hashCode(files);
        return result;
    }
}
