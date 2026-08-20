# /ra_settings:row/seed_user {obj,default}
# Internal: give this player a score on a user setting if they have none.

$execute unless score @s $(obj) matches -2147483648.. run scoreboard players set @s $(obj) $(default)
