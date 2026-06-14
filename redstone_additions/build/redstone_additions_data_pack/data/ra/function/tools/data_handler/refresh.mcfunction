# /ra:tools/data_handler/refresh
# Refresh the stored data and re-show menu
# As player.

# Re-read data from target block
execute as @e[tag=ra.dh_target,limit=1] run function ra:tools/data_handler/update_storage

# Show menu again
function ra:tools/data_handler/show_menu
