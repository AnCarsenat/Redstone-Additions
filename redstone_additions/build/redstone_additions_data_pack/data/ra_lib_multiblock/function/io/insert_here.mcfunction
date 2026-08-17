# /ra_lib_multiblock:io/insert_here
# Internal: positioned at the IO container by io/at.
#
# Uses insert_or_drop: a multiblock producing into a full output barrel used to
# have its product deleted by `loot insert`.

data modify storage ra:inventory item set from storage ra:multiblock io_item
function ra_lib:inventory/insert_or_drop
