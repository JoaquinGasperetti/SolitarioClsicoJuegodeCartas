package com.HKemtrentainment.solitaro.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HKemtrentainment.solitaro.model.Achievement
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.CardBackTheme
import com.HKemtrentainment.solitaro.model.PlayerStats
import com.HKemtrentainment.solitaro.model.ScoringMode
import com.HKemtrentainment.solitaro.model.Suit
import com.HKemtrentainment.solitaro.model.TableTheme
import com.HKemtrentainment.solitaro.ui.theme.SolitarioClásicoJuegoDeCartasTheme
import com.HKemtrentainment.solitaro.viewmodel.AutoMoveDestination
import com.HKemtrentainment.solitaro.viewmodel.CardSource
import com.HKemtrentainment.solitaro.viewmodel.SolitaireViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class AnimatingCardInfo(
    val cards: List<Card>,
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
    val playerStats by viewModel.playerStats.collectAsState()
    val activeHint by viewModel.activeHint.collectAsState()
    val isNoMoreMoves by viewModel.isNoMoreMoves.collectAsState()
    val newlyUnlockedAchievements by viewModel.newlyUnlockedAchievements.collectAsState()
    val isAutoCompleting by viewModel.isAutoCompleting.collectAsState()

    val tableTheme = playerStats.selectedTableTheme
    val cardBackTheme = playerStats.selectedCardBack
    val feltColor = Color(tableTheme.feltColorHex)
    val headerColor = Color(tableTheme.headerColorHex)
    val accentColor = Color(tableTheme.accentColorHex)

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val localView = LocalView.current

    var showDailyChallengeDialog by remember { mutableStateOf(false) }
    var showStatsAndAchievementsDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNewGameMenuDialog by remember { mutableStateOf(false) }

    val triggerHaptic = {
        if (playerStats.hapticsEnabled) {
            try {
                localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    // Attach context & preload interstitial ad once on initial composition
    LaunchedEffect(Unit) {
        viewModel.attachContext(context)
        AdMobInterstitialHelper.loadAd(context)
    }

    // Trigger Google Play In-App Review on key win milestones
    LaunchedEffect(boardState.isWon) {
        if (boardState.isWon) {
            InAppReviewHelper.requestReviewIfAppropriate(
                context = context,
                totalWins = playerStats.gamesWon,
                currentStreak = playerStats.currentStreak
            )
        }
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
        if (animatingCardInfo == null && !isAutoCompleting) {
            triggerHaptic()
            val destination = viewModel.findAutoMoveDestination(card, source)
            if (destination != null) {
                val startBounds = cardBounds[card.id]
                val targetBounds = when (destination) {
                    is AutoMoveDestination.Foundation -> foundationBounds[destination.suit]
                    is AutoMoveDestination.Tableau -> tableauBounds[destination.pileIndex]
                }

                // Determine full stack of cards moving (for Tableau stack tap-to-move)
                val movingCards = if (source is CardSource.Tableau) {
                    val pile = boardState.tableauPiles.getOrNull(source.pileIndex) ?: listOf(card)
                    val idx = pile.indexOfFirst { it.id == card.id }
                    if (idx != -1) pile.subList(idx, pile.size) else listOf(card)
                } else {
                    listOf(card)
                }

                if (startBounds != null && targetBounds != null) {
                    coroutineScope.launch {
                        animatingCardInfo = AnimatingCardInfo(movingCards, startBounds, targetBounds, destination)
                        animOffset.snapTo(Offset.Zero)
                        animOffset.animateTo(
                            targetValue = Offset(targetBounds.left - startBounds.left, targetBounds.top - startBounds.top),
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
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

            // 1. Check Foundations (only if single card and not already from foundation)
            if (cards.size == 1 && source !is CardSource.Foundation) {
                for ((suit, rect) in foundationBounds) {
                    if (rect.contains(dropCenter) || rect.overlaps(dropRect)) {
                        val foundationPile = boardState.foundationPiles[suit] ?: emptyList()
                        if (viewModel.canMoveToFoundation(primaryCard, foundationPile)) {
                            triggerHaptic()
                            viewModel.moveCardToFoundation(primaryCard, suit)
                            droppedSuccessfully = true
                            break
                        }
                    }
                }
            }

            // 2. Check Tableau columns
            if (!droppedSuccessfully) {
                for ((targetIdx, rect) in tableauBounds) {
                    val isSourcePile = source is CardSource.Tableau && source.pileIndex == targetIdx

                    if (!isSourcePile && (rect.contains(dropCenter) || rect.overlaps(dropRect))) {
                        val targetCard = boardState.tableauPiles[targetIdx].lastOrNull()
                        if (viewModel.canMoveToTableau(primaryCard, targetCard)) {
                            triggerHaptic()
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
        containerColor = feltColor,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BottomControlDock(
                    headerColor = headerColor,
                    accentColor = accentColor,
                    canUndo = boardState.moves > 0 && !boardState.isWon,
                    onNewGameClick = { showNewGameMenuDialog = true },
                    onDailyClick = { showDailyChallengeDialog = true },
                    onHintClick = {
                        triggerHaptic()
                        viewModel.requestHint()
                    },
                    onUndoClick = {
                        triggerHaptic()
                        viewModel.undo()
                    },
                    onThemesClick = { showThemesDialog = true }
                )
                AdMobBanner()
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(feltColor)
                .onGloballyPositioned { coordinates ->
                    containerBounds = coordinates.boundsInRoot()
                }
        ) {
            val availableWidth = maxWidth
            val horizontalPadding = 20.dp
            val totalSpacing = 5.dp * 6
            val cardWidth = ((availableWidth - horizontalPadding - totalSpacing) / 7).coerceIn(38.dp, 74.dp)
            val cardHeight = cardWidth * 1.42f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Retention & Progression Top Bar (Level, XP Bar, Streak, Achievements, Settings)
                ProgressionTopBar(
                    stats = playerStats,
                    headerColor = headerColor,
                    accentColor = accentColor,
                    onDailyClick = { showDailyChallengeDialog = true },
                    onStatsClick = { showStatsAndAchievementsDialog = true },
                    onSettingsClick = { showSettingsDialog = true }
                )

                // 2. Score, Timer & Mode Status Bar
                GameScoreHeader(
                    score = boardState.score,
                    scoringMode = boardState.scoringMode,
                    drawCount = boardState.drawCount,
                    isDailyChallenge = boardState.isDailyChallenge,
                    dailyDate = boardState.dailyChallengeDate,
                    moves = boardState.moves,
                    seconds = secondsElapsed,
                    headerColor = headerColor,
                    accentColor = accentColor
                )

                // Optional Hint Banner
                AnimatedVisibility(
                    visible = activeHint != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    activeHint?.let { hint ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF263238), RoundedCornerShape(10.dp))
                                .border(1.dp, accentColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💡 ${hint.messageEs}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Auto-Complete Banner when all remaining cards are face-up
                AnimatedVisibility(
                    visible = boardState.canAutoComplete && playerStats.autoCompleteEnabled && !isAutoCompleting,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            triggerHaptic()
                            viewModel.autoCompleteGame()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Auto-Completar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✨ ¡Partida Resuelta! Auto-Completar Ahora",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Deadlock Banner when no useful moves remain
                AnimatedVisibility(
                    visible = isNoMoreMoves && !boardState.isWon,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3E2723), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚠️ No quedan movimientos útiles en el mazo ni en el tablero.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.undo() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Text("Deshacer", color = Color.White, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.restartCurrentDeal() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Text("Reiniciar", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.startNewGame(winningDeal = true) },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Mano Ganadora", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 3. Top Row: Stock, Waste (Draw 1 or fanned Draw 3), and 4 Suit Foundations
                // Supports Left-Handed Mode by mirroring the layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stockAndWasteContent: @Composable () -> Unit = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stock Pile
                            val stockCount = boardState.stockPile.size
                            CardView(
                                card = boardState.stockPile.lastOrNull()?.copy(isFaceUp = false),
                                width = cardWidth,
                                height = cardHeight,
                                cardBackTheme = cardBackTheme,
                                emptySlotSymbol = if (boardState.wastePile.isNotEmpty()) "↻" else "∅",
                                badgeText = if (stockCount > 0) stockCount.toString() else null,
                                isHighlighted = activeHint?.isStockTap == true,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.onStockTapped()
                                }
                            )

                            // Waste Pile (displays up to 3 fanned cards when drawCount == 3)
                            val visibleWasteCards = if (boardState.drawCount == 3) {
                                boardState.wastePile.takeLast(3)
                            } else {
                                boardState.wastePile.takeLast(1)
                            }
                            val topWasteCard = boardState.wastePile.lastOrNull()
                            val fanStep = if (boardState.drawCount == 3) 13.dp else 0.dp
                            val wasteBoxWidth = cardWidth + (fanStep * 2)

                            Box(
                                modifier = Modifier
                                    .width(wasteBoxWidth)
                                    .height(cardHeight)
                            ) {
                                if (visibleWasteCards.isEmpty()) {
                                    CardView(
                                        card = null,
                                        width = cardWidth,
                                        height = cardHeight
                                    )
                                } else {
                                    visibleWasteCards.forEachIndexed { idx, wasteCard ->
                                        val isTopPlayable = (idx == visibleWasteCards.size - 1)
                                        val isWasteDragged = isTopPlayable && draggedCardInfo?.let { (cards, source) ->
                                            source is CardSource.Waste && cards.firstOrNull()?.id == wasteCard.id
                                        } == true
                                        val isWasteAnimating = isTopPlayable && animatingCardInfo?.cards?.any { it.id == wasteCard.id } == true

                                        Box(
                                            modifier = Modifier
                                                .padding(start = fanStep * idx)
                                                .then(
                                                    if (isWasteDragged) {
                                                        Modifier
                                                            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                                            .onGloballyPositioned { coordinates ->
                                                                draggedCardBounds = coordinates.boundsInRoot()
                                                            }
                                                            .zIndex(20f)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .onGloballyPositioned { coordinates ->
                                                    if (isTopPlayable) {
                                                        cardBounds[wasteCard.id] = coordinates.boundsInRoot()
                                                    }
                                                }
                                                .then(
                                                    if (isTopPlayable && !isWasteAnimating) {
                                                        Modifier.pointerInput(wasteCard.id) {
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
                                                                    draggedCardInfo = listOf(wasteCard) to CardSource.Waste
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
                                                                        handleCardTap(wasteCard, CardSource.Waste)
                                                                    } else {
                                                                        val droppedSuccessfully = handleDrop(
                                                                            draggedCardInfo?.first ?: emptyList(),
                                                                            draggedCardInfo?.second ?: CardSource.Waste
                                                                        )
                                                                        coroutineScope.launch {
                                                                            if (!droppedSuccessfully) {
                                                                                launch { offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
                                                                                launch { offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
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
                                                                        offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                                                                        offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
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
                                                    card = wasteCard,
                                                    width = cardWidth,
                                                    height = cardHeight,
                                                    cardBackTheme = cardBackTheme,
                                                    isHighlighted = isTopPlayable && activeHint?.sourceCardId == wasteCard.id,
                                                    onClick = if (isTopPlayable && topWasteCard != null) {
                                                        { handleCardTap(topWasteCard, CardSource.Waste) }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val foundationsContent: @Composable () -> Unit = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val suits = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)
                            for (suit in suits) {
                                val foundationCards = boardState.foundationPiles[suit] ?: emptyList()
                                val topCard = foundationCards.lastOrNull()
                                val isFoundationDragged = draggedCardInfo?.let { (cards, source) ->
                                    source is CardSource.Foundation && source.suit == suit && cards.firstOrNull()?.id == topCard?.id
                                } == true
                                val isFoundationAnimating = topCard != null && animatingCardInfo?.cards?.any { it.id == topCard.id } == true

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            foundationBounds[suit] = coordinates.boundsInRoot()
                                            if (topCard != null) {
                                                cardBounds[topCard.id] = coordinates.boundsInRoot()
                                            }
                                        }
                                        .then(
                                            if (isFoundationDragged) {
                                                Modifier
                                                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                                                    .onGloballyPositioned { coordinates ->
                                                        draggedCardBounds = coordinates.boundsInRoot()
                                                    }
                                                    .zIndex(20f)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .then(
                                            if (topCard != null && !isFoundationAnimating) {
                                                Modifier.pointerInput(topCard.id) {
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
                                                            draggedCardInfo = listOf(topCard) to CardSource.Foundation(suit)
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
                                                                handleCardTap(topCard, CardSource.Foundation(suit))
                                                            } else {
                                                                val droppedSuccessfully = handleDrop(
                                                                    draggedCardInfo?.first ?: emptyList(),
                                                                    draggedCardInfo?.second ?: CardSource.Foundation(suit)
                                                                )
                                                                coroutineScope.launch {
                                                                    if (!droppedSuccessfully) {
                                                                        launch { offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
                                                                        launch { offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
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
                                                                offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                                                                offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                                                                draggedCardInfo = null
                                                                draggedCardBounds = null
                                                            }
                                                        }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    CardView(
                                        card = if (isFoundationAnimating) foundationCards.dropLast(1).lastOrNull() else topCard,
                                        width = cardWidth,
                                        height = cardHeight,
                                        cardBackTheme = cardBackTheme,
                                        emptySlotSymbol = suit.symbol,
                                        isHighlighted = activeHint?.targetSuit == suit,
                                        onClick = if (topCard != null) {
                                            { handleCardTap(topCard, CardSource.Foundation(suit)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    if (playerStats.leftHandedMode) {
                        foundationsContent()
                        stockAndWasteContent()
                    } else {
                        stockAndWasteContent()
                        foundationsContent()
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Tableau Piles (7 Columns)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val piles = if (boardState.tableauPiles.isEmpty()) List(7) { emptyList<Card>() } else boardState.tableauPiles
                    piles.forEachIndexed { pileIndex, pile ->
                        val isTargetPileHighlighted = activeHint?.targetPileIndex == pileIndex
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
                                    height = cardHeight,
                                    emptySlotSymbol = "K",
                                    isHighlighted = isTargetPileHighlighted
                                )
                            } else {
                                var cumulativeOffset = 0.dp
                                pile.forEachIndexed { cardIndex, card ->
                                    val topOffset = cumulativeOffset
                                    val isFaceUpCard = card.isFaceUp
                                    val overlap = if (isFaceUpCard) cardHeight * 0.29f else cardHeight * 0.14f
                                    cumulativeOffset += overlap

                                    val draggedStack = draggedCardInfo
                                    val isBeingDragged = draggedStack != null &&
                                            draggedStack.second is CardSource.Tableau &&
                                            (draggedStack.second as CardSource.Tableau).pileIndex == pileIndex &&
                                            pile.indexOf(card) >= pile.indexOf(draggedStack.first.first())

                                    val isLeadingDraggedCard = isBeingDragged && draggedStack?.first?.firstOrNull()?.id == card.id
                                    val isCardAnimating = animatingCardInfo?.cards?.any { it.id == card.id } == true
                                    val isCardHinted = activeHint?.sourceCardId == card.id ||
                                            (isTargetPileHighlighted && cardIndex == pile.size - 1)

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
                                                        .then(
                                                            // Record collision bounds strictly from the leading card of the dragged stack
                                                            if (isLeadingDraggedCard) {
                                                                Modifier.onGloballyPositioned { coordinates ->
                                                                    draggedCardBounds = coordinates.boundsInRoot()
                                                                }
                                                            } else {
                                                                Modifier
                                                            }
                                                        )
                                                        .zIndex(20f + cardIndex)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .then(
                                                if (isFaceUpCard && !isCardAnimating) {
                                                    Modifier.pointerInput(card.id, pile.size) {
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
                                                                    val droppedSuccessfully = handleDrop(
                                                                        draggedCardInfo?.first ?: emptyList(),
                                                                        draggedCardInfo?.second ?: CardSource.Tableau(pileIndex)
                                                                    )
                                                                    coroutineScope.launch {
                                                                        if (!droppedSuccessfully) {
                                                                            launch { offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
                                                                            launch { offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing)) }
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
                                                                    offsetX.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
                                                                    offsetY.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
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
                                                cardBackTheme = cardBackTheme,
                                                isHighlighted = isCardHinted,
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

            // 5. Flying Card Stack Overlay during Tap-to-Move Animation
            animatingCardInfo?.let { info ->
                val containerLeft = containerBounds?.left ?: 0f
                val containerTop = containerBounds?.top ?: 0f
                val localStartX = info.sourceBounds.left - containerLeft
                val localStartY = info.sourceBounds.top - containerTop
                val currentOffset = animOffset.value

                info.cards.forEachIndexed { idx, movingCard ->
                    val stackStepPx = (cardHeight.value * 0.29f * idx).dp
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (localStartX + currentOffset.x).roundToInt(),
                                    (localStartY + currentOffset.y + stackStepPx.toPx()).roundToInt()
                                )
                            }
                            .width(cardWidth)
                            .height(cardHeight)
                            .zIndex(1000f + idx)
                    ) {
                        CardView(
                            card = movingCard,
                            width = cardWidth,
                            height = cardHeight,
                            cardBackTheme = cardBackTheme
                        )
                    }
                }
            }

            // 6. Victory Celebration Modal
            if (boardState.isWon) {
                val formattedTime = String.format("%02d:%02d", secondsElapsed / 60, secondsElapsed % 60)
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "👑 🎉 🏆", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (boardState.isDailyChallenge) "¡Desafío Diario Superado!" else "¡Victoria Magistral!",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Nivel ${playerStats.level} • ${playerStats.playerTitleEs}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            LinearProgressIndicator(
                                progress = { playerStats.xpProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "XP: ${playerStats.currentLevelXp} / ${playerStats.xpForNextLevel} para Nivel ${playerStats.level + 1}",
                                fontSize = 11.sp
                            )
                            HorizontalDivider()
                            Text(text = "⏱️ Tiempo: $formattedTime", fontWeight = FontWeight.SemiBold)
                            Text(text = "🎯 Movimientos: ${boardState.moves}", fontWeight = FontWeight.SemiBold)
                            Text(text = "🏆 Puntuación Final: ${boardState.score}", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "🔥 Racha Actual: ${playerStats.currentStreak} días seguidos (🛡️ ${playerStats.streakFreezesAvailable} escudos)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )

                            if (newlyUnlockedAchievements.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = "🌟 ¡Nuevos Logros Desbloqueados!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                newlyUnlockedAchievements.forEach { ach ->
                                    Text(
                                        text = "${ach.badgeEmoji} ${ach.titleEs} (+${ach.xpReward} XP)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissUnlockedAchievementsDialog()
                                AdMobInterstitialHelper.showInterstitialIfEligible(
                                    context = context,
                                    movesInCurrentGame = boardState.moves
                                ) {
                                    viewModel.startNewGame()
                                }
                            }
                        ) {
                            Text(text = "Jugar Otra Mano", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissUnlockedAchievementsDialog()
                                showDailyChallengeDialog = true
                            }
                        ) {
                            Text(text = "Retos Diarios")
                        }
                    }
                )
            }

            // 7. New Game Options Dialog
            if (showNewGameMenuDialog) {
                AlertDialog(
                    onDismissRequest = { showNewGameMenuDialog = false },
                    title = { Text("Mesa de Juego", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    showNewGameMenuDialog = false
                                    AdMobInterstitialHelper.showInterstitialIfEligible(
                                        context = context,
                                        movesInCurrentGame = boardState.moves
                                    ) {
                                        viewModel.startNewGame(winningDeal = true)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✨ Repartir Mano Ganadora (Resoluble)")
                            }
                            Button(
                                onClick = {
                                    showNewGameMenuDialog = false
                                    AdMobInterstitialHelper.showInterstitialIfEligible(
                                        context = context,
                                        movesInCurrentGame = boardState.moves
                                    ) {
                                        viewModel.startNewGame(winningDeal = false)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎲 Baraja Aleatoria Clásica")
                            }
                            OutlinedButton(
                                onClick = {
                                    showNewGameMenuDialog = false
                                    viewModel.startDailyChallenge()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📅 Jugar Desafío Diario de Hoy")
                            }
                            if (boardState.moves > 0) {
                                OutlinedButton(
                                    onClick = {
                                        showNewGameMenuDialog = false
                                        viewModel.restartCurrentDeal()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔄 Reiniciar Esta Misma Mano desde Cero")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNewGameMenuDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // 8. Daily Challenges & Streak Modal
            if (showDailyChallengeDialog) {
                DailyChallengeDialog(
                    stats = playerStats,
                    onStartDailyForDate = { dateIso ->
                        showDailyChallengeDialog = false
                        viewModel.startDailyChallenge(dateIso)
                    },
                    onDismiss = { showDailyChallengeDialog = false }
                )
            }

            // 9. Stats & Achievements Modal
            if (showStatsAndAchievementsDialog) {
                StatsAndAchievementsDialog(
                    stats = playerStats,
                    onDismiss = { showStatsAndAchievementsDialog = false }
                )
            }

            // 10. Themes & Card Backs Customization Modal
            if (showThemesDialog) {
                ThemesCustomizationDialog(
                    stats = playerStats,
                    onSelectTableTheme = { viewModel.selectTableTheme(it) },
                    onSelectCardBack = { viewModel.selectCardBackTheme(it) },
                    onDismiss = { showThemesDialog = false }
                )
            }

            // 11. Game Settings Modal
            if (showSettingsDialog) {
                GameSettingsDialog(
                    stats = playerStats,
                    onUpdateSettings = { draw, scoring, leftHanded, winningOnly, haptics, autoComplete, startNew ->
                        viewModel.updateGameSettings(
                            drawCount = draw,
                            scoringMode = scoring,
                            leftHandedMode = leftHanded,
                            winningDealsOnly = winningOnly,
                            hapticsEnabled = haptics,
                            autoCompleteEnabled = autoComplete,
                            startNewDealImmediately = startNew
                        )
                    },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
fun ProgressionTopBar(
    stats: PlayerStats,
    headerColor: Color,
    accentColor: Color,
    onDailyClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Level & XP bar
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onStatsClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NV. ${stats.level}",
                        color = Color(0xFF1A1A1A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = stats.playerTitleEs,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stats.xpProgressFraction },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        // Daily Streak Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { onDailyClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🔥 ${stats.currentStreak}d",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (stats.streakFreezesAvailable > 0) {
                Text(
                    text = "🛡️${stats.streakFreezesAvailable}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onStatsClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "Estadísticas y Logros",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GameScoreHeader(
    score: Int,
    scoringMode: ScoringMode,
    drawCount: Int,
    isDailyChallenge: Boolean,
    dailyDate: String?,
    moves: Int,
    seconds: Long,
    headerColor: Color,
    accentColor: Color
) {
    val formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
    val scoreDisplay = if (scoringMode == ScoringMode.VEGAS) {
        if (score < 0) "-$${-score}" else "$$score"
    } else {
        score.toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PUNTOS: $scoreDisplay",
            color = if (scoringMode == ScoringMode.VEGAS && score < 0) Color(0xFFFF8A80) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "⏱️ $formattedTime",
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "MOVS: $moves",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isDailyChallenge) "📅 Reto ${dailyDate?.takeLast(5) ?: ""}" else "Robo $drawCount • ${scoringMode.displayNameEs}",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomControlDock(
    headerColor: Color,
    accentColor: Color,
    canUndo: Boolean,
    onNewGameClick: () -> Unit,
    onDailyClick: () -> Unit,
    onHintClick: () -> Unit,
    onUndoClick: () -> Unit,
    onThemesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(
            icon = Icons.Rounded.Refresh,
            label = "Nueva",
            tint = Color.White,
            onClick = onNewGameClick
        )
        DockButton(
            icon = Icons.Rounded.CalendarMonth,
            label = "Diario",
            tint = accentColor,
            onClick = onDailyClick
        )
        DockButton(
            icon = Icons.Rounded.Lightbulb,
            label = "Pista",
            tint = accentColor,
            onClick = onHintClick
        )
        DockButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            label = "Deshacer",
            tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.38f),
            onClick = onUndoClick
        )
        DockButton(
            icon = Icons.Rounded.Palette,
            label = "Temas",
            tint = Color.White,
            onClick = onThemesClick
        )
    }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DailyChallengeDialog(
    stats: PlayerStats,
    onStartDailyForDate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val recentDays = remember {
        val list = mutableListOf<Pair<String, String>>()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))
        for (offset in 0 downTo -6) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val iso = isoFormat.format(cal.time)
            val label = if (offset == 0) "Hoy (${labelFormat.format(cal.time)})" else labelFormat.format(cal.time)
            list.add(iso to label)
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📅 Desafíos Diarios y Racha", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔥 Racha Actual: ${stats.currentStreak} días | Mejor Racha: ${stats.bestStreak} días",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD84315)
                )
                Text(
                    text = "🛡️ Escudos Protectores de Racha: ${stats.streakFreezesAvailable} (se activan automáticamente si olvidas jugar un día; ganas +1 cada 5 días de racha).",
                    fontSize = 12.sp
                )
                HorizontalDivider()
                Text(
                    text = "Elige un reto del calendario (reparto único resoluble por fecha):",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                recentDays.forEach { (isoDate, label) ->
                    val completed = stats.completedDailyDates.contains(isoDate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (completed) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (completed) "✅ $label" else "🎯 $label",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF212121)
                        )
                        Button(
                            onClick = { onStartDailyForDate(isoDate) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(if (completed) "Rejugar" else "Jugar", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun StatsAndAchievementsDialog(
    stats: PlayerStats,
    onDismiss: () -> Unit
) {
    val bestTimeFormatted = if (stats.bestTimeSeconds > 0L) {
        String.format("%02d:%02d", stats.bestTimeSeconds / 60, stats.bestTimeSeconds % 60)
    } else {
        "--:--"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🏆 Perfil, Estadísticas y Logros", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Nivel ${stats.level} • ${stats.playerTitleEs} (${stats.xp} XP totales)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                LinearProgressIndicator(
                    progress = { stats.xpProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                HorizontalDivider()
                Text("• Partidas Ganadas: ${stats.gamesWon} de ${stats.gamesPlayed} (${stats.winRatePercent}%)", fontSize = 13.sp)
                Text("• Mejor Puntuación: ${stats.bestScore}", fontSize = 13.sp)
                Text("• Mejor Tiempo: $bestTimeFormatted", fontSize = 13.sp)
                Text("• Menos Movimientos: ${if (stats.fewestMoves > 0) stats.fewestMoves else "--"}", fontSize = 13.sp)
                Text("• Banca Acumulada Vegas: $${stats.vegasBankroll}", fontSize = 13.sp)
                Text("• Retos Diarios Superados: ${stats.completedDailyDates.size}", fontSize = 13.sp)

                HorizontalDivider()
                Text(
                    text = "Medallas Desbloqueadas (${stats.unlockedAchievements.size}/${Achievement.entries.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Achievement.entries.forEach { ach ->
                    val unlocked = stats.unlockedAchievements.contains(ach.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (unlocked) Color(0xFFFFF8E1) else Color(0xFFEEEEEE),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (unlocked) ach.badgeEmoji else "🔒",
                            fontSize = 20.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${ach.titleEs} (+${ach.xpReward} XP)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (unlocked) Color(0xFF212121) else Color(0xFF757575)
                            )
                            Text(
                                text = ach.descriptionEs,
                                fontSize = 11.sp,
                                color = Color(0xFF616161)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ThemesCustomizationDialog(
    stats: PlayerStats,
    onSelectTableTheme: (TableTheme) -> Unit,
    onSelectCardBack: (CardBackTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🎨 Tapetes y Dorsos Desbloqueables", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tu Nivel Actual: ${stats.level} (Gana partidas y logros para subir de nivel y desbloquear temas VIP)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text("Color del Tapete:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                TableTheme.entries.forEach { theme ->
                    val unlocked = stats.level >= theme.requiredLevel
                    val isSelected = stats.selectedTableTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                            .clickable(enabled = unlocked) { onSelectTableTheme(theme) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(theme.feltColorHex), CircleShape)
                                    .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                            )
                            Column {
                                Text(
                                    text = theme.titleEs,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF212121)
                                )
                                if (!unlocked) {
                                    Text(
                                        text = "Se desbloquea en Nivel ${theme.requiredLevel}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                isSelected -> "✅ Activo"
                                unlocked -> "Usar"
                                else -> "🔒 Nv.${theme.requiredLevel}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider()
                Text("Dorso de las Cartas:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                CardBackTheme.entries.forEach { back ->
                    val unlocked = stats.level >= back.requiredLevel
                    val isSelected = stats.selectedCardBack == back
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                            .clickable(enabled = unlocked) { onSelectCardBack(back) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp, 32.dp)
                                    .background(Color(back.primaryColorHex), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(back.centerSymbol, color = Color.White, fontSize = 12.sp)
                            }
                            Column {
                                Text(
                                    text = back.titleEs,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF212121)
                                )
                                if (!unlocked) {
                                    Text(
                                        text = "Se desbloquea en Nivel ${back.requiredLevel}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                isSelected -> "✅ Activo"
                                unlocked -> "Usar"
                                else -> "🔒 Nv.${back.requiredLevel}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        }
    )
}

@Composable
fun GameSettingsDialog(
    stats: PlayerStats,
    onUpdateSettings: (
        drawCount: Int,
        scoringMode: ScoringMode,
        leftHanded: Boolean,
        winningOnly: Boolean,
        haptics: Boolean,
        autoComplete: Boolean,
        startNew: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var drawCount by remember { mutableIntStateOf(stats.preferredDrawCount) }
    var scoringMode by remember { mutableStateOf(stats.preferredScoringMode) }
    var leftHanded by remember { mutableStateOf(stats.leftHandedMode) }
    var winningOnly by remember { mutableStateOf(stats.winningDealsOnly) }
    var haptics by remember { mutableStateOf(stats.hapticsEnabled) }
    var autoComplete by remember { mutableStateOf(stats.autoCompleteEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⚙️ Ajustes de Juego y Accesibilidad", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Modo de Robo del Mazo:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = drawCount == 1,
                        onClick = { drawCount = 1 },
                        label = { Text("Robar 1 Carta") }
                    )
                    FilterChip(
                        selected = drawCount == 3,
                        onClick = { drawCount = 3 },
                        label = { Text("Robar 3 Cartas") }
                    )
                }

                Text("Sistema de Puntuación:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = scoringMode == ScoringMode.STANDARD,
                        onClick = { scoringMode = ScoringMode.STANDARD },
                        label = { Text("Clásico Estándar") }
                    )
                    FilterChip(
                        selected = scoringMode == ScoringMode.VEGAS,
                        onClick = { scoringMode = ScoringMode.VEGAS },
                        label = { Text("Casino Vegas (-$52)") }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Priorizar Manos Ganadoras", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = winningOnly, onCheckedChange = { winningOnly = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo Mano Izquierda (Zurdo)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = leftHanded, onCheckedChange = { leftHanded = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sugerir Auto-Completar", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = autoComplete, onCheckedChange = { autoComplete = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Respuesta Háptica (Vibración sutil)", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = haptics, onCheckedChange = { haptics = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val modeChanged = (drawCount != stats.preferredDrawCount || scoringMode != stats.preferredScoringMode)
                    onUpdateSettings(drawCount, scoringMode, leftHanded, winningOnly, haptics, autoComplete, modeChanged)
                    onDismiss()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(name = "Game Screen Preview")
@Composable
fun GameScreenPreview() {
    SolitarioClásicoJuegoDeCartasTheme {
        GameScreen()
    }
}
