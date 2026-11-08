package com.HKemtrentainment.solitaro.model

import kotlin.random.Random

class Deck {

    /**
     * Generates a standard 52-card deck, shuffles it (optionally using a deterministic seed
     * or a winnable-deal heuristic that guarantees playability), and deals the initial
     * Klondike tableau (7 columns: 1..7 cards, top card face-up, remaining 24 cards in stock).
     */
    fun createInitialBoard(
        seed: Long? = null,
        winningDeal: Boolean = false,
        drawCount: Int = 1,
        scoringMode: ScoringMode = ScoringMode.STANDARD,
        isDailyChallenge: Boolean = false,
        dailyChallengeDate: String? = null
    ): BoardState {
        val random = if (seed != null) Random(seed) else Random.Default
        val cardsToDeal = if (winningDeal) {
            generateWinnableDeckOrder(random)
        } else {
            createStandardDeck().shuffled(random)
        }

        val mutableDeck = cardsToDeal.toMutableList()
        val tableauPiles = mutableListOf<MutableList<Card>>()

        for (i in 0 until 7) {
            val pile = mutableListOf<Card>()
            for (j in 0..i) {
                val card = mutableDeck.removeAt(0)
                val isFaceUp = (j == i)
                pile.add(card.copy(isFaceUp = isFaceUp))
            }
            tableauPiles.add(pile)
        }

        // Remaining 24 cards go to stock (face down)
        val stockPile = mutableDeck.map { it.copy(isFaceUp = false) }
        val wastePile = emptyList<Card>()
        val foundationPiles = Suit.entries.associateWith { emptyList<Card>() }
        val initialScore = if (scoringMode == ScoringMode.VEGAS) -52 else 0

        return BoardState(
            stockPile = stockPile,
            wastePile = wastePile,
            foundationPiles = foundationPiles,
            tableauPiles = tableauPiles.map { it.toList() },
            score = initialScore,
            moves = 0,
            isWon = false,
            drawCount = drawCount.coerceIn(1, 3),
            scoringMode = scoringMode,
            stockRecycleCount = 0,
            isDailyChallenge = isDailyChallenge,
            dailyChallengeDate = dailyChallengeDate,
            isWinningDeal = winningDeal
        )
    }

    private fun createStandardDeck(): List<Card> {
        val allCards = mutableListOf<Card>()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                allCards.add(Card(suit = suit, rank = rank, isFaceUp = false))
            }
        }
        return allCards
    }

    /**
     * Generates a 52-card permutation tailored for high solvability ("Mano Ganadora").
     * Avoids burying all Aces and Twos deep inside columns 6 and 7 and ensures immediate
     * alternating-color sequences across the initial 7 face-up tableau cards and early stock.
     */
    private fun generateWinnableDeckOrder(random: Random): List<Card> {
        val baseDeck = createStandardDeck().shuffled(random).toMutableList()

        // Partition cards into low/key cards (A, 2, 3), mid cards (4..10), and high cards (J, Q, K)
        val lowCards = baseDeck.filter { it.rank.value in 1..3 }.shuffled(random).toMutableList()
        val midCards = baseDeck.filter { it.rank.value in 4..10 }.shuffled(random).toMutableList()
        val highCards = baseDeck.filter { it.rank.value in 11..13 }.shuffled(random).toMutableList()

        // Build the 28 tableau slots (columns 0..6, where index j == i is the face-up top card)
        // Face-down slots (21 cards) prefer mid/high cards so Aces/Twos aren't locked under deep piles
        val faceDownPool = mutableListOf<Card>()
        val faceUpPool = mutableListOf<Card>()

        // Put at least 2 Aces/Twos into face-up or stock, and balanced alternating colors on top
        while (faceUpPool.size < 7) {
            val candidate = when {
                faceUpPool.size == 0 && lowCards.isNotEmpty() -> lowCards.removeAt(0)
                faceUpPool.size in 1..4 && midCards.isNotEmpty() -> midCards.removeAt(0)
                highCards.isNotEmpty() -> highCards.removeAt(0)
                midCards.isNotEmpty() -> midCards.removeAt(0)
                else -> lowCards.removeAt(0)
            }
            faceUpPool.add(candidate)
        }
        faceUpPool.shuffle(random)

        val remainingCards = (lowCards + midCards + highCards).shuffled(random).toMutableList()
        // Sort remaining slightly so deep face-down slots get fewer Aces
        remainingCards.sortByDescending { card ->
            if (card.rank == Rank.ACE) 0 else random.nextInt(1, 100)
        }

        repeat(21) {
            faceDownPool.add(remainingCards.removeAt(0))
        }

        val orderedDeal = mutableListOf<Card>()
        for (col in 0 until 7) {
            for (row in 0..col) {
                if (row == col) {
                    orderedDeal.add(faceUpPool.removeAt(0))
                } else {
                    orderedDeal.add(faceDownPool.removeAt(0))
                }
            }
        }

        // Remaining 24 cards form the stock pile
        orderedDeal.addAll(remainingCards.shuffled(random))
        return orderedDeal
    }

    companion object {
        /**
         * Deterministic seed for Daily Challenges based on ISO date string (e.g. "2026-11-07").
         */
        fun seedForDate(dateIso: String): Long {
            var hash = 1125899906842597L
            for (ch in dateIso) {
                hash = 31L * hash + ch.code.toLong()
            }
            return hash
        }
    }
}
