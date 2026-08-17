# /ra_wires:blocks/place_join {net:"fluid",capacity:N}
# Internal: enrol a block in a transport network.
# Only reached for specs that declare a network, so both fields are present.

$function ra_lib:transport/net/join {class:"$(net)",capacity:$(capacity)}
