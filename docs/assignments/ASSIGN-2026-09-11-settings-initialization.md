# Direct assignment: Settings initialization regressions

User request captured on 2026-09-11:

> Тестирую приложение, нашел пару проблем, проведи ресерч в чем может быть дело:
>
> 1. На чистой установке при входе в настройки сразу вижу сообщение Couldn't save the reminder и кнопку Try again
> 2. Также сразу вижу уведомление Advertising privacy hasn't been resolved. Ads stay off. хотя эндпоинт проверки consistency отвечает "consentRequired": false,  нажатия на Refresh privacy status это не исправляют
>
> Проверь в чем дело и давай исправим обе проблемы

Scope and constraints:

- Diagnose and fix the clean-install Settings initialization errors for reminders and advertising privacy.
- Preserve the fail-closed privacy-before-ads contract and the disabled reminder defaults.
- Cover AC-031 through AC-033 and AC-040 through AC-044 at the affected settings/network/platform surfaces.
- Preserve unrelated user work, including the current uncommitted Android version change.
- Run focused deterministic tests and an Android manifest/build check; report Windows/iOS limits honestly.

