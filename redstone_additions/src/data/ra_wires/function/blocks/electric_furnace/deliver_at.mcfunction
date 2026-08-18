# /ra_wires:blocks/electric_furnace/deliver_at {result}
# Internal: insert one result into the container at the current position.
#
# insert is safe here only because the count is one and check_target already
# confirmed there is room -- see ra_lib:inventory/insert on why a larger count
# cannot be trusted.

$execute store result score #ef.done ra.wires.tmp run function ra_lib:inventory/insert {id:"$(result)",count:1,components:{}}
