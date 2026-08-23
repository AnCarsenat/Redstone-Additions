# /ra_wires:fluid/potion_store
# Remember which potion a network is holding.
# Context: as the drain marker, with the emptying player still tagged.
#
# WHY THE NETWORK REMEMBERS AND THE MEDIUM DOES NOT
# Every potion is the medium `potion` as far as volume is concerned -- they mix,
# they share the run's capacity, and none of that needs to know which one it is.
# What does need to know is the drain at the far end, and it needs the whole
# `potion_contents` component rather than a name, because a brewed-with-commands
# potion carries its effects on the item and nothing else knows them.
#
# FIRST POTION IN WINS
# A network already holding one potion keeps its identity when a second,
# different one is poured in. The alternative is refusing the second pour, which
# is the single-medium behaviour this release exists to remove, or averaging two
# effect lists, which means nothing. Mixing potions is a player decision and this
# is the documented consequence of it.

execute if score @s ra.tr.net matches ..0 run return 0

data remove storage ra:wires pot
execute store result storage ra:wires pot.id int 1 run scoreboard players get @s ra.tr.net
data modify storage ra:wires pot.contents set from entity @p[tag=ra.wires.emptying] SelectedItem.components."minecraft:potion_contents"
execute unless data storage ra:wires pot.contents run return 0

function ra_wires:fluid/potion_store_run with storage ra:wires pot
