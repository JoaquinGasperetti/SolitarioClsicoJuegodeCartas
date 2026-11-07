package com.HKemtrentainment.solitaro.model

enum class Rank(val value: Int, val symbol: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    companion object {
        fun fromValue(value: Int): Rank {
            return values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid card rank value: $value")
        }
    }
}
