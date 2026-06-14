# /ra_lib:input/session/storage_create
# Macro storage shape: {req:<int>,mode:<int>}

$data modify storage ra:input sessions.req_$(req) set value {mode:$(mode),backend:0,state:"waiting",result:{}}
$data modify storage ra:input sessions.req_$(req).owner set from entity @s UUID