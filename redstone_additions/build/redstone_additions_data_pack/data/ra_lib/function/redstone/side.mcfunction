# /ra_lib:redstone/side {dx:0,dy:0,dz:-1,side:"north",back:"south",torch:"side",dust:"side"}
# Read the power one neighbour delivers into this block. 0-16.
# Context: as the block's marker, at the block position.
# Returns the level, and leaves it in #rs ra.temp.
#
# Prefer the ra_lib:redstone/local/* wrappers, which name the side the way the
# player sees it — front, back, left, right, up, down — and work out the world
# direction from the block's own facing. Call this directly only when you really
# do mean a compass direction.
#
# ARGUMENTS
#   dx dy dz  offset to the neighbour
#   side      name of the direction from us towards it
#   back      the opposite name, i.e. the direction from it back to us
#   torch     which torch rule applies on this side: "below", "side" or "none"
#   dust      which dust rule applies on this side: "side", "above" or "none"
#
# WHY dust AND torch ARE BOTH NAMED BY THE CALLER
# This function used to substitute $(back) straight into a redstone_wire block
# state on every side. redstone_wire has north, south, east and west connection
# states and nothing else, so the two vertical sides asked it for `up` and
# `down`. A macro line is parsed after substitution and before the function
# runs, so that one unparseable line stopped the whole function from executing
# -- and the up and down sides therefore read 0 for EVERY source, not just for
# dust. A lever sitting directly on a block never turned it on. Naming the rule
# per side is what stops a vertical side being handed a horizontal test again.
#
# LEVELS
#   0       nothing
#   1..15   ordinary power, the number the source is actually carrying
#   16      superpower — a repeater or comparator pointed into this block. Vanilla
#           calls this strong power at level 15; the pack numbers it 16 so a block
#           can tell "a full dust line reached me" from "a repeater is driving me",
#           which is the distinction that matters when deciding whether to pass a
#           signal on.
#
# ORDER
# Sources are written weakest-first so a plain overwrite settles on the strongest,
# with no maximum to compute. Anything reading this must not reorder the block.

scoreboard players set #rs ra.temp 0

# Dust rules differ by side, so the side names which one applies -- exactly as
# the torch rules below do. Horizontal neighbours are gated on the connection
# state facing us; dust above powers us whatever shape it is in; dust below
# never does.
$function ra_lib:redstone/dust/$(dust) {dx:$(dx),dy:$(dy),dz:$(dz),back:"$(back)"}

# Analog sources that power every neighbour regardless of orientation: the two
# weighted pressure plates and the daylight detector. No connection test needed.
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_omni run function ra_lib:redstone/analog {dx:$(dx),dy:$(dy),dz:$(dz)}

# Everything with a `powered` flag that drives all its neighbours at full strength
# when set: levers, every button, the non-weighted pressure plates, tripwire hooks,
# lightning rods. One tag, one test — the old library spent twelve commands a side
# on levers and buttons alone and still never noticed a pressure plate.
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/binary_sources[powered=true] run scoreboard players set #rs ra.temp 15

# A redstone block is a source that is always on. Blocks that sit inside redstone
# machinery opt out with ra.redstone.ignore_blocks, because the pack itself uses
# redstone blocks as gate output and a gate must not read its own answer back.
$execute unless entity @s[tag=ra.redstone.ignore_blocks] if block ~$(dx) ~$(dy) ~$(dz) minecraft:redstone_block run scoreboard players set #rs ra.temp 15

# Torch rules differ by side, so the side names which one applies.
$function ra_lib:redstone/torch/$(torch) {dx:$(dx),dy:$(dy),dz:$(dz),side:"$(side)"}

# Sources that drive one direction only, and strongly: a repeater, a comparator,
# an observer pulsing. All three carry `facing` for the direction they output
# towards and `powered` for whether they are, so the one driving us is the one
# facing `$(back)` — one tag test where this used to be three separate ones.
#
# This is the 16 the level scale reserves for superpower. Vanilla calls it strong
# power at 15; the pack numbers it above a full dust line so a block can tell
# "something is deliberately driving me" from "a signal happened to arrive".
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/directional_sources[facing=$(back),powered=true] run scoreboard players set #rs ra.temp 16

return run scoreboard players get #rs ra.temp
