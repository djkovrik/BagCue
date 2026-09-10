/* Approved navy/apricot concept translated into shared scalable geometry.
 * One path inventory emits both Compose vector XML and editable SVG.
 */
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const gradients = {
  body: ['#304767', '#263653', 6, 10, 42, 43],
  pocket: ['#F7CCA3', '#EDB582', 16, 26, 33, 44],
  handle: ['#92ADD0', '#7895BC', 18, 2, 29, 10],
  card: ['#FFFCF5', '#F5EEDF', 17, 14, 30, 26],
};
const paths = [
  ['#263653', 'M8,22C5.8,20 5.4,11 7.8,8.5C9,7.2 11.6,7.1 12.8,8.4L15,12L12,24Z'],
  ['#263653', 'M40,22C42.2,20 42.6,11 40.2,8.5C39,7.2 36.4,7.1 35.2,8.4L33,12L36,24Z'],
  ['#819DC5', 'M8.4,18.8C7.8,15.5 8,11.6 9.2,9.8C10,9.5 11.5,12.4 12,14L10,20Z'],
  ['#819DC5', 'M39.6,18.8C40.2,15.5 40,11.6 38.8,9.8C38,9.5 36.5,12.4 36,14L38,20Z'],
  ['handle', 'M17.3,10V8.2C17.3,4 19.8,2 24,2C28.2,2 30.7,4 30.7,8.2V10H28.1V8.2C28.1,5.8 26.6,4.6 24,4.6C21.4,4.6 19.9,5.8 19.9,8.2V10Z'],
  ['body', 'M24,8.2C15.1,8.2 9.6,11.2 8,18.8C6.3,26.3 4.9,33.6 5,37.8C5.1,42.6 8.6,45.5 14,45.5H34C39.4,45.5 42.9,42.6 43,37.8C43.1,33.6 41.7,26.3 40,18.8C38.4,11.2 32.9,8.2 24,8.2Z'],
  ['#243550', 'M14.2,17.3C14.2,15.6 15.5,14.5 17.2,14.5H30.8C32.5,14.5 33.8,15.6 33.8,17.3V28H14.2Z'],
  ['card', 'M14.5,16.3C14.5,14.8 15.6,13.7 17.1,13.7H30.9C32.4,13.7 33.5,14.8 33.5,16.3V27H14.5Z'],
  ['#819DC5', 'M18,16.3H19.5Q20.1,16.3 20.1,16.9V18.4Q20.1,19 19.5,19H18Q17.4,19 17.4,18.4V16.9Q17.4,16.3 18,16.3ZM18,20.1H19.5Q20.1,20.1 20.1,20.7V22.2Q20.1,22.8 19.5,22.8H18Q17.4,22.8 17.4,22.2V20.7Q17.4,20.1 18,20.1Z'],
  ['#263653', 'M22.1,16.9H30.5A0.75,0.75 0,0 1,30.5 18.4H22.1A0.75,0.75 0,0 1,22.1 16.9ZM22.1,20.7H30.5A0.75,0.75 0,0 1,30.5 22.2H22.1A0.75,0.75 0,0 1,22.1 20.7Z'],
  ['#22324C', 'M15,24.5H33C36.9,24.5 38.7,26.7 38.7,30L39.1,39.9C39.3,43.3 37.4,45.5 34,45.5H14C10.6,45.5 8.7,43.3 8.9,39.9L9.3,30C9.3,26.7 11.1,24.5 15,24.5Z'],
  ['pocket', 'M15,24H33C36.6,24 38.2,26.1 38.3,29.3L38.7,39.8C38.8,43.2 37.1,45.5 33.7,45.5H14.3C10.9,45.5 9.2,43.2 9.3,39.8L9.7,29.3C9.8,26.1 11.4,24 15,24Z'],
  ['#263653', 'M16.1,33.5C15.4,32.8 15.4,31.7 16.1,31C16.8,30.3 17.9,30.3 18.6,31L21.8,34.2L29.5,26.9C30.2,26.2 31.3,26.3 32,27C32.7,27.7 32.6,28.8 31.9,29.5L23,38C22.3,38.7 21.2,38.7 20.5,38Z'],
];
const svgDefs = Object.entries(gradients).map(([id,[a,b,x1,y1,x2,y2]]) => `<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}"><stop stop-color="${a}"/><stop offset="1" stop-color="${b}"/></linearGradient>`).join('');
const svgPaths = paths.map(([fill,d]) => `<path fill="${gradients[fill] ? `url(#${fill})` : fill}" d="${d}"/>`).join('\n');
fs.writeFileSync(path.join(__dirname, 'sources/bagcue_mark_v3.svg'), `<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 48 48"><defs>${svgDefs}</defs>${svgPaths}</svg>\n`);
const xmlPaths = paths.map(([fill,d]) => {
  if (!gradients[fill]) return `    <path android:fillColor="${fill}" android:pathData="${d}" />`;
  const [a,b,x1,y1,x2,y2] = gradients[fill];
  return `    <path android:pathData="${d}"><aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="${x1}" android:startY="${y1}" android:endX="${x2}" android:endY="${y2}" android:startColor="${a}" android:endColor="${b}" /></aapt:attr></path>`;
}).join('\n');
fs.writeFileSync(path.join(root, 'shared/compose/src/commonMain/composeResources/drawable/bagcue_mark.xml'), `<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt" android:width="48dp" android:height="48dp" android:viewportWidth="48" android:viewportHeight="48">\n${xmlPaths}\n</vector>\n`);
// A dedicated one-color contour with a transparent check cutout. Filling every
// colored path black would lose the check completely inside the silhouette.
const monoBody = paths[5][1];
const check = paths[12][1];
fs.writeFileSync(path.join(__dirname, 'sources/bagcue_mark_v3_monochrome.svg'), `<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 48 48"><path fill="#000" d="${paths[4][1]}"/><path fill="#000" fill-rule="evenodd" d="${monoBody}${check}"/></svg>\n`);
