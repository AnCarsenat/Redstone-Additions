# /ra_settings:admin/grant
# Give the nearest player lasting server-settings access.
#
# Running any function needs permission level 2, so this cannot be used by a
# player to promote themselves. It is the deliberate way to hand access to
# somebody who should have it without making them a full operator.

function ra_settings:admin_grant
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"You can now open server settings directly with the button, or ",color:"green"},{text:"/trigger ra.settings.open set 2",color:"yellow",click_event:{action:"suggest_command",command:"/trigger ra.settings.open set 2"}}]
