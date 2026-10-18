# EagleTalk for EaglercraftX

EagleTalk is a Bukkit/Paper/Spigot management plugin for **EaglerXServer's native voice chat**. The actual microphone capture, audio transport, and in-game voice controls are supplied by EaglerXServer and a compatible EaglercraftX client. EagleTalk does not implement a separate audio relay.

## Requirements

- Java 17+
- A Bukkit/Spigot/Paper server version compatible with the installed EaglerXServer build
- EaglerXServer installed as a server plugin
- A compatible EaglercraftX client with native voice support
- Permission `eagletalk.admin` (operators have it by default) to change the voice setting

EaglerXServer's upstream documentation says Bukkit support is not maintained for versions above 1.17; check compatibility with your exact server version.

## Build artifacts

The GitHub Actions workflow at `.github/workflows/build.yml` builds:
- `eagletalk-plugin.jar` — EagleTalk server configuration plugin
- `eagletalk-chrome-extension.zip` — companion extension package; it does not itself implement microphone/audio transport

## Install

1. Install EaglerXServer on the server using its official installation instructions.
2. Put `eagletalk-plugin.jar` in the server's `plugins` folder.
3. Start the server and let EaglerXServer create its settings file.
4. Run `/voice status` in-game or from the console.
5. As an operator, run `/voice enable`.
6. Fully restart the server. EaglerXServer reads the setting during startup.
7. Join using a compatible EaglercraftX client and use the voice controls already included in that client.

Commands:
- `/voice status` — report dependency and setting status
- `/voice enable` — set `voice_service.enable_voice_service` to true
- `/voice disable` — set `voice_service.enable_voice_service` to false
- `/voice setup` — show setup instructions

EagleTalk searches for EaglerXServer's `settings.cfg`, `settings.gson`, `settings.json`, `settings.yaml`, `settings.yml`, or `settings.toml` file and updates the `enable_voice_service` boolean when it can identify it. If the setting cannot be found, edit it manually in the settings file. Changes require a full restart.

## Client / Chrome extension note

A Chrome extension cannot turn an arbitrary Eaglercraft page into a voice client merely by dispatching a custom event. Actual voice functionality depends on EaglerXServer and a client that implements the Eaglercraft voice protocol. The extension is not required for native voice chat, and it does not capture microphone audio.

## Security note

EaglerXServer documents its voice service as disabled by default and warns that the voice protocol can be stressed or abused, especially on large public servers. Enable it only if you understand and accept that risk. Voice availability also depends on the client build and the hosting setup.

Official upstream documentation: https://github.com/lax1dude/eaglerxserver
