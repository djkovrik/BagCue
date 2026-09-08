const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../..');
const spec = JSON.parse(fs.readFileSync(path.join(root, 'spec/app-spec/app-spec.json'), 'utf8').replace(/^\uFEFF/, ''));
const sha256 = relative => crypto.createHash('sha256').update(fs.readFileSync(path.join(root, relative))).digest('hex');
const usagePath = 'shared/compose/src/commonMain/kotlin/com/sedsoftware/bagcue/compose/assets/BagCueAssets.kt';
const visualPaths = ['docs/assets/asset-contact-sheet.png', 'docs/assets/visual-review.md'];
const names = {
  'ASSET-001': 'Mark', 'ASSET-002': 'Today', 'ASSET-003': 'Sessions', 'ASSET-004': 'Templates',
  'ASSET-005': 'Settings', 'ASSET-006': 'Add', 'ASSET-007': 'Check', 'ASSET-008': 'Calendar',
  'ASSET-009': 'Edit', 'ASSET-010': 'Delete', 'ASSET-011': 'More', 'ASSET-012': 'Search',
  'ASSET-013': 'Duplicate', 'ASSET-014': 'Notifications', 'ASSET-015': 'Privacy',
  'ASSET-016': 'Back', 'ASSET-017': 'Bag'
};
const materialNames = {
  'ASSET-002': 'home', 'ASSET-003': 'calendar_month', 'ASSET-004': 'checklist', 'ASSET-005': 'settings',
  'ASSET-006': 'add', 'ASSET-007': 'check', 'ASSET-008': 'calendar_month', 'ASSET-009': 'edit',
  'ASSET-010': 'delete', 'ASSET-011': 'more_vert', 'ASSET-012': 'search', 'ASSET-013': 'content_copy',
  'ASSET-014': 'notifications', 'ASSET-015': 'privacy_tip', 'ASSET-016': 'arrow_back', 'ASSET-017': 'work'
};

const assets = spec.assetRequirements.items.map(item => {
  const actualPath = `shared/compose/src/commonMain/composeResources/drawable/${item.resourceName}.xml`;
  const provenance = item.id === 'ASSET-001' ? {
    method: 'create-vector',
    source: 'Original project artwork created from the approved BagCue AppSpec brief; editable vector source is the runtime XML.',
    rights: 'Original artwork created for this BagCue project; no third-party artwork incorporated.',
    generationRecord: {
      path: 'docs/assets/ASSET-001-generation-record.json',
      sha256: sha256('docs/assets/ASSET-001-generation-record.json')
    }
  } : {
    method: 'reuse',
    source: `Google Material Symbols Rounded: https://fonts.google.com/icons?icon.query=${materialNames[item.id]}&icon.style=Rounded`,
    rights: 'Google Material Symbols and Icons, Apache License 2.0; adapted to portable Compose vector XML.'
  };
  return {
    id: item.id,
    outputs: [{ path: actualPath, sha256: sha256(actualPath) }],
    provenance,
    usages: [{
      path: usagePath,
      sha256: sha256(usagePath),
      symbol: `val ${names[item.id]}`,
      screenIds: item.screenIds
    }],
    visualEvidence: visualPaths.map(p => ({ path: p, sha256: sha256(p) }))
  };
});

const launcherFiles = [
  ...fs.readdirSync(path.join(root, 'androidApp/src/main/res'), { withFileTypes: true })
    .filter(entry => entry.isDirectory() && entry.name.startsWith('mipmap-'))
    .flatMap(entry => fs.readdirSync(path.join(root, 'androidApp/src/main/res', entry.name))
      .filter(name => name.endsWith('.png'))
      .map(name => `androidApp/src/main/res/${entry.name}/${name}`)),
  ...fs.readdirSync(path.join(root, 'iosApp/iosApp/Assets.xcassets/AppIcon.appiconset'))
    .filter(name => name.endsWith('.png'))
    .map(name => `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/${name}`)
].sort();

const manifest = {
  schemaVersion: '1.0',
  pathAdaptation: {
    reason: 'The frozen AppSpec uses the template module name composeApp; BagCue owns Compose Multiplatform resources in shared/compose.',
    approvedPattern: 'composeApp/src/commonMain/composeResources/drawable/<name>.xml',
    actualPattern: 'shared/compose/src/commonMain/composeResources/drawable/<name>.xml'
  },
  launcherDerivatives: launcherFiles.map(p => ({ path: p, sha256: sha256(p) })),
  assets
};
fs.writeFileSync(path.join(root, 'docs/assets/asset-manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
