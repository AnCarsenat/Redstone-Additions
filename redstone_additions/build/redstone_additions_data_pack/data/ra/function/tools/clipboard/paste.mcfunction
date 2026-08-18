# /ra:tools/clipboard/paste
# Apply the holder's clipboard onto this block.
# Context: as the target block's marker.

data modify storage ra:temp clip_found set value 1b

execute store result storage ra:temp clip.slot int 1 run scoreboard players get @a[tag=ra.clip_active,limit=1] ra.clip.id
data remove storage ra:temp clip.kind
function ra:tools/clipboard/load with storage ra:temp clip

execute unless data storage ra:temp clip.kind run return run tellraw @a[tag=ra.clip_active,limit=1] [{text:"[Clipboard] ",color:"gold"},{text:"No origin set — click a block to choose one.",color:"gray"}]

# Same kind only. Property names overlap between unrelated blocks -- half the
# pack has `enabled` -- so without this a Gas Valve's rate would happily land on
# a Randomizer's chance and quietly mean something else.
data modify storage ra:temp name_only set value 1b
function ra:tools/goggles/draw_block
data remove storage ra:temp name_only

function ra:tools/clipboard/check_kind with storage ra:temp clip
execute unless data storage ra:temp clip_ok run return run function ra:tools/clipboard/refuse

function ra:tools/clipboard/apply with storage ra:temp clip

tellraw @a[tag=ra.clip_active,limit=1] [{text:"[Clipboard] ",color:"gold"},{text:"Matched this ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:" to the origin.",color:"gray"}]
playsound minecraft:block.amethyst_block.chime block @a[tag=ra.clip_active,limit=1] ~ ~ ~ 0.7 1.4
particle minecraft:happy_villager ~ ~0.8 ~ 0.3 0.3 0.3 0.02 6
data remove storage ra:temp clip
data remove storage ra:temp clip_ok
