# /ra:tools/clipboard/check_kind {kind:"..."}
# Internal: does the copied block's name match the one being pasted onto?
# Sets storage ra:temp clip_ok when it does.
#
# A macro because this compares two runtime strings, which no block or entity
# predicate can do -- one arrives as the macro argument, the other is read out of
# storage where the name dispatch just left it.

$execute if data storage ra:temp {block_name:"$(kind)"} run data modify storage ra:temp clip_ok set value 1b
