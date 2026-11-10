# Farming

Blocks that tend crops and animals. The **Sprinkler** waters the farmland around it from a fluid
network, and with liquid experience makes the crops there grow about four times as fast. The
**Breeder** feeds the animals in front of it.

## Getting them

| Block | Looks like | Recipe |
| --- | --- | --- |
| Sprinkler | oxidized copper grate | ![Sprinkler recipe](images/recipes/ra_farming/sprinkler.png){ width="220" } |
| Breeder | dispenser | ![Breeder recipe](images/recipes/ra_interactive/breeder.png){ width="220" } |

Operators: `/function ra_farming:items/give_all`.

!!! note "The Breeder moved here"
    The Breeder was part of [Interactive Machines](interactive-machines.md) until v6.0.3. Only
    the module it is listed under moved: Breeders already placed keep working, its recipe is the
    same, and its ids (`ra_interactive:breeder`) did not change.

## At a glance

| Block | Needs redstone? | Does |
| --- | --- | --- |
| Sprinkler | no (redstone locks it) | Waters the farmland around it; with liquid experience, crops grow about 4x as fast |
| Breeder | while powered | Feeds the animals in front of it from its inventory |

## Sprinkler

1. Place it in the middle of your field. It works at farmland level, where the water hole of a
   vanilla farm would be, or one block higher, standing among the crops.
2. Put a pipe, tank or drain of a [fluid network](transport-networks.md) against any of its six
   faces, and fill the network with **water**, **liquid experience** or both.
3. Leave it unpowered: a redstone signal locks it, as it does a hopper.

It is not part of the network itself, so placing or breaking it never splits or joins one. The
first neighbour whose network holds the whole price pays.

**The area** is a square of `radius` blocks each way around it (4 by default, a 9x9 field), from
one block below the Sprinkler to one block above it: crops planted level with it, one block above
or one below are all reached.

| Fluid | What it does | Price |
| --- | --- | --- |
| Water | Every 5 seconds, all the farmland in the area becomes fully moist, so the crops grow at their best vanilla rate and the farmland never dries out | 4 mL per block of the area each time (324 mL at radius 4, about a bucket every 75 seconds) |
| Liquid experience | Once a second, extra growth: crops in a moist field grow about **4x** as fast as on their own | 10 mL (a tenth of an experience point) per crop stage it grows |

With both in the network it does both. The extra growth picks random spots in the area, as the
game's own random ticks do. A second's experience is taken from the network before anything
grows, and what a spot with nothing to grow (dirt, a ripe crop) did not use is put back, so in
the end only crops that grew are paid for.

**Crops it grows:** wheat, carrots, potatoes, beetroots, nether wart, melon and pumpkin stems,
torchflower crops and sweet berry bushes. A ripe stem still needs the game's own growth to set a
melon or pumpkin, and a torchflower crop stops one stage short of the flower.

| Setting | Default | Range | Meaning |
| --- | --- | --- | --- |
| `radius` | 4 | 1 to 8 (admins up to 16) | How many blocks each way the square reaches |

Change it with the [Data Handler](tools.md) (Shift + right-click the Sprinkler). Server admins
set the radius new Sprinklers start with on the **Farming** settings page, 1 to 16, the same
ceiling as their own. A default above 8 gives every new Sprinkler that radius whoever places it;
a player can then only set it within 1 to 8. The page can also switch the Sprinkler or the
Breeder off for the world.

The [Goggles](tools.md) show its state (running or locked), whether it is watering, whether the
experience boost is on, and its radius.

## Breeder

1. Put the right food in the Breeder itself (it has a chest-like inventory and never throws its
   items). It feeds only from its own inventory, not from a container next to it.
2. Keep two adult animals of the same kind in front of it (within two blocks of the block it
   faces).
3. Power it. It uses one food per pair it feeds.

| Animal | Food |
|---|---|
| Cow, Mooshroom, Sheep, Goat | wheat |
| Pig | carrot, potato, beetroot |
| Chicken | wheat seeds, melon seeds, pumpkin seeds, beetroot seeds, torchflower seeds, pitcher pod |
| Rabbit | carrot, golden carrot, dandelion |
| Horse | golden apple, golden carrot, enchanted golden apple |
| Llama | hay block |
| Turtle | seagrass |
| Panda | bamboo |
| Fox | sweet berries, glow berries |
| Bee | any flower |
| Wolf | any raw or cooked meat, rotten flesh |
| Cat | cod, salmon |
| Axolotl | bucket of tropical fish |
| Strider | warped fungus |
| Hoglin | crimson fungus |
| Camel | cactus |
| Sniffer | torchflower seeds |
| Frog | slime ball |
| Armadillo | spider eye |

---

## Advanced

- **Cost.** Per tick a Sprinkler costs only the break check every block has. Its work runs once a
  second on the network machines' clock (the one the Spout and the Tesla Coil use), which only runs
  while one of them is loaded. Watering is a single `fill` of the farmland in the area, which
  changes nothing on farmland that is already wet.
- **Growth rate.** The game gives each block 3 random ticks per 4096 per game tick, and a random
  tick grows a crop in a full, moist field 1 time in 6 (1 in 13 on dry farmland). The Sprinkler
  adds three times the moist-field figure: `area x 15 / 2048` growths a second (0.6 at radius 4,
  4 at radius 12), the remainder rolled, each growing the crop at a random spot by one stage. With
  the game's own growth that is about 4x on moist farmland, which its water keeps; on dry
  farmland the game is slower, so the Sprinkler's share is larger.
- **Paid first.** The second's experience is taken before any crop grows, and only as many growths
  as were really taken are made; what a spot with nothing to grow did not use is put back into
  the same network. A short take of water (the network changed since it was read) is put back and
  waters nothing.
- **Physical blocks.** The Sprinkler is a waxed oxidized copper grate. The Breeder is a barrel
  wearing a dispenser skin, so redstone never makes it throw its food.
- **Ids.** The Breeder keeps the `ra_interactive` namespace it was created in: its functions
  (`ra_interactive:blocks/breeder/...`), its recipe and the entity tag `#ra_interactive:breedable`.
  The crop list is the block tag `#ra_farming:sprinkler_crops`.
