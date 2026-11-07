package com.HKemtrentainment.solitaro.model

class Deck {
    /**
     * Generates a standard 52-card deck, shuffles it, and deals initial Klondike tableau
     * (7 columns: 1st has 1 face up, 2nd has 1 face down + 1 face up, up to 7th, remaining cards in stock).
     */
    fun createInitialBoard(): BoardState {
        val allCards = mutableListOf<Card>()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                allCards.add(Card(suit = suit, rank = rank, isFaceUp = false))
            }
        }

        val shuffledCards = allCards.shuffled().toMutableList()

        val tableauPiles = mutableListOf<MutableList<Card>>()
        for (i in 0 until 7) {
            val pile = mutableListOf<Card>()
            for (j in 0..i) {
                val card = shuffledCards.removeAt(0)
                // The top card (last card in the column) of each tableau pile is face up
                val isFaceUp = (j == i)
                pile.add(card.copy(isFaceUp = isFaceUp))
            }
            tableauPiles.add(pile)
        }

        val stockPile = shuffledCards.toList() // Remaining cards go to stock (face down)
        val wastePile = emptyList<Card>()
        val foundationPiles = Suit.values().associateWith { emptyList<Card>() }

        return BoardState(
            stockPile = stockPile,
            wastePile = wastePile,
            foundationPiles = foundationPiles,
            tableauPiles = tableauPiles.map { it.toList() },
            score = 0,
            moves = 0,
            isWon = false
        )
    }
}
