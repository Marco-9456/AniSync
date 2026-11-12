package com.anisync.android.data

import com.anisync.android.domain.Supporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SupportersJsonTest {

    private fun parse(vararg entries: String) =
        SupportersJson.parse("""{"supporters":[${entries.joinToString(",")}]}""")

    @Test
    fun newestFirstWithAnAnonymousTipLeadingItsDay() {
        val list = parse(
            """{"name":"Rafik","date":"2026-07-25"}""",
            """{"name":"Warren","date":"2026-09-25"}""",
            """{"name":"Evie","date":"2026-09-25"}""",
            """{"date":"2026-09-25"}""",
        )
        assertEquals(listOf(null, "Evie", "Warren", "Rafik"), list?.map { it.name })
    }

    @Test
    fun blankNameIsAnonymous() {
        val list = parse("""{"name":"  ","date":"2026-07-21"}""")
        assertEquals(listOf(Supporter(name = null, date = LocalDate.of(2026, 7, 21))), list)
    }

    @Test
    fun entryWithoutAUsableDateIsDropped() {
        val list = parse(
            """{"name":"Evie","date":"2026-11-12"}""",
            """{"name":"Nobody","date":"yesterday"}""",
            """{"name":"Nowhen"}""",
        )
        assertEquals(listOf("Evie"), list?.map { it.name })
    }

    @Test
    fun onlyHttpsProfilesAreKept() {
        val list = parse(
            """{"name":"a","date":"2026-01-02","url":"https://github.com/a"}""",
            """{"name":"b","date":"2026-01-01","url":"javascript:alert(1)"}""",
        )
        assertEquals(listOf("https://github.com/a", null), list?.map { it.url })
    }

    @Test
    fun theOldSponsorsFileIsNotASupportersFile() {
        assertNull(SupportersJson.parse("""{"sponsors":[],"updatedAt":"2026-05-16T16:19:09Z"}"""))
        assertNull(SupportersJson.parse("<html>404</html>"))
    }

    @Test
    fun anEmptyListIsStillAList() {
        assertEquals(emptyList<Supporter>(), SupportersJson.parse("""{"supporters":[]}"""))
    }

    @Test
    fun theBundledFileParses() {
        val list = SupportersJson.parse(File("src/main/assets/supporters.json").readText())
        assertNotNull(list)
        assertTrue(list!!.isNotEmpty())
    }
}
