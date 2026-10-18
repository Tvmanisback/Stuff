# EagleTalk for EaglercraftX

EagleTalk is a small Bukkit/Paper/Spigot plugin that helps administrators configure **EaglerXServer's built-in voice service**. It does not run a separate voice relay.

## Requirements

- Java 17+
- Paper, Spigot, or Bukkit compatible with the EaglerXServer Bukkit API
- EaglerXServer installed in the same server's `plugins` folder
- A compatible Eaglercraft client

EaglerXServer currently notes that Bukkit plugin support is not maintained for versions above 1.17. Check the upstream documentation for compatibility with your exact server version.

## Build artifacts

The GitHub Actions workflow at `.github/workflows/build.yml` builds:
- `eagletalk-plugin.jar` — Paper/Bukkit/Spigot setup-helper plugin
- `eagletalk-chrome-extension.zip` — activation-only Chrome extension (no voice UI, microphone access, networking, or audio handling)

## Install and configure

1. Install EaglerXServer in the server's `plugins` folder.
2. Put `eagletalk-plugin.jar` in the same `plugins` folder.
3. Start the server once and open `plugins/EaglercraftXServer/settings.cfg`.
4. Enable `voice_service.enable_voice_service` and configure the permitted worlds as appropriate. EaglerXServer's setting is disabled by default.
5. Restart the server and test with a compatible Eaglercraft client.
6. Use `/voice status` to check that the dependency and settings file are present, or `/voice setup` for setup instructions.

## Client controls and extension

The extension only dispatches an `eagletalk:activate` activation event. It does not inject a voice UI or implement voice transport. The browser extension alone cannot create a client mod; it requires a compatible client-side integration to listen for that event. Use the voice controls supplied by your Eaglercraft client to self-mute or mute another player.

## Important limitations

This plugin is a setup helper, not a replacement for EaglerXServer's voice implementation. It does not claim to implement server-enforced per-player voice muting or a custom client UI. EaglerXServer's native voice service uses WebRTC-related signaling and must be enabled/configured in EaglerXServer itself. Review the upstream security warnings before enabling voice on a public server.

Official documentation: https://github.com/lax1dude/eaglerxserver
