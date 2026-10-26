package com.paperscreen

/** The home screen's favourite apps: an ordered list of flattened ComponentNames. */
object Favourites {
    const val MAX = 10

    fun decode(text: String?): List<String> =
        text.orEmpty().lines().filter { it.isNotBlank() }.distinct().take(MAX)

    fun encode(list: List<String>): String = list.joinToString("\n")

    /** Adds at the end; unchanged if it's already there or the list is full. */
    fun add(list: List<String>, app: String): List<String> =
        if (app in list || list.size >= MAX) list else list + app

    fun remove(list: List<String>, app: String): List<String> = list - app

    /** Moves [app] by [by] places (negative is up), stopping at either end. */
    fun move(list: List<String>, app: String, by: Int): List<String> {
        val from = list.indexOf(app)
        if (from < 0) return list
        val to = (from + by).coerceIn(0, list.lastIndex)
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }
}
