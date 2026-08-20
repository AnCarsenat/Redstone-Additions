# /ra:tools/clipboard/copy
# Make this block the origin: its settings become what every later click hands out.
# Context: as the target block's marker.

data modify storage ra:temp clip_found set value 1b

# Only data.properties travels. data.data is a block's private working state --
# a pump's cooldown, a gate's latch, a network id -- and copying that onto
# another block would hand it a half-finished operation belonging to somewhere
# else. Settings are what a player means by "these settings".
execute unless data entity @s data.properties run return run tellraw @a[tag=ra.clip_active,limit=1] [{text:"[Clipboard] ",color:"gold"},{text:"That block has no settings to copy.",color:"gray"}]

# The name doubles as the paste guard, so a Pump's settings cannot be pasted onto
# a Gate. Taken from the goggles name dispatch, which is the single source.
data modify storage ra:temp name_only set value 1b
function ra:tools/goggles/draw_block
data remove storage ra:temp name_only

data modify storage ra:temp clip.props set from entity @s data.properties
data modify storage ra:temp clip.kind set from storage ra:temp block_name
execute store result storage ra:temp clip.slot int 1 run scoreboard players get @a[tag=ra.clip_active,limit=1] ra.clip.id
function ra:tools/clipboard/store with storage ra:temp clip

tellraw @a[tag=ra.clip_active,limit=1] [{text:"[Clipboard] ",color:"gold"},{text:"Origin set: ",color:"gray"},{nbt:"block_name",storage:"ra:temp",color:"yellow"},{text:". Click others of the same kind to match it.",color:"gray"}]
playsound minecraft:item.book.page_turn block @a[tag=ra.clip_active,limit=1,scores={ra.u.snd=1..}] ~ ~ ~ 0.8 1.6
data remove storage ra:temp clip
