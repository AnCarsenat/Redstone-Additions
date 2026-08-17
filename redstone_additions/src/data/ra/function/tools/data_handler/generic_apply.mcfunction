# /ra:tools/data_handler/generic_apply
# Write a finished input into the property it was asked for. As player.
# storage ra:dh pending_name / pending_kind say which property and which form.

execute unless data storage ra:dh pending_name run return 0

data modify storage ra:dh q set value {}
data modify storage ra:dh q.name set from storage ra:dh pending_name

execute if data storage ra:dh {pending_kind:"number"} run function ra:tools/data_handler/props/write_number with storage ra:dh q
execute if data storage ra:dh {pending_kind:"text"} run function ra:tools/data_handler/props/write_text with storage ra:dh q

# A list is typed rather than quoted, so the text is pasted into the command as
# SNBT. Bad syntax fails the macro and leaves the property alone.
data modify storage ra:dh q.text set from storage ra:input consume.text
execute if data storage ra:dh {pending_kind:"list"} run function ra:tools/data_handler/props/write_list with storage ra:dh q
