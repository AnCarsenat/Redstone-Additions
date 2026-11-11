package ra.modules.farming.net

import io.github.ayfri.kore.features.tags.entityTypeTag
import ra.core.Ns
import ra.core.Ra
import ra.core.define
import ra.core.raw
import ra.core.ref

/**
 * Which mobs the nets catch, as entity type tags, and the one function that names a mob's type.
 *
 * - [SMALL] (`#ra_farming:capture_net/small`): the Capture Net's. Farm animals and small, peaceful
 *   creatures. No allay: it takes any item it is offered, the net included.
 * - [ANY] (`#ra_farming:capture_net/any`): the Master Capture Net's. Every mob but the wither, the
 *   ender dragon and players. Armor stands and mannequins are not mobs.
 * - [HOSTILE] (`#ra_farming:capture_net/hostile`): the mobs Peaceful removes. A net never lets one
 *   out on Peaceful, where it would vanish the next tick.
 *
 * A mob's type id cannot be read off the entity, so [typeOf] tests it against each type in [ANY]
 * in turn: one function call per catch, never per tick.
 */
object CaptureNetMobs {
	val SMALL = listOf(
		"armadillo", "axolotl", "bat", "bee", "cat", "chicken", "cod", "cow", "fox", "frog",
		"glow_squid", "goat", "mooshroom", "ocelot", "parrot", "pig", "pufferfish", "rabbit",
		"salmon", "sheep", "squid", "tadpole", "tropical_fish", "turtle", "wolf",
	)

	val HOSTILE = listOf(
		"blaze", "bogged", "breeze", "camel_husk", "cave_spider", "creaking", "creeper", "drowned",
		"elder_guardian", "enderman", "endermite", "evoker", "ghast", "giant", "guardian", "hoglin",
		"husk", "illusioner", "magma_cube", "parched", "phantom", "piglin", "piglin_brute", "pillager",
		"ravager", "shulker", "silverfish", "skeleton", "slime", "spider", "stray", "sulfur_cube",
		"vex", "vindicator", "warden", "witch", "wither_skeleton", "zoglin", "zombie",
		"zombie_nautilus", "zombie_villager", "zombified_piglin",
	)

	val ANY = (SMALL + HOSTILE + listOf(
		"allay", "camel", "copper_golem", "dolphin", "donkey", "happy_ghast", "horse", "iron_golem",
		"llama", "mule", "nautilus", "panda", "polar_bear", "skeleton_horse", "sniffer", "snow_golem",
		"strider", "trader_llama", "villager", "wandering_trader", "zombie_horse",
	)).distinct().sorted()

	const val SMALL_TAG = "#ra_farming:capture_net/small"
	const val ANY_TAG = "#ra_farming:capture_net/any"
	const val HOSTILE_TAG = "#ra_farming:capture_net/hostile"

	/**
	 * As the mob: `storage ra:farming net.mob` gets `id` and `type` (its name, a translated text
	 * component). Nothing is written for a type outside [ANY].
	 */
	val typeOf = ref(Ns.FARMING, "capture_net/type_of")

	/**
	 * `storage ra:farming capture_net.{small,any}."<id>"`: the same lists as maps, for the check a
	 * net's stored type passes before it is summoned ([allowed]). Written at the module load.
	 */
	fun allowMapsSnbt(): String {
		fun map(list: List<String>) = list.joinToString(",", "{", "}") { "\"minecraft:$it\":1b" }
		return "{small:${map(SMALL)},any:${map(ANY)}}"
	}

	/**
	 * `{id}`, as the player holding a full net: 1 when a net of that tier may let this type out
	 * (a Master net: [ANY]; a Capture Net: [SMALL]), 0 otherwise. `id` passed `ra_lib:safe/check`
	 * (format ID), so it holds no quote and is safe inside the path.
	 */
	val allowed = ref(Ns.FARMING, "capture_net/allowed")

	fun Ra.registerCaptureNetMobs() {
		pack.define(allowed) {
			raw("\$execute unless data storage ${CaptureNet.STORE} capture_net.any.\"\$(id)\" run return 0")
			raw("execute if items entity @s weapon.mainhand ${CaptureNet.net(CaptureNet.MASTER)} run return 1")
			raw("\$execute if data storage ${CaptureNet.STORE} capture_net.small.\"\$(id)\" run return 1")
			raw("return 0")
		}

		for ((name, list) in listOf("small" to SMALL, "any" to ANY, "hostile" to HOSTILE)) {
			pack.entityTypeTag("capture_net/$name", Ns.FARMING.id) {
				for (id in list) add("minecraft:$id")
			}
		}

		pack.define(typeOf) {
			for (id in ANY) {
				raw("execute if entity @s[type=minecraft:$id] run return run data modify storage ${CaptureNet.STORE} net.mob merge value {id:\"minecraft:$id\",type:{translate:\"entity.minecraft.$id\"}}")
			}
		}
	}
}
