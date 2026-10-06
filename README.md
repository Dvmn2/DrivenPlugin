# DrivenPlugin

A plugin for Paper 1.21.x that simulates the movement of entities (submarines, etc.)
through controlled forces/speeds in local and global coordinates.

## How it works

- A managed entity is an `Interaction` (invisible, without a model) with the scoreboard tag `driven'.
  The plugin only works with such entities. The visual model (the hull of the submarine) needs to be
  implement separately, for example, a resource pack/display entities synchronized
with the position and rotation of this `Interaction` entity outside the plugin. The `Interaction` itself
  movement and rotation are given outwards via `get`, teleport and its `Location'.
- The entity does not have vanilla gravity and physics — the entire position calculation is completely in the hands of
  the plugin: every tick, the speed and position are recalculated, then the entity is teleported. Collisions with blocks
  **are not handled** (an entity can pass through them) — this can be added separately (for example, raycast before
  teleport), was not included in the current TOR.
- Physics (simplified, linear):
- `F = F_global + (F_local rotated to world coordinates)`
    - `F_drag = -density * velocity` (medium density as a coefficient of linear friction)
- `a = (F + F_drag) / mass`
    - `velocity += a`, `position += velocity` (each tick, dt = 1 tick)
- Gravity is not applied — if it is necessary for the entity to sink/float itself, set a constant force: `set force
  global 0 <F> 0'.
- Local coordinates (for `force local`, `velocity local`, `get local_velocity_*`):
  `x` = right, `y` = local "top" (tilts with pitch, roll=0 is not supported),
  'z` = forward (the direction of the entity's gaze). That is why the ballast/engine pushes "up the nose" along the
  local Y-axis, which gives an oblique movement with a non-zero pitch.
- The state (mass, density, forces, speed) is stored in the `PersistentDataContainer`
  in essence, it is experiencing a reboot of the chunk and a server restart.

## Assembly

```
mvn package
```

Ready jar: `target/driven-1.0.0.jar `. Copy it to the `plugins/` folder of the Paper server.

> In the `pom.xml` specifies the version of the dependency `paper-api' — `1.21.11-R0.1-SNAPSHOT'.
> If such a version is not yet in the PaperMC repository at the time of the build, replace
> to the nearest available server version (`1.21.x-R0.1-SNAPSHOT`).

## Rights

- `driven.use` (by default— operator only) — access to the entire `/driven` command.

## Teams

Root: `/driven'. The target everywhere is the standard Brigadier entity selector
(`@e[tag=driven]`, `@e[tag=driven,limit=1]`, a specific UUID via `@e[...]`, etc.);
the command itself additionally filters out only entities with the `driven` tag.

| Team                                                | Description                                                                      |
|-----------------------------------------------------|----------------------------------------------------------------------------------|
| `/driven create`                                    | Creates a new driven entity at the point of the performer (player/command block) |
| `/driven <targets> remove`                          | Removes driven entities from the list of targets                                 |
| `/driven <targets> stop`                            | Resets forces (local/global) and speed                                           |
| `/driven <targets> set mass <value>`                | Sets mass                                                                        |
| `/driven <targets> set density <value>`             | Sets the density of the medium (coefficient of friction)                         |
| `/driven <targets> set rotation <yaw> <pitch>`      | Sets the rotation (pitch: -90..90)                                               |
| `/driven <targets> set force local <x> <y> <z>`     | Constant thrust in local axes (engines)                                          |
| `/driven <targets> set force global <x> <y> <z>`    | Constant thrust in global axes                                                   |
| `/driven <targets> set velocity local <x> <y> <z>`  | Instantly sets the speed in local axes (ballast-jerk)                            |
| `/driven <targets> set velocity global <x> <y> <z>` | Instantly sets the speed in world axes                                           |
| `/driven <targets> get local_velocity_x\|y\|z`      | Local speed (see below for the scale)                                            |
| `/driven <targets> get global_velocity_x\|y\|z`     | World speed                                                                      |
| `/driven <targets> get speed`                       | The speed module                                                                 |
| `/driven <targets> get mass` / `get density`        | Current mass/density                                                             |

The 'get` commands require the selector to specify **exactly one** driven entity
(otherwise it is unclear which number to return), and return an `int` result, readable through:

```
/execute store result storage movement:temp modifier int 1 run driven @e[tag=driven,limit=1] get local_velocity_x
```

## config.yml

```yaml
settings:
  language: "auto"   # ru | en | auto

physics:
  default-mass: 1000.0
  default-density: 1.0
  tick-period: 1
  get-scale: 20.0
```