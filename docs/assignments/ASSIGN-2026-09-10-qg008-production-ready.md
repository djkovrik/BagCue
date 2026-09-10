# Direct assignment: QG-008 production endpoint reconciliation

## User request

> Эндпоинт privacy-region endpoint уже финальный и production ready, все работы по нему завершены. Возможно в [APA-EXTERNAL-SETUP.md](D:/Sources/Android/BagCue/docs/APA-EXTERNAL-SETUP.md) и [quality.md](D:/Sources/Android/BagCue/spec/app-spec/quality.md) были зафиксированы излишне строгие требования, этим эндпоинтом пользуются все наши приложения и он уже стабилен точно. Давай явно закроем QG-008 (плюс обновим требования и доки если надо), с остальными OG поработаю еще.

## Scope and constraints

- Accept the owner's confirmation that the shared production privacy-region endpoint is final, stable, operationally owned, and production-ready.
- Close QG-008 without changing the status or candidate-specific requirements of any other quality gate.
- Keep the client-side fail-closed schema, expiry, timeout, consent, and ad-suppression contracts under QG-007.
- Preserve the endpoint protocol and privacy-minimization documentation while removing unsupported release-blocking evidence demands from QG-008.

