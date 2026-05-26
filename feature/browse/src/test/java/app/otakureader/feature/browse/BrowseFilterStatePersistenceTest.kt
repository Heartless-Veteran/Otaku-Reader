package app.otakureader.feature.browse

import app.otakureader.sourceapi.Filter
import app.otakureader.sourceapi.FilterList
import app.otakureader.sourceapi.Filters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BrowseFilterStatePersistenceTest {

    private fun buildFilters(
        select: Int = 0,
        text: String = "",
        checkbox: Boolean = false,
        triState: Int = Filter.TriState.STATE_IGNORE,
        sort: Filter.Sort.Selection? = null,
        groupChild: Int = Filter.TriState.STATE_IGNORE,
    ) = FilterList(
        Filters.SelectFilter("Status", arrayOf("Any", "Ongoing", "Completed"), select),
        Filters.TextFilter("Author", text),
        Filters.CheckBoxFilter("Completed only", checkbox),
        Filters.TriStateFilter("Action", triState),
        Filters.SortFilter("Sort", arrayOf("Title", "Date"), sort),
        Filters.GroupFilter("Genres", listOf(Filters.TriStateFilter("Romance", groupChild))),
    )

    @Test
    fun `encode then apply restores all filter states`() {
        val active = buildFilters(
            select = 2,
            text = "Naruto",
            checkbox = true,
            triState = Filter.TriState.STATE_INCLUDE,
            sort = Filter.Sort.Selection(1, ascending = false),
            groupChild = Filter.TriState.STATE_EXCLUDE,
        )

        val encoded = BrowseFilterStatePersistence.encode(active)
        assertNotNull(encoded)

        val restored = buildFilters()
        BrowseFilterStatePersistence.apply(restored, encoded!!)

        assertEquals(2, (restored.filters[0] as Filter.Select<*>).state)
        assertEquals("Naruto", (restored.filters[1] as Filter.Text).state)
        assertEquals(true, (restored.filters[2] as Filter.CheckBox).state)
        assertEquals(Filter.TriState.STATE_INCLUDE, (restored.filters[3] as Filter.TriState).state)
        val sort = (restored.filters[4] as Filter.Sort).state
        assertEquals(1, sort?.index)
        assertEquals(false, sort?.ascending)
        val groupChild = ((restored.filters[5] as Filter.Group<*>).state
            .first() as Filter.TriState)
        assertEquals(Filter.TriState.STATE_EXCLUDE, groupChild.state)
    }

    @Test
    fun `encode returns null for empty filter list`() {
        assertNull(BrowseFilterStatePersistence.encode(FilterList()))
    }

    @Test
    fun `apply with malformed payload leaves filters unchanged`() {
        val restored = buildFilters(select = 1)
        BrowseFilterStatePersistence.apply(restored, "not-valid-json")
        assertEquals(1, (restored.filters[0] as Filter.Select<*>).state)
    }
}
