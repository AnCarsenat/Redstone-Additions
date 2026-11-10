package ra.modules.farming

import io.github.ayfri.kore.arguments.types.resources.FunctionArgument
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.functions.Function
import ra.core.Obj
import ra.core.Ra
import ra.core.define
import ra.core.raw
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.arguments.types.literals.self
import ra.core.executeRun

/**
 * The Breeder's food table: `blocks/breeder/try_breed` and one `blocks/breeder/breed/<name>`
 * per (animal, food) pair, generated from [breeds].
 *
 * Every breed function runs at the breeder and looks one block in front of it (`positioned
 * ^ ^ ^1`) for an adult (`Age:0`) not already in love (`InLove:0`), puts it in love, and consumes
 * the food through `breed_success`, which sets `@s ra.temp` to 1 so the rest of the chain is
 * skipped.
 */
object BreederBreeds {
	private val block = Breeder.block

	/** How a pair breeds. */
	enum class Kind {
		/** One animal type, one food. */
		SIMPLE,
		/** Horses and llamas: only tamed ones, and two animal types share the food. */
		TAMED,
		/** The bucket comes back as a water bucket instead of being consumed. */
		BUCKET,
	}

	/**
	 * @property name `breed/<name>`.
	 * @property gate the food `try_breed` checks the container for before calling, null when the
	 *   function looks through several foods itself (bees, wolves).
	 */
	class Breed(
		val name: String,
		val gate: String?,
		val animals: List<String> = emptyList(),
		val food: String = gate ?: "",
		val kind: Kind = Kind.SIMPLE,
	) {
		val fn: FunctionArgument = block.fn("breed/$name")
	}

	private fun simple(name: String, animal: String, food: String) = Breed(name, food, listOf(animal))

	private fun tamed(name: String, animals: List<String>, food: String) = Breed(name, food, animals, kind = Kind.TAMED)

	private val horses = listOf("horse", "donkey")

	/** In try_breed order: the first pair whose food and animal are both present wins. */
	val breeds = listOf(
		simple("cow", "cow", "wheat"),
		simple("mooshroom", "mooshroom", "wheat"),
		simple("sheep", "sheep", "wheat"),
		simple("pig_carrot", "pig", "carrot"),
		simple("pig_potato", "pig", "potato"),
		simple("pig_beetroot", "pig", "beetroot"),
		simple("chicken_wheat_seeds", "chicken", "wheat_seeds"),
		simple("chicken_melon_seeds", "chicken", "melon_seeds"),
		simple("chicken_pumpkin_seeds", "chicken", "pumpkin_seeds"),
		simple("chicken_beetroot_seeds", "chicken", "beetroot_seeds"),
		simple("chicken_torchflower_seeds", "chicken", "torchflower_seeds"),
		simple("chicken_pitcher_pod", "chicken", "pitcher_pod"),
		simple("goat", "goat", "wheat"),
		simple("rabbit_carrot", "rabbit", "carrot"),
		simple("rabbit_golden_carrot", "rabbit", "golden_carrot"),
		simple("rabbit_dandelion", "rabbit", "dandelion"),
		tamed("horse_golden_apple", horses, "golden_apple"),
		tamed("horse_golden_carrot", horses, "golden_carrot"),
		tamed("horse_enchanted_golden_apple", horses, "enchanted_golden_apple"),
		tamed("llama", listOf("llama", "trader_llama"), "hay_block"),
		simple("turtle", "turtle", "seagrass"),
		// Pandas need 8 bamboo blocks within 5 blocks to breed normally; the breeder bypasses that.
		simple("panda", "panda", "bamboo"),
		simple("fox_sweet_berries", "fox", "sweet_berries"),
		simple("fox_glow_berries", "fox", "glow_berries"),
		// Bees take any flower and wolves any meat; both look through their foods themselves.
		Breed("bee", null),
		Breed("wolf", null),
		simple("cat_cod", "cat", "cod"),
		simple("cat_salmon", "cat", "salmon"),
		Breed("axolotl", "tropical_fish_bucket", listOf("axolotl"), kind = Kind.BUCKET),
		simple("strider", "strider", "warped_fungus"),
		simple("hoglin", "hoglin", "crimson_fungus"),
		simple("camel", "camel", "cactus"),
		simple("sniffer", "sniffer", "torchflower_seeds"),
		simple("frog", "frog", "slime_ball"),
		simple("armadillo", "armadillo", "spider_eye"),
	)

	/** Flowers a bee takes, first match wins. */
	private val flowers = listOf(
		"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
		"white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "torchflower",
	)

	/** Meats a wolf takes, raw or cooked, first match wins. */
	private val meats = listOf(
		"beef", "cooked_beef", "porkchop", "cooked_porkchop", "chicken", "cooked_chicken",
		"mutton", "cooked_mutton", "rabbit", "cooked_rabbit", "rotten_flesh",
	)

	private val beeConsumeFlower = block.fn("breed/bee_consume_flower")

	private fun ready(animal: String, extra: String = "") = "@e[type=$animal,distance=..2,limit=1,nbt={Age:0,InLove:0$extra}]"

	fun Ra.registerBreederBreeds() {
		for (b in breeds) pack.define(b.fn) {
			when {
				b.name == "bee" -> bee()
				b.name == "wolf" -> wolf()
				b.kind == Kind.TAMED -> tamed(b)
				else -> simple(b)
			}
		}
		pack.define(beeConsumeFlower) { beeConsumeFlower() }
	}

	/**
	 * Try to breed the animals in front. As the breeder marker, at the breeder.
	 *
	 * THE FOOD IS THE BREEDER'S OWN. Every container test and every consume runs at the breeder
	 * (`block ~ ~ ~`); only the animal searches move one block forward (`positioned ^ ^ ^1`). The
	 * beet pack ran the breed functions from `^ ^ ^-1`, so the food was looked for in whatever
	 * container stood BEHIND the breeder -- while the tick only called this when the breeder's own
	 * inventory had something in it, and the tamed animals (horses, llamas) ate from the breeder's
	 * own inventory. A breeder stocked the way the docs say bred nothing.
	 */
	fun Function.tryBreed() {
		scoreboard.players.set(self(), Obj.TEMP, 0)

		// ONE CHECK BEFORE THIRTY-FIVE. What follows is a line per (animal, food) pair, and every
		// one of them runs an `if items ... container.*` against the same container before the
		// first can match. That is thirty-five commands per powered breeder per tick, paid in full
		// whenever nothing matches -- which is nearly always, because animals wander off, and the
		// ones that are there spend most of their time either already in love or still on their
		// breeding cooldown.
		//
		// The pairs all end up asking the same question first: is there an animal in front of me
		// that could breed right now? Asking it once, with an entity type tag, costs one command
		// and skips the other thirty-five outright. It repeats the Age/InLove test each breed
		// function does for itself, which is the point -- those tests were the reason the chain ran
		// to the end and found nothing.
		//
		// Measured from the block in front, with the same radius and the same NBT test the breed
		// functions use.
		raw("execute positioned ^ ^ ^1 unless entity ${ready("#${Breeder.BREEDABLE_ID}")} run return 0")

		for (b in breeds) {
			val items = if (b.gate == null) "" else " if items block ~ ~ ~ container.* ${b.gate}"
			executeRun("if score @s ${Obj.TEMP} matches 0$items", b.fn)
		}
	}

	private fun Function.simple(b: Breed) {
		val animal = b.animals.single()
		raw("execute positioned ^ ^ ^1 unless entity ${ready(animal)} run return 0")
		raw("execute positioned ^ ^ ^1 as ${ready(animal)} run data modify entity @s InLove set value 600")
		raw("data modify storage ra:temp breed_item set value \"${b.food}\"")
		function(if (b.kind == Kind.BUCKET) Breeder.breedSuccessBucket else Breeder.breedSuccess)
	}

	/**
	 * Tamed animals, each type in turn. The food is consumed when a tamed adult is there, in love
	 * or not -- the second and third tests leave `InLove` out, as the beet pack had them.
	 */
	private fun Function.tamed(b: Breed) {
		b.animals.forEachIndexed { i, animal ->
			if (i > 0) raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")
			val tame = ",Tame:1b"
			raw("execute positioned ^ ^ ^1 if entity ${ready(animal, tame)} as ${ready(animal, tame)} run data modify entity @s InLove set value 600")
			val adult = "@e[type=$animal,distance=..2,limit=1,nbt={Age:0$tame}]"
			raw("execute positioned ^ ^ ^1 if entity $adult run data modify storage ra:temp breed_item set value \"${b.food}\"")
			// Tested in front, consumed back at the breeder.
			executeRun("positioned ^ ^ ^1 if entity $adult positioned ^ ^ ^-1", Breeder.breedSuccess)
		}
	}

	/** Bees: any flower. `bee_consume_flower` puts the bee in love and eats the first flower found. */
	private fun Function.bee() {
		raw("execute positioned ^ ^ ^1 unless entity ${ready("bee")} run return 0")
		// The source's trailing `if score @s ra.temp matches 1 run return 1` never reached the build
		// that shipped, and nothing reads this function's result; the shipped shape is kept.
		function(beeConsumeFlower)
	}

	/** One flower from the breeder for bee breeding. At the breeder block. */
	private fun Function.beeConsumeFlower() {
		flowers.forEachIndexed { i, flower ->
			if (i > 0) raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")
			raw("execute if items block ~ ~ ~ container.* $flower positioned ^ ^ ^1 as ${ready("bee")} run data modify entity @s InLove set value 600")
			raw("execute if items block ~ ~ ~ container.* $flower run data modify storage ra:temp breed_item set value \"$flower\"")
			executeRun("if items block ~ ~ ~ container.* $flower", Breeder.breedSuccess)
		}
	}

	/** Wolves: any raw or cooked meat, the first one found. */
	private fun Function.wolf() {
		raw("execute positioned ^ ^ ^1 unless entity ${ready("wolf")} run return 0")
		raw("execute positioned ^ ^ ^1 as ${ready("wolf")} run data modify entity @s InLove set value 600")
		meats.forEachIndexed { i, meat ->
			if (i > 0) raw("execute if score @s ${Obj.TEMP} matches 1 run return 1")
			raw("execute if items block ~ ~ ~ container.* $meat run data modify storage ra:temp breed_item set value \"$meat\"")
			executeRun("if items block ~ ~ ~ container.* $meat", Breeder.breedSuccess)
		}
	}
}
