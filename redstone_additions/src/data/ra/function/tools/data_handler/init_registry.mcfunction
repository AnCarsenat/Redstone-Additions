# /ra:tools/data_handler/init_registry
# The list of property names the Handler knows how to show, in display order.
#
# A data pack cannot iterate the keys of a compound, which is why this list exists
# at all — it is the only hand-maintained part left. Adding a property to a block
# means adding its name here, and nothing else: the row, the type and the editor are
# all worked out at runtime. Position matters only in that it decides the action id
# a row's button carries (100 + index).

# Fields a survival player has no business retuning: internal timing and tier
# knobs that exist for builders and addon authors. Held as a compound rather than a
# list so membership is one macro test on the name. The Creative Data Handler shows
# everything; this is the survival tool.
data modify storage ra:dh creative_only set value {cooldown:1b,transfer_rate:1b,generation_rate:1b,eu_use:1b,tier:1b,tier_level:1b}

data modify storage ra:dh registry set value ["enabled","inverted","mode","channel","target","tag","entity_selector","message_block","gate","gate_type","range","delay","cooldown","power","distance","extend","pulse","chance","transfer_rate","generation_rate","eu_use","tier","tier_level","input1","output1","anchor_id","targets"]
