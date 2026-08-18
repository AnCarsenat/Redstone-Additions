# /ra:tools/wrench/cycle_index {i}
# Internal: rebuild this block's list and run entry i, then redraw the menu.
# Context: as the marker, at the block.
#
# The list is rebuilt rather than kept from when the menu was drawn, because
# menu_row_next consumes it as it goes -- and because rebuilding is what makes
# the click correct even if the player has opened a different block's menu since.
# Through load_for_block both times, so the indices match what was on screen.

function ra:tools/wrench/load_for_block
$function ra:tools/wrench/run_entry {i:$(i)}

function ra:tools/wrench/load_for_block
function ra:tools/wrench/open_menu
