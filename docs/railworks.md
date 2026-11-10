# Railworks

Blocks that work with minecarts and their tracks. The **Route Rail** sits under a rail junction
and switches it for each cart: carts on its route go one way, every other cart the other way.

## Getting them

| Item | Looks like | Recipe |
| --- | --- | --- |
| Route Rail | waxed cut copper | ![Route Rail recipe](images/recipes/ra_railworks/route_rail.png){ width="220" } |

Six copper ingots around a rail, a name tag and a redstone dust. Operators:
`/function ra_railworks:items/give_all` (a Route Rail).

## Route Rail

1. Build a junction with a plain **rail** (only a plain rail can curve; powered, detector and
   activator rails cannot be switched).
2. Break the block under the junction rail, place the Route Rail there and put the rail back on
   top of it.
3. Set the **route** and the two shapes with the Data Handler, and the source with the Wrench.

When a minecart comes within 2.5 blocks of the junction, the Route Rail looks at the nearest
cart and sets the rail on top to `on_match` if that cart carries the route, or to `on_miss` if it
does not. The rail is set before the cart gets there, even at the game's top speed.

### What a cart carries

`source` says where the route is read from:

| `source` | The cart matches when | Set it with |
| --- | --- | --- |
| `name` (default) | its name is exactly `route` | a name tag renamed in an anvil, used on the cart |
| `tag` | it has the entity tag `route` | a [Tag Adder](sensors.md) next to the track, or `/tag` |
| `item` | it is a chest or hopper minecart holding an item whose id is `route` | put the item in it; write the full id, like `minecraft:coal` |
| `rider` | someone or something rides it | `route` is not read |

Names and tags are compared exactly: `Express` and `express` are different routes. An empty
`route` matches nothing (except with `rider`).

### Shapes

`on_match` and `on_miss` are rail shapes, as F3 shows them: `north_south`, `east_west`,
`north_east`, `north_west`, `south_east`, `south_west`. `none` leaves the rail as it is. The
defaults are `south_east` on a match and `north_south` otherwise: a north-south line with a
branch to the east, for carts heading north. A curve named after two sides joins those two
sides: a cart heading north comes in from the south, so it needs `south_east` to leave by the
east; for carts heading south the same branch is `north_east`.

Only a plain rail is changed, and its `waterlogged` state is kept. The rail is set again only when
its shape differs, with a soft click for players who keep block sounds on.

### Example: an express line

A north-south line, carts heading north, and a station on a branch to the east.

1. Route Rail under the junction, `source` = `name`, `route` = `local`, `on_match` = `south_east`,
   `on_miss` = `north_south`.
2. Name a minecart `local` with a name tag. That cart takes the branch to the station; every
   other cart stays on the main line.

With `source` = `item` and `route` = `minecraft:coal`, chest minecarts carrying coal go to the
branch: the same junction sorts freight.

### Wrench and tools

| Tool | Does |
| --- | --- |
| Wrench, Shift+RMB | The cycle menu: **Reads** (`source`: name, tag, item, rider), **On match** and **Otherwise** (the six shapes, then `none`) |
| Goggles | Route, source, both shapes, and whether the last cart matched |
| Data Handler | `route` (any text), `source`, `on_match`, `on_miss` |

A value typed in the Data Handler that is not one of the listed sources or shapes does nothing:
the rail is left as it is.

Server admins can switch the Route Rail off for the world on the **Railworks** settings page.
Breaking it drops it; the rail on top stays where it is.

## Troubleshooting

| What happens | Check |
| --- | --- |
| The rail never changes | It must be a plain rail directly on top of the Route Rail |
| Every cart goes the same way | The route is compared exactly: check the case, and the full item id for `item` |
| A cart takes the wrong branch | Two carts close together: the nearest one decides. Space carts a few blocks apart |
| The curve points the wrong way | Pick the shape joining the side the cart comes from and the side it should leave by |
| A named cart does not match | The name must be the whole route; `local 2` is not `local` |

---

## Advanced

### Route Rail data

| Where | What |
| --- | --- |
| `data.properties.route` | the route, text |
| `data.properties.source` | `name`, `tag`, `item` or `rider` |
| `data.properties.on_match`, `data.properties.on_miss` | a rail shape or `none` |
| `data.status.last` | `match` or `no match`, the last cart's result (the Goggles) |

**Nothing is pasted.** The route is copied into storage and the cart's value copied over it;
`store success` is 0 when nothing changed, that is, when both are equal. Tags and items are walked
one entry at a time the same way. A missing value is never copied (a failed copy would read as
equal). A name tag's name is a plain string; a name set by a command may be a text component, and
its `text` is compared as well. The sources and shapes are matched against fixed strings. No
player text reaches a selector or a command, so `route` takes any text.

**What runs.** Each tick the Route Rail tick does the break check every block has. Every other
tick, one test: is a minecart loaded anywhere (`#ra_railworks:minecarts`)? With none, that is all.
With some, each Route Rail runs one selector bounded to 2.5 blocks around its rail, and only a
Route Rail with a cart that close reads its properties, checks the nearest cart and sets the rail.
A cart at 8 blocks a second needs more than 4 ticks to cross 2.5 blocks, so it is always seen in
time.
