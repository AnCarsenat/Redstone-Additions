# /ra_interactive:blocks/item_pipe/filter_match_id
# Input: storage ra:temp pipe_item.id
# Output: return 1 if storage ra:temp filter_item.id matches, else 0

$execute if data storage ra:temp filter_item{id:"$(id)"} run return 1
return 0
