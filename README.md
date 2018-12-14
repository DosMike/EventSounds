# EventSounds
### Add custom sounds to your server!

Included Events: Joinsounds, Chatsounds, Quakesounds

#### Commands and Permissions
`/eventsounds`, `/es` - Reload the resource-pack  
Permission: none

`/es reload [-b|-u]` - Reload the config  
Permission: `es.command.reload`  
*Flags:*  
`-b` - Rebuild the resource-pack  
Permission: `es.command.rebuild`  
`-u` - Rebuild and upload the resource-pack  
Permission: `es.command.upload`

`/soundlist`, `/chatsounds` - List all sounds available in chat  
Permission: `es.command.soundlist`

`/stopsound [player] [source] [sound]` - Overwrite the vanilla /stopsound to allow no args for self targeting
Permission: `minecraft.command.stopsound`  
*Arguments:*  
`player` - The target to stop sounds for, can be selector  
Permission: `minecraft.command.stopsound.player`  
`source` - The channel, the sound shall be stop on  
Permission: `minecraft.command.stopsound.source`  
`sound` - The sound that should be stopped  
Permission: `minecraft.command.stopsound.sound`

### How to add sounds:
Put them inside a assets/eventsounds/ folder on your server (you have to create that folder).
Then add them to the configuration and update your resource-pack (/es reload).

**Note:** Changing the resource-pack requires the minecraft-server to restart! This is a limitation of minecraft servers themself, not of the plugin or Sponge!

### [Can I get a YouTube tutorial?](https://youtu.be/Cl-5OnnBGwU)

### [Configuration]()

### [I'm plugin developer]()