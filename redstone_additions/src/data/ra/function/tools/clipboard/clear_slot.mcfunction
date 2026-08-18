# /ra:tools/clipboard/clear_slot {slot:N}
# Internal: drop this player's slot.

$data remove storage ra:clipboard boards.p$(slot)
