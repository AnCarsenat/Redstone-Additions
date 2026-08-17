# /ra:tools/data_handler/init_registry
# The list of property names the Handler knows how to show, in display order.
#
# A data pack cannot iterate the keys of a compound, which is why this list exists
# at all — it is the only hand-maintained part left. Adding a property to a block
# means adding its name here, and nothing else: the row, the type and the editor are
# all worked out at runtime. Position matters only in that it decides the action id
# a row's button carries (100 + index).

data modify storage ra:dh registry set value ["enabled","inverted","mode","channel","target","tag","entity_selector","message_block","gate","gate_type","range","delay","cooldown","power","distance","extend","pulse","chance","transfer_rate","generation_rate","eu_use","tier","tier_level","input1","output1","anchor_id","targets"]
