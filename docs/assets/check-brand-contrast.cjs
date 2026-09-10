const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const source = fs.readFileSync(path.join(root, 'shared/compose/src/commonMain/kotlin/com/sedsoftware/bagcue/compose/theme/Color.kt'), 'utf8');
const colors = Object.fromEntries([...source.matchAll(/internal val (\w+) = Color\(0xFF([0-9A-F]{6})\)/g)].map(m => [m[1], m[2]]));
const luminance = hex => hex.match(/../g).map(v => parseInt(v,16)/255).map(v => v <= .04045 ? v/12.92 : ((v+.055)/1.055)**2.4).reduce((sum,v,i) => sum+v*[.2126,.7152,.0722][i],0);
const ratio = (a,b) => { const x=luminance(a), y=luminance(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); };
const checks = [];
for (const theme of ['Light','Dark']) {
  for (const role of ['Primary','PrimaryContainer','Secondary','SecondaryContainer','Tertiary','TertiaryContainer','Error','ErrorContainer','Background','Surface','SurfaceVariant']) {
    checks.push({pair:`On${role}/${role} ${theme}`, ratio:ratio(colors[`On${role}${theme}`],colors[`${role}${theme}`]),minimum:4.5});
  }
  for (const surface of ['SurfaceContainerLowest','SurfaceContainerLow','SurfaceContainer','SurfaceContainerHigh','SurfaceContainerHighest','SurfaceDim','SurfaceBright']) {
    checks.push({pair:`OnSurface/${surface} ${theme}`,ratio:ratio(colors[`OnSurface${theme}`],colors[`${surface}${theme}`]),minimum:4.5});
  }
  checks.push({pair:`Outline/Surface ${theme}`,ratio:ratio(colors[`Outline${theme}`],colors[`Surface${theme}`]),minimum:3});
}
checks.push({pair:'Logo pocket/body (least contrasting gradient endpoints)',ratio:ratio('EDB582','304767'),minimum:3});
checks.push({pair:'Logo check/pocket',ratio:ratio('263653','EDB582'),minimum:3});
const report={valid:checks.every(c=>c.ratio>=c.minimum),checks:checks.map(c=>({...c,ratio:Number(c.ratio.toFixed(2))}))};
fs.writeFileSync(path.join(__dirname,'brand-contrast.json'), JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
if(!report.valid) process.exitCode=1;
