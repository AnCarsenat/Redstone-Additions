# Design note: Minecart Speed Upgrade (#169)

> **Dropped (2026-11-11).** The maintainer decided not to build this: 26.3 caps every minecart
> at 8 blocks per second, so it could not make a cart faster than vanilla. Kept for reference.

> **Speed upgrade**: increases a minecart's speed. To design first: a note in `planning/design/`
> with the options, then the maintainer decides. (#169)

Status: proposal. Nothing here is built; the Ram Upgrade and the Rail Builder from the same issue
are. Implementation waits for the maintainer's choice.

## The question

An upgrade applied to one cart, the way the Ram Upgrade is (sneak and use it on the cart, or use it
while riding; the Wrench takes it off; breaking the cart drops it), that makes that cart faster.
What can "faster" mean in 26.3, per cart, without breaking rails, the Route Rail, or the tick budget?

## What 26.3 gives a data pack (checked)

- **The vanilla speed limit is 8 blocks per second per axis** (0.4 blocks per tick). On rails the
  game clamps the cart's velocity to that limit every tick. A larger `Motion` written by command is
  clamped the next tick, so a pack cannot raise one cart's top speed by writing `Motion`
  ([minecart-speed-setter.md](minecart-speed-setter.md), "What 26.3 allows").
- **`Motion` is writable** on every minecart (`execute store result entity @s Motion[0] double ...`).
  Writing a speed up to the limit works, and it is what the Speed Setter design (#97, O2) relies on.
- **Minecarts have no attributes.** They are not living entities: no `movement_speed`, no
  `attribute` command, no effect (Speed does nothing to a vehicle).
- **The `max_minecart_speed` game rule** (renamed from `minecartMaxSpeed` in 1.21.11; 1 to 1000,
  default 8) exists only with the **Minecart Improvements** experiment, which is still experimental
  in 26.3 and can only be switched on when a world is created. It is **global**: every cart in the
  world gets the same limit. A function that names the rule fails to load in a world without the
  experiment, so the pack must never write it (see the #97 note).
- **The experiment's physics** keep carts on the rail at high speed and turn them smoothly round
  curves; without it, a cart faster than about 0.4 b/t can leave the track on a curve or a slope.
  Built-in air resistance still slows a cart, so even with a high limit a cart only goes as fast
  as powered rails or slopes push it.
- **Furnace minecarts** have their own push (`Fuel`, `PushX`, `PushZ`) and a lower top speed; they
  are not a way to make other carts faster.

## Options

### S1: Cruise (keep the cart at the limit, per cart)

While the cart moves on a straight rail (or a slope), each tick it is pushed back to the game's
limit along the rail axis, in its own direction of travel. In a normal world that is 8 b/s: the cart
never slows down on flat track or uphill, as if every rail were a powered rail. In a world with the
experiment it is pushed to `max_minecart_speed`, through a GLOBAL setting the admin sets to the
same number (the pack cannot read the rule).

- Per cart: yes. Only upgraded carts change; the others behave as before.
- Faster than vanilla: no in a normal world. What it gives is "no powered rails needed and no
  slowing down", which is the common reason players want speed. With the experiment, yes, up to the
  rule.
- Derailing: at 8 b/s none, it is the vanilla speed. Curves are skipped (no push on a curved rail),
  so the cart takes them at whatever speed it has, as vanilla does.
- Per-tick cost: one selector for upgraded carts (tag), two `data get` on each, one write when it
  is below the limit. Nothing for carts without the upgrade. The same order as the Ram Upgrade.
- Multiplayer: the server writes the velocity; riders see no jitter (no teleport). No setting per
  player.
- Interplay: the Route Rail looks REACH = 2 blocks ahead; at 8 b/s a cart is always seen in time.
  With the experiment and a high cruise speed, the Route Rail's reach must grow with the speed
  (a cart at 40 b/s covers 2 blocks per tick).

### S2: Teleport boost (move the cart further each tick, per cart)

Each tick, teleport the upgraded cart a little further along its rail, beyond what the game moves it.

- Faster than vanilla in any world, per cart.
- Rejected for an upgrade: the pack would follow the rails itself (curves, slopes, junctions,
  detector and activator rails, the Route Rail), carts would skip over blocks that act on a cart
  passing over them, riders' cameras would jitter, and collisions are skipped. The cost per moving
  cart per tick is higher than S1, and a skipped detector rail is a broken build.

### S3: Change the global limit (`max_minecart_speed`)

An item or a block that sets the game rule.

- Rejected: global (not an upgrade), experimental only, and the function would not load in a
  normal world.

### S4: Per-cart limit under the experiment (brake the others)

The admin raises `max_minecart_speed`; the pack brakes every cart WITHOUT the upgrade back to 8 b/s.

- Per cart in effect, faster than vanilla for upgraded carts.
- Rejected: it costs every tick for every cart in the world, not just upgraded ones, and it still
  needs the experiment.

### S5: A Speed Setter block instead (#97)

[minecart-speed-setter.md](minecart-speed-setter.md) recommends a block under a rail that sets a
passing cart's speed (0 to 8 b/s, set / brake only / boost only). That is a place on the track; the
upgrade is a property of a cart. They share the code that reads the rail axis from the rail's
`shape` and writes `Motion` along it, keeping the sign of travel.

## How it relates to #97

- The Speed Setter decides speed at a point (a station brake, a launch, a slow scenic section).
  The Speed Upgrade decides it for one cart everywhere (a fast express cart on a line where the
  others are slow). Both are capped by the same game limit.
- Build the shared part once: a function "as a cart on a straight rail, write speed V along the
  rail axis in the travel direction" (axis from `shape`, ascending shapes mapped to their axis,
  curves skipped). The Setter calls it with its property; the upgrade with the limit (or the
  admin's cruise setting).
- If both exist, the Setter wins where it stands (it runs at that block; the cruise push skips a
  cart standing on a Speed Setter, or the Setter's "brake only" would be undone the next tick).

## Recommendation

**Build S1, the Cruise Upgrade**, and call it what it does: "keeps the cart at full speed". In a
normal world it holds 8 b/s on flat track and uphill with no powered rails; in a world created with
Minecart Improvements, an admin setting `cart_cruise_speed` (GLOBAL, default 8, max 1000, read with
a fail-open default) lets it hold the speed of the world's `max_minecart_speed`. Build it together
with the #97 Speed Setter so the axis-and-sign code exists once. Do not promise "faster than
vanilla" in a normal world: the game does not allow it, and S2 to S4 cost more than they give.

### Implementation sketch (when chosen)

- Item: clickable like the Ram Upgrade, `custom_data {ra:{speed_upgrade:1b}}`; cart tag
  `ra.rw.speed`; the cart tracker of the Ram Upgrade already drops it when the cart breaks and the
  Wrench already removes upgrades, so it is one more tag in those lists.
- Tick: `as @e[type=#ra_railworks:minecarts,tag=ra.rw.speed] at @s`, skip unless on a straight or
  ascending rail and moving (|v| > 0.01), read the axis speed, write the limit along the axis with
  the current sign when it is lower.
- Docs: Railworks page, item reference, recipe atlas; changelog.
