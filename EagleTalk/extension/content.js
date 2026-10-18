(() => {
  // Activation only: no voice UI, microphone access, networking, or audio handling.
  if (window.__EAGLETALK_ACTIVATOR_INSTALLED__) return;
  Object.defineProperty(window, '__EAGLETALK_ACTIVATOR_INSTALLED__', {
    value: true,
    configurable: false
  });
  window.dispatchEvent(new CustomEvent('eagletalk:activate', {
    detail: { version: '1.0.0', source: 'chrome-extension' }
  }));
  document.documentElement?.setAttribute('data-eagletalk-activator', 'ready');
})();
