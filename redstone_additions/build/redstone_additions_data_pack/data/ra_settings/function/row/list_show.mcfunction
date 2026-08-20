# /ra_settings:row/list_show {v:N}
# Internal: resolve choice N of the current row into cur.show.
#
# Falls back to the raw index when the stored value is out of range, which can
# happen when a module ships a shorter list than the one a value was saved
# against. Better a visible number than a blank bracket.

data modify storage ra:settings cur.show set value "?"
$execute if data storage ra:settings cur.values[$(v)] run data modify storage ra:settings cur.show set from storage ra:settings cur.values[$(v)]
