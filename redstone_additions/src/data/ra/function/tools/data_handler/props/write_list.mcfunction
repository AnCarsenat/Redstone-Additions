# /ra:tools/data_handler/props/write_list {name,text}
$data modify entity @e[type=marker,tag=ra.dh_target,limit=1] data.properties.$(name) set value $(text)
