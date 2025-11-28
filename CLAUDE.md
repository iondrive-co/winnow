# Winnow Project Context

## In a new session before making changes

Load the `Readme.md` into context so you understand the project

## While making changes

Replace unnecessarily verbose code with more terse statements. 
Use the latest java features up to java 24. 
Remove unnecessary comments and use better variable naming where needed. 
Ensure all variables that can be final are.

If functionality is modified or added:
- Write comprehensive JUnit tests, using assertJ where needed.
- Write or modify at least one integration test which displays the actual window on screen

## After Making Changes

Compile the project and make sure all tests pass. If a test is failing and you can't work out why, don't proceed and
don't remove it unless you can provide a clear justification why it is no longer needed for me to review.

Once tests are passing:
Update the **Classes** section in `Readme.md` if component responsibilities change.
Update the **Design Decisions** section if architectural patterns or key strategies change
Only if issues were encountered which required user interaction to correct, then update the .claude.md with something
that fixes any problems encountered automatically in the future.
Keep descriptions concise for all of these.

When finished, do not commit changes to git, I will review them and commit myself

## Java Platform Module System (JPMS)

This project uses JPMS modules. Non-modular dependencies (Clojure, commons-io) are accessed via `--add-reads org.win=ALL-UNNAMED`:
- **Compilation**: Already configured in `build.gradle` tasks.withType(JavaCompile)
- **Runtime**: Already configured in `build.gradle` application.applicationDefaultJvmArgs
- **IntelliJ**: Already configured in `.idea/runConfigurations/Main.xml`
  When adding new non-modular dependencies that are accessed from module code, both compile and runtime flags are already in place.
- 
# Coordinate Systems and Rendering

When working with JavaFX Canvas:
- **Canvas coordinates**: The underlying image/canvas coordinate system (e.g., 800x600 pixels in image)
- **Screen coordinates**: What appears on screen after scale transform (e.g., 800 canvas px × 2x scale = 1600 screen px)
- **GraphicsContext draws in canvas space**: When you call `gc.fillRect(100, 100, 200, 200)`, you're drawing in canvas coordinates. The scale transform (via `setScaleX/setScaleY`) converts this to screen space.
- **Mouse events use screen coordinates**: When handling mouse events, convert to canvas coordinates by dividing by scale: `canvasX = event.getX() / getScaleX()`

# JavaFX Testing Patterns

When writing tests that use JavaFX Platform.runLater():
- **Avoid deeply nested Platform.runLater() calls** - They create a queue on the JavaFX thread that can cause timeouts when many tests run together
- **Use sequential Platform.runLater() with separate CountDownLatches** - Instead of nesting callbacks, use separate runLater calls with individual latches
- **Use a waitForFXThread() helper** - Create a helper method that waits for the JavaFX thread to process all pending tasks
- **Build configuration**: The test task in build.gradle uses `forkEvery = 1` to spawn a fresh JVM for each test class, preventing JavaFX thread exhaustion across test suites

## Running Tests

### Gradle (Recommended)
```bash
# Run all tests
./gradlew test

# Force re-run tests (ignore up-to-date checks)
./gradlew test --rerun-tasks

# Run specific test class
./gradlew test --tests "*ZoomVisualVerificationTest"

# Run with clean
./gradlew clean test
```

### IntelliJ IDEA
IntelliJ requires special configuration to match Gradle's fork behavior. Use one of these options:

**Option 1: Use the pre-configured run configuration (Easiest)**
- A run configuration "All Tests (fork per class)" is provided in `.idea/runConfigurations/`
- Select it from the run configurations dropdown and run tests with it

**Option 2: Manually configure test template**
1. Run → Edit Configurations → Edit configuration templates → JUnit
2. Set "Fork mode" to "class"
3. This applies to all new test runs

**Option 3: Run tests via Gradle in IntelliJ**
- Use IntelliJ's Gradle tool window to run tests instead of JUnit runner
- This uses Gradle's configuration automatically

### GitHub Actions / CI
- Linux builds use `xvfb-run` to provide a virtual display for JavaFX tests
- See `.github/workflows/build-release.yml` for configuration