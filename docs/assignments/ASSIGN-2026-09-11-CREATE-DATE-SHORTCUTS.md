# Create checklist date shortcut correction

User request captured on 2026-09-11:

> Посмотри еще один момент - на экране Create checklist кнопка Today будто бы ничего не делает, а кнопка Tomorrow каждый раз увеличивает день на +1, проверь корректная ли у них логика

## Scope

- Verify the Today and Tomorrow shortcut behavior on SCREEN-002 against FLOW-003.
- Fix the behavior if it does not select the current local date and the next local date respectively.
- Keep date calculation outside Compose-local mutable state and use the Store/component model as the source of truth.
- Add focused deterministic coverage and run the affected checks.
- Preserve all unrelated working-tree changes.

## Relevant obligations

- REQ-003
- AC-012
- SCREEN-002
- FLOW-003
- QG-001
- QG-002
