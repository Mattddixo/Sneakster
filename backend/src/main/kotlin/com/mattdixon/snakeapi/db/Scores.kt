package com.mattdixon.snakeapi.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object Scores : LongIdTable("scores") {
    val nickname = varchar("nickname", 20)
    val score = integer("score")
    val difficulty = varchar("difficulty", 16)
    val createdAt = timestamp("created_at")
}
