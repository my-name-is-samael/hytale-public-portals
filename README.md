# IntelliJ plugins

- [Hytale Development Tools](https://plugins.jetbrains.com/plugin/29807-hytale-development-tools)
- [Hytale .ui Support](https://plugins.jetbrains.com/plugin/29783-hytale-ui-support)

# Environment Setup

- Having a working IntelliJ setup with JDK/SDK 25
- Copy the server jar from `%game_path%/install/release/package/game/latest/Server/HytaleServer.jar` to
  `./local-repo/com/hypixel/hytale/HytaleServer-parent/1.0-SNAPSHOT/HytaleServer-parent-1.0-SNAPSHOT.jar` (for gradle
  source compilation)
- Set your Hytale server path inside `build.gradle` (line 11) for the builder to deploy it automatically
- Set up you run configurations (screenshots present in the `dev` folder)
- Configure gradle to run with the SDK/JDK 25 if this is not already done (File > Settings > Build, Execution,
  Deployment >
  Build Tools > Gradle > Gradle Projects > Gradle JVM = 25)

# Run Configuration Context

## Build

- Will build the project to a jar, placed under `./build/libs` and in your server's `mods` folder.
- Other versions of the mod will be removed from your server's `mods` folder.
- The generated jar will be automatically named after the project name and version set in the
  `./src/main/resources/manifest.json` file (the hytale mod description file)

## Hytale Server

- Will execute the build configuration first (just above).
- The server will be launched automatically once the deployment is done.
- During the first launch, the server will need you to authenticate your account then it will be saved for the next
  launches.
- When stopping or restarting the run configuration, the server will shutdown automatically and properly.