package com.anisync.android.domain

/** The ranked lists on the People view. */
enum class PeopleList { ANIME_VOICE_ACTORS, ANIME_STAFF, ANIME_STUDIOS, MANGA_STAFF }

/** The orders a People list can take, the three AniList can sort by. */
enum class PeopleSort { COUNT, PROGRESS, SCORE }

/**
 * One person or studio in a People list. [count] is roles for voice actors and titles otherwise,
 * [amount] is hours for anime and chapters for manga.
 */
data class PersonStat(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val meanScore: Float,
    val amount: Float,
)

/**
 * The list in [sort] order. The score order leaves out anyone under [MIN_SCORED_ROLES] roles or
 * titles, the same idea as the best rated genres, so a two role actor at 100 cannot lead it.
 */
fun List<PersonStat>.inPeopleOrder(sort: PeopleSort): List<PersonStat> = when (sort) {
    PeopleSort.COUNT -> sortedByDescending { it.count }
    PeopleSort.PROGRESS -> sortedByDescending { it.amount }
    PeopleSort.SCORE -> filter { it.count >= MIN_SCORED_ROLES && it.meanScore > 0f }.sortedByDescending { it.meanScore }
}

const val MIN_SCORED_ROLES = 5

/** AniList returns at most this many entries of a statistics list. */
const val PEOPLE_LIMIT = 100
