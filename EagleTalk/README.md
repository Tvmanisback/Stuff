# EagleTalk for Eaglercraft

A small, opt-in browser voice-chat prototype for EaglercraftX-style clients. It is a separate WebSocket relay: ordinary players do not connect to it unless they install/enable the extension and press **Connect**.

## Important compatibility note
Eaglercraft variants differ. This extension adds its own overlay; it is not a native EaglerForge/EaglerX mod and cannot guarantee integration with every client. It does not patch the game protocol. The server-side JAR is a standalone voice relay, not an EaglerX plugin.

## Build outputs
The GitHub Actions workflow at `.github/workflows/build.yml` builds:
- `eagletalk-relay.jar` — standalone Java WebSocket relay
- `eagletalk-chrome-extension.zip` — unpacked Chrome extension packaged as ZIP

## Run the relay
Requires Java 17+. Run `java -jar eagletalk-relay.jar 8765`. Put it behind a TLS-enabled reverse proxy for public use and set `EAGLETALK_TOKEN` to a long random shared token before launching. Do not expose an unauthenticated relay to the public internet.

## Extension setup
1. Unzip the extension artifact.
2. In Chrome, open `chrome://extensions`, enable Developer mode, choose **Load unpacked**, and select the extracted `extension` folder.
3. Open your Eaglercraft client over HTTPS, set the relay URL (prefer `wss://your-host/`) and token in the overlay, then connect.
4. Allow microphone access when prompted. Hold **Talk** to transmit; use headphones to reduce feedback.

The relay forwards short PCM audio frames to other connected clients in the same room. It is a minimal prototype, not a hardened production voice service: add TLS, rate limits, moderation, proper identity/authentication, and server-side room authorization before public deployment. Audio is not end-to-end encrypted by this app; use TLS in transit.
