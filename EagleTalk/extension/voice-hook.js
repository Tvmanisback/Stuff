(() => {
  // Runs in the page's MAIN world so it can observe the browser voice APIs used by
  // Eaglercraft clients. EaglerX/client versions may use different internals; this
  // deliberately does not create a separate relay or replace the game's signaling.
  if (window.__eagleTalkVoiceHook) return;
  window.__eagleTalkVoiceHook = true;
  const state = window.__eagleTalkState = { selfMuted: false, peers: new Map(), nextPeerId: 1 };
  const send = (type, detail = {}) => window.postMessage({ __eagleTalk: true, type, ...detail }, location.origin);
  const originalGetUserMedia = navigator.mediaDevices?.getUserMedia?.bind(navigator.mediaDevices);
  if (originalGetUserMedia) {
    navigator.mediaDevices.getUserMedia = async constraints => {
      const stream = await originalGetUserMedia(constraints);
      if (constraints?.audio) {
        for (const track of stream.getAudioTracks()) {
          track.enabled = !state.selfMuted;
          track.addEventListener('ended', () => send('local-track-ended'));
        }
        send('local-mic-available');
      }
      return stream;
    };
  }
  const OriginalPC = window.RTCPeerConnection;
  if (OriginalPC) {
    const patched = new Proxy(OriginalPC, {
      construct(Target, args, NewTarget) {
        const pc = Reflect.construct(Target, args, NewTarget);
        const id = 'Peer ' + state.nextPeerId++;
        state.peers.set(id, { pc, muted: false });
        send('peer-added', { id });
        const applyMute = event => {
          const streams = event.streams || [];
          for (const stream of streams) for (const track of stream.getAudioTracks()) track.enabled = !state.peers.get(id)?.muted;
          // Some clients provide a streamless track event.
          if (event.track?.kind === 'audio') event.track.enabled = !state.peers.get(id)?.muted;
        };
        pc.addEventListener('track', applyMute);
        const remove = () => { state.peers.delete(id); send('peer-removed', { id }); };
        pc.addEventListener('connectionstatechange', () => { if (['closed','failed'].includes(pc.connectionState)) remove(); });
        return pc;
      }
    });
    try { window.RTCPeerConnection = patched; } catch {}
  }
  window.addEventListener('message', event => {
    if (event.source !== window || event.origin !== location.origin || event.data?.__eagleTalk !== true) return;
    const { type, id, value } = event.data;
    if (type === 'set-self-muted') {
      state.selfMuted = !!value;
      // Tracks acquired after the setting changes are handled by getUserMedia above.
      send('self-muted-changed', { value: state.selfMuted });
    } else if (type === 'set-peer-muted' && state.peers.has(id)) {
      const entry = state.peers.get(id); entry.muted = !!value;
      for (const receiver of entry.pc.getReceivers()) if (receiver.track?.kind === 'audio') receiver.track.enabled = !entry.muted;
      send('peer-muted-changed', { id, value: entry.muted });
    }
  });
})();
