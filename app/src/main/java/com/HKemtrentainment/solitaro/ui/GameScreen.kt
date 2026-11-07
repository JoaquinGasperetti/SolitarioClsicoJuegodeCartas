package com.HKemtrentainment.solitaro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.Suit
import com.HKemtrentainment.solitaro.ui.theme.SolitarioClásicoJuegoDeCartasTheme
import com.HKemtrentainment.solitaro.viewmodel.AutoMoveDestination
import com.HKemtrentainment.solitaro.viewmodel.CardSource
import com.HKemtrentainment.solitaro.viewmodel.SolitaireViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AnimatingCardInfo(
    val card: Card,
    val sourceBounds: Rect,
    val targetBounds: Rect,
    val destination: AutoMoveDestination
)

@Composable
fun GameScreen(
    viewModel: SolitaireViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val boardState by viewModel.boardState.collectAsState()
    val secondsElapsed by viewModel.secondsElapsed.collectAsState()
    val casinoGreen = Color(0xFF1B4D3E)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Preload interstitial ad on start
    LaunchedEffect(Unit) {
        AdMobInterstitialHelper.loadAd(context)
    }

    // Drag and Drop State
    var draggedCardInfo by remember { mutableStateOf<Pair<List<Card>, CardSource>?>(null) }
    var draggedCardBounds by remember { mutableStateOf<Rect?>(null) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Bounds tracking for drop targets & cards
    val foundationBounds = remember { mutableMapOf<Suit, Rect>() }
    val tableauBounds = remember { mutableMapOf<Int, Rect>() }
    val cardBounds = remember { mutableMapOf<String, Rect>() }

    // Animation state for Tap-to-Move
    var animatingCardInfo by remember { mutableStateOf<AnimatingCardInfo?>(null) }
    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }

    val handleCardTap = { card: Card, source: CardSource ->
        if (animatingCardInfo == null) {
            val destination = viewModel.findAutoMoveDestination(card, source)
            if (destination != null) {
                val startBounds = cardBounds[card.id]
                val targetBounds = when (destination) {
                    is AutoMoveDestination.Foundation -> foundationBounds[destination.suit]
                    is AutoMoveDestination.Tableau -> tableauBounds[destination.pileIndex]
                }

                if (startBounds != null && targetBounds != null) {
                    coroutineScope.launch {
                        animatingCardInfo = AnimatingCardInfo(card, startBounds, targetBounds, destination)
                        animOffset.snapTo(Offset.Zero)
                        animOffset.animateTo(
                            targetValue = Offset(targetBounds.left - startBounds.left, targetBounds.top - startBounds.top),
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )
                        when (destination) {
                            is AutoMoveDestination.Foundation -> viewModel.moveCardToFoundation(card, destination.suit)
                            is AutoMoveDestination.Tableau -> viewModel.moveCardToTableau(card, destination.pileIndex)
                        }
                        animatingCardInfo = null
                        animOffset.snapTo(Offset.Zero)
                    }
                } else {
                    viewModel.autoMoveCard(card, source)
                }
            } else {
                viewModel.onCardTapped(card, source)
            }
        }
    }

    val handleDrop: (List<Card>, CardSource) -> Boolean = { cards, source ->
        val dropRect = draggedCardBounds
        val primaryCard = cards.firstOrNull()
        var droppedSuccessfully = false

        if (primaryCard != null && dropRect != null) {
            val dropCenter = dropRect.center

            // 1. Check Foundations (only if single card)
            if (cards.size == 1) {
                for ((suit, rect) in foundationBounds) {
                    if (rect.contains(dropCenter) || rect.overlaps(dropRect)) {
                        val foundationPile = boardState.foundationPiles[suit] ?: emptyList()
                        if (viewModel.canMoveToFoundation(primaryCard, foundationPile)) {
                            viewModel.moveCardToFoundation(primaryCard, suit)
                            droppedSuccessfully = true
                            break
                        }
                    }
                }
            }

            // 2. Check Tableau
            if (!droppedSuccessfully) {
                for ((targetIdx, rect) in tableauBounds) {
                    val isSourcePile = source is CardSource.Tableau && source.pileIndex == targetIdx

                    if (!isSourcePile && (rect.contains(dropCenter) || rect.overlaps(dropRect))) {
                        val targetCard = boardState.tableauPiles[targetIdx].lastOrNull()
                        if (viewModel.canMoveToTableau(primaryCard, targetCard)) {
                            viewModel.moveCardToTableau(primaryCard, targetIdx)
                            droppedSuccessfully = true
                            break
                        }
                    }
                }
            }
        }
        droppedSuccessfully
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = casinoGreen,
        bottomBar = {
            AdMobBanner()
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(casinoGreen)
                .onGloballyPositioned { coordinates ->
                    containerBounds = coordinates.boundsInRoot()
                }
        ) {
            val availableWidth = maxWidth
            val horizontalPadding = 24.dp
            val totalSpacing = 6.dp * 6
            val cardWidth = ((availableWidth - horizontalPadding - totalSpacing) / 7).coerceIn(36.dp, 72.dp)
            val cardHeight = cardWidth * 1.4f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header with Reactive Timer & Moves
                GameHeader(
                    score = boardState.score,
                    moves = boardState.moves,
                    seconds = secondsElapsed,
                    onUndo = { viewModel.undo() },
                    onNewGame = {
                        AdMobInterstitialHelper.showInterstitial(context) {
                            viewModel.initializeGame()
                        }
                    }
                )

                // 2. Top Row (Stock, Waste, Foundations)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stock & Waste
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Stock
                        CardView(
                            card = boardState.stockPile.lastOrNull()?.copy(isFaceUp = false),
                            width = cardWidth,
                            height = cardHeight,
                            onClick = { viewModel.onStockTapped() }
                        )

                        // Waste
                        val topWasteCard = boardState.wastePile.lastOrNull()
                        val isWasteDragged = draggedCardInfo?.let { (cards, source) ->
                            source is CardSource.Waste && cards.firstOrNull()?.id == topWasteCard?.id
                        } == true

                        val isWasteAnimating = animatingCardInfo?.card?.id == topWasteCard?.id

                        Box(
                            modifier = Modifier
                                .then(
                                    if (isWasteDragged) {
                                        Modifier
                                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                            .onGloballyPositioned { coordinates ->
                                                draggedCardBounds = coordinates.boundsInRoot()
                                            }
                                            .zIndex(10f)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onGloballyPositioned { coordinates ->
                                    if (topWasteCard != null) {
                                        cardBounds[topWasteCard.id] = coordinates.boundsInRoot()
                                    }
                                }
                                .then(
                                    if (topWasteCard != null && topWasteCard.isFaceUp && !isWasteAnimating) {
                                        Modifier.pointerInput(topWasteCard.id) {
                                            var hasMoved = false
                                            detectDragGestures(
                                                onDragStart = {
                                                    hasMoved = false
                                                    coroutineScope.launch {
                                                        offsetX.stop()
                                                        offsetY.stop()
                                                        offsetX.snapTo(0f)
                                                        offsetY.snapTo(0f)
                                                    }
                                                    draggedCardInfo = listOf(topWasteCard) to CardSource.Waste
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    hasMoved = true
                                                    coroutineScope.launch {
                                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                                        offsetY.snapTo(offsetY.value + dragAmount.y)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (!hasMoved) {
                                                        draggedCardInfo = null
                                                        draggedCardBounds = null
                                                        handleCardTap(topWasteCard, CardSource.Waste)
                                                    } else {
                                                        val droppedSuccessfully = handleDrop(draggedCardInfo?.first ?: emptyList(), draggedCardInfo?.second ?: CardSource.Waste)

                                                        coroutineScope.launch {
                                                            if (!droppedSuccessfully) {
                                                                launch { offsetX.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                                                                launch { offsetY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                                                            } else {
                                                                offsetX.snapTo(0f)
                                                                offsetY.snapTo(0f)
                                                            }
                                                            draggedCardInfo = null
                                                            draggedCardBounds = null
                                                        }
                                                    }
                                                },
                                                onDragCancel = {
                                                    hasMoved = false
                                                    coroutineScope.launch {
                                                        offsetX.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                                        offsetY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                                        draggedCardInfo = null
                                                        draggedCardBounds = null
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            if (!isWasteAnimating) {
                                CardView(
                                    card = topWasteCard,
                                    width = cardWidth,
                                    height = cardHeight,
                                    onClick = { if (topWasteCard != null) handleCardTap(topWasteCard, CardSource.Waste) }
                                )
                            }
                        }
                    }

                    // Foundations (4 suits)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val suits = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)
                        for (suit in suits) {
                            val foundationCards = boardState.foundationPiles[suit] ?: emptyList()
                            val topCard = foundationCards.lastOrNull()
                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        foundationBounds[suit] = coordinates.boundsInRoot()
                                    }
                            ) {
                                CardView(
                                    card = topCard,
                                    width = cardWidth,
                                    height = cardHeight
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Tableau Piles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val piles = if (boardState.tableauPiles.isEmpty()) List(7) { emptyList<Card>() } else boardState.tableauPiles
                    piles.forEachIndexed { pileIndex, pile ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned { coordinates ->
                                    tableauBounds[pileIndex] = coordinates.boundsInRoot()
                                }
                        ) {
                            if (pile.isEmpty()) {
                                CardView(
                                    card = null,
                                    width = cardWidth,
                                    height = cardHeight
                                )
                            } else {
                                var cumulativeOffset = 0.dp
                                pile.forEachIndexed { cardIndex, card ->
                                    val topOffset = cumulativeOffset
                                    val isFaceUpCard = card.isFaceUp
                                    val overlap = if (isFaceUpCard) cardHeight * 0.28f else cardHeight * 0.15f
                                    cumulativeOffset += overlap

                                    val isBeingDragged = draggedCardInfo?.let { (cards, source) ->
                                        source is CardSource.Tableau && source.pileIndex == pileIndex &&
                                                pile.indexOf(card) >= pile.indexOf(cards.first())
                                    } == true

                                    val isCardAnimating = animatingCardInfo?.card?.id == card.id

                                    Box(
                                        modifier = Modifier
                                            .padding(top = topOffset)
                                            .onGloballyPositioned { coordinates ->
                                                cardBounds[card.id] = coordinates.boundsInRoot()
                                            }
                                            .then(
                                                if (isBeingDragged) {
                                                    Modifier
                                                        .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                                        .onGloballyPositioned { coordinates ->
                                                            draggedCardBounds = coordinates.boundsInRoot()
                                                        }
                                                        .zIndex(10f)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .then(
                                                if (isFaceUpCard && !isCardAnimating) {
                                                    Modifier.pointerInput(card.id) {
                                                        var hasMoved = false
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                hasMoved = false
                                                                coroutineScope.launch {
                                                                    offsetX.stop()
                                                                    offsetY.stop()
                                                                    offsetX.snapTo(0f)
                                                                    offsetY.snapTo(0f)
                                                                }
                                                                val subSequence = pile.subList(cardIndex, pile.size)
                                                                if (subSequence.all { it.isFaceUp }) {
                                                                    draggedCardInfo = subSequence to CardSource.Tableau(pileIndex)
                                                                }
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                hasMoved = true
                                                                coroutineScope.launch {
                                                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                if (!hasMoved) {
                                                                    draggedCardInfo = null
                                                                    draggedCardBounds = null
                                                                    handleCardTap(card, CardSource.Tableau(pileIndex))
                                                                } else {
                                                                    val droppedSuccessfully = handleDrop(draggedCardInfo?.first ?: emptyList(), draggedCardInfo?.second ?: CardSource.Tableau(pileIndex))

                                                                    coroutineScope.launch {
                                                                        if (!droppedSuccessfully) {
                                                                            launch { offsetX.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                                                                            launch { offsetY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                                                                        } else {
                                                                            offsetX.snapTo(0f)
                                                                            offsetY.snapTo(0f)
                                                                        }
                                                                        draggedCardInfo = null
                                                                        draggedCardBounds = null
                                                                    }
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                hasMoved = false
                                                                coroutineScope.launch {
                                                                    offsetX.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                                                    offsetY.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                                                                    draggedCardInfo = null
                                                                    draggedCardBounds = null
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else if (!isCardAnimating) {
                                                    Modifier.pointerInput(card.id) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                handleCardTap(card, CardSource.Tableau(pileIndex))
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        if (!isCardAnimating) {
                                            CardView(
                                                card = card,
                                                width = cardWidth,
                                                height = cardHeight,
                                                onClick = {
                                                    handleCardTap(card, CardSource.Tableau(pileIndex))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Flying Card Overlay during Tap-to-Move Animation
            animatingCardInfo?.let { info ->
                val containerLeft = containerBounds?.left ?: 0f
                val containerTop = containerBounds?.top ?: 0f
                val localStartX = info.sourceBounds.left - containerLeft
                val localStartY = info.sourceBounds.top - containerTop
                val currentOffset = animOffset.value

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (localStartX + currentOffset.x).roundToInt(),
                                (localStartY + currentOffset.y).roundToInt()
                            )
                        }
                        .width(cardWidth)
                        .height(cardHeight)
                        .zIndex(1000f)
                ) {
                    CardView(
                        card = info.card,
                        width = cardWidth,
                        height = cardHeight
                    )
                }
            }

            // 6. Win Dialog Overlay
            if (boardState.isWon) {
                val formattedTime = String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60)
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Text(
                            text = "🎉 Victory!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Congratulations! You successfully completed Solitaire.")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "⏱️ Time: $formattedTime", fontWeight = FontWeight.SemiBold)
                            Text(text = "🎯 Moves: ${boardState.moves}", fontWeight = FontWeight.SemiBold)
                            Text(text = "🏆 Score: ${boardState.score}", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                AdMobInterstitialHelper.showInterstitial(context) {
                                    viewModel.startNewGame()
                                }
                            }
                        ) {
                            Text(text = "Play Again")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GameHeader(
    score: Int,
    moves: Int,
    seconds: Long,
    onUndo: () -> Unit,
    onNewGame: () -> Unit
) {
    val formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F3B2E), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "SCORE: $score",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "⏱️ $formattedTime",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "MOVES: $moves",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUndo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo Move",
                    tint = Color.White
                )
            }
            IconButton(onClick = onNewGame) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "New Game",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(name = "Game Screen Preview")
@Composable
fun GameScreenPreview() {
    SolitarioClásicoJuegoDeCartasTheme {
        GameScreen()
    }
}
