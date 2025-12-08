# SlayerScape RuneLite Plugin

SlayerScape is a unique game mode plugin for Old School RuneScape (OSRS) that locks the entire world behind a "Fog of War". You must complete randomly generated Slayer tasks to unlock adjacent tiles and progress through the game.

## Features

*   **Fog of War**: The world is hidden. You start with only the center tile unlocked.
*   **Task System**: Tiles contain tasks ranging from Skilling (e.g., "Mining Level 5"), Combat (e.g., "Kill 10 Cows"), to Quests.
*   **Key System**: Completing tasks and leveling up skills awards you "Slayer Keys".
*   **Progression**: Spend keys to unlock adjacent tiles and reveal more of the world.

## How to Run Locally (Development)

The easiest way to run the plugin for development is using IntelliJ IDEA.

1.  Open this project in **IntelliJ IDEA**.
2.  Locate the test file: `src/test/java/com/example/slayerscape/SlayerScapePluginTest.java`.
3.  Right-click the file or the `main` method and select **Run 'SlayerScapePluginTest.main()'**.
4.  The RuneLite client will launch with the SlayerScape plugin enabled.

## How to Build

To build the plugin JAR file for distribution or side-loading:

1.  Open a terminal in the project root.
2.  Run the build command:
    *   **Windows**: `.\gradlew.bat build`
    *   **Linux/Mac**: `./gradlew build`
3.  The compiled JAR file will be located in `build/libs/`.

## Installation

### Side-Loading (Local)
1.  Build the plugin using the steps above.
2.  Enable "External Plugin Manager" in your RuneLite settings (if available) or follow the [RuneLite Wiki guides](https://github.com/runelite/runelite/wiki) for loading local plugins.
    *   *Note: Standard RuneLite clients may require Developer Mode or a custom build to load local JARs easily.*

### Official Release (Plugin Hub)
To make this plugin available to all RuneLite users:
1.  Fork the [RuneLite Plugin Hub](https://github.com/runelite/plugin-hub) repository.
2.  Edit `plugins.properties` to include this plugin's repository URL and commit hash.
3.  Submit a Pull Request to the Plugin Hub repository.
4.  Once accepted, the plugin will appear in the "Plugin Hub" panel within the RuneLite client.
