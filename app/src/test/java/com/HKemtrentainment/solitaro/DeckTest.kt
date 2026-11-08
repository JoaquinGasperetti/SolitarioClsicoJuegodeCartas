package com.HKemtrentainment.solitaro

import com.HKemtrentainment.solitaro.model.Deck
import com.HKemtrentainment.solitaro.model.ScoringMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckTest {

    @Test
    fun testInitialBoardCreation() {
        val deck = Deck()
        val board = deck.createInitialBoard()

        // Total cards should be 52
        val totalTableauCards = board.tableauPiles.sumOf { it.size }
        val totalStockCards = board.stockPile.size
        val totalWasteCards = board.wastePile.size
        val totalFoundationCards = board.foundationPiles.values.sumOf { it.size }

        assertEquals(52, totalTableauCards + totalStockCards + totalWasteCards + totalFoundationCards)
        assertEquals(24, totalStockCards)
        assertEquals(0, totalWasteCards)

        // Tableau should have 7 piles
        assertEquals(7, board.tableauPiles.size)

        // Each tableau pile i (0..6) should have i+1 cards
        for (i in 0 until 7) {
            val pile = board.tableauPiles[i]
            assertEquals(i + 1, pile.size)
            // The top card (last card) of each pile must be face up
            assertTrue(pile.last().isFaceUp)
            // Cards below the top card in the pile should be face down
            for (j in 0 until i) {
                assertTrue(!pile[j].isFaceUp)
            }
        }
    }

    @Test
    fun testDeterministicDailyChallengeSeed() {
        val deck = Deck()
        val seed = Deck.seedForDate("2026-11-07")
        val board1 = deck.createInitialBoard(seed = seed, winningDeal = true, isDailyChallenge = true, dailyChallengeDate = "2026-11-07")
        val board2 = deck.createInitialBoard(seed = seed, winningDeal = true, isDailyChallenge = true, dailyChallengeDate = "2026-11-07")

        assertEquals(board1.stockPile.map { it.id }, board2.stockPile.map { it.id })
        for (col in 0 until 7) {
            assertEquals(board1.tableauPiles[col].map { it.id }, board2.tableauPiles[col].map { it.id })
        }
    }

    @Test
    fun testVegasScoringInitialScore() {
        val deck = Deck()
        val vegasBoard = deck.createInitialBoard(scoringMode = ScoringMode.VEGAS)
        assertEquals(-52, vegasBoard.score)
    }
}
