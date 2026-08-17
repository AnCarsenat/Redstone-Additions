# /ra_lib_multiblock:build/write_io {type,facing,name,x,y,z}
# Internal: write one rotated IO slot into inputs/outputs/controls, keyed by name.
# work.kind selects which of the three maps receives it.

$execute if data storage ra:multiblock work{kind:"inputs"} run data modify storage ra:multiblock types.$(type).facings.$(facing).inputs.$(name) set value {x:$(x),y:$(y),z:$(z)}
$execute if data storage ra:multiblock work{kind:"outputs"} run data modify storage ra:multiblock types.$(type).facings.$(facing).outputs.$(name) set value {x:$(x),y:$(y),z:$(z)}
$execute if data storage ra:multiblock work{kind:"controls"} run data modify storage ra:multiblock types.$(type).facings.$(facing).controls.$(name) set value {x:$(x),y:$(y),z:$(z)}
