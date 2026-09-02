package com.mattdixon.snakeapi.model

/** A small, intentionally non-exhaustive list of common English profanity - a blunt first line
 * of defense against the most obviously offensive nicknames on a public leaderboard, not a
 * real moderation system. Matches case-insensitively as a substring, so e.g. "classic" would
 * also be blocked by a "ass" entry - a deliberate false-positive-over-false-negative trade-off
 * that favors simplicity at this scale. Extend this list (or swap it for a proper word-list
 * library) if that trade-off stops being acceptable. */
private val BLOCKED_SUBSTRINGS = listOf(
    "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "dick", "piss", "slut", "whore",
    "nigger", "faggot", "retard",
)

fun containsBlockedContent(text: String): Boolean {
    val normalized = text.lowercase()
    return BLOCKED_SUBSTRINGS.any { normalized.contains(it) }
}
