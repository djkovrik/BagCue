const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const root = path.resolve(__dirname,'../..');
const source = path.join(root,'shared/compose/visual-test/src/test/snapshots/images');
const output = path.join(__dirname,'brand-review');
fs.mkdirSync(output,{recursive:true});
async function main() {
  const files=fs.readdirSync(source).filter(f=>f.endsWith('.png')).sort();
  const inventory=[];
  for(let screen=1;screen<=11;screen++) {
    const id=String(screen).padStart(3,'0');
    const group=files.filter(f=>f.includes(`screen_${id}_`));
    const layers=[];
    for(let i=0;i<group.length;i++) {
      layers.push({input:await sharp(path.join(source,group[i])).resize(280,780,{fit:'inside'}).toBuffer(),left:(i%4)*300,top:Math.floor(i/4)*800});
    }
    await sharp({create:{width:1200,height:Math.ceil(group.length/4)*800,channels:3,background:'#909090'}}).composite(layers).png().toFile(path.join(output,`screen-${id}.png`));
    inventory.push({screen:`SCREEN-${id}`,snapshots:group});
  }
  fs.writeFileSync(path.join(output,'inventory.json'),JSON.stringify({count:files.length,screens:inventory},null,2)+'\n');
  const layers=[];
  for(const [i,bg] of ['#FFFBF7','#13171F'].entries()) {
    const svg=fs.readFileSync(path.join(__dirname,'sources/bagcue_mark_v3.svg'),'utf8');
    for(const [j,size] of [24,32,48,96].entries()) {
      layers.push({input:await sharp({create:{width:140,height:140,channels:3,background:bg}}).png().toBuffer(),left:j*150,top:i*150});
      layers.push({input:await sharp(Buffer.from(svg)).resize(size,size).png().toBuffer(),left:j*150+Math.floor((140-size)/2),top:i*150+Math.floor((140-size)/2)});
    }
  }
  await sharp({create:{width:600,height:300,channels:3,background:'#909090'}}).composite(layers).png().toFile(path.join(output,'brand-sizes.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
