# /ra_settings:placement/seed_at {t:"electric_generator"}
# Internal: merge this block type's configured defaults over its own.
#
# merge, not set: the block's placement spec has already written whatever
# properties it needs, and only the ones an admin has actually configured should
# be replaced. A `set` would delete every property the settings page does not
# happen to mention.

$execute if data storage ra:settings global.props."$(t)" run data modify entity @s data.properties merge from storage ra:settings global.props."$(t)"
