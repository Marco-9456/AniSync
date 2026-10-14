package com.anisync.android.data.network

import com.anisync.android.domain.MediaThemes
import com.anisync.android.domain.ThemeAudio
import com.anisync.android.domain.ThemePerformer
import com.anisync.android.domain.ThemeType
import com.anisync.android.domain.VideoOverlap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the reading of the AnimeThemes GraphQL response. The shapes are trimmed from live
 * responses for One Piece, Bocchi the Rock, Bakemonogatari and Frieren.
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
    fun `an insert song without a number keeps a clean slug and no qualifier`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"IN","sequence":null,"slug":"IN","song":null,
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        val theme = api.decodeThemes(body).themes.single()
        assertEquals(ThemeType.IN, theme.type)
        assertNull(theme.qualifier)
    }

    @Test
    fun `an insert song without a video is kept, an opening without one is not`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"IN","slug":"IN","song":{"title":{"romaji":"Trio Dreams"}},
               "animethemeentries":[{"id":2,"version":1,"episodes":"4","spoiler":false,
                 "nsfw":false,"videos":{"nodes":[]}}]},
              {"id":3,"type":"OP","sequence":1,"slug":"OP1",
               "animethemeentries":[{"id":4,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[]}}]}
            ]}]}}
        """.trimIndent()

        val theme = api.decodeThemes(body).themes.single()
        assertEquals(ThemeType.IN, theme.type)
        assertFalse(theme.isPlayable)
        assertEquals("4", theme.versions.single().rawEpisodes)
    }

    @Test
    fun `the group name labels a variant slot`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"one_piece","animethemes":[
              {"id":1,"type":"OP","sequence":1,"slug":"OP1-EN","group":{"name":"English Version"},
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        val theme = api.decodeThemes(body).themes.single()
        assertEquals("EN", theme.qualifier)
        assertEquals("English Version", theme.variantLabel)
    }

    @Test
    fun `band members and character songs become performers`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"OP","sequence":1,"slug":"OP1",
               "song":{"title":{"romaji":"Seishun Complex"},"performances":[
                 {"relevance":1,"artist":{"name":{"main":"Kessoku Band"}},
                  "member":{"name":{"main":"Yoshino Aoyama"}},"memberAs":"Hitori Gotou"},
                 {"relevance":1,"artist":{"name":{"main":"Kessoku Band"}},
                  "member":{"name":{"main":"Saku Mizuno"}},"memberAs":"Ryou Yamada"}]},
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]},
              {"id":4,"type":"OP","sequence":2,"slug":"OP2",
               "song":{"title":{"romaji":"Staple Stable"},"performances":[
                 {"relevance":1,"as":"Hitagi Senjougahara","artist":{"name":{"main":"Chiwa Saitou"}}}]},
               "animethemeentries":[{"id":5,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":6,"link":"https://v.animethemes.moe/b.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        val (band, solo) = api.decodeThemes(body).themes
        assertEquals(listOf("Kessoku Band"), band.artists)
        assertEquals(
            listOf(
                ThemePerformer("Yoshino Aoyama", "Hitori Gotou", "Kessoku Band"),
                ThemePerformer("Saku Mizuno", "Ryou Yamada", "Kessoku Band")
            ),
            band.performers
        )
        assertEquals(listOf(ThemePerformer("Chiwa Saitou", "Hitagi Senjougahara")), solo.performers)
        assertTrue(solo.hasPerformerDetail)
    }

    @Test
    fun `a plain credit adds no performer detail`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"ED","sequence":1,"slug":"ED1",
               "song":{"title":{"romaji":"Anytime Anywhere"},"performances":[
                 {"relevance":1,"artist":{"name":{"main":"milet"}}}]},
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[{"id":3,"link":"https://v.animethemes.moe/a.webm"}]}}]}
            ]}]}}
        """.trimIndent()

        assertFalse(api.decodeThemes(body).themes.single().hasPerformerDetail)
    }

    @Test
    fun `audio comes from the videos and an overlapping cut is not preferred`() {
        val body = """
            {"data":{"findAnimeByExternalSite":[{"slug":"x","animethemes":[
              {"id":1,"type":"OP","sequence":1,"slug":"OP1",
               "animethemeentries":[{"id":2,"version":1,"spoiler":false,"nsfw":false,
                 "videos":{"nodes":[
                   {"id":10,"link":"https://v.animethemes.moe/a-trans.webm","resolution":1080,
                    "nc":true,"overlap":"TRANS",
                    "audio":{"link":"https://a.animethemes.moe/a.ogg","size":2621172}},
                   {"id":11,"link":"https://v.animethemes.moe/a.webm","resolution":720,
                    "nc":true,"overlap":"NONE",
                    "audio":{"link":"https://a.animethemes.moe/a.ogg","size":2621172}}]}}]}
            ]}]}}
        """.trimIndent()

        val version = api.decodeThemes(body).themes.single().versions.single()
        assertEquals(ThemeAudio("https://a.animethemes.moe/a.ogg", 2621172), version.audio)
        assertEquals(VideoOverlap.Transition, version.videos.first { it.id == 10 }.overlap)
        assertEquals(11, version.preferredVideo?.id)
    }
}
