package com.HKemtrentainment.solitaro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.CardBackTheme
import com.HKemtrentainment.solitaro.model.CardColor
import com.HKemtrentainment.solitaro.model.Rank
import com.HKemtrentainment.solitaro.model.Suit

/**
 * Renders an individual playing card (face up or face down) or an empty pile placeholder slot,
 * supporting dynamic responsive sizing, high-contrast senior-friendly indices, custom card backs,
 * hint highlighting, and stock count badges.
 */
@Composable
fun CardView(
    card: Card?,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 80.dp,
    cardBackTheme: CardBackTheme = CardBackTheme.ROYAL_BLUE,
    emptySlotSymbol: String? = null,
    badgeText: String? = null,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    val highlightBorder = if (isHighlighted) {
        BorderStroke(2.5.dp, Color(0xFFFFD700))
    } else {
        null
    }

    if (card == null) {
        // Empty placeholder slot (for Foundation suits, Stock recycle, or empty King tableau column)
        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = if (isHighlighted) 0.28f else 0.12f)
            ),
            border = highlightBorder ?: BorderStroke(1.2.dp, Color.White.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (!emptySlotSymbol.isNullOrEmpty()) {
                    Text(
                        text = emptySlotSymbol,
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = (width.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else if (!card.isFaceUp) {
        // Face-down card back with customizable theme
        val primaryBackColor = Color(cardBackTheme.primaryColorHex)
        val innerBackColor = Color(cardBackTheme.innerColorHex)

        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = primaryBackColor
            ),
            border = highlightBorder ?: BorderStroke(1.2.dp, Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.5.dp)
                    .background(innerBackColor, shape = RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cardBackTheme.centerSymbol,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = (width.value * 0.36f).sp
                )
                if (!badgeText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = (width.value * 0.19f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        // Face-up card with high-contrast indices and royal center emblem
        val textColor = if (card.color == CardColor.RED) Color(0xFFC62828) else Color(0xFF1A1A1A)
        val suitSymbol = card.suit.symbol
        val isRoyal = card.rank == Rank.JACK || card.rank == Rank.QUEEN || card.rank == Rank.KING
        val centerText = when (card.rank) {
            Rank.KING -> "♔"
            Rank.QUEEN -> "♕"
            Rank.JACK -> "♘"
            else -> suitSymbol
        }

        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) Color(0xFFFFFDE7) else Color(0xFFFAFAFA)
            ),
            border = highlightBorder ?: BorderStroke(1.dp, Color(0xFFB0BEC5)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-left rank & suit (high visibility when stacked in tableau)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.rank.symbol,
                        color = textColor,
                        fontSize = (width.value * 0.25f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Text(
                        text = suitSymbol,
                        color = textColor,
                        fontSize = (width.value * 0.21f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center emblem (royal figure or large suit)
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = centerText,
                        color = if (isRoyal) textColor.copy(alpha = 0.85f) else textColor,
                        fontSize = (width.value * 0.42f).sp
                    )
                }

                // Bottom-right rank & suit
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suitSymbol,
                        color = textColor,
                        fontSize = (width.value * 0.19f).sp
                    )
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Text(
                        text = card.rank.symbol,
                        color = textColor,
                        fontSize = (width.value * 0.22f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(name = "Face Up Card Preview")
@Composable
fun CardViewFaceUpPreview() {
    CardView(
        card = Card(suit = Suit.HEARTS, rank = Rank.ACE, isFaceUp = true)
    )
}

@Preview(name = "Face Down Card Preview")
@Composable
fun CardViewFaceDownPreview() {
    CardView(
        card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false),
        badgeText = "24"
    )
}

@Preview(name = "Empty Slot Preview")
@Composable
fun CardViewEmptyPreview() {
    CardView(card = null, emptySlotSymbol = "♥")
}
