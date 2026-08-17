# /ra_ender:link/move_stack {x,y,z,src_slot,dst_slot}
# Context: at the receiving barrel. Wraps the library primitive, whose src is one
# string, with the coordinates the sender stored as three numbers.

$function ra_lib:inventory/move_slot {src:"$(x) $(y) $(z)",src_slot:$(src_slot),dst_slot:$(dst_slot)}
