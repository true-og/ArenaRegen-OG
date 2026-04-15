# ArenaRegen-OG

A free and open source fork of [DE-ArenaRegen](https://github.com/Realizedd/DE-ArenaRegen) including fixes by [BoomEaro](https://github.com/BoomEaro/DE-ArenaRegen) maintained for Purpur 1.19.4 by [TrueOG Network](https://trueog.net).

## Changes from upstream

- **Chunk send throttling** — `ChunkRefreshTask` now skips chunk packets for players farther than 48 blocks (horizontal) from the chunk center, preventing needless network traffic and client-side lag during large arena resets.
- **Task logging** — `ScanBlocksTask`, `FilterBlocksTask`, `ResetBlocksTask`, `RelightBlocksTask`, and `ChunkRefreshTask` accept a `Logger` and emit start/progress messages so zone operations are visible in server logs.
- **Java 17 target** — toolchain and bytecode bumped to 17 for modern Purpur/Paper 1.19.4.
- **Kotlin DSL build** — migrated `build.gradle`/`settings.gradle` to `build.gradle.kts`/`settings.gradle.kts`; subproject `build.gradle` files collapsed into the root script.
- **Duels API** — targets the [true-og/Duels-OG](https://github.com/true-og/Duels-OG) fork; `extension.yml` pinned to `api-version: 3.4.1` with quoted `@VERSION@` placeholder so Gradle token replacement works.
- **Dropped legacy Paper NMS** — `v1_8_R3_paper/NMSHandler.java` removed; 1.19.4 is the only supported target.
