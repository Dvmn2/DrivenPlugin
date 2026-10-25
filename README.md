# DrivenPlugin

Paper plugin that simulates simple physics for marker entities (`Interaction`): mass, medium density, constant
thrust and velocity in local and world coordinates. It is meant for vehicles such as submarines that are driven
by command blocks and datapacks. No companion mod.

## Requirements
- Minecraft 1.21.11, Paper, Java 21
- No other plugins or mods

## Build and run
```bash
./gradlew build      # build/libs/DrivenPlugin-<version>.jar
./gradlew runServer  # Paper 1.21.11 test server
```

The Gradle wrapper is not part of the repository snapshot; generate it once with `gradle wrapper`.

## Architecture
```text
net.dvmn2.drivenplugin
├── DrivenPlugin.java       composition root
├── Lang.java               localisation (RU / EN, MiniMessage)
├── Permissions.java        permission constants
├── PluginKeys.java         persistent data keys
├── DrivenSettings.java     config snapshot
├── DrivenManager.java      runtime state and physics
├── DrivenListener.java     entity load / unload / remove events
├── command/                DrivenCommand
├── model/                  DrivenData
├── storage/                DrivenDataStore
└── util/                   LocalFrame, Numbers
```

| Class | Responsibility |
|---|---|
| `DrivenPlugin` | wiring, enable/disable order, `reloadAll()` |
| `DrivenSettings` | validated, immutable snapshot of `config.yml`; invalid values log a warning and use the default |
| `DrivenManager` | owns the state of all loaded driven entities, runs the physics step and the periodic save |
| `DrivenListener` | adopts entities on load, writes and forgets them on unload and removal |
| `DrivenCommand` | the `/driven` Brigadier tree |
| `DrivenData` | mutable physical state of one entity (mass, density, local force, global force, velocity) |
| `DrivenDataStore` | reads and writes `DrivenData` from and to the entity's `PersistentDataContainer` |
| `LocalFrame` | converts vectors between entity-local and world coordinates |

Threading: every class runs on the main server thread. There is no asynchronous code; state is stored in the
entity's persistent data, which is Bukkit API and therefore main-thread only.

Lifecycle:

- Enable: `saveDefaultConfig()` -> settings -> language -> store and manager -> `manager.load()` (adopts already
  loaded entities) -> listener -> command -> `manager.start()` (physics and save tasks) -> one `info` line.
- Disable: cancel tasks -> write all changed state synchronously -> clear.
- Reload (`/driven reload`): re-reads the config, rebuilds the settings snapshot, updates the language and
  reschedules the tasks. Entity state is kept.

Physics step, run every `physics.tick-period-ticks`:

```text
F    = F_global + toWorld(F_local) - density * velocity
v   += F / mass
pos += v
```

Gravity is not applied. One step is applied per run regardless of the period, so a larger period gives larger,
less frequent jumps instead of scaling the step. Entities with zero forces and zero velocity are skipped.

The local basis is `x = forward` (yaw and pitch), `y = up`, `z = right`, with roll fixed at zero. See `LocalFrame`.

## Public API
None.

## Protocol
None.

## Data and files
There are no data files. State lives in the persistent data of each driven entity (namespace `driven`):

| Key | Type | Meaning |
|---|---|---|
| `driven` | byte | marker, `1` on every entity owned by the plugin |
| `data_version` | int | layout version, currently `1` |
| `mass`, `density` | double | physical parameters |
| `force_local_x`, `force_local_y`, `force_local_z` | double | thrust in entity-local coordinates |
| `force_global_x`, `force_global_y`, `force_global_z` | double | thrust in world coordinates |
| `velocity_x`, `velocity_y`, `velocity_z` | double | velocity in world coordinates, blocks per physics step |

A driven entity is an `Interaction` with the scoreboard tag `driven`. Missing values get defaults; non-finite or
out-of-range values are replaced by defaults with a warning. Entities created before `data_version` existed use
the same layout and need no migration.

Persistence: the in-memory state is written back every `settings.save-interval-seconds`, when the entity's chunk
unloads and on disable. After a crash, at most one save interval of velocity and force changes is lost; the
entity position is kept by the normal chunk save.

## Conventions and constraints
- Driven entities are persistent by design, so the "remove orphaned entities on load" rule does not apply:
  marked entities found on load are adopted into the manager instead of being deleted.
- An `Interaction` that was tagged `driven` by hand (for example with `/tag`) is adopted the first time a
  `/driven` command selects it.
- The manager copies `Vector` values on the way in and out of `DrivenData`; mutate state only through its setters
  and call `DrivenManager#markDirty` afterwards, otherwise the change is not saved.
- `DrivenManager#stepAll` iterates over a snapshot of the tracked entities, because teleporting may load a chunk,
  which adopts further entities while the step is running.
- If a step produces a non-finite velocity, forces and velocity are reset and one warning is logged; teleporting
  to a non-finite position would otherwise throw on every tick.
- `/driven <targets> get` returns an integer so it can feed `/execute store result`; velocity results are
  multiplied by `physics.get-scale`.

## License
All Rights Reserved.
