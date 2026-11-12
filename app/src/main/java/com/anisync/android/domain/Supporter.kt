package com.anisync.android.domain

import java.time.LocalDate

/** Someone who has left a tip, through Ko-fi or GitHub Sponsors. */
data class Supporter(
    /** The name they gave, or null for a tip left anonymously. */
    val name: String?,
    /** When they last tipped, which orders the list. */
    val date: LocalDate,
    /** Their profile page, which only a GitHub sponsor has. */
    val url: String? = null,
)
