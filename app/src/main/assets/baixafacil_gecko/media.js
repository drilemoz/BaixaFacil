/* Executa somente no documento principal. Sem envio de cookies, formulários ou tokens. */
(() => {
  const port = browser.runtime.connectNative('baixafacil');
  // Inspeciona apenas fontes do DOM principal: não realiza fetch nem varre recursos de outros apps.
  function normalize(value) {
    if (typeof value !== 'string' || !value || value.length > 4096) return '';
    try {
      const url = new URL(value, document.baseURI);
      if (!['http:', 'https:'].includes(url.protocol) || !url.hostname ||
          url.username || url.password || url.hash || url.href.length > 4096) return '';
      return url.href;
    } catch (_) { return ''; }
  }
  function srcSetCandidates(srcset) {
    if (!srcset || srcset.length > 4096) return [];
    // srcset de URLs HTTP(S); data: e blob: não são candidatos à transferência.
    return srcset.split(',').slice(0, 8).map(part => part.trim().split(/\s+/)[0]).filter(Boolean);
  }
  function scan(includeImages) {
    const data = { media: [], images: [], players: 0, unsupported: 0 };
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
      if (includeImages) {
        // Somente varredura manual: fontes responsivas/lazy e pôsteres já presentes no DOM.
        const imageSet = new Set();
        function addImage(value) {
          const safe = normalize(value);
          if (safe && !imageSet.has(safe) && imageSet.size < 60) imageSet.add(safe);
        }
        for (const img of [...document.querySelectorAll('img')].slice(0, 120)) {
          addImage(img.currentSrc || img.src);
          addImage(img.getAttribute('data-src'));
          addImage(img.getAttribute('data-original'));
          for (const candidate of srcSetCandidates(img.getAttribute('srcset'))) addImage(candidate);
        }
        for (const source of [...document.querySelectorAll('picture source[srcset]')].slice(0, 40)) {
          for (const candidate of srcSetCandidates(source.getAttribute('srcset'))) addImage(candidate);
        }
        for (const video of videos) addImage(video.poster || video.getAttribute('poster'));
        // CSS inline apenas; não força getComputedStyle em todo o documento.
        for (const element of [...document.querySelectorAll('[style*="background-image"]')].slice(0, 40)) {
          const style = element.style && element.style.backgroundImage || '';
          for (const match of style.matchAll(/url\(\s*(['"]?)(.*?)\1\s*\)/gi)) addImage(match[2]);
        }
        for (const link of [...document.querySelectorAll('a[href]')].slice(0, 100)) {
          const href = normalize(link.href);
          if (href && /\.(png|jpe?g|webp|gif|avif)(\?|$)/i.test(href)) addImage(href);
        }
        data.images = [...imageSet];
      }
      for (const link of [...document.querySelectorAll('a[href]')].slice(0, 100))
        if (/\.(mp4|m4v|webm|mov|ogv|3gp|m3u8|mpd)(\?|#|$)/i.test(link.href))
          add(link.href, 'link', '');
    } catch (_) {}
    return data;
  }
  port.onMessage.addListener(msg => {
    if (!msg || typeof msg.type !== 'string') return;
    if (msg.type === 'scan') port.postMessage({ type:'scan', data:scan(Boolean(msg.includeImages)) });
    if (msg.type === 'cosmetic') {
      if (document.getElementById('bf-ad-css')) return;
      const css = document.createElement('style');
      css.id = 'bf-ad-css';
      css.textContent = "ins.adsbygoogle,[id^='div-gpt-ad'],[id^='google_ads_'],[data-ad-slot],.advertisement,.advertising-banner,.sponsored-banner,.ad-container[data-ad],.ad-banner,.adsbox[data-ad]{display:none!important;visibility:hidden!important}";
      (document.head || document.documentElement).appendChild(css);
    }
  });
})();
