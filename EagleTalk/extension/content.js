(() => {
  if (window.__eagleTalkInstalled) return;
  window.__eagleTalkInstalled = true;
  const style = document.createElement('style');
  style.textContent = '#eagletalk-box{position:fixed;z-index:2147483647;right:16px;bottom:16px;width:270px;padding:12px;background:#151515;color:#fff;border:1px solid #a11;border-radius:10px;font:13px Arial,sans-serif;box-shadow:0 4px 18px #0008}#eagletalk-box input{box-sizing:border-box;width:100%;margin:4px 0;padding:7px;background:#222;color:#fff;border:1px solid #555;border-radius:5px}#eagletalk-box button{padding:8px;margin:4px 4px 4px 0;border:0;border-radius:5px;background:#a11;color:#fff;cursor:pointer}#eagletalk-status{font-size:12px;color:#ddd;margin:5px 0}';
  document.documentElement.appendChild(style);
  const box = document.createElement('section'); box.id = 'eagletalk-box';
  box.innerHTML = '<strong>EagleTalk · Voice</strong><div id="eagletalk-status">Disconnected — voice is opt-in</div><label>Relay URL (wss:// recommended)<input id="et-url" placeholder="wss://voice.example.com"></label><label>Room<input id="et-room" maxlength="64" value="lobby"></label><label>Shared token<input id="et-token" type="password" autocomplete="off"></label><button id="et-connect">Connect</button><button id="et-talk" disabled>Hold to Talk</button><button id="et-disconnect">Disconnect</button>';
  document.documentElement.appendChild(box);
  const $ = id => box.querySelector('#' + id); const status = s => $('eagletalk-status').textContent = s;
  chrome.storage.local.get(['url','room'], v => { $('et-url').value = v.url || ''; $('et-room').value = v.room || 'lobby'; });
  let ws = null, stream = null, ctx = null, processor = null, source = null, audioQueue = [], playing = false, transmitting = false;
  function stopAudio() { transmitting = false; if (processor) { processor.disconnect(); processor.onaudioprocess = null; } if (source) source.disconnect(); if (ctx) ctx.close().catch(()=>{}); if (stream) stream.getTracks().forEach(t => t.stop()); processor = source = ctx = stream = null; }
  function disconnect() { stopAudio(); if (ws) { try { ws.close(); } catch {} } ws = null; $('et-talk').disabled = true; $('et-connect').disabled = false; status('Disconnected'); }
  $('et-connect').addEventListener('click', async () => {
    const url = $('et-url').value.trim(), room = $('et-room').value.trim(), token = $('et-token').value;
    if (!/^wss:\/\//i.test(url) && !/^ws:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/?$/i.test(url)) { status('Use wss:// (ws:// is localhost-only).'); return; }
    if (!room || !token) { status('Enter a room and shared token.'); return; }
    $('et-connect').disabled = true; status('Connecting…');
    try {
      ws = new WebSocket(url); ws.binaryType = 'arraybuffer';
      ws.onopen = () => { ws.send(JSON.stringify({type:'join', room, token})); chrome.storage.local.set({url, room}); status('Connected. Click and hold Talk to transmit.'); $('et-talk').disabled = false; };
      ws.onmessage = ev => { if (typeof ev.data === 'string') { try { const m=JSON.parse(ev.data); if(m.type==='ready') status('Connected to room: '+m.room+' · hold Talk to speak'); } catch {} } else playPcm(ev.data); };
      ws.onerror = () => status('Connection error. Check relay URL and server logs.');
      ws.onclose = () => { stopAudio(); ws = null; $('et-talk').disabled = true; $('et-connect').disabled = false; status('Disconnected'); };
    } catch (e) { status('Could not connect: '+e.message); $('et-connect').disabled = false; }
  });
  $('et-disconnect').addEventListener('click', disconnect);
  async function beginTalk() {
    if (!ws || ws.readyState !== WebSocket.OPEN || transmitting) return;
    try {
      stream = await navigator.mediaDevices.getUserMedia({audio:{echoCancellation:true, noiseSuppression:true, autoGainControl:true}, video:false});
      ctx = new AudioContext(); source = ctx.createMediaStreamSource(stream); processor = ctx.createScriptProcessor(2048,1,1);
      processor.onaudioprocess = e => { if (!transmitting || !ws || ws.readyState !== WebSocket.OPEN) return; const input=e.inputBuffer.getChannelData(0); const out=new Int16Array(input.length); for(let i=0;i<input.length;i++){const x=Math.max(-1,Math.min(1,input[i]));out[i]=x<0?x*32768:x*32767;} ws.send(out.buffer); };
      source.connect(processor); processor.connect(ctx.destination); transmitting = true; $('et-talk').textContent='Talking… release to stop'; status('Microphone active — release Talk to mute.');
    } catch (e) { status('Microphone permission failed: '+e.message); stopAudio(); }
  }
  function endTalk() { transmitting=false; stopAudio(); $('et-talk').textContent='Hold to Talk'; if (ws && ws.readyState===WebSocket.OPEN) status('Connected · microphone muted'); }
  $('et-talk').addEventListener('pointerdown', e => { e.preventDefault(); beginTalk(); });
  ['pointerup','pointerleave','pointercancel'].forEach(ev => $('et-talk').addEventListener(ev, endTalk));
  function playPcm(buffer) {
    if (!ctx || ctx.state==='closed') { ctx = new AudioContext(); }
    const pcm = new Int16Array(buffer.slice(0)); const audio = ctx.createBuffer(1, pcm.length, 22050); const data=audio.getChannelData(0);
    for(let i=0;i<pcm.length;i++) data[i]=pcm[i]/32768;
    const node=ctx.createBufferSource(); node.buffer=audio; node.connect(ctx.destination); node.start();
  }
  window.addEventListener('pagehide', disconnect);
})();
