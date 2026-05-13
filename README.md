# Tweaked

Tweaked is a Minecraft 1.21.1 NeoForge mod.

## Client and server usage

Tweaked is optional on multiplayer servers for the client-side/base features. Install it on the client to use the standard client tweaks when joining servers that do not have Tweaked installed.

When Tweaked is also installed on a server, the server-side hooks in `de.maax.tweaked.server` are enabled automatically. These hooks provide the extra world/server behavior, such as applying sandbox startup time/weather and pending sandbox spawn options.

## Development

Build the mod:

```powershell
.\gradlew.bat build
```

Run the Minecraft client:

```powershell
.\gradlew.bat runClient
```
