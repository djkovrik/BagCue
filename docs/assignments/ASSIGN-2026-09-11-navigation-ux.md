# Assignment: navigation and launch UX fixes

User request captured on 2026-09-11:

1. Match the Android launch/loading background to BagCue's active light or dark app background.
2. Add directional slide transitions between the four primary Decompose destinations. Direction must follow destination order for both directions, including Today → Settings and Settings → Today. Use scale plus fade for child components that are not primary tabs and open over the app.
3. Center the `No sessions yet` empty-state placeholder on the Sessions screen.
4. Add a simple animated response for the selected indicator in the compact bottom navigation bar.

Constraints:

- Preserve the existing Decompose stack, selected destination, restoration, and back behavior.
- Respect system reduced-motion settings.
- Keep the Android launch colors equal to the Compose theme background roles.
- Preserve EN/RU resources and the existing AppSpec.
- Scope implementation to Android launch resources, shared Compose UI, and focused tests/goldens required by the change.

Relevant obligations: AC-026, AC-047, AC-049, QG-001, QG-002.
