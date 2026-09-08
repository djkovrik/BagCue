/* Deterministic BagCue launcher derivative and review-sheet generator.
 * Runtime vectors remain the source of truth under shared/compose resources.
 */
const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');
const requireFromRuntime = createRequire(__filename);
let sharp;
try {
  sharp = requireFromRuntime('sharp');
} catch (error) {
  throw new Error('Sharp is required. Install it or expose the bundled package directory through NODE_PATH.', { cause: error });
}

const root = path.resolve(__dirname, '../..');
const blue = '#2563EB';
const teal = '#0E7490';
const light = '#ECFEFF';
const dark = '#082F49';

const iconPaths = {
  today: 'M12,3.2L3,10.4V20C3,21.1 3.9,22 5,22H10V16H14V22H19C20.1,22 21,21.1 21,20V10.4L12,3.2ZM19,20H16V14H8V20H5V11.4L12,5.8L19,11.4V20Z',
  sessions: 'M19,4H18V2H16V4H8V2H6V4H5C3.9,4 3,4.9 3,6V20C3,21.1 3.9,22 5,22H19C20.1,22 21,21.1 21,20V6C21,4.9 20.1,4 19,4ZM19,20H5V9H19V20ZM19,7H5V6H19V7ZM7,11H12V16H7V11Z',
  templates: 'M19,3H5C3.9,3 3,3.9 3,5V19C3,20.1 3.9,21 5,21H19C20.1,21 21,20.1 21,19V5C21,3.9 20.1,3 19,3ZM19,19H5V5H19V19ZM10.7,15.7L17.4,9L16,7.6L10.7,12.9L8,10.2L6.6,11.6L10.7,15.7Z',
  settings: 'M19.14,12.94C19.18,12.64 19.2,12.32 19.2,12C19.2,11.68 19.18,11.36 19.13,11.06L21.16,9.48L19.16,6.02L16.77,6.98C16.27,6.59 15.72,6.27 15.12,6.04L14.76,3.5H10.76L10.4,6.04C9.81,6.28 9.26,6.6 8.76,6.98L6.37,6.02L4.37,9.48L6.4,11.06C6.35,11.36 6.32,11.68 6.32,12C6.32,12.32 6.35,12.64 6.4,12.94L4.37,14.52L6.37,17.98L8.76,17.02C9.26,17.41 9.81,17.73 10.4,17.96L10.76,20.5H14.76L15.12,17.96C15.72,17.72 16.27,17.4 16.77,17.02L19.16,17.98L21.16,14.52L19.14,12.94ZM12.76,15.5C10.83,15.5 9.26,13.93 9.26,12C9.26,10.07 10.83,8.5 12.76,8.5C14.69,8.5 16.26,10.07 16.26,12C16.26,13.93 14.69,15.5 12.76,15.5Z',
  add: 'M19,13H13V19C13,19.55 12.55,20 12,20C11.45,20 11,19.55 11,19V13H5C4.45,13 4,12.55 4,12C4,11.45 4.45,11 5,11H11V5C11,4.45 11.45,4 12,4C12.55,4 13,4.45 13,5V11H19C19.55,11 20,11.45 20,12C20,12.55 19.55,13 19,13Z',
  check: 'M9,16.17L5.53,12.7C5.14,12.31 4.51,12.31 4.12,12.7C3.73,13.09 3.73,13.72 4.12,14.11L8.3,18.29C8.69,18.68 9.32,18.68 9.71,18.29L20.29,7.71C20.68,7.32 20.68,6.69 20.29,6.3C19.9,5.91 19.27,5.91 18.88,6.3L9,16.17Z',
  calendar: 'M19,4H18V3C18,2.45 17.55,2 17,2C16.45,2 16,2.45 16,3V4H8V3C8,2.45 7.55,2 7,2C6.45,2 6,2.45 6,3V4H5C3.9,4 3,4.9 3,6V20C3,21.1 3.9,22 5,22H19C20.1,22 21,21.1 21,20V6C21,4.9 20.1,4 19,4ZM19,20H5V10H19V20ZM5,8V6H19V8H5Z',
  edit: 'M3,17.25V20C3,20.55 3.45,21 4,21H6.75C7.01,21 7.27,20.9 7.45,20.71L17.33,10.83L13.17,6.67L3.29,16.54C3.1,16.73 3,16.98 3,17.25ZM20.54,7.62C20.93,7.23 20.93,6.6 20.54,6.21L17.79,3.46C17.4,3.07 16.77,3.07 16.38,3.46L14.23,5.61L18.39,9.77L20.54,7.62Z',
  delete: 'M6,19C6,20.1 6.9,21 8,21H16C17.1,21 18,20.1 18,19V7H6V19ZM9,9H15C15.55,9 16,9.45 16,10V18C16,18.55 15.55,19 15,19H9C8.45,19 8,18.55 8,18V10C8,9.45 8.45,9 9,9ZM15.5,4L14.79,3.29C14.61,3.11 14.35,3 14.09,3H9.91C9.65,3 9.39,3.11 9.21,3.29L8.5,4H6C5.45,4 5,4.45 5,5C5,5.55 5.45,6 6,6H18C18.55,6 19,5.55 19,5C19,4.45 18.55,4 18,4H15.5Z',
  more: 'M12,8C13.1,8 14,7.1 14,6C14,4.9 13.1,4 12,4C10.9,4 10,4.9 10,6C10,7.1 10.9,8 12,8ZM12,10C10.9,10 10,10.9 10,12C10,13.1 10.9,14 12,14C13.1,14 14,13.1 14,12C14,10.9 13.1,10 12,10ZM12,16C10.9,16 10,16.9 10,18C10,19.1 10.9,20 12,20C13.1,20 14,19.1 14,18C14,16.9 13.1,16 12,16Z',
  search: 'M9.5,3C5.91,3 3,5.91 3,9.5C3,13.09 5.91,16 9.5,16C11.11,16 12.59,15.41 13.73,14.44L18.29,19C18.68,19.39 19.31,19.39 19.7,19C20.09,18.61 20.09,17.98 19.7,17.59L15.14,13.03C15.67,12.02 16,10.82 16,9.5C16,5.91 13.09,3 9.5,3ZM9.5,5C11.99,5 14,7.01 14,9.5C14,11.99 11.99,14 9.5,14C7.01,14 5,11.99 5,9.5C5,7.01 7.01,5 9.5,5Z',
  duplicate: 'M16,1H4C2.9,1 2,1.9 2,3V15C2,15.55 2.45,16 3,16C3.55,16 4,15.55 4,15V4C4,3.45 4.45,3 5,3H16C16.55,3 17,2.55 17,2C17,1.45 16.55,1 16,1ZM20,5H8C6.9,5 6,5.9 6,7V19C6,20.1 6.9,21 8,21H20C21.1,21 22,20.1 22,19V7C22,5.9 21.1,5 20,5ZM20,19H8V7H20V19Z',
  notifications: 'M18,16V11C18,7.93 16.36,5.36 13.5,4.68V4C13.5,3.17 12.83,2.5 12,2.5C11.17,2.5 10.5,3.17 10.5,4V4.68C7.63,5.36 6,7.92 6,11V16L4.7,17.29C4.07,17.92 4.52,19 5.41,19H18.59C19.48,19 19.93,17.92 19.3,17.29L18,16ZM16,17H8V11C8,8.52 9.51,6.5 12,6.5C14.49,6.5 16,8.52 16,11V17ZM12,22C13.1,22 14,21.1 14,20H10C10,21.1 10.9,22 12,22Z',
  privacy: 'M12,2L4,5V11C4,16.05 7.41,20.74 12,22C16.59,20.74 20,16.05 20,11V5L12,2ZM12,4.13L18,6.38V11C18,14.91 15.48,18.61 12,19.93C8.52,18.61 6,14.91 6,11V6.38L12,4.13ZM11,7H13V13H11V7ZM11,15H13V17H11V15Z',
  arrow_back: 'M20,11H7.83L13.42,5.41L12,4L4,12L12,20L13.41,18.59L7.83,13H20C20.55,13 21,12.55 21,12C21,11.45 20.55,11 20,11Z',
  bag: 'M20,6H16V4C16,2.89 15.11,2 14,2H10C8.89,2 8,2.89 8,4V6H4C2.89,6 2,6.89 2,8V19C2,20.11 2.89,21 4,21H20C21.11,21 22,20.11 22,19V8C22,6.89 21.11,6 20,6ZM10,4H14V6H10V4ZM20,19H4V14H9V15C9,15.55 9.45,16 10,16H14C14.55,16 15,15.55 15,15V14H20V19ZM11,14V12H13V14H11ZM20,12H15V11C15,10.45 14.55,10 14,10H10C9.45,10 9,10.45 9,11V12H4V8H20V12Z'
};

const markSvg = (size, background = null, monochrome = false) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 48 48">${background ? `<rect width="48" height="48" rx="10" fill="${background}"/>` : ''}<g transform="translate(4.8 4.8) scale(.8)"><path fill="${monochrome ? '#000' : teal}" d="M10 15C10 11.69 12.69 9 16 9H32C35.31 9 38 11.69 38 15V19H40C42.21 19 44 20.79 44 23V37C44 40.31 41.31 43 38 43H10C6.69 43 4 40.31 4 37V23C4 20.79 5.79 19 8 19H10V15ZM14 19H34V15C34 13.9 33.1 13 32 13H16C14.9 13 14 13.9 14 15V19ZM8 23V37C8 38.1 8.9 39 10 39H38C39.1 39 40 38.1 40 37V23H8Z"/><path fill="${monochrome ? '#000' : blue}" d="M18.2 27.2L22.3 31.3L31.8 21.8C32.58 21.02 33.84 21.02 34.62 21.8C35.4 22.58 35.4 23.84 34.62 24.62L23.72 35.52C22.94 36.3 21.68 36.3 20.9 35.52L15.38 30C14.6 29.22 14.6 27.96 15.38 27.18C16.16 26.4 17.42 26.4 18.2 27.2Z"/></g></svg>`);

const opaqueMarkSvg = (size, background, monochrome = false) =>
  Buffer.from(markSvg(size, background, monochrome).toString().replace(' rx="10"', ''));

async function png(svg, output, size) {
  await sharp(svg).resize(size, size).png({ compressionLevel: 9, adaptiveFiltering: true }).toFile(output);
}

async function main() {
  const android = path.join(root, 'androidApp/src/main/res');
  const density = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
  for (const [name, size] of Object.entries(density)) {
    const dir = path.join(android, `mipmap-${name}`);
    await png(opaqueMarkSvg(size, light), path.join(dir, 'ic_launcher.png'), size);
    await png(opaqueMarkSvg(size, light), path.join(dir, 'ic_launcher_background.png'), size);
    await png(markSvg(size), path.join(dir, 'ic_launcher_foreground.png'), size);
    await png(markSvg(size, null, true), path.join(dir, 'ic_launcher_monochrome.png'), size);
  }

  const iosDir = path.join(root, 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset');
  const contentsText = fs.readFileSync(path.join(iosDir, 'Contents.json'), 'utf8').replace(/,\s*([}\]])/g, '$1');
  const contents = JSON.parse(contentsText);
  for (const item of contents.images) {
    if (!item.filename) continue;
    const points = Number(item.size.split('x')[0]);
    const scale = Number(item.scale.replace('x', ''));
    const size = Math.round(points * scale);
    await png(opaqueMarkSvg(size, light), path.join(iosDir, item.filename), size);
  }

  const names = Object.keys(iconPaths);
  const cells = names.map((name, index) => {
    const x = 28 + (index % 4) * 164;
    const y = 110 + Math.floor(index / 4) * 94;
    return `<g transform="translate(${x} ${y})"><rect width="136" height="70" rx="12" fill="#fff"/><path transform="translate(12 11) scale(2)" fill="#0F172A" d="${iconPaths[name]}"/><text x="64" y="64" text-anchor="middle" font-family="Arial" font-size="11" fill="#334155">${name}</text></g>`;
  }).join('');
  const darkCells = names.map((name, index) => {
    const x = 708 + (index % 4) * 164;
    const y = 110 + Math.floor(index / 4) * 94;
    return `<g transform="translate(${x} ${y})"><rect width="136" height="70" rx="12" fill="#0F172A"/><path transform="translate(12 11) scale(2)" fill="#E2E8F0" d="${iconPaths[name]}"/><text x="64" y="64" text-anchor="middle" font-family="Arial" font-size="11" fill="#CBD5E1">${name}</text></g>`;
  }).join('');
  const sheet = Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="1360" height="560"><rect width="680" height="560" fill="#F1F5F9"/><rect x="680" width="680" height="560" fill="#020617"/><text x="28" y="42" font-family="Arial" font-size="22" font-weight="700" fill="#0F172A">BagCue assets · light · 24dp icons</text><text x="708" y="42" font-family="Arial" font-size="22" font-weight="700" fill="#F8FAFC">BagCue assets · dark · 24dp icons</text><g transform="translate(28 52)"><rect width="48" height="48" rx="10" fill="#ECFEFF"/>${markSvg(48).toString().replace(/^.*?<svg[^>]*>|<\/svg>$/g, '')}</g><g transform="translate(708 52)"><rect width="48" height="48" rx="10" fill="#082F49"/>${markSvg(48).toString().replace(/^.*?<svg[^>]*>|<\/svg>$/g, '')}</g>${cells}${darkCells}</svg>`);
  await sharp(sheet).png({ compressionLevel: 9 }).toFile(path.join(root, 'docs/assets/asset-contact-sheet.png'));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
