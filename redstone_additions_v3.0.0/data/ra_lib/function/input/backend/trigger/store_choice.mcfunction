# /ra_lib:input/backend/trigger/store_choice
# Macro storage shape: {req:<int>,value:<int>}

$data modify storage ra:input sessions.req_$(req).result.choice set value $(value)
$data modify storage ra:input sessions.req_$(req).state set value "ready"