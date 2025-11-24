# Winnow Project Context

## In a new session before making changes

Load the `Readme.md` into context so you understand the project

## While making changes

Replace unnecessarily verbose code with more terse statements. 
Use the latest java features up to java 24. 
Remove unnecessary comments and use better variable naming where needed. 
Ensure all variables that can be final are.
For all changes, write comprehensive JUnit tests, using assertJ where needed.
Write or modify at least one integration test which displays the actual window on screen

## After Making Changes

Compile the project and make sure all tests pass. If a test is failing and you can't work out why, don't proceed and
don't remove it unless you can provide a clear justification why it is no longer needed for me to review.

Once tests are passing:
Update the **Classes** section in `Readme.md` if component responsibilities change.
Update the **Design Decisions** section if architectural patterns or key strategies change
Only if issues were encountered which required user interaction to correct, then update the .claude.md with something
that fixes any problems encountered automatically in the future.
Keep descriptions concise for all of these.

# Coordinate Systems and Rendering

When working with JavaFX Canvas:
- **Canvas coordinates**: The underlying image/canvas coordinate system (e.g., 800x600 pixels in image)
- **Screen coordinates**: What appears on screen after scale transform (e.g., 800 canvas px × 2x scale = 1600 screen px)
- **GraphicsContext draws in canvas space**: When you call `gc.fillRect(100, 100, 200, 200)`, you're drawing in canvas coordinates. The scale transform (via `setScaleX/setScaleY`) converts this to screen space.

# JavaFX Testing Patterns

When writing tests that use JavaFX Platform.runLater():
- **Avoid deeply nested Platform.runLater() calls** - They create a queue on the JavaFX thread that can cause timeouts when many tests run together
- **Use sequential Platform.runLater() with separate CountDownLatches** - Instead of nesting callbacks, use separate runLater calls with individual latches
- **Use a waitForFXThread() helper** - Create a helper method that waits for the JavaFX thread to process all pending tasks
- **Build configuration**: The test task in build.gradle uses `forkEvery = 1` to spawn a fresh JVM for each test class, preventing JavaFX thread exhaustion across test suites