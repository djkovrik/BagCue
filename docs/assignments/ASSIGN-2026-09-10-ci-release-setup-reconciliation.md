# Direct assignment: CI/release setup reconciliation

## User request

> Я выполнил все настройки по docs/CI-RELEASE-SETUP.md, давай обновим спеку и соответствующие релиз гейты.

## Scope and constraints

- Treat the user's completed checklist and newly supplied Firebase configuration files as accepted external-setup evidence.
- Reconcile the AppSpec and release-related quality gates with that completed setup.
- Preserve unrelated working-tree changes and never expose or invent secret values.
- Do not claim a credential-dependent publication or platform build passed unless there is matching execution evidence.
- Validate the revised AppSpec, ledger reconciliation, repository contracts, and all locally runnable release gates.
