package com.paperscreen

import org.junit.Assert.assertEquals
import org.junit.Test

class FavouritesTest {

    @Test
    fun roundTripsAndDropsDuplicates() {
        assertEquals(listOf("a", "b"), Favourites.decode(Favourites.encode(listOf("a", "b", "a"))))
        assertEquals(emptyList<String>(), Favourites.decode(null))
    }

    @Test
    fun addStopsAtTheLimit() {
        val full = (1..Favourites.MAX).map { "app$it" }
        assertEquals(full, Favourites.add(full, "extra"))
        assertEquals(listOf("a", "b"), Favourites.add(listOf("a", "b"), "a"))
        assertEquals(listOf("a", "b"), Favourites.add(listOf("a"), "b"))
    }

    @Test
    fun moveStopsAtEitherEnd() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), Favourites.move(list, "a", 1))
        assertEquals(listOf("c", "a", "b"), Favourites.move(list, "c", -5))
        assertEquals(list, Favourites.move(list, "a", -1))
        assertEquals(list, Favourites.move(list, "missing", 1))
    }
}
