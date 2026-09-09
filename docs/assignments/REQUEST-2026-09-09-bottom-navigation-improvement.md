# Bottom navigation improvement request

Captured: 2026-09-09

## User request

Improve BagCue's main bottom navigation bar:

- use Lazyweb to find strong bottom-navigation references;
- consider a distinctive, stylish animation;
- keep every change aligned with the established BagCue design system;
- use the asset workflow if the tab icons need to be redrawn.

Captured verbatim:

> Давай улучшим дизайн нашей основной нижней панели навигации:
>
> - подключи lazyweb на предмет улучшения дизайна, чтобы поискать сильные референсы именно для bottom navigation bar
> - возможно можем добавить какую-нибудь нестандартную стильную анимацию
> - все доработки не должны выбиваться из нашей общей схемы дизайна
> - если нужно будет перерисовать иконки вкладок, задействуй vibe-assets-creator

## Delivery constraints

- Preserve the four stable destinations and their always-visible labels.
- Preserve the compact navigation-bar / expanded navigation-rail adaptation.
- Preserve 48dp minimum targets, selected semantics, light/dark themes, English/Russian localization, and the 200% font-scale layout.
- Prefer canonical Material 3 navigation semantics and theme tokens.
- Keep motion brief, interruptible, reversible, and compatible with the platform reduced-motion setting.
- Do not add or redraw icons unless the existing family fails the asset audit.
