# /ra:tools/wrench/menu_row {label,prop,i}
# Internal: one row -- name, current value, and a button that cycles it.
#
# The action number carried by the button is the entry's index, so the handler
# only has to look the same list up again rather than keep any state of its own.

$tellraw @a[tag=ra.wrench_user,limit=1] [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"data.properties.$(prop)",entity:"@e[tag=ra.wrench.sel,limit=1]",color:"aqua"},{text:"]",color:"dark_gray"},{text:"  "},{text:"[ CYCLE ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Step this setting to its next value"},click_event:{action:"run_command",command:"/trigger ra.wrench set $(i)"}}]
