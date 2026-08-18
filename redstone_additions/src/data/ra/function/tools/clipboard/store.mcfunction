# /ra:tools/clipboard/store {slot:N,kind:"...",props:{...}}
# Internal: write the payload into this player's slot.

$data modify storage ra:clipboard boards.p$(slot) set value {}
$data modify storage ra:clipboard boards.p$(slot).kind set from storage ra:temp clip.kind
$data modify storage ra:clipboard boards.p$(slot).props set from storage ra:temp clip.props
