# Additional Pipes (Legacy)

Additional Pipes for Minecraft 1.7.10 / BuildCraft 7.1.63, built with the GTNH
RetroFuturaGradle toolchain.

## Requirements

- Minecraft 1.7.10
- Forge 10.13.4.1614
- BuildCraft 7.1.63 (Transport and Silicon are required)
- Logistics Pipes is optional; the logistics teleport pipe is only registered when LP is installed.

## Pipes

- Item, fluid, power, and optional logistics teleport pipes
- Weighted item distributor
- Addition pipe (inserts only where the item already exists)
- Gravity feed pipe
- Buffered closed pipe
- Item/fluid/power redstone switch pipes
- Passive water pump pipe

## Building

Use Java 25 for Gradle (the Forge 1.7.10 run tasks use Java 8, which the build
registers automatically).

```bash
./gradlew build
./gradlew spotlessCheck
```

The output jar is in `build/libs`.

## Running

```bash
./gradlew runClient
./gradlew runServer
```

## Tests

```bash
./gradlew test
./gradlew selfTest --no-configuration-cache
```

`test` runs the JUnit suite. `selfTest` starts a disposable headless dedicated
server and verifies registration, teleport lifecycle, buffering, the water pump,
power teleport accounting, the addition pipe, commands, and manager reset.

## License

Original Additional Pipes code is MIT-licensed (see `LICENSE`). Files carrying
the BuildCraft header remain under the BuildCraft license; see `NOTICE`,
`LICENSE-BUILDCRAFT`, and `LICENSE.API`.
