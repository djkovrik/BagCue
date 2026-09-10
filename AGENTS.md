# BagCue repository guide

This file applies to the entire repository. Keep changes scoped, preserve unrelated user work, and use the checked-in Gradle wrapper.

## Sources of truth

1. The current user request.
2. `spec/app-spec/app-spec.json`, including requirement, acceptance-scenario, screen, flow, asset, and quality-gate IDs.
3. The human-readable contracts in `spec/app-spec/product.md`, `domain.md`, `data.md`, `design.md`, and `quality.md`.
4. The relevant files under `spec/app-spec/flows/` and `spec/app-spec/screens/`.
5. Existing production code and tests as implementation evidence, not as permission to weaken the spec.

The approved AppSpec revision is `2026-09-10-legal-policy-stable`. Do not edit the spec merely to make an implementation pass. When a requested behavior conflicts with the spec, identify the affected IDs and make the conflict explicit.

For full Vibe delivery or acceptance work, also follow `docs/DEVELOPMENT-PROMPT.md` and the current `.vibe` protocol. Historical receipts and status claims are not fresh verification.

## Product invariants

- BagCue is a local-first daily packing assistant, not a travel planner or generic inventory system.
- There is at most one `PackingSession` per local calendar date. Store date identity independently of time-zone changes.
- A session is a snapshot. Later catalog or template edits must not silently rewrite it.
- Merge template items by stable item ID, take the maximum quantity, and require an explicit choice for conflicting target bags.
- Never carry `Packed` state automatically to another date. Completing with skipped items is explicit and is not a fully successful completion.
- Core catalog, templates, sessions, history, and checklist behavior must work offline. Network, Analytics, privacy checks, and Ads must not block it.
- English is the complete base locale and Russian is the supported additional locale. Follow the system language; do not add an in-app language or theme picker.
- Preserve user input on recoverable failures and expose a localized recovery action.

## Architecture boundaries

- `shared/domain`: pure, platform-neutral models, value rules, algorithms, IDs, events/errors, and repository/service contracts. No Compose, persistence, network, or platform APIs.
- `shared/data`: SQLDelight and Multiplatform Settings implementations, migrations, transactions, and mapping. Depend on domain contracts.
- `shared/network`: Ktor transport, DTOs, mapping, timeouts, and privacy-region failures. Do not leak transport models into domain or UI.
- `shared/platform`: expect/actual or target-specific notifications, Analytics, Ads, and native services. Keep unavailable capabilities nullable and fail closed.
- `shared/component/*`: one Decompose component contract and MVIKotlin Store-backed implementation per UI-visible feature. Stores coordinate through domain-facing managers/repositories; reducers remain pure.
- `shared/root`: the real production component graph, navigation, lifecycle, and dependency wiring. A feature is not integrated until it is reachable here.
- `shared/compose`: stateless UI over component models and callbacks, shared resources, theme, adaptive layouts, and previews. Do not access persistence or network directly.
- `androidApp` and `iosApp`: thin platform entry points and platform configuration.

Keep manual dependency injection and the existing module direction. Do not bypass public contracts to reach another feature's internals.

## UI, resources, and assets

- Before designing, critiquing, or materially changing product UI, use the applicable Lazyweb workflow and ground decisions in real references as required by the repository instructions.
- Implement the state matrix from the relevant `SCREEN-*` file, including loading, empty, error, permission, conflict, and completion states where declared.
- Use Material 3, BagCue design tokens, production `Res.*` resources, safe insets, accessible semantics, and touch targets.
- Add every user-visible string to both EN and RU resources. Stable starter content uses resource keys; user-authored overrides remain literal and must not auto-translate.
- Update production previews and Paparazzi goldens for affected screen states. Inspect changed images; do not accept snapshots blindly.
- Reuse the asset inventory and record required asset changes instead of adding untracked placeholders.

## Privacy, analytics, ads, and secrets

- Analytics is opt-in and off by default. Only the six allowlisted events and categorical parameters from the AppSpec are permitted; never send user text, local IDs, exact dates, tokens, or consent content.
- Ads are Yandex-only, Android-production-only for the first public release, and eligible only after a fully packed completion. Privacy-region uncertainty, expiry, mismatch, decline, or error suppresses SDK initialization and requests.
- Apply current consent before every eligible ad initialization. Never add Advertising ID use, ad personalization, mediation, or Firebase-to-Ads linkage.
- Keep the canonical privacy URL and policy version consistent across code, policy documents, endpoint validation, and store disclosures.
- `androidApp/google-services.json` and `iosApp/iosApp/GoogleService-Info.plist` are committed app configuration required by the spec. Signing material, passwords, API/service-account credentials, and local machine configuration are secrets and must not be committed or printed.

## Working method

1. Inspect `git status` and the relevant AppSpec sections before editing.
2. Map behavior changes to their `AC-*`, `SCREEN-*`, `FLOW-*`, and `QG-*` obligations.
3. Make the smallest coherent change through the production architecture. Do not replace real wiring with preview-only or test-only implementations.
4. Add or update tests at the declared verification surfaces. Prefer deterministic fakes, controlled clocks/local dates, and explicit failure-cause assertions.
5. Run focused checks while iterating, then combined relevant checks at a stable capability boundary.
6. Report what was verified, what was not run, and which external or platform gates remain. Never claim iOS verification from Windows or external console/device verification without evidence.

Do not run concurrent Gradle builds against this workspace. During orchestrated Vibe work on Windows, use the serialized Gradle execution process required by `docs/DEVELOPMENT-PROMPT.md`.

## Common checks

Use `.\gradlew.bat` on Windows and `./gradlew` on macOS/Linux.

```shell
./gradlew detekt
./gradlew koverVerify koverXmlReport
./gradlew :shared:domain:allTests
./gradlew :shared:data:testAndroidHostTest
./gradlew :shared:network:allTests
./gradlew :shared:platform:testAndroidHostTest
./gradlew :shared:component:catalog:testAndroidHostTest
./gradlew :shared:component:templates:testAndroidHostTest
./gradlew :shared:component:session:testAndroidHostTest
./gradlew :shared:component:history:testAndroidHostTest
./gradlew :shared:component:settings:testAndroidHostTest
./gradlew :shared:root:testAndroidHostTest
./gradlew :shared:compose:testAndroidHostTest
./gradlew :shared:data:verifySqlDelightMigration
./gradlew :shared:compose:generateComposeResClass
./gradlew :shared:compose:visual-test:verifyPaparazziDebug
./gradlew :androidApp:verifyAndroidReleaseConfiguration :androidApp:assembleDebug
python .github/workflows/scripts/validate_repository.py
```

Choose the affected subset for a focused change. Use the exact full matrix in `.github/workflows/AnalysisAndTest.yml` for broad or release-facing changes. iOS tests and linkage require macOS and must match the `Test and link iOS` CI job.

## Definition of done

- Production behavior matches the relevant acceptance scenarios and is reachable from the real root.
- Required unit, Store/component, persistence/network/platform, localization, and visual tests are updated and passing on available targets.
- EN/RU resources, previews, goldens, assets, privacy/release documents, and trace or migration paths are updated when affected.
- No unrelated files, secrets, generated build output, or stale completion claims are introduced.
- The hand-off names exact checks and evidence, plus any honest `blocked-external` or platform limitations.
