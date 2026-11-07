package com.HKemtrentainment.solitaro.model

enum class CardColor {
    RED,
    BLACK
}

enum class Suit(val color: CardColor) {
    HEARTS(CardColor.RED),
    DIAMONDS(CardColor.RED),
    CLUBS(CardColor.BLACK),
    SPADES(CardColor.BLACK)
}
