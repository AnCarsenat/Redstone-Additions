# /ra_wires:fluid/drain_place_orb {points:N}
# Internal: drop one orb carrying the whole cycle's worth.
# Context: as the drain marker, at the drain position.
#
# One orb rather than one per point: a stack of single-point orbs is the same
# experience but a great deal more entity, and vanilla merges them anyway.

$summon minecraft:experience_orb ~ ~1 ~ {Value:$(points)s}
