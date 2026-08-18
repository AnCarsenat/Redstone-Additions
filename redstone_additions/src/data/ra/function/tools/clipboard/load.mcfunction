# /ra:tools/clipboard/load {slot:N}
# Internal: pull this player's slot back into scratch.

$execute if data storage ra:clipboard boards.p$(slot) run data modify storage ra:temp clip.kind set from storage ra:clipboard boards.p$(slot).kind
$execute if data storage ra:clipboard boards.p$(slot) run data modify storage ra:temp clip.props set from storage ra:clipboard boards.p$(slot).props
