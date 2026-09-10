# Direct assignment: legal review and privacy-policy reconciliation

## User request

> Следующей итерацией давай обновим спеку и релиз гейты во всем что касается legal review и политики конфиденциальности - считаем что все юридические ревью завершены и политика у нас стабильна, финальная ссылка готова - https://sedsoftware.com/apps/bagcue/policy.html

## Scope and constraints

- Accept the user's explicit confirmation that legal reviews are complete and the privacy policy is stable.
- Record `https://sedsoftware.com/apps/bagcue/policy.html` as the canonical public privacy-policy URL.
- Verify the public URL and compare its content with the repository policy artifact before changing policy-related gates.
- Reconcile only legal-review and privacy-policy obligations; do not infer completion of unrelated endpoint operations, publication, or candidate-specific build checks.
- Preserve existing working-tree changes and do not expose credentials or private data.
