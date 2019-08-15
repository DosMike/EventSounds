# Developer Stuff

## Adding this plugin as dependency

```{groovy}
repositories {
    ...
    maven { url "https://jitpack.io" }
}
dependencies {
    ...
    compile 'com.github.DosMike:EventSounds:master-SNAPSHOT'
}
```

## Adding sounds for EventSounds to register

* Create the asset-folder for your plugins (see Sponge docs for that)
* Thorw all sounds as .ogg in that folder
* Create an eventsounds.json within your asset folder.

The eventsounds.json looks like this: 
```Json
{
  "soundName": ["fileName.ogg", "fileName.ogg"],
  "soundName": ["fileName.ogg", "fileName.ogg"]
}
```

## Getting the SoundTypes for these Sounds

Add something like this to your event-listeners:
```Java
private static Map<String, SoundType> pluginSounds = new HashMap<>();
public static Optional<SoundType> getSound(String name) {
  return Optional.ofNullable(pluginSounds.get(name));
}
@Listener
public void onSoundsCollected(SoundsCollectedEvent event) {
  pluginSounds = SoundCollector.getForPlugin(getContainer()); //pass your PluginContainer here
}
```

Now you can access the SoundTypes with the name you've choosen in your eventsounds.json
like this:
```Java
SoundType soundType = ClassFromAbove.getSound("cash").orElse(SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP);
player.playSound(soundType, player.getLocation().getPosition(), 1.0);
```