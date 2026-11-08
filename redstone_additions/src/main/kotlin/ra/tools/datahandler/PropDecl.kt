package ra.tools.datahandler

/**
 * The type a block property is declared with, and so which editor the Data Handler gives it.
 * [code] is the `#dh.type` value the in-game probe produces for it (5 is a detected compound,
 * which cannot be declared: there is no editor for one).
 */
enum class PropType(val id: String, val code: Int) {
	TEXT("text", 0),
	NUMBER("number", 1),
	BOOL("bool", 2),
	LIST("list", 3),

	/** Text that is an item id; gets a [Set from hand] button. */
	ITEM("item", 4),
}

/**
 * One property's declaration: its name, its type and, for a number, the range the number form
 * accepts. Written to `storage ra:dh types` by `ra:tools/data_handler/init_registry`.
 */
data class PropDecl(val name: String, val type: PropType, val min: Int? = null, val max: Int? = null) {
	init {
		require(type == PropType.NUMBER || (min == null && max == null)) { "$name: only a number has bounds" }
	}

	/** `{type:"number",min:1,max:9999}` */
	fun snbt(): String = buildList {
		add("type:\"${type.id}\"")
		min?.let { add("min:$it") }
		max?.let { add("max:$it") }
	}.joinToString(",", "{", "}")

	companion object {
		fun text(name: String) = PropDecl(name, PropType.TEXT)
		fun number(name: String, min: Int = 0, max: Int = 9999) = PropDecl(name, PropType.NUMBER, min, max)
		fun bool(name: String) = PropDecl(name, PropType.BOOL)
		fun list(name: String) = PropDecl(name, PropType.LIST)
		fun item(name: String) = PropDecl(name, PropType.ITEM)
	}
}
