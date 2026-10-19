/* Executa somente no documento principal. Sem envio de cookies, formulários ou tokens. */
(() => {
  const port = browser.runtime.connectNative('baixafacil');
  const supported = /^https?:\/\//i;
  const normalize = value => typeof value === 'string' && supported.test(value) && value.length <= 4096 ? value : '';
  function scan() {
    const data = { media: [], players: 0, unsupported: 0 };
    try {
      const videos = [...document.querySelectorAll('video')].slice(0, 25);
      data.players = videos.length;
      function add(url, kind, mime) {
        if (!url) return;
        if (/^(blob:|data:|mediasource:)/i.test(url)) { data.unsupported++; return; }
        const safe = normalize(url);
        if (safe && data.media.length < 40 && !data.media.some(x => x.url === safe))
          data.media.push({ url: safe, kind, mime: mime || '' });
      }
      for (const video of videos) {
        add(video.currentSrc || video.src, 'video', video.getAttribute('type'));
        for (const source of [...video.querySelectorAll('source')].slice(0, 5))
          add(source.src, 'source', source.type);
      }
      for (const link of [...document.querySelectorAll('a[href]')].slice(0, 100))
        if (/\.(mp4|m4v|webm|mov|ogv|3gp|m3u8|mpd)(\?|#|$)/i.test(link.href))
          add(link.href, 'link', '');
    } catch (_) {}
    return data;
  }
  port.onMessage.addListener(msg => {
    if (!msg || typeof msg.type !== 'string') return;
    if (msg.type === 'scan') port.postMessage({ type:'scan', data:scan() });
    if (msg.type === 'cosmetic') {
      if (document.getElementById('bf-ad-css')) return;
      const css = document.createElement('style');
      css.id = 'bf-ad-css';
      css.textContent = "ins.adsbygoogle,[id^='div-gpt-ad'],[id^='google_ads_'],[data-ad-slot],.advertisement,.advertising-banner,.sponsored-banner,.ad-container[data-ad],.ad-banner,.adsbox[data-ad]{display:none!important;visibility:hidden!important}";
      (document.head || document.documentElement).appendChild(css);
    }
  });
})();
