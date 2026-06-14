# /ra_multiblock:blast_forge/debug_structure
# MACRO FUNCTION — Debug: check each block and report results
# Called: function ... with storage ra:multiblock bf_dir.{direction}
# Context: as multiblock marker, at base position
# Usage: /tag @s add ra.debug then assemble. Or run manually:
#   execute as @e[tag=ra.multiblock.blast_forge,limit=1] at @s run function ra_multiblock:blast_forge/debug_structure with storage ra:multiblock bf_dir.north

$tellraw @a [{text:"[MB Debug] ",color:"gold"},{text:"Checking $(facing) structure:",color:"aqua"}]

# Functional blocks
$execute if block $(furnace) minecraft:blast_furnace run tellraw @a [{text:"  furnace $(furnace): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(furnace) minecraft:blast_furnace run tellraw @a [{text:"  furnace $(furnace): ",color:"gray"},{text:"FAIL",color:"red"},{text:" (found: "},{text:"",nbt:"",block:"$(furnace)",interpret:false,color:"yellow"},{text:")"}]

$execute if block $(in1) minecraft:barrel run tellraw @a [{text:"  in1 $(in1): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(in1) minecraft:barrel run tellraw @a [{text:"  in1 $(in1): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(in2) minecraft:barrel run tellraw @a [{text:"  in2 $(in2): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(in2) minecraft:barrel run tellraw @a [{text:"  in2 $(in2): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(out1) minecraft:barrel run tellraw @a [{text:"  out1 $(out1): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(out1) minecraft:barrel run tellraw @a [{text:"  out1 $(out1): ",color:"gray"},{text:"FAIL",color:"red"}]

# Corner bricks
$execute if block $(b1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b1 $(b1): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(b1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b1 $(b1): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(b2) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b2 $(b2): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(b2) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b2 $(b2): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(b3) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b3 $(b3): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(b3) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b3 $(b3): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(b4) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b4 $(b4): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(b4) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  b4 $(b4): ",color:"gray"},{text:"FAIL",color:"red"}]

# Floor (Y-1) — spot check first and last
$execute if block $(f1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f1 $(f1): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(f1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f1 $(f1): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(f5) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f5 $(f5): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(f5) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f5 $(f5): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(f9) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f9 $(f9): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(f9) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  f9 $(f9): ",color:"gray"},{text:"FAIL",color:"red"}]

# Ceiling (Y+1) — spot check
$execute if block $(c1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c1 $(c1): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(c1) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c1 $(c1): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(c5) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c5 $(c5): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(c5) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c5 $(c5): ",color:"gray"},{text:"FAIL",color:"red"}]

$execute if block $(c9) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c9 $(c9): ",color:"gray"},{text:"OK",color:"green"}]
$execute unless block $(c9) #ra_multiblock:blast_forge_bricks run tellraw @a [{text:"  c9 $(c9): ",color:"gray"},{text:"FAIL",color:"red"}]

tellraw @a [{text:"[MB Debug] ",color:"gold"},{text:"Done. See above for results.",color:"gray"}]
