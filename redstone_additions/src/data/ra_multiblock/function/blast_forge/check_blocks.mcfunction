# /ra_multiblock:blast_forge/check_blocks
# MACRO FUNCTION — Unified block pattern check for blast forge
# Replaces both validate_facing and check_facing with a single source of truth
# Called: function ... with storage ra:multiblock bf_dir.{north|south|east|west}
# Context: at multiblock base/marker position
# Returns: 1 if all blocks valid, 0 if any block invalid
# NO side effects — does not modify storage or scoreboards

# === Functional blocks ===
$execute unless block $(furnace) minecraft:blast_furnace run return 0
$execute unless block $(in1) minecraft:barrel run return 0
$execute unless block $(in2) minecraft:barrel run return 0
$execute unless block $(out1) minecraft:barrel run return 0

# === Middle layer (Y=0) corner bricks ===
$execute unless block $(b1) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(b2) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(b3) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(b4) #ra_multiblock:blast_forge_bricks run return 0

# === Floor (Y-1) — 9 nether brick blocks ===
$execute unless block $(f1) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f2) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f3) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f4) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f5) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f6) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f7) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f8) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(f9) #ra_multiblock:blast_forge_bricks run return 0

# === Ceiling (Y+1) — 9 nether brick blocks ===
$execute unless block $(c1) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c2) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c3) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c4) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c5) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c6) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c7) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c8) #ra_multiblock:blast_forge_bricks run return 0
$execute unless block $(c9) #ra_multiblock:blast_forge_bricks run return 0

# All checks passed
return 1
