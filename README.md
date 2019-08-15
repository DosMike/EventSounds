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

### [Can I get a YouTube tutorial?](https://youtu.be/Cl-5OnnBGwU)

### [Configuration](https://github.com/DosMike/EventSounds/blob/master/configuration.md)

### [I'm plugin developer](https://github.com/DosMike/EventSounds/blob/master/developer.md)

### Dependencies

**ftp4j as FTP client**  
Copyright (C) 2012  Sauron Software

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

### Remote connections

If you set up a ftp connection this plugin will connect to the configured
ftp server on request (`/es reload -u`)

**[Version Checker](https://github.com/DosMike/SpongePluginVersionChecker)**  
This plugin uses a version checker to notify you about available updates.  
This updater is **disabled by default** and can be enabled in `config/eventsounds.conf`.
by setting the value `plugin.VersionChecker` to `true`.  
If enabled it will asynchronously check (once per server start) if the Ore repository has any updates.  
This will *only print update notes into the server log*, no files are being downlaoded!