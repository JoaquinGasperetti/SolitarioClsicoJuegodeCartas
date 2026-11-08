package com.HKemtrentainment.solitaro.model

enum class ScoringMode(val displayNameEs: String) {
    STANDARD("Clásico"),
    VEGAS("Casino Vegas")
}

data class BoardState(
    val stockPile: List<Card> = emptyList(),
    val wastePile: List<Card> = emptyList(),
    val foundationPiles: Map<Suit, List<Card>> = Suit.entries.associateWith { emptyList() },
    val tableauPiles: List<List<Card>> = emptyList(),
    val score: Int = 0,
    val moves: Int = 0,
    val isWon: Boolean = false,
    val drawCount: Int = 1,
    val scoringMode: ScoringMode = ScoringMode.STANDARD,
    val stockRecycleCount: Int = 0,
    val isDailyChallenge: Boolean = false,
    val dailyChallengeDate: String? = null,
    val isWinningDeal: Boolean = false
) {
    /**
     * Returns true when all remaining cards are in the tableau and every single card is face up.
     * In this state, the game is mathematically solved and can be auto-completed to the foundations.
     */
    val canAutoComplete: Boolean
        get() = !isWon &&
                stockPile.isEmpty() &&
                wastePile.isEmpty() &&
                tableauPiles.any { it.isNotEmpty() } &&
                tableauPiles.all { pile -> pile.all { it.isFaceUp } }

    /**
     * Total number of cards currently placed in the 4 foundations (out of 52).
     */
    val foundationCardCount: Int
        get() = foundationPiles.values.sumOf { it.size }
}
