# /ra_settings:prop {block:"electric_generator",prop:"generation_rate",default:60}
# The configured default for one block's property, into #setting ra.set.tmp.
#
# Falls back to the literal default when the admin has never touched it, so a
# caller does not have to care whether the settings page exists yet.

$scoreboard players set #setting ra.set.tmp $(default)
$execute if data storage ra:settings global.props."$(block)".$(prop) store result score #setting ra.set.tmp run data get storage ra:settings global.props."$(block)".$(prop)
return run scoreboard players get #setting ra.set.tmp
