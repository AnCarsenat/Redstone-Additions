# /ra:tools/goggles/billboard/stack_reset {top,step}
# Start a stack of status lines at a height this block chooses.
# Context: as the block marker, before any stacked_* line call.
#
# top and step are in hundredths of a block, so {top:80,step:20} puts the first
# line 0.8 above the block and each following one 0.2 lower. Integers because
# macro arguments are pasted as text and scoreboards do the arithmetic.
#
# Hand-picking a y per line, which is what prop_line / text_line / data_line take,
# means every block invents its own ladder and a block with one line too many ends
# up drawing inside itself. A block that stacks says where its lines start and how
# far apart they sit, once.

$scoreboard players set #gg.stack_y ra.temp $(top)
$scoreboard players set #gg.stack_step ra.temp $(step)
