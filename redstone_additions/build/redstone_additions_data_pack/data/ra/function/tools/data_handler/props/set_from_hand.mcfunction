# /ra:tools/data_handler/props/set_from_hand {name}
# Internal: copy the held item's id into an item_name property. As player.
#
# SelectedItem is absent when the hand is empty, so the guard is a real one and
# not politeness -- writing an absent source would leave the old value standing
# while the player was told it had changed.

execute unless data entity @s SelectedItem run return run tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"Hold the item you want to filter for, then press it again.",color:"red"}]

data modify storage ra:dh hand set from entity @s SelectedItem.id
$data modify storage ra:dh properties.$(name) set from storage ra:dh hand
$data modify entity @e[type=marker,tag=ra.dh_target,limit=1] data.properties.$(name) set from storage ra:dh hand
data remove storage ra:dh hand

$tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"$(name)",color:"yellow"},{text:" set to ",color:"gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"green"}]
playsound minecraft:block.note_block.pling block @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.7 1.4
