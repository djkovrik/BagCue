# Requirement traceability

- Protocol: `2.0`
- Ledger: `LEDGER-daf88a72-e8ed-4785-a86a-1a3cc7429e83`
- Phase: `reconciling`
- Active AC: `none`
- Ledger digest: `239c716b061b946418332c246895cee3d8268018f4580e12b705c2a6ca2655bd`
- AppSpec fingerprint: `ffac9713c8bbaa0b082619582efdfd31a532863c428b8fe942ce6bb16315e978`
- Workspace fingerprint: `9867bc1fb37a1c58df1b769b74468f1de8c14a0e9c184689c845a123304bf1e3`
- Next action: Continue work on the remaining release gates; QG-008 is verified and closed.

## Acceptance scenarios

| ID | Requirement | Priority | Dependencies | Status | Surfaces | Owner | Receipts |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AC-001 | REQ-001 | must | — | not-started | component-test, persistence-integration-test | — | — |
| AC-002 | REQ-001 | must | — | not-started | component-test, persistence-integration-test | — | — |
| AC-003 | REQ-001 | must | AC-002 | not-started | component-test, persistence-integration-test | — | — |
| AC-004 | REQ-001 | must | AC-001 | not-started | component-test, persistence-integration-test | — | — |
| AC-005 | REQ-001 | must | AC-001 | not-started | domain-test, component-test | — | — |
| AC-006 | REQ-002 | must | — | not-started | component-test, persistence-integration-test | — | — |
| AC-007 | REQ-002 | must | — | not-started | component-test, persistence-integration-test | — | — |
| AC-008 | REQ-002 | must | AC-007 | not-started | component-test, persistence-integration-test | — | — |
| AC-009 | REQ-002 | must | AC-007 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-010 | REQ-002 | must | AC-007 | not-started | component-test, persistence-integration-test | — | — |
| AC-011 | REQ-002 | must | — | not-started | domain-test, localization-contract-test, persistence-integration-test | — | — |
| AC-012 | REQ-003 | must | AC-001, AC-007 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-013 | REQ-003 | must | AC-012 | not-started | domain-test, component-test | — | — |
| AC-014 | REQ-003 | must | AC-013 | not-started | domain-test, component-test, golden-test | — | — |
| AC-015 | REQ-003 | must | AC-012 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-016 | REQ-003 | must | AC-007 | not-started | domain-test, component-test, golden-test | — | — |
| AC-017 | REQ-003 | must | AC-012 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-018 | REQ-004 | must | AC-012 | not-started | component-test, persistence-integration-test | — | — |
| AC-019 | REQ-004 | must | AC-018 | not-started | component-test, persistence-integration-test, golden-test | — | — |
| AC-020 | REQ-004 | must | AC-018 | not-started | component-test, persistence-integration-test | — | — |
| AC-021 | REQ-004 | must | AC-018 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-022 | REQ-004 | must | AC-018 | not-started | component-test, persistence-integration-test | — | — |
| AC-023 | REQ-004 | must | AC-014, AC-019 | not-started | domain-test, component-test, persistence-integration-test, golden-test | — | — |
| AC-024 | REQ-004 | must | AC-014, AC-018 | not-started | domain-test, component-test, persistence-integration-test, golden-test | — | — |
| AC-025 | REQ-004 | must | AC-007, AC-021 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-026 | REQ-005 | must | AC-012 | not-started | component-test, persistence-integration-test, golden-test | — | — |
| AC-027 | REQ-005 | must | AC-023 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-028 | REQ-005 | must | AC-026 | not-started | domain-test, component-test, persistence-integration-test | — | — |
| AC-029 | REQ-005 | must | AC-026 | not-started | component-test, persistence-integration-test | — | — |
| AC-030 | REQ-005 | must | AC-012 | not-started | domain-test, component-test, platform-test | — | — |
| AC-031 | REQ-006 | should | — | not-started | settings-integration-test, component-test | — | — |
| AC-032 | REQ-006 | should | AC-031 | not-started | settings-integration-test, component-test | — | — |
| AC-033 | REQ-006 | should | AC-032 | not-started | settings-integration-test, component-test, platform-test | — | — |
| AC-034 | REQ-006 | should | AC-033 | not-started | component-test, platform-test, golden-test | — | — |
| AC-035 | REQ-006 | should | AC-012, AC-033 | not-started | domain-test, platform-test | — | — |
| AC-036 | REQ-007 | should | — | not-started | settings-integration-test, firebase-contract-test | — | — |
| AC-037 | REQ-007 | should | AC-036 | not-started | component-test, settings-integration-test | — | — |
| AC-038 | REQ-007 | should | AC-037 | not-started | component-test, settings-integration-test, firebase-contract-test | — | — |
| AC-039 | REQ-007 | should | AC-038 | not-started | analytics-event-contract-test, privacy-static-check | — | — |
| AC-040 | REQ-008 | should | — | not-started | network-mock-test, settings-integration-test | — | — |
| AC-041 | REQ-008 | should | AC-040 | not-started | network-mock-test, settings-integration-test | — | — |
| AC-042 | REQ-008 | should | AC-040 | not-started | component-test, settings-integration-test, ad-lifecycle-test | — | — |
| AC-043 | REQ-008 | should | AC-040 | not-started | network-mock-test, settings-integration-test, ad-lifecycle-test | — | — |
| AC-044 | REQ-008 | should | AC-041 | not-started | component-test, ad-lifecycle-test, network-mock-test | — | — |
| AC-045 | REQ-008 | should | AC-023, AC-044 | not-started | component-test, ad-lifecycle-test, golden-test | — | — |
| AC-046 | REQ-008 | should | AC-045 | not-started | component-test, ad-lifecycle-test, ios-link-test, privacy-static-check | — | — |
| AC-047 | REQ-009 | must | — | not-started | component-test, golden-test, accessibility-test | — | — |
| AC-048 | REQ-009 | must | AC-011 | not-started | localization-contract-test, component-test, golden-test | — | — |
| AC-049 | REQ-009 | must | AC-047, AC-048 | not-started | accessibility-test, golden-test, manual-visual-review | — | — |
| AC-050 | REQ-010 | must | — | not-started | component-test, persistence-integration-test, golden-test | — | — |
| AC-051 | REQ-010 | must | AC-004, AC-010, AC-029 | not-started | domain-test, persistence-integration-test | — | — |

## Quality gates

| ID | Category | Platform | Applicability | Status | Surfaces | Receipts |
| --- | --- | --- | --- | --- | --- | --- |
| QG-001 | repository | all | applicable | not-started | repository-check, domain-test, component-test, persistence-integration-test | — |
| QG-002 | platform | android | applicable | not-started | android-build, android-platform-test | — |
| QG-003 | platform | ios | applicable | not-started | ios-build, ios-link-test | — |
| QG-004 | release | all | applicable | not-started | release-check | — |
| QG-005 | external | android | needs-review | implemented-unverified | google-play-publication-check | — |
| QG-006 | repository | all | applicable | not-started | asset-check, asset-visual | — |
| QG-007 | repository | all | applicable | not-started | network-mock-test, ad-lifecycle-test, firebase-contract-test, privacy-static-check | — |
| QG-008 | external | all | applicable | verified | privacy-endpoint-check | .vibe/receipts/RECEIPT-d31155f9-4f7d-4d55-ad09-081e8d59f180.json |
| QG-009 | external | android | needs-review | implemented-unverified | android-ad-config-check | — |
| QG-010 | repository | ios | applicable | not-started | documentation-check | — |
| QG-011 | external | all | needs-review | implemented-unverified | firebase-console-check | — |
| QG-012 | release | all | applicable | implemented-unverified | privacy-policy-review, data-safety-review | — |

## Integrated flows (separate from AC verification)

| Package | ACs | Integration | Goal |
| --- | --- | --- | --- |
| adaptive-localization-resilience | AC-047, AC-048, AC-049, AC-050, AC-051 | integrated | Deliver a four-destination adaptive product shell, complete EN/RU fallback and authored-text preservation, accessible reflow, failure-retained input, and atomic cascade deletion across the production Android/iOS roots. |
| analytics-event-continuation | AC-036, AC-037, AC-038, AC-039 | integrated | Complete the local analytics integration contract and record TemplateCreated and PackingSessionReopened only after their exact production success points, using the existing content-free six-event adapter and production root injection on Android and iOS. |
| analytics-privacy-ads | AC-036, AC-037, AC-038, AC-039, AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046 | integrated | Opt-in content-free analytics and a fail-closed privacy-region decision govern one production Yandex inline ad after completion on Android, while every packing screen and default iOS build remain ad-free. |
| assets-and-visual-matrix | AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046, AC-047, AC-048, AC-049, AC-050 | integrated | Deliver the complete BagCue asset inventory and deterministic preview/golden matrix for all product screens and risk states, then inspect and review the production visual system across themes, locales, text scale and adaptive widths. |
| catalog-core | AC-001, AC-002, AC-003, AC-004, AC-005 | integrated | Launch the real BagCue root, open the catalog, save a catalog item, read it, and restore it after process restart |
| final-design-review |  | in-progress | Review every declared BagCue primary screen against current Lazyweb evidence and the Material 3 contract, correct cross-screen typography, shapes, hierarchy, contrast, iconography and redundant icon-plus-text actions, then re-verify the affected deterministic goldens. |
| reminders | AC-031, AC-032, AC-033, AC-034, AC-035 | integrated | Users can configure disabled-by-default global and per-session reminders while notification denial leaves packing usable and clock or zone changes reschedule one stable notification per session on Android and iOS. |
| repository-release-convergence | AC-036, AC-037, AC-038, AC-039, AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046 | integrated | Converge BagCue on strict Detekt/Kover and five-workflow CI, reproducible Android release configuration, iOS native linkage contracts, real consent-gated analytics and ads adapters, bounded privacy-region transport, and evidence-backed release documentation without fabricating externally owned credentials or console state. |
| session-checklist | AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024, AC-025 | integrated | Launch the production root, create or replace the sole dated session from templates, resolve conflicts, pack/edit/add/remove/undo items, complete with or without skipped items, and restore the exact persisted snapshot after restart. |
| session-history | AC-026, AC-027, AC-028, AC-029, AC-030 | integrated | A user can browse planned and completed sessions, reopen or repeat history, delete with confirmation and undo, and keep the original local date across timezone changes from both production entry points. |
| template-management | AC-006, AC-007, AC-008, AC-009, AC-010, AC-011 | integrated | Launch the production root, open Templates, create or edit a reusable template, reopen it after restart, and retain independent catalog/session snapshot semantics. |

## Durable hand-offs

- `HANDOFF-catalog-build-architecture-20260908` — `1e5d252a75af7633ae3ddad0351dffe1e7c3a7ddb7734bfa5a202282a4317a4a` (.vibe/handoffs/HANDOFF-catalog-build-architecture-20260908.json)
- `HANDOFF-catalog-build-architecture-20260908-v2` — `d4811f602cffdd44700892554ed5157667ad784609c7f0d6542e12255cc0abcf` (.vibe/handoffs/HANDOFF-catalog-build-architecture-20260908-v2.json)
- `HANDOFF-catalog-domain-persistence-20260908` — `c4bc043d048dec7365807a19aba6cbd62284d04d26d5e4e9e319bfe0dfb8a72f` (.vibe/handoffs/HANDOFF-catalog-domain-persistence-20260908.json)
- `HANDOFF-catalog-domain-persistence-freeze-20260908` — `bfb03e8595dae74847970b60081d351573666736559559dbeeeef3c9b61bcc28` (.vibe/handoffs/HANDOFF-catalog-domain-persistence-freeze-20260908.json)
- `HANDOFF-catalog-component-root-20260908` — `a10e6c256a99a662c3dc0578a599e2daf281b73d3b342c41a676be03efa5c9b5` (.vibe/handoffs/HANDOFF-catalog-component-root-20260908.json)
- `HANDOFF-catalog-compose-production-integration-20260908` — `d496594e352f930805e52becb0ae701dc8ec2a08878d1a7025cc477fd279de86` (.vibe/handoffs/HANDOFF-catalog-compose-production-integration-20260908.json)
- `HANDOFF-template-build-architecture-20260908` — `77543a849c9efcb67e5eebed763710d13d0bbf2e6ff775f9f59dbb54b0a7c28a` (.vibe/handoffs/HANDOFF-template-build-architecture-20260908.json)
- `HANDOFF-template-domain-persistence-20260908` — `1ed7f62dcb977594c538706c34bcab8cf8d82e4d28ee39159232d23eb05693ff` (.vibe/handoffs/HANDOFF-template-domain-persistence-20260908.json)
- `HANDOFF-template-compose-production-integration-20260908` — `b56c9caed171d7a724aa59b1a8d0a84ad0e34116deaa8605e44f29133ce7881d` (.vibe/handoffs/HANDOFF-template-compose-production-integration-20260908.json)
- `HANDOFF-template-component-root-20260908` — `eb178d9e0c3080d64155affbb8d62d4c96001b908fefc1c2bb48be1cf8a792a0` (.vibe/handoffs/HANDOFF-template-component-root-20260908.json)
- `HANDOFF-session-domain-persistence-20260908` — `790cc9c88a82fcca06d641329ec8e8c7ee3693557c5608e4a2348705992a93a4` (.vibe/handoffs/HANDOFF-session-domain-persistence-20260908.json)
- `HANDOFF-session-component-root-20260908` — `6018950e8b22c64db13c184f28bf39516ebcda4109555e589c833edb36378ecd` (.vibe/handoffs/HANDOFF-session-component-root-20260908.json)
- `HANDOFF-session-compose-production-integration-20260908` — `4d4daa70d39a7948abbec46dd83c84465a82f6726b11a7d2786651e500e8fe54` (.vibe/handoffs/HANDOFF-session-compose-production-integration-20260908.json)
- `HANDOFF-session-build-architecture-20260908` — `e4819010885476673d38a3c6d95ae051a6189aace5afc97008e7f06e2408fb07` (.vibe/handoffs/HANDOFF-session-build-architecture-20260908.json)
- `HANDOFF-history-build-architecture-20260908` — `aff15ccea3de54c44fcb97039882ff88a82395611704f1dfe4dd23d2ea54f409` (.vibe/handoffs/HANDOFF-history-build-architecture-20260908.json)
- `HANDOFF-history-domain-persistence-20260908` — `a2348d99ef3ec2619299bf7cce6c67c09c3d9f88aa231a259460dfbc6bdf49ea` (.vibe/handoffs/HANDOFF-history-domain-persistence-20260908.json)
- `HANDOFF-history-compose-production-integration-20260908` — `8f90940c37af7c352604c1ac864cc09bd35d3ffe9acd93b6c16a05e6280113c2` (.vibe/handoffs/HANDOFF-history-compose-production-integration-20260908.json)
- `HANDOFF-history-component-root-20260908` — `93a748d9bd0115aafbcb1da75c35253b7025685922febc189b095f719c232b79` (.vibe/handoffs/HANDOFF-history-component-root-20260908.json)
- `HANDOFF-reminders-build-architecture-20260908` — `bee58441d6b6d38d2ac8938c284e6393591209b4704e9773882627e3f77ba1fd` (.vibe/handoffs/HANDOFF-reminders-build-architecture-20260908.json)
- `HANDOFF-reminders-data-build-edge-20260908` — `795bf8731d30c9d65524f1feba23b0942bf003811d2c274fe27c7f51b85666d0` (.vibe/handoffs/HANDOFF-reminders-data-build-edge-20260908.json)
- `HANDOFF-reminders-domain-data-platform-20260908` — `72c661bd49417e025095f098c51ad6fed180905a63286f4d6f25e21c706689a3` (.vibe/handoffs/HANDOFF-reminders-domain-data-platform-20260908.json)
- `HANDOFF-reminders-component-root-20260908` — `129a764f3486d4d8eb922ba8733471f92b56c896d386898a3d2d64b11d5f7c94` (.vibe/handoffs/HANDOFF-reminders-component-root-20260908.json)
- `HANDOFF-reminders-compose-20260908` — `423efc1c54e9047c91c009a53171354d50a72e8bc59464dadfa48a2218a84164` (.vibe/handoffs/HANDOFF-reminders-compose-20260908.json)
- `HANDOFF-apa-architecture-20260908` — `eafd51f1786ca0d32d673785367e4a24fad3c1b6614bca6792812bb0ec472547` (.vibe/handoffs/HANDOFF-apa-architecture-20260908.json)
- `HANDOFF-apa-domain-platform-20260908` — `b731f8e25394b17ce78eb68d3d521f6de6005877a69f8233c196d1fcb3f9cf57` (.vibe/handoffs/HANDOFF-apa-domain-platform-20260908.json)
- `HANDOFF-apa-component-root-20260908` — `6dfa89b423f2954c68c5501326f2245ed2d18c42361b20e54f864163a46efd2f` (.vibe/handoffs/HANDOFF-apa-component-root-20260908.json)
- `HANDOFF-apa-compose-20260908` — `7a6caa6159459f9d5312efb16e6a4637e777e85216a0877f5ab9cdd63a020011` (.vibe/handoffs/HANDOFF-apa-compose-20260908.json)
- `HANDOFF-analytics-event-continuation-20260908` — `ce9522995b60c88d4b655bc41f6ce2bd37a4bdebbe91c17b652c2dde6e1ba379` (.vibe/handoffs/HANDOFF-analytics-event-continuation-20260908.json)
- `HANDOFF-alr-root-navigation-20260908` — `7ab38d927e976d5b849d228e42aab104a8e25e43c3415a5390cf1e3a2913d871` (.vibe/handoffs/HANDOFF-alr-root-navigation-20260908.json)
- `HANDOFF-alr-root-navigation-20260908-v2` — `9b069d82e5ffb68f8e97e24469d8c0c19fa319cfa9c7499080d40056abd43e54` (.vibe/handoffs/HANDOFF-alr-root-navigation-20260908-v2.json)
- `HANDOFF-alr-compose-localization-20260908` — `ffa87e5f5da94ba9e2ca6c0a3913d6e66b27b67fcb80d5ff6e0f70a0849cd1ec` (.vibe/handoffs/HANDOFF-alr-compose-localization-20260908.json)
- `HANDOFF-alr-domain-data-components-20260908` — `d82de547b4abb2b6eb8341d5d3b8bcaed4212874eafcbb870141dd26e91eee9f` (.vibe/handoffs/HANDOFF-alr-domain-data-components-20260908.json)
- `HANDOFF-assets-production-inventory-20260908` — `39755a403e0da8e926a3d5ec4fc3e533e21fa95c96a5e968ffde337d9b28c706` (.vibe/handoffs/HANDOFF-assets-production-inventory-20260908.json)
- `HANDOFF-visual-responsive-production-20260908` — `fe44aa7ba2c402297cb7995ad2768dda6ed1c402c82a8aa7e6e6b6c184a138eb` (.vibe/handoffs/HANDOFF-visual-responsive-production-20260908.json)
- `HANDOFF-release-architecture-nochange-20260908` — `2a097ed64b080f302ce2d02d5bd63d7467f134eedd645abaa19b04069ea0a10b` (.vibe/handoffs/HANDOFF-release-architecture-nochange-20260908.json)
- `HANDOFF-release-architecture-v2-nochange-20260908` — `a82b67d1bd34bd5d8107eb712e9e59822c078a3fbe3bef852ceacdee44d9c2d7` (.vibe/handoffs/HANDOFF-release-architecture-v2-nochange-20260908.json)
- `HANDOFF-release-architecture-convergence-v3-20260908` — `516a7d820ba4bbfb398359e3450c8cba3b766f5b193f67636d186f37a833a6ba` (.vibe/handoffs/HANDOFF-release-architecture-convergence-v3-20260908.json)
- `HANDOFF-privacy-release-platform-convergence-20260908` — `0d9cc56a9b8671e1fd1c8436172a4b71445ae944daf5976d54c8ff9fa475e8a0` (.vibe/handoffs/HANDOFF-privacy-release-platform-convergence-20260908.json)
- `HANDOFF-detekt-production-remediation-20260908` — `3cfed4d0f45f3c29da8a9f8f8b549443942b61f7767f720f262b796b3d6069c2` (.vibe/handoffs/HANDOFF-detekt-production-remediation-20260908.json)
- `HANDOFF-detekt-semantic-policy-20260908` — `9a59d4259e3c22ed0622d05f22d30b4ba956df00cef351e57c56e6971dc806aa` (.vibe/handoffs/HANDOFF-detekt-semantic-policy-20260908.json)
- `HANDOFF-full-local-regression-fix-20260908` — `8085c7842578792e02f655a29ec01468f33e425e3a3b3ead139c317539df649e` (.vibe/handoffs/HANDOFF-full-local-regression-fix-20260908.json)
- `HANDOFF-kover-threshold-recovery-20260908` — `01d0b0ed124bffdfd028bfcdf342c9fb494cfc67d6f2918a4d11d0ecc62f8b26` (.vibe/handoffs/HANDOFF-kover-threshold-recovery-20260908.json)
- `HANDOFF-android-fragment-lint-20260908` — `b4dc1806fe657789c2d6c7ae632d77ec3a7c5f8b9b83f47a68251b1de23d41ea` (.vibe/handoffs/HANDOFF-android-fragment-lint-20260908.json)
- `HANDOFF-release-artifact-validator-20260908` — `8f02267768ddb736408d5b645235bf28a68f5fdd573a44f12caf7cdace190b05` (.vibe/handoffs/HANDOFF-release-artifact-validator-20260908.json)
- `HANDOFF-final-compose-ui-audit-final-20260909` — `295f09e51099b1ad49156860f144e71f2d6bd962a44aa1b0a8842fc38149f204` (.vibe/handoffs/HANDOFF-final-compose-ui-audit-final-20260909.json)
- `HANDOFF-final-product-design-review-20260909` — `0f7bc8def049099bc8ac77c399e2a002d546677f9b97af12618f724652ca7c61` (.vibe/handoffs/HANDOFF-final-product-design-review-20260909.json)
- `HANDOFF-final-product-design-review-corrected-20260909` — `c05f9de2f0b46468388dfe0f4082f92fb4b2f01d66082c414f21e8b2804c416d` (.vibe/handoffs/HANDOFF-final-product-design-review-corrected-20260909.json)
- `HANDOFF-final-ui-corrections-20260909` — `0b1b765c1538988cd16a2aba9b55f6010d41e0fd9d69c541dde61c0eb3204826` (.vibe/handoffs/HANDOFF-final-ui-corrections-20260909.json)
- `HANDOFF-final-visual-evidence-refresh-20260909` — `22b3855a12e0d9f0162d68566a226a1a6040a92a3f7462fb9e030204b168cfc9` (.vibe/handoffs/HANDOFF-final-visual-evidence-refresh-20260909.json)
- `HANDOFF-final-compose-ui-audit-20260909` — `1594943c9bb146173117c2e3ca62a1b7f1a363fca530d3127b82e58ae264bd98` (.vibe/handoffs/HANDOFF-final-compose-ui-audit-20260909.json)
- `HANDOFF-final-compose-ui-audit-refresh-20260909` — `81a13d2594ec29e9580ee56f04cfe0a077a3230babfa60f6b9deaa2a54846244` (.vibe/handoffs/HANDOFF-final-compose-ui-audit-refresh-20260909.json)
- `HANDOFF-final-compose-ui-audit-supersede-20260909` — `b822276534d724fe60ef0888cd9735c8bf6f0b751d4773737dbc03e6f5841ad4` (.vibe/handoffs/HANDOFF-final-compose-ui-audit-supersede-20260909.json)

## Closure bindings

- Closure manifest: `none`
- Audit request: `none`
- Closure audit: `none`
