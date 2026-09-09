# Final product design review request

Captured: 2026-09-09

User request (verbatim):

> Давай проведем итоговое дизайн ревью с lazyweb. Пройдись по всем экранам с компетенциями vibe-product-designer и рекомендациями от lazyweb, также обращай внимание на:
>
> - консистентность дизайна между экранами (у нас везде должны быть одинаковыми типографика, формы, иерархия заголовков и шрифтов)
> - контрастность цветов и читаемость в светлой и темной темах
> - приверженность Material 3, чтобы наш дизайн выглядел не слишком "стандартно"
> - обязательно проверь все иконки, если нужно их где-то улучшить подключи vibe-assets-creator, хочется тоже чтобы иконки выглядели не слишком стандартно, а был свой стиль
> - у некоторых иконок у нас отображается и текст тоже (например, стрелка назад + Back), будто бы это не нужно

Implementation interpretation:

- Review all declared primary screens and their existing deterministic visual evidence.
- Use current Lazyweb evidence and one-screen-at-a-time review reports.
- Audit cross-screen typography, shapes, title hierarchy, light/dark contrast, Material 3 component semantics and restrained expressive identity.
- Inventory all icons and remove redundant adjacent action text where the icon is universally understood and accessibility remains explicit.
- Route any approved custom icon/resource defects through the asset workflow and revalidate affected goldens.
