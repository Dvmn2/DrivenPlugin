# DrivenPlugin

Paper plugin that adds simple physics to marker entities on your server: mass, medium density, thrust and
velocity in local and world coordinates. It is designed for vehicles such as submarines controlled by command
blocks and datapacks.

## Features
- Driven entities with configurable mass and medium density (drag).
- Constant thrust and velocity in the entity's own coordinates (forward, up, right) or in world coordinates.
- Rotation controlled with commands; roll is not simulated.
- Integer `get` queries that work with `/execute store result`.
- Entity state survives chunk unloads and server restarts.
- Messages in Russian and English.

## Requirements
- Paper 1.21.11, Java 21
- No other plugins required

## Installation
1. Put `DrivenPlugin-<version>.jar` into the `plugins` folder.
2. Start the server; `plugins/DrivenPlugin/config.yml` is created.

## Commands
Targets are normal entity selectors; only driven entities (an `Interaction` with the tag `driven`) are affected.

| Command | Permission | Description |
|---|---|---|
| `/driven create` | `driven.use` | Create a driven entity at the executor's position |
| `/driven reload` | `driven.reload` | Reload `config.yml` |
| `/driven <targets> remove` | `driven.use` | Remove the entities |
| `/driven <targets> stop` | `driven.use` | Reset both forces and the velocity |
| `/driven <targets> <mode> mass <value>` | `driven.use` | Set or add mass (the result must be at least `0.0001`) |
| `/driven <targets> <mode> density <value>` | `driven.use` | Set or add medium density (the result must not be negative) |
| `/driven <targets> <mode> rotation vector <yaw> <pitch>` | `driven.use` | Set or add yaw and pitch |
| `/driven <targets> <mode> rotation yaw <value>` | `driven.use` | Set or add yaw |
| `/driven <targets> <mode> rotation pitch <value>` | `driven.use` | Set or add pitch (the result must be in `-90..90`) |
| `/driven <targets> <mode> <quantity> <frame> vector <x> <y> <z>` | `driven.use` | Set or add a whole vector |
| `/driven <targets> <mode> <quantity> <frame> <axis> <value>` | `driven.use` | Set or add one component |
| `/driven <targets> get <property>` | `driven.use` | Return one value as an integer; needs exactly one driven target |

Argument values:

| Argument | Values |
|---|---|
| `<mode>` | `set`, `add` |
| `<quantity>` | `force`, `velocity` |
| `<frame>` | `local` (x = forward, y = up, z = right), `global` (world axes) |
| `<axis>` | `x`, `y`, `z` |

Properties for `get`:

| Property | Description |
|---|---|
| `local_velocity_x`, `local_velocity_y`, `local_velocity_z` | Velocity in the entity's coordinates, multiplied by `physics.get-scale` |
| `global_velocity_x`, `global_velocity_y`, `global_velocity_z` | Velocity in world coordinates, multiplied by `physics.get-scale` |
| `local_force_x`, `local_force_y`, `local_force_z` | Local thrust as stored |
| `global_force_x`, `global_force_y`, `global_force_z` | World thrust as stored |
| `speed` | Speed, multiplied by `physics.get-scale` |
| `mass`, `density` | Physical parameters |
| `yaw`, `pitch` | Entity rotation in degrees |
| `azimuth` | Compass heading, `0..359` |
| `slant` | Negated pitch, positive when the nose points up |

Example:

```text
/execute as @e[tag=driven,limit=1] store result score #speed sub run driven @s get speed
```

## Permissions
| Permission | Default | Description |
|---|---|---|
| `driven.use` | op | Use `/driven` |
| `driven.reload` | op | Use `/driven reload` |
| `driven.admin` | op | All administrative Driven permissions |

## Configuration
File: `plugins/DrivenPlugin/config.yml`

| Key | Type | Default | Range | Description |
|---|---|---|---|---|
| `settings.language` | string | `auto` | `ru`, `en`, `auto` | Message language; `auto` uses the player's client locale and English for the console |
| `settings.save-interval-seconds` | int | `30` | `5..3600` | How often changed state is written to the entity |
| `physics.default-mass` | number | `1000.0` | `0.0001..1000000000` | Mass of newly created entities |
| `physics.default-density` | number | `1.0` | `0.0..1000000000` | Medium density of newly created entities |
| `physics.tick-period-ticks` | int | `1` | `1..20` | Ticks between physics steps |
| `physics.get-scale` | number | `20.0` | `0.001..1000000` | Multiplier for velocity results of `get` (`20` gives blocks per second) |

Invalid values are reported in the console and replaced by the default. The physics is
`acceleration = (force - density * velocity) / mass` per step; gravity is not simulated, so give an entity a
constant global force to make it sink or float.

Example for a heavy, slow vehicle:

```yaml
physics:
  default-mass: 50000.0
  default-density: 20.0
```

## Data
The plugin creates no files besides `config.yml`. The state of each entity is stored inside the entity, so it is
included in normal world backups. To reset one entity use `/driven <targets> stop`; to delete it use
`/driven <targets> remove`. After a crash, the last `settings.save-interval-seconds` seconds of force and velocity
changes may be lost.

## Companion mod
None.

## Troubleshooting
| Problem | Solution |
|---|---|
| `/driven` is unknown or "no permission" | Grant `driven.use` (or `driven.admin`) |
| A command affects nothing | The selector must match an `Interaction` entity with the tag `driven` |
| `get` reports "exactly one driven entity" | Narrow the selector, for example with `limit=1` |
| The entity does not move | Set a force or velocity; gravity is not applied |
| The entity moves in jumps | Lower `physics.tick-period-ticks` to `1` |
| Rotation is rejected | The resulting pitch must stay in `-90..90` |

## License
All Rights Reserved.
