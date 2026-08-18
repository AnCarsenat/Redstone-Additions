# /ra:tools/data_handler/props/redact
# Build storage ra:dh display_props: everything the block carries.
#
# Nothing is removed. Locked fields are still listed in the Properties summary and
# still get a row -- see props/row_locked. This function is kept as the one place
# that decides what the summary shows, so a future rule has somewhere to live.

data modify storage ra:dh display_props set from storage ra:dh properties
