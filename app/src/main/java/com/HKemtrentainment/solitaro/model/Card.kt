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
}
