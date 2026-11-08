package com.HKemtrentainment.solitaro.model

enum class CardColor {
    RED,
    BLACK
}

enum class Suit(
    val color: CardColor,
    val symbol: String,
    val displayNameEs: String
) {
    HEARTS(CardColor.RED, "♥", "Corazones"),
    DIAMONDS(CardColor.RED, "♦", "Diamantes"),
    CLUBS(CardColor.BLACK, "♣", "Tréboles"),
    SPADES(CardColor.BLACK, "♠", "Picas")
}
