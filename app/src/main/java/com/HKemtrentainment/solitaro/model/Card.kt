package com.HKemtrentainment.solitaro.model

data class Card(
    val id: String = "${suit.name}_${rank.value}",
    val suit: Suit,
    val rank: Rank,
    val isFaceUp: Boolean = false,
    val color: CardColor = suit.color
) {
    constructor(suit: Suit, rank: Rank, isFaceUp: Boolean = false) : this(
        id = "${suit.name}_${rank.value}",
        suit = suit,
        rank = rank,
        isFaceUp = isFaceUp,
        color = suit.color
    )

    /**
     * Klondike rule: A card can be placed on a tableau column if:
     * - The target column is empty AND this card is a KING (rank 13), OR
     * - The target card is face up, has the opposite color, and its rank is exactly this card's rank + 1.
     */
    fun canPlaceOnTableau(targetTopCard: Card?): Boolean {
        if (targetTopCard == null) {
            return rank == Rank.KING
        }
        if (!targetTopCard.isFaceUp) return false
        return color != targetTopCard.color && targetTopCard.rank.value == rank.value + 1
    }

    /**
     * Klondike rule: A single card can be placed on its suit foundation if:
     * - The foundation is empty AND this card is an ACE (rank 1), OR
     * - The top foundation card has the same suit and its rank is exactly this card's rank - 1.
     */
    fun canPlaceOnFoundation(foundationTopCard: Card?): Boolean {
        if (foundationTopCard == null) {
            return rank == Rank.ACE
        }
        return suit == foundationTopCard.suit && rank.value == foundationTopCard.rank.value + 1
    }
}
