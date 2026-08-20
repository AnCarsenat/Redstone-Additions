# /ra_wires:fluid/drain_potion_next
# Internal: apply the head of the effect list, then the rest.
# Context: as the drain marker, at the drain position. #pt.vol holds the millilitres
# that were actually drawn.
#
# The two shapes are normalised here rather than in two appliers. custom_effects
# uses id/duration/amplifier; the preset table uses e/d/a, which keeps a
# forty-entry table readable. Whichever is present is copied into the same three
# fields before the scaling runs.

execute unless data storage ra:wires eff.list[0] run return 0

data remove storage ra:wires one
data modify storage ra:wires one.id set from storage ra:wires eff.list[0].id
data modify storage ra:wires one.dur set from storage ra:wires eff.list[0].duration
data modify storage ra:wires one.amp set from storage ra:wires eff.list[0].amplifier
data modify storage ra:wires one.id set from storage ra:wires eff.list[0].e
data modify storage ra:wires one.dur set from storage ra:wires eff.list[0].d
data modify storage ra:wires one.amp set from storage ra:wires eff.list[0].a

# An amplifier of 0 is legal and common, and an absent one means the same thing,
# so it is seeded rather than guarded.
execute unless data storage ra:wires one.amp run data modify storage ra:wires one.amp set value 0

execute if data storage ra:wires one.id run function ra_wires:fluid/drain_potion_scale

data remove storage ra:wires eff.list[0]
function ra_wires:fluid/drain_potion_next
