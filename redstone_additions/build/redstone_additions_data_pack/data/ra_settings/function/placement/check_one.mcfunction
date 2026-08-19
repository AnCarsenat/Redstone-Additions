# /ra_settings:placement/check_one {b:"electric_furnace"}
# Internal: does this bat carry the routing tag of a disabled block?

$execute if entity @s[tag=ra.place.$(b)] run scoreboard players set #blocked ra.set.tmp 1
