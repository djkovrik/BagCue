# Data

## Storage ownership

SQLDelight owns the relational catalog, templates, template positions, sessions, session snapshots, seed overrides, and history. Multiplatform Settings owns singleton reminder defaults, Analytics preference, and the versioned AdvertisingPrivacyState payload. Local notifications are derived scheduled effects, not authoritative storage.

All persistence calls cross Manager interfaces as Kotlin `Result`. Transactions own cascading deletes, session recomposition, one-off item creation, completion, and undo restoration.

## Conceptual tables

### packing_item

- stable ID;
- seed resource key or nullable user-authored name override;
- optional user-authored usual-location text;
- created/updated metadata needed only for deterministic local ordering;
- no resolved translation.

### kit_template

- stable ID;
- seed resource key or nullable user-authored name override;
- stable local sort position;
- durable seed deleted/modified state where applicable.

### template_position

- template ID and PackingItem ID;
- quantity 1–99;
- seed bag-label resource key or nullable user-authored bag-label text;
- stable order within the template;
- uniqueness of one item ID per template.

### packing_session

- stable ID;
- unique local calendar date;
- status `active` or `completed`;
- completion mode `all-packed` or `with-skipped` when completed;
- created/completed timestamps for local history and duration bucketing only;
- selected-template snapshot metadata.

### session_packing_item

- session ID and stable position ID;
- nullable catalog PackingItem ID so completed history survives catalog deletion;
- display identity snapshot: seed resource key and/or user-authored name at completion;
- quantity 1–99;
- target-bag seed key or user text;
- optional source-hint override;
- `not-packed` or `packed` runtime state;
- skipped outcome only after confirmed completion;
- stable order and conflict marker derived/recomputed transactionally.

### seed_override

Records renamed or deleted starter template/item/label identities. It prevents silent reseeding while retaining resource-key resolution for untouched starter data.

### undo payload

Undo for template recomposition, session-item removal, or session deletion may use an in-memory command plus a transactional snapshot. It must remain available until the actionable snackbar is dismissed by the user; it is not promised after process death.

## Settings payloads

### ReminderPreferences

- global evening enabled false, default time 20:00;
- global morning enabled false, default time 07:30;
- each session copies effective enabled/time values and may override them;
- no exact alarm requirement;
- no locale or theme preference.

### AnalyticsPreference

- default false;
- toggle changes collection immediately;
- disabling invokes Firebase collection disablement and local analytics/app-instance reset;
- no User-ID or custom user properties.

### AdvertisingPrivacyState

One versioned private payload stores only schemaVersion, consentRequired, policyVersion, expiresAt, and the matching app-owned consent choice when required. It stores no IP, country, GeoIP metadata, advertising ID, or request identity. Freshness is clamped to at most 72 hours.

## Starter local dataset

The resource-owning Compose module contains the complete English base keys and matching Russian keys.

| Stable ID | Resource key | EN meaning | RU meaning |
| --- | --- | --- | --- |
| `template_office` | `starter_template_office` | Office | Офис |
| `template_pool` | `starter_template_pool` | Pool | Бассейн |
| `bag_backpack` | `starter_bag_backpack` | Backpack | Рюкзак |
| `bag_sports` | `starter_bag_sports` | Sports bag | Спортивная сумка |
| `item_laptop` | `starter_item_laptop` | Laptop | Ноутбук |
| `item_charger` | `starter_item_charger` | Charger | Зарядное устройство |
| `item_headphones` | `starter_item_headphones` | Headphones | Наушники |
| `item_badge` | `starter_item_badge` | Badge | Пропуск |
| `item_keys` | `starter_item_keys` | Keys | Ключи |
| `item_water_bottle` | `starter_item_water_bottle` | Water bottle | Бутылка воды |
| `item_lunch_snack` | `starter_item_lunch_snack` | Lunch/snack | Обед/перекус |
| `item_swimsuit` | `starter_item_swimsuit` | Swimsuit/trunks | Купальник/плавки |
| `item_swim_cap` | `starter_item_swim_cap` | Swim cap | Шапочка |
| `item_goggles` | `starter_item_goggles` | Goggles | Очки |
| `item_towel` | `starter_item_towel` | Towel | Полотенце |
| `item_shower_gel` | `starter_item_shower_gel` | Shampoo/shower gel | Шампунь/гель |
| `item_flip_flops` | `starter_item_flip_flops` | Flip-flops | Сланцы |
| `item_wet_bag` | `starter_item_wet_bag` | Wet-items bag | Пакет для мокрых вещей |

All starter quantities are 1 and all usual-location values are empty. Office positions use `bag_backpack`; Pool positions use `bag_sports`.

## Localized text storage

English keys live in `composeResources/values/strings.xml`; Russian keys live in `composeResources/values-ru/strings.xml`. Every bundled label, error, dialog, content description, notification, starter value, privacy explanation, and analytics toggle explanation uses the same shared key across locales. Unsupported locales fall back to English.

Domain, Store, component, SQL rows, settings, and snapshots carry stable IDs/resource keys or user-authored text, never resolved starter translations. Native notification text follows the same English-base/Russian-additional contract in platform resources where Compose Resources cannot be used.

## Network boundaries

### Privacy region

`GET https://168.222.252.178/v1/privacy/region` uses `Accept: application/json` and optional `X-Privacy-Client: bagcue/<app-version>`. Redirects are disabled. Response size is capped at 4 KiB with five-second connect/request/socket limits. Only 200 JSON matching schema 1, a non-empty policyVersion, and a future expiresAt no more than 72 hours is accepted. Every other result fails closed for advertising only.

### Firebase Analytics

Collection is disabled by default. When enabled, the platform adapter sends only the approved event/parameter allowlist. Android disables Advertising ID collection. Google Signals, ads personalization, User-ID, user properties, Google Ads linking, and user-content parameters are prohibited. User/event retention is configured to two months in the external property; the app never claims instant deletion of already processed server data.

### Yandex Mobile Ads

Android receives a real production ad unit ID only from external release configuration. iOS production/CI configuration is null: the SDK compiles and links but does not initialize or request ads. An official demo ID exists only in a separately invoked manual diagnostic path and can never be selected by release or CI configuration.

## Retention and deletion

Local catalog, templates, and history remain until user deletion, application-data clearing, or uninstall, subject to OS backup behavior. No automatic history expiry exists. Analytics user/event retention is two months; aggregated provider reports may follow provider behavior described in the privacy policy. Endpoint operational retention and log redaction follow `docs/PRIVACY-REGION-ENDPOINT.md` and must be independently verified before release.

## Migration and corruption

Before the first shipped database baseline, evolve the initial SQLDelight schema directly; discarded development-only schemas and test data do not require upgrade paths. Once a baseline ships, each schema change requires a numbered migration and upgrade tests from every supported shipped schema. Reusing code from the paused iteration does not make its development schema a shipped baseline. Invalid quantities, duplicate LocalDate rows, broken references, or malformed Settings payloads must produce a typed failure or conservative reset limited to the affected optional setting; the app must not silently delete the catalog/session database. Use stable IDs and localization resource keys from the initial schema. Do not introduce hypothetical legacy localization formats or parsers.

