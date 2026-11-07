package com.HKemtrentainment.solitaro.viewmodel

import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.Rank
import com.HKemtrentainment.solitaro.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolitaireViewModelTest {

    @Test
    fun testViewModelInitializationAndStartNewGame() {
        val viewModel = SolitaireViewModel()
        val state = viewModel.boardState.value

        assertNotNull(state)
        assertEquals(7, state.tableauPiles.size)
        assertTrue(state.stockPile.isNotEmpty())
        assertEquals(0, state.moves)
    }

    @Test
    fun testOnStockTapped() {
        val viewModel = SolitaireViewModel()
        val initialState = viewModel.boardState.value
        val initialStockSize = initialState.stockPile.size
        val initialWasteSize = initialState.wastePile.size

        viewModel.onStockTapped()

        val newState = viewModel.boardState.value
        assertEquals(initialStockSize - 1, newState.stockPile.size)
        assertEquals(initialWasteSize + 1, newState.wastePile.size)
        assertTrue(newState.wastePile.last().isFaceUp)
        assertEquals(1, newState.moves)
    }

    @Test
    fun testStockTappingAndRecycling() {
        val viewModel = SolitaireViewModel()
        val initialStockSize = viewModel.boardState.value.stockPile.size

        // Tap stock until it's empty
        repeat(initialStockSize) {
            viewModel.onStockTapped()
        }

        // Stock should be empty, waste should have all stock cards
        var state = viewModel.boardState.value
        assertEquals(0, state.stockPile.size)
        assertEquals(initialStockSize, state.wastePile.size)

        // One more tap should recycle waste back to stock
        viewModel.onStockTapped()
        state = viewModel.boardState.value
        assertEquals(initialStockSize, state.stockPile.size)
        assertEquals(0, state.wastePile.size)
        assertFalse(state.stockPile.last().isFaceUp)
    }

    @Test
    fun testUndo() {
        val viewModel = SolitaireViewModel()
        val initialMoves = viewModel.boardState.value.moves

        viewModel.onStockTapped()
        assertEquals(initialMoves + 1, viewModel.boardState.value.moves)

        viewModel.undo()
        assertEquals(initialMoves, viewModel.boardState.value.moves)
    }

    @Test
    fun testCanMoveToTableau() {
        val viewModel = SolitaireViewModel()

        val kingSpades = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = true)
        val queenHearts = Card(suit = Suit.HEARTS, rank = Rank.QUEEN, isFaceUp = true)
        val queenSpades = Card(suit = Suit.SPADES, rank = Rank.QUEEN, isFaceUp = true)
        val aceClubs = Card(suit = Suit.CLUBS, rank = Rank.ACE, isFaceUp = true)
        val fiveDiamonds = Card(suit = Suit.DIAMONDS, rank = Rank.FIVE, isFaceUp = true)
        val sevenClubs = Card(suit = Suit.CLUBS, rank = Rank.SEVEN, isFaceUp = true)

        // Empty column requires King
        assertTrue(viewModel.canMoveToTableau(kingSpades, null))
        assertFalse(viewModel.canMoveToTableau(queenHearts, null))
        assertFalse(viewModel.canMoveToTableau(aceClubs, null))

        // Non-empty column requires alternate color and target rank == card rank + 1
        // queenHearts (Red Q) on kingSpades (Black K) -> true
        assertTrue(viewModel.canMoveToTableau(queenHearts, kingSpades))

        // queenSpades (Black Q) on kingSpades (Black K) -> false (same color)
        assertFalse(viewModel.canMoveToTableau(queenSpades, kingSpades))

        // aceClubs on kingSpades -> false (wrong rank sequence)
        assertFalse(viewModel.canMoveToTableau(aceClubs, kingSpades))

        // 5 on 7 -> false (wrong rank sequence)
        assertFalse(viewModel.canMoveToTableau(fiveDiamonds, sevenClubs))
    }

    @Test
    fun testCanMoveToFoundation() {
        val viewModel = SolitaireViewModel()

        val aceHearts = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)
        val aceSpades = Card(suit = Suit.SPADES, rank = Rank.ACE, isFaceUp = true)
        val twoHearts = Card(suit = Suit.HEARTS, rank = Rank.TWO, isFaceUp = true)
        val twoSpades = Card(suit = Suit.SPADES, rank = Rank.TWO, isFaceUp = true)
        val kingHearts = Card(suit = Suit.HEARTS, rank = Rank.KING, isFaceUp = true)

        // Empty foundation requires Ace
        assertTrue(viewModel.canMoveToFoundation(aceHearts, emptyList()))
        assertTrue(viewModel.canMoveToFoundation(aceSpades, emptyList()))
        assertFalse(viewModel.canMoveToFoundation(twoHearts, emptyList()))

        val foundationWithAce = listOf(aceHearts)

        // Non-empty foundation requires same suit and target rank == card rank - 1
        assertTrue(viewModel.canMoveToFoundation(twoHearts, foundationWithAce))
        assertFalse(viewModel.canMoveToFoundation(twoSpades, foundationWithAce)) // wrong suit
        assertFalse(viewModel.canMoveToFoundation(kingHearts, foundationWithAce)) // wrong rank
    }

    @Test
    fun testAutoMoveCardLogic() {
        val viewModel = SolitaireViewModel()
        val aceHearts = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)
        viewModel.autoMoveCard(aceHearts, CardSource.Waste)
        assertFalse(viewModel.boardState.value.isWon)
    }
}


