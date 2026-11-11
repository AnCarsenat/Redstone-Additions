package ra.modules.wires.fluid

import io.github.ayfri.kore.features.predicates.providers.constant
import io.github.ayfri.kore.features.predicates.providers.floorDiv
import io.github.ayfri.kore.features.predicates.providers.max
import io.github.ayfri.kore.features.predicates.providers.mul
import ra.core.computeInto
import ra.core.provider
import ra.core.scoreOf
import ra.core.storeInt
import io.github.ayfri.kore.commands.data
import io.github.ayfri.kore.commands.function
import ra.api.RaLib
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.core.Storage
import ra.core.block.particleViewersSel
import ra.core.define
import ra.core.executeRun
import ra.core.raw
import ra.core.ref

object DrainPotion : PackPart {
	val drainPotion = ref(Ns.WIRES, "fluid/drain_potion")
	val drainPotionGive = ref(Ns.WIRES, "fluid/drain_potion_give")
	val drainPotionNext = ref(Ns.WIRES, "fluid/drain_potion_next")
	val drainPotionPreset = ref(Ns.WIRES, "fluid/drain_potion_preset")
	val drainPotionPresetRun = ref(Ns.WIRES, "fluid/drain_potion_preset_run")
	val drainPotionRead = ref(Ns.WIRES, "fluid/drain_potion_read")
	val drainPotionScale = ref(Ns.WIRES, "fluid/drain_potion_scale")
	val drainPotionColor = ref(Ns.WIRES, "fluid/drain_potion_color")
	val drainPotionColorPreset = ref(Ns.WIRES, "fluid/drain_potion_color_preset")
	val drainPotionColorEffect = ref(Ns.WIRES, "fluid/drain_potion_color_effect")
	val drainPotionParticle = ref(Ns.WIRES, "fluid/drain_potion_particle")

	/** A potion with no colour of its own (water, awkward, unknown): the vanilla water bottle's blue. */
	private const val DEFAULT_COLOR = 3694022

	override fun Ra.register() {
		pack.define(DrainPotion.drainPotion) {
			// Pour the network's potion over whoever is standing by the drain.
			// Context: as the drain marker, at the block in front of it (ra_wires:fluid/drain_pour).
			//
			// The output side of the potion medium. A potion has no world block to place, so
			// the thing a drain does with it is pour it out as a cloud in the potion's colour
			// and apply it -- up to one action's worth (Drain.ML_PER_ACTION, five bottles) per
			// operation, to every player within four blocks.
			//
			// DURATION FOLLOWS THE VOLUME
			// The issue this implements asks for "a duration that matches the amount being
			// drained and the potion duration field". So the effect's own duration is the
			// duration of one full bottle, and a partial draw is a proportional share of it:
			// 250 mL of a three-minute strength potion is forty-five seconds. Draining a
			// network dry gives a shorter effect than draining a full one, which is what
			// makes a tank farm of potion worth building.
			//
			// The level is the potion's own and is not scaled. Amplifier is a step, not a
			// quantity -- half a bottle of strength II is still strength II, for less time.

			raw("execute if score @s ra.tr.net matches ..0 run return 0")

			function(RaLib.Transport.Net.read)
			raw("execute unless data storage ra:transport cur.amounts.potion run return run data modify entity @s data.status.drain_state set value \"nothing_to_pour\"")

			// The colour is read before the take: a pour that empties the network must still show
			// the potion it poured, not the default blue. pc.id is the network id for the macro.
			data(Storage.WIRES) { remove("pc") }
			raw("data modify storage ra:wires pc.color set value $DEFAULT_COLOR")
			raw("execute store result storage ra:wires pc.id int 1 run scoreboard players get @s ra.tr.net")
			function(DrainPotion.drainPotionColor, Storage.WIRES, "pc")

			executeRun("store result score #pt.got ra.wires.tmp", RaLib.Transport.Net.take, """{amount:${Drain.ML_PER_ACTION},medium:"potion"}""")
			raw("execute if score #pt.got ra.wires.tmp matches ..0 run data remove storage ra:wires pc")
			raw("execute if score #pt.got ra.wires.tmp matches ..0 run return run data modify entity @s data.status.drain_state set value \"nothing_to_pour\"")

			// Nobody to pour it over is not a failure, but the potion has already left the
			// network, so say so rather than silently binning it.
			raw("execute unless entity @a[distance=..4] run data modify entity @s data.status.drain_state set value \"poured_away\"")
			raw("execute if entity @a[distance=..4] run data modify entity @s data.status.drain_state set value \"applying\"")

			data(Storage.WIRES) { remove("apply") }
			raw("execute store result storage ra:wires apply.got int 1 run scoreboard players get #pt.got ra.wires.tmp")
			raw("execute store result storage ra:wires apply.id int 1 run scoreboard players get @s ra.tr.net")
			function(DrainPotion.drainPotionRead, Storage.WIRES, "apply")

			function(DrainPotion.drainPotionParticle, Storage.WIRES, "pc")
			data(Storage.WIRES) { remove("pc") }
		}

		pack.define(DrainPotion.drainPotionColor) {
			// Internal: the colour of this network's potion, into storage ra:wires pc.color.
			// Macro: {id} (the network id, pc.id). Run before the take, see drain_potion.
			//
			// A potion that names its colour (custom_color) is that colour. Otherwise it is the
			// colour of its first effect -- custom_effects first, then the preset's table entry --
			// looked up in ra:wires effect_colors (ra_wires:media/potions). That is the colour a
			// one-effect vanilla potion has; a mixed one shows its first effect's.

			raw("\$execute if data storage ra:transport nets.n\$(id).potion.custom_color run return run data modify storage ra:wires pc.color set from storage ra:transport nets.n\$(id).potion.custom_color")
			raw("\$data modify storage ra:wires pc.e set from storage ra:transport nets.n\$(id).potion.custom_effects[0].id")
			raw("\$execute unless data storage ra:wires pc.e run data modify storage ra:wires pc.potion set from storage ra:transport nets.n\$(id).potion.potion")
			raw("execute unless data storage ra:wires pc.e if data storage ra:wires pc.potion run function ${DrainPotion.drainPotionColorPreset.asId()} with storage ra:wires pc")
			raw("execute if data storage ra:wires pc.e run function ${DrainPotion.drainPotionColorEffect.asId()} with storage ra:wires pc")
		}

		pack.define(DrainPotion.drainPotionColorPreset) {
			// Internal: a preset potion's first effect. Macro: {potion}.
			raw("\$data modify storage ra:wires pc.e set from storage ra:wires potion_effects.\"\$(potion)\"[0].e")
		}

		pack.define(DrainPotion.drainPotionColorEffect) {
			// Internal: an effect's colour, when the table has it. Macro: {e}.
			raw("\$data modify storage ra:wires pc.color set from storage ra:wires effect_colors.\"\$(e)\"")
		}

		pack.define(DrainPotion.drainPotionParticle) {
			// Internal: the poured potion, as a cloud of its colour. Macro: {color}.
			// Context: at the block in front of the drain.
			raw("\$particle minecraft:dust{color:\$(color),scale:1.5f} ~ ~ ~ 0.4 0.4 0.4 0 16 normal ${particleViewersSel(16)}")
		}

		pack.define(DrainPotion.drainPotionGive) {
			// Internal: hand one scaled effect to everyone standing by the drain.
			// Context: as the drain marker, at the drain position.
			//
			// Four blocks, which is about arm's reach of the block plus the space in front of
			// it -- close enough that standing there is a decision.

			raw("\$effect give @a[distance=..4] \$(id) \$(secs) \$(amp) false")
		}

		pack.define(DrainPotion.drainPotionNext) {
			// Internal: apply the head of the effect list, then the rest.
			// Context: as the drain marker, at the drain position. #pt.vol holds the millilitres
			// that were actually drawn.
			//
			// The two shapes are normalised here rather than in two appliers. custom_effects
			// uses id/duration/amplifier; the preset table uses e/d/a, which keeps a
			// forty-entry table readable. Whichever is present is copied into the same three
			// fields before the scaling runs.

			raw("execute unless data storage ra:wires eff.list[0] run return 0")

			data(Storage.WIRES) { remove("one") }
			data(Storage.WIRES) { modify("one.id", Storage.WIRES, "eff.list[0].id") }
			data(Storage.WIRES) { modify("one.dur", Storage.WIRES, "eff.list[0].duration") }
			data(Storage.WIRES) { modify("one.amp", Storage.WIRES, "eff.list[0].amplifier") }
			data(Storage.WIRES) { modify("one.id", Storage.WIRES, "eff.list[0].e") }
			data(Storage.WIRES) { modify("one.dur", Storage.WIRES, "eff.list[0].d") }
			data(Storage.WIRES) { modify("one.amp", Storage.WIRES, "eff.list[0].a") }

			// An amplifier of 0 is legal and common, and an absent one means the same thing,
			// so it is seeded rather than guarded.
			raw("execute unless data storage ra:wires one.amp run data modify storage ra:wires one.amp set value 0")

			executeRun("if data storage ra:wires one.id", DrainPotion.drainPotionScale)

			data(Storage.WIRES) { remove("eff.list[0]") }
			function(DrainPotion.drainPotionNext)
		}

		pack.define(DrainPotion.drainPotionPreset) {
			// Internal: look a preset potion's effects up in the table.
			// Two steps, because the potion's own id has to be substituted into a path that
			// already needed the network id substituted into it.

			data(Storage.WIRES) { remove("pre") }
			raw("\$data modify storage ra:wires pre.potion set from storage ra:transport nets.n\$(id).potion.potion")
			raw("execute unless data storage ra:wires pre.potion run return 0")
			function(DrainPotion.drainPotionPresetRun, Storage.WIRES, "pre")
		}

		pack.define(DrainPotion.drainPotionPresetRun) {
			// Internal: the dynamic-name half of drain_potion_preset. Rewrites the table's
			// short field names into the same shape custom_effects uses, so the applier only
			// ever sees one format.
			//
			// A potion with no entry -- water, mundane, thick, awkward, or one added by a
			// future version -- leaves the list absent and nothing is applied.

			raw("\$execute unless data storage ra:wires potion_effects.\"\$(potion)\" run return 0")
			raw("\$data modify storage ra:wires eff.list set from storage ra:wires potion_effects.\"\$(potion)\"")
		}

		pack.define(DrainPotion.drainPotionRead) {
			// Internal: work out which effects this network's potion carries, and hand each
			// one to drain_potion_apply.
			//
			// A potion brewed with commands carries `custom_effects` on the item, which is
			// authoritative and needs no table. A preset potion carries only its id, so its
			// effects come out of ra_wires:media/potions. Custom first, because a potion can
			// carry both and the explicit list is the one that means something.

			raw("\$execute unless data storage ra:transport nets.n\$(id).potion run return 0")

			data(Storage.WIRES) { remove("eff") }
			raw("\$data modify storage ra:wires eff.list set from storage ra:transport nets.n\$(id).potion.custom_effects")
			raw("\$execute unless data storage ra:wires eff.list run function ${DrainPotion.drainPotionPreset.asId()} {id:\$(id)}")
			raw("execute unless data storage ra:wires eff.list[0] run return 0")

			raw("\$scoreboard players set #pt.vol ra.wires.tmp \$(got)")
			function(DrainPotion.drainPotionNext)
		}

		pack.define(DrainPotion.drainPotionScale) {
			// Internal: scale one effect's duration by how much was actually drawn, and apply it.
			//
			// ticks = duration * drawn / 1000
			//
			// 1000 mL is one bottle, and the table's durations are what one bottle gives. The
			// multiply happens before the divide so that a small draw does not round to
			// nothing on the way through -- 100 mL of a 3600 tick effect is 360 ticks, not
			// 3600 * (100/1000 = 0).
			//
			// Instant effects are stored with a duration of one tick, so they survive any
			// scaling as at least one tick and behave the way drinking one does.

			raw("execute store result score #pt.dur ra.wires.tmp run data get storage ra:wires one.dur")
			raw("execute if score #pt.dur ra.wires.tmp matches ..0 run return 0")

			// A draw too small to earn a whole tick still earns one: the player paid volume
			// for it, and an effect of zero ticks is a command error rather than a no-op.
			//
			// /effect give takes seconds, not ticks, and rounds down -- so an effect worth
			// less than a second would vanish. Ticks are what the table and the scaling work
			// in, so the conversion is done here and floored to one second.
			//
			// secs = max(1, max(1, dur * drawn / 1000) / 20), both divisions floored as the
			// scoreboard `/=` they replaced.
			val ticks = provider { max(constant(1), floorDiv(mul(scoreOf("#pt.dur", "ra.wires.tmp"), scoreOf("#pt.vol", "ra.wires.tmp")), constant(1000))) }
			computeInto(storeInt(Storage.WIRES, "one.secs"), provider { max(constant(1), floorDiv(ticks, constant(20))) })
			function(DrainPotion.drainPotionGive, Storage.WIRES, "one")
		}
	}
}
