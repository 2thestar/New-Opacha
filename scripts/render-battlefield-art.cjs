/** Export the approved Canvas material, without approximating its gradients in Java.
 * Requires the existing Playwright CLI and Chrome. No network access is used.
 */
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),http=require('node:http'),{spawn}=require('node:child_process');
const root=path.resolve(__dirname,'..'),ref='.impeccable/mocks/battlefield/2026-10-21/fine-ink-troops.html';
const source=fs.readFileSync(path.join(root,ref),'utf8');
const original=source.slice(source.indexOf('  function towerShape('),source.indexOf('  function text('));
if(!original.includes('r*.943')||!original.includes('r*.067'))throw Error('Approved material source changed');
const material=original.replace('function material(c,r,blue,tower=false,neutral=false)','function material(c,r,p,tower=false)')
 .replace(/const colors=neutral\?[^;]+;/,'const colors=p.rim;')
 .replace(/c.strokeStyle=neutral\?[^;]+;/,'c.strokeStyle=p.stroke;')
 .replace(/const faceColors=neutral\?[^;]+;/,'const faceColors=p.face;');
if(/neutral\?|blue\?/.test(material))throw Error('Palette substitution incomplete');
const palette=[
 {rim:['#faf9f6','#eeeae5','#b9b5b0'],face:['#fffefd','#f7f4ef','#e9e4dd'],stroke:'rgba(94,85,77,.19)'},
 {rim:['#d4fbb5','#abe981','#71b14e'],face:['#d6fcb9','#baf196','#a7e581'],stroke:'rgba(78,153,49,.40)'},
 {rim:['#ffd1d8','#f89baa','#c8697c'],face:['#ffdee3','#ffa8b3','#ed8c9d'],stroke:'rgba(163,74,94,.40)'},
 {rim:['#fff0b4','#f6d47c','#c3a14c'],face:['#fff4c8','#ffdc83','#edc36a'],stroke:'rgba(158,119,47,.40)'},
 {rim:['#9cf4ff','#55d7eb','#259db8'],face:['#a3f4ff','#77e5f2','#51d2e5'],stroke:'rgba(17,135,160,.48)'}
];
const accessible=[palette[0],palette[1],palette[2],
 {rim:['#e9c9ff','#b58add','#8260aa'],face:['#eed7ff','#cba4ed','#ae83d2'],stroke:'rgba(115,77,151,.40)'},palette[4]];
const script=`
const tau=Math.PI*2,CELL=256,R=96;
function circle(c,x,y,r){c.beginPath();c.arc(x,y,r,0,tau)}
function fill(c,color){c.fillStyle=color;c.fill()}
${material}
const palettes=${JSON.stringify([palette,accessible])};
const canvas=document.createElement('canvas');canvas.width=CELL*5;canvas.height=CELL*4;
canvas.id='nodes';document.body.append(canvas);const c=canvas.getContext('2d');
for(let mode=0;mode<2;mode++)for(let tower=0;tower<2;tower++)for(let faction=0;faction<5;faction++){
 c.save();c.translate((faction+.5)*CELL,(mode*2+tower+.5)*CELL);material(c,R,palettes[mode][faction],!!tower);c.restore();
}
// A neutral radial alpha mask supplies the soft vapor in the approved impact.
const vapor=document.createElement('canvas');vapor.id='vapor';vapor.width=vapor.height=128;
const v=vapor.getContext('2d'),g=v.createRadialGradient(64,64,0,64,64,63);
g.addColorStop(0,'#fff');g.addColorStop(.4,'rgba(255,255,255,.88)');g.addColorStop(1,'rgba(255,255,255,0)');
v.fillStyle=g;v.fillRect(0,0,128,128);document.body.append(vapor);
// White rounded-box primitive, used with NinePatch rather than oval vehicle substitutes.
const rounded=document.createElement('canvas');rounded.id='rounded';rounded.width=rounded.height=64;
const q=rounded.getContext('2d');q.beginPath();q.roundRect(0,0,64,64,16);q.fillStyle='#fff';q.fill();document.body.append(rounded);
const hull=document.createElement('canvas');hull.id='rig-hull';hull.width=256;hull.height=128;
const h=hull.getContext('2d'),hg=h.createLinearGradient(0,0,0,128);hg.addColorStop(0,'#f6e8ba');hg.addColorStop(1,'#dec88f');
h.beginPath();h.roundRect(0,0,256,128,48);h.fillStyle=hg;h.fill();document.body.append(hull);
window.exportArt=(id)=>{const a=document.createElement('a');a.download=id+'.png';a.href=document.getElementById(id).toDataURL();a.click()};
document.documentElement.dataset.ready='true';`;
const qa=path.join(root,'.tools/qa');fs.mkdirSync(qa,{recursive:true});
const html=path.join(qa,'battlefield-art-export.html');fs.writeFileSync(html,'<!doctype html><meta charset="utf-8"><body style="margin:0"><script>'+script+'</script>');
const out=path.join(root,'assets/game/battlefield');fs.mkdirSync(out,{recursive:true});
const runFile=path.join(qa,'export-battlefield-art.js');fs.writeFileSync(runFile,`async(page)=>{
 await page.locator('html[data-ready="true"]').waitFor();
 for(const id of ['nodes','vapor','rounded','rig-hull']){
  const wait=page.waitForEvent('download');await page.evaluate(id=>window.exportArt(id),id);
  await(await wait).saveAs(${JSON.stringify(out.replaceAll('\\','/'))}+'/'+id+'.png');
 }
 return {nodes:'1280x1024',source:'approved Canvas material',variants:20};
}`);
const cli=process.env.PLAYWRIGHT_CLI||'C:/Users/boris/AppData/Roaming/npm/node_modules/@playwright/cli/playwright-cli.js';
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');
function pixelDataSha(file){const data=fs.readFileSync(file),chunks=[];for(let at=8;at<data.length;){const n=data.readUInt32BE(at);if(data.toString('ascii',at+4,at+8)==='IDAT')chunks.push(data.subarray(at+8,at+8+n));at+=n+12;}return sha(Buffer.concat(chunks));}
function run(args){return new Promise((resolve,reject)=>{const child=spawn(process.execPath,[cli,'-s=opacha-candy-assets',...args],{cwd:root,windowsHide:true});let log='';child.stdout.on('data',s=>log+=s);child.stderr.on('data',s=>log+=s);child.on('error',reject);child.on('exit',code=>code||/### Error/.test(log)?reject(Error(log)):resolve(log));});}
const server=http.createServer((req,res)=>{res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});res.end(fs.readFileSync(html));});
(async()=>{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(8793,'127.0.0.1',resolve);});
 try{await run(['open','http://127.0.0.1:8793/','--browser','chrome']);console.log((await run(['run-code','--filename',runFile])).slice(0,600));}
 finally{await run(['close']).catch(()=>{});server.close();}
 fs.writeFileSync(path.join(out,'provenance.json'),JSON.stringify({origin:'Authored Canvas geometry exported from the user-approved executable reference; no AI bitmap generation',reference:ref,referenceSha256:sha(source),materialSourceSha256:sha(original),cell:256,radius:96,columns:['neutral','green','red','yellow','cyan'],rows:['plain','tower','colorblind-plain','colorblind-tower'],files:['nodes.png','vapor.png','rounded.png','rig-hull.png'].map(file=>({file,pngPixelDataSha256:pixelDataSha(path.join(out,file))}))},null,2)+'\n');
})().catch(error=>{console.error(error.message);process.exitCode=1;server.close();});
