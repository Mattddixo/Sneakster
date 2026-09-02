package com.mattdixon.snakeapi

import com.mattdixon.snakeapi.model.ScoreSubmission
import com.mattdixon.snakeapi.model.ValidationException
import com.mattdixon.snakeapi.model.validated
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScoreValidationTest {

    @Test
    fun `accepts a normal submission and normalizes it`() {
        val result = ScoreSubmission(nickname = "  Mat_Dixon-77  ", score = 4200, difficulty = "HARD").validated()
        assertEquals("Mat_Dixon-77", result.nickname)
        assertEquals("hard", result.difficulty)
    }

    @Test
    fun `rejects blank nickname`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "   ", score = 10).validated()
        }
    }

    @Test
    fun `rejects nickname with disallowed characters`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "<script>", score = 10).validated()
        }
    }

    @Test
    fun `rejects a nickname containing a blocked word`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "shitlord", score = 10).validated()
        }
    }

    @Test
    fun `rejects negative score`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "player", score = -1).validated()
        }
    }

    @Test
    fun `rejects implausibly large score`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "player", score = 10_000_000).validated()
        }
    }

    @Test
    fun `rejects unknown difficulty`() {
        assertFailsWith<ValidationException> {
            ScoreSubmission(nickname = "player", score = 10, difficulty = "impossible").validated()
        }
    }
}
