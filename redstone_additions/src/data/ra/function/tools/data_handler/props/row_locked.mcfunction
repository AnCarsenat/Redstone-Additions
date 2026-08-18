# /ra:tools/data_handler/props/row_locked {name}
# Internal: a field the block does not let a survival player change.
#
# Shown, not censored. Hiding a field made a block look like it had fewer settings
# than it has, and left the player guessing why a number they could see in the
# Goggles was missing here. The value is printed exactly as any other row; only
# the button is dead, struck through, and says why on hover.

$tellraw @s [{text:"  $(name): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"properties.$(name)",storage:"ra:dh",color:"gray"},{text:"]",color:"dark_gray"},{text:" "},{text:"locked",color:"dark_gray"},{text:" "},{text:"[Modify]",color:"red",strikethrough:true,hover_event:{action:"show_text",value:"This block sets this itself — it cannot be changed here. Use the Creative Data Handler."}}]
