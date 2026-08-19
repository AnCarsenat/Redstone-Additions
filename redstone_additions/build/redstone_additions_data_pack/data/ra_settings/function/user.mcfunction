# /ra_settings:user {obj:"ra.u.snd",default:1}
# Read THIS PLAYER's value for a user setting into #setting ra.set.tmp.
# Context: as the player.
#
# A player who has never changed the setting has no score on the objective at
# all. That is not the same as a score of 0: `matches -2147483648..` is the only
# way to ask "does this player have a score here", because every comparison
# against an absent score is false. So the default stands until the player has
# actually chosen something, and a setting that defaults to ON is ON for everyone
# who has never opened the menu.

$scoreboard players set #setting ra.set.tmp $(default)
$execute if score @s $(obj) matches -2147483648.. store result score #setting ra.set.tmp run scoreboard players get @s $(obj)
return run scoreboard players get #setting ra.set.tmp
