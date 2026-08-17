# /ra:tools/data_handler/props/write_number {name}
$data modify entity @e[type=marker,tag=ra.dh_target,limit=1] data.properties.$(name) set from storage ra:input consume.number
