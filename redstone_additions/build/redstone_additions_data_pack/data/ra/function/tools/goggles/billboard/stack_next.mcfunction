# /ra:tools/goggles/billboard/stack_next
# Write the next stacked height into the line being built, then step down.
# Context: after storage ra:temp status_literal has been filled in.

# A block that forgot stack_reset still gets a sane ladder rather than a broken line.
execute unless score #gg.stack_y ra.temp = #gg.stack_y ra.temp run scoreboard players set #gg.stack_y ra.temp 80
execute unless score #gg.stack_step ra.temp = #gg.stack_step ra.temp run scoreboard players set #gg.stack_step ra.temp 20

execute store result storage ra:temp status_literal.y double 0.01 run scoreboard players get #gg.stack_y ra.temp
scoreboard players operation #gg.stack_y ra.temp -= #gg.stack_step ra.temp
