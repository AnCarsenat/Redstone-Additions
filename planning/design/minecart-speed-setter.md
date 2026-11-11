# Design note: Minecart Speed Setter feasibility (#97)

> **Dropped (2026-11-11).** The maintainer decided not to build this: 26.3 caps every minecart
> at 8 blocks per second, so it could not make a cart faster than vanilla. Kept for reference.

> **Minecart Speed Setter**: sets the speed of a minecart passing over it when triggered.
> **(to check)**: may not be feasible. (planning/v6.0.3-ideas.md, Railworks)

Status: proposal. Nothing here is built yet; implementation waits for the maintainer's choice.

## The question

Can a data pack set the speed of a minecart in Minecraft 26.3, and what does "set" mean given the
game's own speed limit? Railworks is a new module; this would be its first block next to the
Route Rail.

## What 26.3 allows (checked)

- **`Motion` is writable on minecarts.** The 26.3 entity schema (`minecraft:entity[minecart]`)
  still has `Motion: [double, double, double]` from the common entity base, in blocks per tick.
  `execute as <cart> store result entity @s Motion[0] double 0.01 run scoreboard players get …`
  parses for 26.3 (`validate_command`). Minecarts are not players, so `/data` works on them, and the
  server sends the new velocity to clients.
- **The default speed limit is 8 blocks per second per axis** (0.4 blocks per tick; diagonal travel
  up to ~11.3). On rails, the game clamps the cart's velocity to that limit every tick. So writing
  a larger `Motion` has no lasting effect: the cart is back to 8 b/s the next tick. Writing a
  **smaller** value works: the cart slows down (and then keeps slowing with friction, or speeds up
  again on powered rails).
- **The rail re-projects the velocity.** On a straight rail the velocity is turned along the rail
  axis each tick, so the setter must write the axis the rail runs along (X for `east_west`, Z for
  `north_south`) and zero the other, keeping the sign of the current travel direction.
- **Faster carts need the "Minecart Improvements" experiment.** It adds the
  `max_minecart_speed` game rule (default 8, range 1–1000 b/s; renamed from `minecartMaxSpeed` in
  1.21.11). It is still experimental in 26.3 and an experiment can only be switched on when a
  world is created; a data pack cannot turn it on in an existing world. The rule is also not a
  guarantee: the wiki notes built-in air resistance means slopes or powered rails still have to
  push the cart up to it.
- **Never write `gamerule max_minecart_speed` in a function.** In a world without the experiment
  the rule does not exist, the line does not parse, and the whole function fails to load (the same
  class of failure as gotchas.md's macro lines). The pack cannot safely test for the experiment
  from a function either; it would need an admin setting saying "this world has it".
- **Powered rails** already do "boost to max" and (unpowered) "brake to a stop". The vanilla
  toolkit has no "go exactly this speed" block, which is the gap this block would fill.

**Conclusion: feasible, within limits.** A pack can set any speed from 0 up to the game's maximum
(8 b/s in a normal world), in either direction along a straight rail. It cannot make carts go
faster than the game allows.

## Options

### O1: drop it

- Player experience: nothing new; powered rails already cover boost and stop.
- Cost / complexity: none.
- Loses: exact speeds (slow scenic lines, station approach, timing for redstone contraptions),
  setting a cart's direction, and a working first block for Railworks.

### O2: a block under the rail that writes `Motion` while powered (recommended)

The Speed Setter is a custom block that a rail sits on (like a detector under a track). While it
receives redstone, each tick it looks for carts in the block space above it and writes their
velocity.

- Settings (property schema): `speed` (number, 0–80, in tenths of a block per second, so 0.0 to
  8.0 b/s), `mode` (list: set exactly / at most (brake only) / at least (boost only)), `direction`
  (list: keep travel direction / towards facing / away from facing). A stopped cart (`|v|` under
  0.01) uses the facing, so the block can also **launch** carts from a station.
- Player experience: an exact, predictable speed on demand; works with ridden, chest, hopper and
  furnace carts. Readable on the Goggles (`4.0 b/s, brake only`).
- Cost: only while powered; idle it costs nothing beyond the usual break check. Powered: one
  selector per block per tick (`positioned ~ ~1 ~` and `dx=0,dy=0,dz=0` for the cart types). A cart
  at 8 b/s moves 0.4 blocks per tick, so it spends at least two ticks over the block and cannot be
  missed.
- Complexity: low-medium. Reading the rail's `shape` state picks the axis (generated `if block ~ ~1
  ~ #minecraft:rails[shape=north_south]` branches; ascending shapes map to their axis). Curves are
  skipped, or treated by the axis with the larger current velocity.
- Safety: no player text anywhere. `speed` is an integer property scaled through `store result …
  double 0.005` (tenths of b/s → blocks per tick: × 0.1 / 20), so nothing typed reaches a command.
- Limits to document: the maximum is the game's (8 b/s); a set speed decays with friction after
  the block, as any cart's does; in water the vanilla limit is lower.

### O3: the same block, also raising the limit with the experiment

O2 plus, when an admin setting says the world has Minecart Improvements, allow `speed` up to the
admin's chosen maximum (matching their `max_minecart_speed`).

- Player experience: fast lines on servers that opted into the experiment.
- Cost: as O2. At high speeds the selector box must grow with the speed (a cart at 40 b/s moves 2
  blocks per tick and can skip over a one-block detector).
- Complexity: an extra GLOBAL setting; the pack must still never write the game rule itself.
- Note: the experiment changes cart physics (smoother turns, collisions on slopes); its `Motion`
  handling has to be tried in such a world before relying on it. This can be a follow-up.

### O4: set the global speed limit (`max_minecart_speed`)

A block that changes the game rule when triggered.

- Rejected: it is global (every cart in the world), experimental-only, and the function would fail
  to load in normal worlds (see above).

### O5: move carts ourselves (teleport along the track)

Take over cart movement: teleport the cart forward each tick by the wanted distance.

- Rejected: rider camera jitter, collisions and rail following would all be reimplemented, and it
  costs per tick per moving cart.

### O6: swap powered rails on and off

Toggle powered rails near the block to approximate speeds.

- Rejected: coarse (boost to max or stop), and it rewrites blocks the player placed.

## Recommendation

**Build O2** as the Railworks module's first block: the **Minecart Speed Setter**, a block placed
under a rail that, while powered, sets passing carts to an exact speed from 0 to 8 b/s, with
set / brake-only / boost-only modes and keep / towards-facing / away-from-facing direction. Leave
O3 (experiment speeds) as a later, admin-enabled extension. Do not drop it: the one real limit (no
speed above the game's maximum) is easy to explain and the block is still useful within it.

### Implementation sketch

- `modules/railworks/` (new module, its own page in docs and settings like the other modules):
  `SpeedSetter.kt` with `block(...)`, recipe (iron, redstone, powered rail), give, goggles readout.
- Properties: `number("speed", min = 0, max = 80, default = 40)`,
  `list`-style `mode` and `direction` cycled with the Wrench and shown in the Data Handler.
- Tick (`block.tick`): `breakCheck`, `detectSwitch`, then
  `as markers tagged powered at @s positioned ~ ~1 ~ as @e[type=#ra_railworks:minecarts,dx=0,dy=0,dz=0]`
  run `apply`. An entity type tag lists the cart types (minecart, chest, furnace, hopper, TNT,
  command block).
- `apply`, as the cart, with the setter's speed in a score:
  1. read the current velocity along the rail axis into a score (`data get entity @s Motion[2]
     1000`);
  2. pick the sign: current sign for "keep", the marker's facing for the other two or when the cart
     is stopped;
  3. mode check: brake-only returns if the current speed is already lower; boost-only if higher;
  4. write the axis with `store result entity @s Motion[<axis>] double 0.005` and zero the other
     horizontal axis. Two generated variants (X and Z), chosen from the rail `shape`; no macro.
- No idle cost: everything is behind the `powered` tag. Particles (a small `electric_spark` on
  change) behind `ra.u.par`, a soft sound behind `ra.u.snd`.
- docs: a Railworks page, item reference and recipe atlas rows, uninstall and refunds like the other
  modules.
