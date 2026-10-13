package com.anisync.android.data.network

import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.ThemeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Locks the reading of the AnimeThemes GraphQL response. The shapes are trimmed from live
 * responses for One Piece and Bocchi the Rock.
 */
class AnimeThemesApiTest {

    private val api = AnimeThemesApi()

    @Test
    fun `errors with HTTP 200 fail instead of reading as no themes`() {
        val body = """
            {"errors":[{"message":"Query complexity of 10200 exceeds the maximum of 10000"}]}
        """.trimIndent()

        val error = assertThrows(AnimeThemesException.Query::class.java) { api.decodeThemes(body) }
        assertEquals(listOf("Query complexity of 10200 exceeds the maximum of 10000"), error.messages)
    }

    @Test
    fun `errors next to partial data still fail`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[]},"errors":[{"message":"Max query depth"}]}
        """.trimIndent()

        assertThrows(AnimeThemesException.Query::class.java) { api.decodeThemes(body) }
    }

    @Test
    fun `a title AnimeThemes does not list is an empty answer`() {
        assertEquals(MediaThemes(), api.decodeThemes("""{"data":{"findAnimeByExternalSite":[]}}"""))
    }

    @Test
    fun `themes, versions and videos map through the connection nodes`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"one_piece","animethemes":[
              {"id":950,"type":"OP","sequence":1,"slug":"OP1",
               "song":{"title":{"romaji":"We Are!"},"performances":[
                 {"relevance":1,"artist":{"name":{"main":"Hiroshi Kitadani"}}}]},
               "animethemeentries":[
                 {"id":1058,"version":1,"episodes":"1-47","notes":null,"spoiler":false,"nsfw":false,
                  "videos":{"nodes":[
                    {"id":13360,"link":"https://v.animethemes.moe/OnePiece-OP1-NCDVD480.webm",
                     "resolution":480,"nc":true,"subbed":false,"lyrics":false,"source":"DVD"}]}}]},
              {"id":2001,"type":"OP","sequence":1,"slug":"OP1-EN","song":null,
               "animethemeentries":[{"id":3001,"version":1,"episodes":null,"spoiler":false,
                 "nsfw":false,"videos":{"nodes":[]}}]}
            ]}]}}
        """.trimIndent()

        val themes = api.decodeThemes(body)

        assertEquals("one_piece", themes.animeSlug)
        val op1 = themes.themes.single()
        assertEquals(ThemeType.OP, op1.type)
        assertEquals("We Are!", op1.songTitle)
        assertEquals(listOf("Hiroshi Kitadani"), op1.artists)
        assertEquals("1-47", op1.versions.single().rawEpisodes)
        assertEquals(13360, op1.preferredVideo?.id)
    }

    @Test
    fun `a band credited once per member is listed once, in relevance order`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"bocchi_the_rock","animethemes":[
              {"id":1,"type":"ED","sequence":1,"slug":"ED1",
               "song":{"title":{"romaji":"Distortion!!"},"performances":[
                 {"relevance":2,"artist":{"name":{"main":"Guest"}}},
                 {"relevance":1,"artist":{"name":{"main":"Kessoku Band"}}},
                 {"relevance":1,"artist":{"name":{"main":"Kessoku Band"}}},
                 {"relevance":1,"artist":{"name":{"main":"Kessoku Band"}}}]},
               "animethemeentries":[{"id":2,"version":1,"episodes":"1-12","spoiler":false,
                 "nsfw":false,"videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        assertEquals(listOf("Kessoku Band", "Guest"), api.decodeThemes(body).themes.single().artists)
    }

    @Test
    fun `insert songs are left out`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"IN","sequence":1,"slug":"IN1","song":null,
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        assertEquals(emptyList<Any>(), api.decodeThemes(body).themes)
    }
}
