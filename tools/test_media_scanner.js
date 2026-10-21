#!/usr/bin/env node
// Testa a extensão empacotada de verdade com DOM fictício. Não faz requisições de rede.
const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const script=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/baixafacil_gecko/media.js'),'utf8');
let listener=null, sent=[];
const attrs=(obj)=>({
  ...obj,
  getAttribute(name){return (obj.attrs||{})[name]??null;},
  querySelectorAll(name){return name==='source'?obj.sources||[]:[];}
});
const images=[
  attrs({currentSrc:'https://static.test/pic.jpg',src:'https://static.test/pic.jpg',attrs:{srcset:'https://static.test/pic.jpg 1x, /wide.webp 2x','data-src':'/lazy.png'}}),
  attrs({currentSrc:'blob:fake',src:'blob:fake',attrs:{'data-original':'/original.gif'}}),
  attrs({currentSrc:'',src:'',attrs:{srcset:'https://user:pass@evil.test/secret.jpg 1x, data:image/png;base64,abc 2x'}})
];
const videos=[attrs({currentSrc:'https://static.test/video.mp4',src:'',poster:'/poster.webp',sources:[attrs({src:'blob:not-direct',type:'video/mp4'})]})];
const items={
 'img':images,'video':videos,
 'picture source[srcset]':[attrs({attrs:{srcset:'/responsive.avif 1x, /responsive-2.jpg 2x'}})],
 '[style*="background-image"]':[{style:{backgroundImage:'url("/hero.jpg")'}}],
 'a[href]':[{href:'https://static.test/gallery.webp'},{href:'https://static.test/clip.mp4'},
            {href:'https://user:password@evil.test/x.png'}]
};
const doc={baseURI:'https://static.test/page',querySelectorAll(sel){return items[sel]||[];}};
const sandbox={document:doc,URL,Set,browser:{runtime:{connectNative(){return {onMessage:{addListener(fn){listener=fn;}},postMessage(v){sent.push(v);}};}}}};
vm.runInNewContext(script,sandbox,{timeout:1000});
assert.equal(typeof listener,'function');
listener({type:'scan',includeImages:false});
assert.equal(sent.length,1);
assert.equal(sent[0].data.images.length,0,'sem varredura de imagens automática');
assert.equal(sent[0].data.players,1);
assert.equal(sent[0].data.media.length,2,'vídeo e link direto deduplicados');
listener({type:'scan',includeImages:true});
const urls=[...sent[1].data.images].sort();
for(const expect of ['/lazy.png','/wide.webp','/poster.webp','/responsive.avif','/hero.jpg','/original.gif','/gallery.webp'])
  assert(urls.some(x=>x==='https://static.test'+expect),'imagem ausente '+expect);
assert.equal(urls.filter(x=>x==='https://static.test/pic.jpg').length,1,'sem duplicatas');
assert(!urls.some(x=>x.includes('evil.test')||x.startsWith('blob:')||x.startsWith('data:')),'esquemas ou credenciais proibidos');
// Cap firme de 60 imagens, nenhuma requisição mesmo com muitas imagens na página.
items.img=Array.from({length:150},(_,i)=>attrs({currentSrc:`https://static.test/image${i}.jpg`,src:''}));
listener({type:'scan',includeImages:true});
assert.equal(sent[2].data.images.length,60);
console.log('PASSOU: scanner JS manual/responsivo/lazy, credenciais bloqueadas, sem fetch, limites e scan automático leve');
