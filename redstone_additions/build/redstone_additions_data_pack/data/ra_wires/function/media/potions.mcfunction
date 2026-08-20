# /ra_wires:media/potions
# What each vanilla potion does, so a drained potion can be applied rather than
# just poured away.
#
# WHY A TABLE AT ALL
# A potion item carries `potion_contents`, but a PRESET potion carries only its
# id -- {potion:"minecraft:strength"} -- and nothing that says strength lasts
# three minutes at level I. Only a brewed-with-commands potion fills in
# `custom_effects`. So the effects of the forty vanilla potions have to be
# written down somewhere, and this is that somewhere. A potion that does carry
# `custom_effects` is read straight off the item and never comes here.
#
# Durations are in TICKS, at the values the wiki lists for the drinkable potion.
# Instant health and harm are given one tick, because an instant effect ignores
# duration and a zero would be read as "no effect at all".
#
# The four potions with no effect -- water, mundane, thick, awkward -- are
# deliberately absent. A drain pouring one out applies nothing, which is correct.

data modify storage ra:wires potion_effects set value {"minecraft:night_vision":[{e:"minecraft:night_vision",d:3600,a:0}],"minecraft:long_night_vision":[{e:"minecraft:night_vision",d:9600,a:0}],"minecraft:invisibility":[{e:"minecraft:invisibility",d:3600,a:0}],"minecraft:long_invisibility":[{e:"minecraft:invisibility",d:9600,a:0}],"minecraft:leaping":[{e:"minecraft:jump_boost",d:3600,a:0}],"minecraft:long_leaping":[{e:"minecraft:jump_boost",d:9600,a:0}],"minecraft:strong_leaping":[{e:"minecraft:jump_boost",d:1800,a:1}],"minecraft:fire_resistance":[{e:"minecraft:fire_resistance",d:3600,a:0}],"minecraft:long_fire_resistance":[{e:"minecraft:fire_resistance",d:9600,a:0}],"minecraft:swiftness":[{e:"minecraft:speed",d:3600,a:0}],"minecraft:long_swiftness":[{e:"minecraft:speed",d:9600,a:0}],"minecraft:strong_swiftness":[{e:"minecraft:speed",d:1800,a:1}],"minecraft:slowness":[{e:"minecraft:slowness",d:1800,a:0}],"minecraft:long_slowness":[{e:"minecraft:slowness",d:4800,a:0}],"minecraft:strong_slowness":[{e:"minecraft:slowness",d:400,a:3}],"minecraft:water_breathing":[{e:"minecraft:water_breathing",d:3600,a:0}],"minecraft:long_water_breathing":[{e:"minecraft:water_breathing",d:9600,a:0}],"minecraft:healing":[{e:"minecraft:instant_health",d:1,a:0}],"minecraft:strong_healing":[{e:"minecraft:instant_health",d:1,a:1}],"minecraft:harming":[{e:"minecraft:instant_damage",d:1,a:0}],"minecraft:strong_harming":[{e:"minecraft:instant_damage",d:1,a:1}],"minecraft:poison":[{e:"minecraft:poison",d:900,a:0}],"minecraft:long_poison":[{e:"minecraft:poison",d:1800,a:0}],"minecraft:strong_poison":[{e:"minecraft:poison",d:420,a:1}],"minecraft:regeneration":[{e:"minecraft:regeneration",d:900,a:0}],"minecraft:long_regeneration":[{e:"minecraft:regeneration",d:1800,a:0}],"minecraft:strong_regeneration":[{e:"minecraft:regeneration",d:440,a:1}],"minecraft:strength":[{e:"minecraft:strength",d:3600,a:0}],"minecraft:long_strength":[{e:"minecraft:strength",d:9600,a:0}],"minecraft:strong_strength":[{e:"minecraft:strength",d:1800,a:1}],"minecraft:weakness":[{e:"minecraft:weakness",d:1800,a:0}],"minecraft:long_weakness":[{e:"minecraft:weakness",d:4800,a:0}],"minecraft:luck":[{e:"minecraft:luck",d:6000,a:0}],"minecraft:slow_falling":[{e:"minecraft:slow_falling",d:1800,a:0}],"minecraft:long_slow_falling":[{e:"minecraft:slow_falling",d:4800,a:0}],"minecraft:turtle_master":[{e:"minecraft:slowness",d:400,a:3},{e:"minecraft:resistance",d:400,a:2}],"minecraft:long_turtle_master":[{e:"minecraft:slowness",d:800,a:3},{e:"minecraft:resistance",d:800,a:2}],"minecraft:strong_turtle_master":[{e:"minecraft:slowness",d:400,a:5},{e:"minecraft:resistance",d:400,a:3}],"minecraft:wind_charged":[{e:"minecraft:wind_charged",d:3600,a:0}],"minecraft:weaving":[{e:"minecraft:weaving",d:3600,a:0}],"minecraft:oozing":[{e:"minecraft:oozing",d:3600,a:0}],"minecraft:infested":[{e:"minecraft:infested",d:3600,a:0}]}
