package com.mattdixon.snakeapi.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Scores : LongIdTable("scores") {
    val nickname = varchar("nickname", 20)
    val score = integer("score")
    val difficulty = varchar("difficulty", 16)
    val createdAt = timestamp("created_at")

    init {
        // Backs LeaderboardRepository.submit()'s rank count (WHERE score > ?, no difficulty
        // filter) and its ORDER BY score fallback.
        index(false, score)
        // Backs top(limit, difficulty)'s WHERE difficulty = ? ORDER BY score DESC.
        index(false, difficulty, score)
        // Backs personalBest()'s WHERE nickname = ?.
        index(false, nickname)
    }
}
