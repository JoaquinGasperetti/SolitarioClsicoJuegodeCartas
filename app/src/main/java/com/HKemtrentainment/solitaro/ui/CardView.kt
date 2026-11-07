package com.HKemtrentainment.solitaro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.HKemtrentainment.solitaro.model.CardColor
import com.HKemtrentainment.solitaro.model.Rank
import com.HKemtrentainment.solitaro.model.Suit

/**
 * Renders an individual playing card (face up or face down) or an empty pile placeholder slot,
 * supporting dynamic responsive width and height sizing.
 */
@Composable
fun CardView(
    card: Card?,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 80.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)

    if (card == null) {
        // Empty placeholder slot
        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {}
        }
    } else if (!card.isFaceUp) {
        // Face down card back
        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1565C0) // Classic blue card back
            ),
            border = BorderStroke(1.dp, Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Color(0xFF0D47A1), shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♠",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = (width.value * 0.35f).sp
                )
            }
        }
    } else {
        // Face up card
        val textColor = if (card.color == CardColor.RED) Color(0xFFD32F2F) else Color(0xFF212121)
        val suitSymbol = when (card.suit) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }

        Card(
            modifier = modifier
                .size(width, height)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFAFAFA)
            ),
            border = BorderStroke(1.dp, Color(0xFFB0BEC5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                // Top-left rank & suit
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.rank.symbol,
                        color = textColor,
                        fontSize = (width.value * 0.22f).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = suitSymbol,
                        color = textColor,
                        fontSize = (width.value * 0.18f).sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Center suit symbol
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = suitSymbol,
                        color = textColor,
                        fontSize = (width.value * 0.4f).sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom-right rank & suit
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suitSymbol,
                        color = textColor,
                        fontSize = (width.value * 0.18f).sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
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
        card = Card(suit = Suit.SPADES, rank = Rank.KING, isFaceUp = false)
    )
}

@Preview(name = "Empty Slot Preview")
@Composable
fun CardViewEmptyPreview() {
    CardView(card = null)
}
