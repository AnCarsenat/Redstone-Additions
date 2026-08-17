# /ra_lib:inventory/remove/keep
# Internal helper for ra_lib:inventory/remove.
# Carry the current entry over to the output list untouched.

data modify storage ra:inventory out append from storage ra:inventory scan[0]
