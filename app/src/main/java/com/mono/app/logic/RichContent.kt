package com.mono.app.logic

import org.json.JSONArray
import org.json.JSONObject

enum class BlockType(val code: String) {
    Paragraph("P"), Bullet("B"), Numbered("N"), Check("C");

    companion object {
        fun fromCode(code: String?) = values().firstOrNull { it.code == code } ?: Paragraph
    }
}

/** One line/paragraph of a note. Bold/italic apply to the whole block. */
data class Block(
    val type: BlockType = BlockType.Paragraph,
    val text: String = "",
    val checked: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/**
 * Versioned JSON document: {"v":1,"blocks":[{"t":"P","x":"text","c":false,"b":false,"i":false}]}
 * Malformed input never throws: it is recovered as plain text, one paragraph per line.
 */
object RichContent {
    const val VERSION = 1

    fun encode(blocks: List<Block>): String {
        val arr = JSONArray()
        blocks.forEach {
            arr.put(
                JSONObject().put("t", it.type.code).put("x", it.text)
                    .put("c", it.checked).put("b", it.bold).put("i", it.italic)
            )
        }
        return JSONObject().put("v", VERSION).put("blocks", arr).toString()
    }

    fun decode(raw: String?): List<Block> {
        if (raw.isNullOrBlank()) return listOf(Block())
        return try {
            val arr = JSONObject(raw).getJSONArray("blocks")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Block(
                    BlockType.fromCode(o.optString("t")), o.optString("x"),
                    o.optBoolean("c"), o.optBoolean("b"), o.optBoolean("i"),
                )
            }.ifEmpty { listOf(Block()) }
        } catch (e: Exception) {
            raw.lines().map { Block(text = it) }
        }
    }

    fun plainText(raw: String?): String = decode(raw).joinToString("\n") { it.text }

    fun isBlank(blocks: List<Block>) = blocks.all { it.text.isBlank() }

    /** Non-empty blocks for card previews. */
    fun preview(raw: String?, max: Int = 5): List<Block> =
        decode(raw).filter { it.text.isNotBlank() }.take(max)
}
