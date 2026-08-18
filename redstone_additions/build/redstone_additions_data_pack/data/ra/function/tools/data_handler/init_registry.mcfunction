# /ra:tools/data_handler/init_registry
# The list of property names the Handler knows how to show, in display order.
#
# A data pack cannot iterate the keys of a compound, which is why this list exists
# at all — it is the only hand-maintained part left. Adding a property to a block
# means adding its name here, and nothing else: the row, the type and the editor are
# all worked out at runtime. Position matters only in that it decides the action id
# a row's button carries (100 + index).

data modify storage ra:dh registry set value ["enabled","inverted","mode","channel","target","tag","entity_selector","message_block","gate","gate_type","range","delay","cooldown","power","distance","extend","pulse","chance","transfer_rate","generation_rate","eu_use","tier","tier_level","input1","output1","anchor_id","targets","rate"]

# Which of those names hold numbers, in one place beside the registry itself.
#
# props/probe works a type out from the value that is stored, which is right until
# the stored value is already wrong: a delay that somehow became the string "5"
# probes as a string, so the editor asks for text, the player types 5, and it
# writes a string again. The field can never climb back out, and every reader of
# it is measuring the length of a word.
#
# This is the fix, and it lives HERE rather than in seven per-block functions,
# because the point of the registry is that adding a property means editing one
# list. `channel`, `anchor_id`, `tag` and `entity_selector` are deliberately
# absent: they are string identifiers that blocks compare, and a channel called
# "5" must stay the text 5 -- turning one into a number is the v5.1.6 bug that
# stopped a vault matching its partner.
data modify storage ra:dh numeric set value ["range","delay","cooldown","power","distance","extend","pulse","chance","transfer_rate","generation_rate","eu_use","tier_level","rate"]
