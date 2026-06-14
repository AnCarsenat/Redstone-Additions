# /ra_lib:orientation/set_facing {f:N,yaw:Nf,pitch:Nf}
# Internal helper. Sets facing score and rotation storage.

$scoreboard players set @s ra.facing $(f)
$data modify storage ra:temp Rotation set value [$(yaw), $(pitch)]
$execute store result storage ra:temp facing int 1 run scoreboard players set #facing ra.temp $(f)
