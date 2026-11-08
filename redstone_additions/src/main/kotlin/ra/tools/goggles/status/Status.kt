package ra.tools.goggles.status

import ra.api.Goggles
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw

/**
 * Multiblock-specific status lines (heat). As the multiblock marker, at its base.
 *
 * There used to be an "Enabled:" line as well, read from `data.properties.enabled`. That property
 * was removed in v6.0.1 (a structure's redstone control is its only off switch), so the line went
 * with it.
 */
internal fun Ra.registerGogglesStatus() = pack.define(Goggles.Status.readStatusMultiblock) {
	// Heat is scoreboard-backed for the blast forge and updates live.
	raw("""data modify storage ra:temp status_literal set value {label:"Heat: ",value:"N/A",value_color:"gold",suffix:"/1000"}""")
	raw("execute if score @s ra.heat matches -2147483648..2147483647 store result storage ra:temp status_literal.value int 1 run scoreboard players get @s ra.heat")
	callWith(Goggles.Billboard.showLiteralLine, "storage ra:temp status_literal")
}
