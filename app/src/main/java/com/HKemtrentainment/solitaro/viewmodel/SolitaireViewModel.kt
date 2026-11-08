package com.HKemtrentainment.solitaro.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HKemtrentainment.solitaro.analytics.AnalyticsManager
import com.HKemtrentainment.solitaro.data.GamePreferencesRepository
import com.HKemtrentainment.solitaro.model.Achievement
import com.HKemtrentainment.solitaro.model.BoardState
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.CardBackTheme
import com.HKemtrentainment.solitaro.model.Deck
import com.HKemtrentainment.solitaro.model.HintMove
import com.HKemtrentainment.solitaro.model.PlayerStats
import com.HKemtrentainment.solitaro.model.Rank
import com.HKemtrentainment.solitaro.model.ScoringMode
import com.HKemtrentainment.solitaro.model.Suit
import com.HKemtrentainment.solitaro.model.TableTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class CardSource {
    object Stock : CardSource()
    object Waste : CardSource()
    data class Foundation(val suit: Suit) : CardSource()
    data class Tableau(val pileIndex: Int) : CardSource()
}

sealed class AutoMoveDestination {
    data class Foundation(val suit: Suit) : AutoMoveDestination()
    data class Tableau(val pileIndex: Int) : AutoMoveDestination()
}

class SolitaireViewModel : ViewModel() {

    private val deck = Deck()
    private var prefsRepo: GamePreferencesRepository? = null
    private var isContextAttached = false

    private val _playerStats = MutableStateFlow(PlayerStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    private val _boardState = MutableStateFlow(deck.createInitialBoard())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _secondsElapsed = MutableStateFlow(0L)
    val secondsElapsed: StateFlow<Long> = _secondsElapsed.asStateFlow()

    private val _activeHint = MutableStateFlow<HintMove?>(null)
    val activeHint: StateFlow<HintMove?> = _activeHint.asStateFlow()

    private val _isNoMoreMoves = MutableStateFlow(false)
    val isNoMoreMoves: StateFlow<Boolean> = _isNoMoreMoves.asStateFlow()

    private val _newlyUnlockedAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val newlyUnlockedAchievements: StateFlow<List<Achievement>> = _newlyUnlockedAchievements.asStateFlow()

    private val _isAutoCompleting = MutableStateFlow(false)
    val isAutoCompleting: StateFlow<Boolean> = _isAutoCompleting.asStateFlow()

    private var timerJob: Job? = null
    private var autoCompleteJob: Job? = null
    private val historyStack = mutableListOf<BoardState>()
    private var usedUndoInCurrentGame = false

    init {
        initializeGame()
    }

    /**
     * Attaches application context once to restore saved player progression and any active game in progress.
     */
    fun attachContext(context: Context) {
        if (isContextAttached) return
        isContextAttached = true
        val repo = GamePreferencesRepository(context.applicationContext)
        prefsRepo = repo

        val loadedStats = repo.loadPlayerStats()
        _playerStats.value = loadedStats

        val savedGame = repo.loadSavedGame()
        if (savedGame != null && (savedGame.first.moves > 0 || savedGame.second > 0L)) {
            historyStack.clear()
            _boardState.value = savedGame.first
            _secondsElapsed.value = savedGame.second
            startTimer(resetSeconds = false)
            evaluateBoardDeadlock()
        } else {
            // Apply user's preferred draw count and winning-deal setting to initial board if untouched
            if (_boardState.value.moves == 0) {
                initializeGame(
                    drawCount = loadedStats.preferredDrawCount,
                    scoringMode = loadedStats.preferredScoringMode,
                    winningDeal = loadedStats.winningDealsOnly
                )
            }
        }
    }

    private fun startTimer(resetSeconds: Boolean = true) {
        timerJob?.cancel()
        if (resetSeconds) {
            _secondsElapsed.value = 0L
        }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_boardState.value.isWon) {
                    _secondsElapsed.value += 1L
                    // Periodically auto-save every 5 seconds
                    if (_secondsElapsed.value % 5L == 0L) {
                        prefsRepo?.saveActiveGame(_boardState.value, _secondsElapsed.value)
                    }
                }
            }
        }
    }

    /**
     * Initializes a new standard or winning-deal game, shuffling the deck and resetting history.
     */
    fun initializeGame(
        drawCount: Int = _playerStats.value.preferredDrawCount,
        scoringMode: ScoringMode = _playerStats.value.preferredScoringMode,
        winningDeal: Boolean = false,
        seed: Long? = null,
        isDailyChallenge: Boolean = false,
        dailyChallengeDate: String? = null
    ) {
        autoCompleteJob?.cancel()
        _isAutoCompleting.value = false
        _activeHint.value = null
        _isNoMoreMoves.value = false
        usedUndoInCurrentGame = false

        val newState = deck.createInitialBoard(
            seed = seed,
            winningDeal = winningDeal,
            drawCount = drawCount,
            scoringMode = scoringMode,
            isDailyChallenge = isDailyChallenge,
            dailyChallengeDate = dailyChallengeDate
        )
        historyStack.clear()
        _boardState.value = newState
        startTimer(resetSeconds = true)
        prefsRepo?.saveActiveGame(newState, 0L)
        AnalyticsManager.logGameStart(
            drawCount = drawCount,
            isDailyChallenge = isDailyChallenge,
            isWinningDeal = winningDeal
        )
    }

    fun startNewGame(
        winningDeal: Boolean = _playerStats.value.winningDealsOnly,
        drawCount: Int = _playerStats.value.preferredDrawCount,
        scoringMode: ScoringMode = _playerStats.value.preferredScoringMode
    ) {
        recordGamePlayedIfStarted()
        initializeGame(
            drawCount = drawCount,
            scoringMode = scoringMode,
            winningDeal = winningDeal
        )
    }

    /**
     * Starts today's deterministic Daily Challenge (or a specific date in YYYY-MM-DD format).
     */
    fun startDailyChallenge(dateIso: String = getTodayIsoDate()) {
        recordGamePlayedIfStarted()
        val seed = Deck.seedForDate(dateIso)
        initializeGame(
            drawCount = 1,
            scoringMode = ScoringMode.STANDARD,
            winningDeal = true,
            seed = seed,
            isDailyChallenge = true,
            dailyChallengeDate = dateIso
        )
    }

    /**
     * Replays the exact same deal from move 0.
     */
    fun restartCurrentDeal() {
        if (historyStack.isEmpty()) return
        autoCompleteJob?.cancel()
        _isAutoCompleting.value = false
        _activeHint.value = null
        _isNoMoreMoves.value = false
        usedUndoInCurrentGame = false

        val initialBoard = historyStack.first()
        historyStack.clear()
        _boardState.value = initialBoard
        startTimer(resetSeconds = true)
        prefsRepo?.saveActiveGame(initialBoard, 0L)
    }

    private fun recordGamePlayedIfStarted() {
        val current = _boardState.value
        if (current.moves > 0 && !current.isWon) {
            val updated = _playerStats.value.copy(
                gamesPlayed = _playerStats.value.gamesPlayed + 1,
                xp = _playerStats.value.xp + 10 // Participation XP for engagement loop
            )
            _playerStats.value = updated
            prefsRepo?.savePlayerStats(updated)
        }
    }

    /**
     * Checks whether a card can be moved to a target tableau pile card (or empty pile).
     */
    fun canMoveToTableau(card: Card, targetCard: Card?): Boolean {
        return card.canPlaceOnTableau(targetCard)
    }

    /**
     * Checks whether a card can be moved to a foundation pile.
     */
    fun canMoveToFoundation(card: Card, foundationPile: List<Card>): Boolean {
        return card.canPlaceOnFoundation(foundationPile.lastOrNull())
    }

    /**
     * Taps stock pile to draw 1 or 3 cards to waste pile, or recycles waste back to stock.
     * Fixes bug where tapping an empty stock+waste still incremented moves and polluted undo history.
     */
    fun onStockTapped() {
        val currentState = _boardState.value
        if (currentState.isWon) return

        val stock = currentState.stockPile.toMutableList()
        val waste = currentState.wastePile.toMutableList()

        // Do nothing if both stock and waste are empty
        if (stock.isEmpty() && waste.isEmpty()) return

        // In Vegas mode, passes through the deck are limited (1 pass in Draw 1, 3 passes in Draw 3)
        if (stock.isEmpty() && currentState.scoringMode == ScoringMode.VEGAS) {
            val maxRecycles = if (currentState.drawCount == 3) 2 else 0
            if (currentState.stockRecycleCount >= maxRecycles) {
                evaluateBoardDeadlock()
                return
            }
        }

        historyStack.add(currentState)
        _activeHint.value = null

        var newScore = currentState.score
        var newRecycleCount = currentState.stockRecycleCount

        if (stock.isNotEmpty()) {
            val countToDraw = minOf(currentState.drawCount, stock.size)
            repeat(countToDraw) {
                val card = stock.removeAt(stock.size - 1)
                waste.add(card.copy(isFaceUp = true))
            }
        } else {
            // Recycle waste back into stock in reverse order (face down)
            stock.addAll(waste.reversed().map { it.copy(isFaceUp = false) })
            waste.clear()
            newRecycleCount += 1

            // Official Klondike standard scoring penalty when recycling waste
            if (currentState.scoringMode == ScoringMode.STANDARD) {
                if (currentState.drawCount == 1 && newRecycleCount >= 2) {
                    newScore = (newScore - 100).coerceAtLeast(0)
                } else if (currentState.drawCount == 3 && newRecycleCount > 3) {
                    newScore = (newScore - 20).coerceAtLeast(0)
                }
            }
        }

        val updatedState = currentState.copy(
            stockPile = stock,
            wastePile = waste,
            moves = currentState.moves + 1,
            score = newScore,
            stockRecycleCount = newRecycleCount
        )
        _boardState.value = updatedState
        prefsRepo?.saveActiveGame(updatedState, _secondsElapsed.value)
        evaluateBoardDeadlock()
    }

    /**
     * Moves a card (or valid face-up subsequence) from waste, tableau, or foundation
     * to a target tableau column index.
     * Fixes:
     * 1. Supports moving a top card back from Foundation -> Tableau (essential Klondike strategy).
     * 2. Eliminates infinite score exploit on Tableau -> Tableau moves that don't reveal a face-down card.
     */
    fun moveCardToTableau(card: Card, targetColumnIndex: Int) {
        val currentState = _boardState.value
        if (currentState.isWon) return
        val targetPile = currentState.tableauPiles.getOrNull(targetColumnIndex) ?: return
        val targetCard = targetPile.lastOrNull()

        val newWaste = currentState.wastePile.toMutableList()
        val newTableau = currentState.tableauPiles.map { it.toMutableList() }.toMutableList()
        val newFoundations = currentState.foundationPiles.toMutableMap()

        var isWasteMove = false
        var isFoundationMove = false
        var foundSourcePileIndex = -1
        var cardIndexInPile = -1
        val movingCards: List<Card>

        if (newWaste.isNotEmpty() && newWaste.last().id == card.id) {
            isWasteMove = true
            movingCards = listOf(newWaste.last())
        } else if (newFoundations[card.suit]?.lastOrNull()?.id == card.id) {
            isFoundationMove = true
            movingCards = listOf(card)
        } else {
            for (i in newTableau.indices) {
                val idx = newTableau[i].indexOfFirst { it.id == card.id }
                if (idx != -1) {
                    foundSourcePileIndex = i
                    cardIndexInPile = idx
                    break
                }
            }
            // Cannot move to the exact same column
            if (foundSourcePileIndex == targetColumnIndex) return

            if (foundSourcePileIndex != -1 && cardIndexInPile != -1) {
                val sourcePile = newTableau[foundSourcePileIndex]
                val subList = sourcePile.subList(cardIndexInPile, sourcePile.size)
                if (subList.all { it.isFaceUp }) {
                    movingCards = subList.toList()
                } else {
                    return
                }
            } else {
                return
            }
        }

        // Validate using the bottom-most card of the moving stack against the destination's top card
        if (!canMoveToTableau(movingCards.first(), targetCard)) {
            return
        }

        historyStack.add(currentState)
        _activeHint.value = null

        var revealedHiddenCard = false

        when {
            isWasteMove -> {
                newWaste.removeAt(newWaste.size - 1)
                newTableau[targetColumnIndex].addAll(movingCards.map { it.copy(isFaceUp = true) })
            }
            isFoundationMove -> {
                val currentSuitPile = newFoundations[card.suit] ?: emptyList()
                newFoundations[card.suit] = currentSuitPile.dropLast(1)
                newTableau[targetColumnIndex].add(card.copy(isFaceUp = true))
            }
            else -> {
                val sourcePile = newTableau[foundSourcePileIndex]
                repeat(movingCards.size) { sourcePile.removeAt(sourcePile.size - 1) }
                newTableau[targetColumnIndex].addAll(movingCards)

                if (sourcePile.isNotEmpty() && !sourcePile.last().isFaceUp) {
                    sourcePile[sourcePile.size - 1] = sourcePile.last().copy(isFaceUp = true)
                    revealedHiddenCard = true
                }
            }
        }

        // Official Klondike scoring:
        // - Waste -> Tableau: +5 pts (Standard), 0 (Vegas)
        // - Tableau -> Tableau: +5 pts ONLY if a face-down card was revealed, 0 otherwise
        // - Foundation -> Tableau: -15 pts (Standard, floor 0), -5 pts (Vegas)
        val scoreDelta = when (currentState.scoringMode) {
            ScoringMode.STANDARD -> when {
                isWasteMove -> 5
                isFoundationMove -> -15
                revealedHiddenCard -> 5
                else -> 0
            }
            ScoringMode.VEGAS -> if (isFoundationMove) -5 else 0
        }

        val updatedScore = if (currentState.scoringMode == ScoringMode.STANDARD) {
            (currentState.score + scoreDelta).coerceAtLeast(0)
        } else {
            currentState.score + scoreDelta
        }

        val updatedState = currentState.copy(
            wastePile = newWaste,
            tableauPiles = newTableau.map { it.toList() },
            foundationPiles = newFoundations,
            moves = currentState.moves + 1,
            score = updatedScore
        )
        _boardState.value = updatedState
        prefsRepo?.saveActiveGame(updatedState, _secondsElapsed.value)
        evaluateBoardDeadlock()
    }

    /**
     * Checks if the game has been won (all 4 foundation piles have 13 cards each = 52 cards).
     */
    private fun checkWinCondition(foundationPiles: Map<Suit, List<Card>>): Boolean {
        return foundationPiles.values.all { it.size == 13 }
    }

    /**
     * Moves a card from waste or tableau to its suit foundation pile.
     */
    fun moveCardToFoundation(card: Card, suit: Suit) {
        val currentState = _boardState.value
        if (currentState.isWon) return
        if (card.suit != suit) return
        val foundationPile = currentState.foundationPiles[suit] ?: emptyList()

        if (!canMoveToFoundation(card, foundationPile)) return

        val newWaste = currentState.wastePile.toMutableList()
        val newTableau = currentState.tableauPiles.map { it.toMutableList() }.toMutableList()
        val newFoundations = currentState.foundationPiles.toMutableMap()

        var moved = false
        var revealedHiddenCard = false

        if (newWaste.isNotEmpty() && newWaste.last().id == card.id) {
            newWaste.removeAt(newWaste.size - 1)
            moved = true
        } else {
            for (pile in newTableau) {
                if (pile.isNotEmpty() && pile.last().id == card.id) {
                    pile.removeAt(pile.size - 1)
                    if (pile.isNotEmpty() && !pile.last().isFaceUp) {
                        pile[pile.size - 1] = pile.last().copy(isFaceUp = true)
                        revealedHiddenCard = true
                    }
                    moved = true
                    break
                }
            }
        }

        if (!moved) return

        historyStack.add(currentState)
        _activeHint.value = null

        newFoundations[suit] = foundationPile + card.copy(isFaceUp = true)
        val isWon = checkWinCondition(newFoundations)

        val basePoints = if (currentState.scoringMode == ScoringMode.VEGAS) {
            5
        } else {
            10 + (if (revealedHiddenCard) 5 else 0)
        }

        // Calculate time bonus on victory in Standard mode (classic Windows Klondike formula: 700,000 / seconds)
        val elapsed = _secondsElapsed.value
        val timeBonus = if (isWon && currentState.scoringMode == ScoringMode.STANDARD && elapsed >= 30L) {
            (700_000L / elapsed).toInt().coerceAtMost(10_000)
        } else {
            0
        }

        val finalScore = currentState.score + basePoints + timeBonus
        val finalMoves = currentState.moves + 1

        val updatedState = currentState.copy(
            wastePile = newWaste,
            tableauPiles = newTableau.map { it.toList() },
            foundationPiles = newFoundations,
            moves = finalMoves,
            score = finalScore,
            isWon = isWon
        )
        _boardState.value = updatedState

        if (isWon) {
            timerJob?.cancel()
            _isAutoCompleting.value = false
            _isNoMoreMoves.value = false
            prefsRepo?.clearSavedGame()
            onGameWonUpdateProgression(
                finalScore = finalScore,
                finalMoves = finalMoves,
                seconds = elapsed,
                board = updatedState
            )
        } else {
            prefsRepo?.saveActiveGame(updatedState, elapsed)
            evaluateBoardDeadlock()
        }
    }

    /**
     * Updates player XP, level, daily streak (with Streak Freeze protection), statistics, and achievements.
     */
    private fun onGameWonUpdateProgression(
        finalScore: Int,
        finalMoves: Int,
        seconds: Long,
        board: BoardState
    ) {
        val currentStats = _playerStats.value
        val todayIso = getTodayIsoDate()
        val yesterdayIso = getYesterdayIsoDate()

        var newStreak = currentStats.currentStreak
        var newFreezes = currentStats.streakFreezesAvailable

        when {
            currentStats.lastWinDateIso == todayIso -> {
                // Already won today; streak stays intact
                if (newStreak == 0) newStreak = 1
            }
            currentStats.lastWinDateIso == yesterdayIso || currentStats.lastWinDateIso.isEmpty() -> {
                newStreak += 1
            }
            else -> {
                // Missed at least one day: use Streak Freeze if available
                if (newFreezes > 0 && newStreak > 0) {
                    newFreezes -= 1
                    newStreak += 1
                } else {
                    newStreak = 1
                }
            }
        }

        // Award an extra Streak Freeze every 5 consecutive days (up to 2 stored)
        if (newStreak > 0 && newStreak % 5 == 0 && newFreezes < 2) {
            newFreezes += 1
        }

        val newBestStreak = maxOf(currentStats.bestStreak, newStreak)
        val newGamesWon = currentStats.gamesWon + 1
        val newGamesPlayed = currentStats.gamesPlayed + 1
        val newBestScore = maxOf(currentStats.bestScore, finalScore)
        val newBestTime = if (currentStats.bestTimeSeconds == 0L) seconds else minOf(currentStats.bestTimeSeconds, seconds)
        val newFewestMoves = if (currentStats.fewestMoves == 0) finalMoves else minOf(currentStats.fewestMoves, finalMoves)

        val updatedCompletedDailies = if (board.isDailyChallenge && board.dailyChallengeDate != null) {
            currentStats.completedDailyDates + board.dailyChallengeDate
        } else {
            currentStats.completedDailyDates
        }

        // Check newly unlocked achievements
        val unlockedSet = currentStats.unlockedAchievements.toMutableSet()
        val newlyUnlocked = mutableListOf<Achievement>()

        fun tryUnlock(achievement: Achievement, condition: Boolean) {
            if (condition && unlockedSet.add(achievement.id)) {
                newlyUnlocked.add(achievement)
                AnalyticsManager.logAchievementUnlocked(achievementId = achievement.id)
            }
        }

        tryUnlock(Achievement.FIRST_WIN, newGamesWon >= 1)
        tryUnlock(Achievement.SPEED_DEMON, seconds in 1..179)
        tryUnlock(Achievement.SHARPSHOOTER, finalMoves in 1..90)
        tryUnlock(Achievement.DRAW_THREE_MASTER, board.drawCount == 3)
        tryUnlock(Achievement.PURE_SKILL, !usedUndoInCurrentGame)
        tryUnlock(Achievement.DAILY_CHAMPION, board.isDailyChallenge)
        tryUnlock(Achievement.STREAK_3, newStreak >= 3)
        tryUnlock(Achievement.STREAK_7, newStreak >= 7)
        tryUnlock(Achievement.VEGAS_HIGH_ROLLER, board.scoringMode == ScoringMode.VEGAS && finalScore > 0)
        tryUnlock(Achievement.CENTURION, newGamesWon >= 10)

        val achievementXpBonus = newlyUnlocked.sumOf { it.xpReward }
        val baseWinXp = 120 + (if (board.isDailyChallenge) 80 else 0) + (if (board.drawCount == 3) 40 else 0)
        val newVegasBankroll = if (board.scoringMode == ScoringMode.VEGAS) {
            currentStats.vegasBankroll + finalScore
        } else {
            currentStats.vegasBankroll
        }

        val updatedStats = currentStats.copy(
            gamesPlayed = newGamesPlayed,
            gamesWon = newGamesWon,
            bestScore = newBestScore,
            bestTimeSeconds = newBestTime,
            fewestMoves = newFewestMoves,
            currentStreak = newStreak,
            bestStreak = newBestStreak,
            streakFreezesAvailable = newFreezes,
            lastWinDateIso = todayIso,
            completedDailyDates = updatedCompletedDailies,
            xp = currentStats.xp + baseWinXp + achievementXpBonus,
            vegasBankroll = newVegasBankroll,
            unlockedAchievements = unlockedSet
        )

        _playerStats.value = updatedStats
        _newlyUnlockedAchievements.value = newlyUnlocked
        prefsRepo?.savePlayerStats(updatedStats)

        AnalyticsManager.logGameWon(
            seconds = seconds,
            moves = finalMoves,
            score = finalScore,
            drawCount = board.drawCount,
            isDailyChallenge = board.isDailyChallenge,
            streak = newStreak
        )
    }

    fun dismissUnlockedAchievementsDialog() {
        _newlyUnlockedAchievements.value = emptyList()
    }

    /**
     * Finds the best valid auto-move destination for a tapped card.
     * Avoids moving a King that is already at the base (index 0) of a tableau column to another empty column.
     */
    fun findAutoMoveDestination(card: Card, source: CardSource): AutoMoveDestination? {
        if (_boardState.value.isWon) return null
        val currentState = _boardState.value

        val isSingleCard = when (source) {
            is CardSource.Waste -> true
            is CardSource.Foundation -> true
            is CardSource.Tableau -> {
                val pile = currentState.tableauPiles.getOrNull(source.pileIndex) ?: emptyList()
                pile.lastOrNull()?.id == card.id
            }
            else -> false
        }

        // 1. Try Foundation first (only if card is not already coming from a Foundation)
        if (isSingleCard && source !is CardSource.Foundation) {
            val foundationPile = currentState.foundationPiles[card.suit] ?: emptyList()
            if (canMoveToFoundation(card, foundationPile)) {
                return AutoMoveDestination.Foundation(card.suit)
            }
        }

        // Check if this card is a King already sitting at index 0 of a Tableau pile
        val isKingAlreadyOnEmptyBase = if (source is CardSource.Tableau && card.rank == Rank.KING) {
            val sourcePile = currentState.tableauPiles.getOrNull(source.pileIndex) ?: emptyList()
            sourcePile.firstOrNull()?.id == card.id
        } else {
            false
        }

        // 2. Try non-empty Tableau columns first (preferring columns with cards over empty columns)
        for (i in currentState.tableauPiles.indices) {
            if (source is CardSource.Tableau && source.pileIndex == i) continue
            val targetPile = currentState.tableauPiles[i]
            if (targetPile.isNotEmpty()) {
                val targetCard = targetPile.lastOrNull()
                if (canMoveToTableau(card, targetCard)) {
                    return AutoMoveDestination.Tableau(i)
                }
            }
        }

        // 3. Try empty Tableau columns (for Kings not already at the base of a tableau column)
        if (!isKingAlreadyOnEmptyBase) {
            for (i in currentState.tableauPiles.indices) {
                if (source is CardSource.Tableau && source.pileIndex == i) continue
                val targetPile = currentState.tableauPiles[i]
                if (targetPile.isEmpty() && canMoveToTableau(card, null)) {
                    return AutoMoveDestination.Tableau(i)
                }
            }
        }

        return null
    }

    /**
     * Automatically attempts to move a card to foundation or any valid tableau pile (Tap-to-Move).
     */
    fun autoMoveCard(card: Card, source: CardSource): Boolean {
        val destination = findAutoMoveDestination(card, source) ?: return false
        when (destination) {
            is AutoMoveDestination.Foundation -> moveCardToFoundation(card, destination.suit)
            is AutoMoveDestination.Tableau -> moveCardToTableau(card, destination.pileIndex)
        }
        return true
    }

    /**
     * Handles card tap for auto-move (Tap-to-Move) or flipping exposed face-down tableau cards.
     */
    fun onCardTapped(card: Card, source: CardSource) {
        if (_boardState.value.isWon) return
        when (source) {
            is CardSource.Waste, is CardSource.Foundation -> {
                autoMoveCard(card, source)
            }
            is CardSource.Tableau -> {
                val pile = _boardState.value.tableauPiles.getOrNull(source.pileIndex) ?: return
                val cardIndex = pile.indexOfFirst { it.id == card.id }
                if (cardIndex != -1) {
                    if (!card.isFaceUp && cardIndex == pile.size - 1) {
                        val currentState = _boardState.value
                        historyStack.add(currentState)
                        _activeHint.value = null
                        val newTableau = currentState.tableauPiles.mapIndexed { idx, p ->
                            if (idx == source.pileIndex) {
                                p.toMutableList().apply {
                                    if (isNotEmpty()) this[this.size - 1] = this[this.size - 1].copy(isFaceUp = true)
                                }
                            } else {
                                p
                            }
                        }
                        val scoreBonus = if (currentState.scoringMode == ScoringMode.STANDARD) 5 else 0
                        val updatedState = currentState.copy(
                            tableauPiles = newTableau,
                            moves = currentState.moves + 1,
                            score = currentState.score + scoreBonus
                        )
                        _boardState.value = updatedState
                        prefsRepo?.saveActiveGame(updatedState, _secondsElapsed.value)
                        evaluateBoardDeadlock()
                    } else if (card.isFaceUp) {
                        autoMoveCard(card, source)
                    }
                }
            }
            else -> {}
        }
    }

    /**
     * Computes and highlights the highest-value strategic move currently available on the board.
     */
    fun requestHint() {
        val state = _boardState.value
        if (state.isWon) return
        AnalyticsManager.logHintUsed()

        // 1. Check if top Waste card can move to Foundation
        val wasteTop = state.wastePile.lastOrNull()
        if (wasteTop != null) {
            val foundation = state.foundationPiles[wasteTop.suit] ?: emptyList()
            if (canMoveToFoundation(wasteTop, foundation)) {
                _activeHint.value = HintMove(
                    card = wasteTop,
                    messageEs = "Sube ${wasteTop.rank.symbol}${wasteTop.suit.symbol} del Descarte a su Fundación",
                    sourceCardId = wasteTop.id,
                    targetSuit = wasteTop.suit
                )
                return
            }
        }

        // 2. Check if any Tableau top card can move to Foundation
        for (colIdx in state.tableauPiles.indices) {
            val topCard = state.tableauPiles[colIdx].lastOrNull() ?: continue
            if (!topCard.isFaceUp) continue
            val foundation = state.foundationPiles[topCard.suit] ?: emptyList()
            if (canMoveToFoundation(topCard, foundation)) {
                _activeHint.value = HintMove(
                    card = topCard,
                    messageEs = "Sube ${topCard.rank.symbol}${topCard.suit.symbol} (Columna ${colIdx + 1}) a su Fundación",
                    sourceCardId = topCard.id,
                    targetSuit = topCard.suit
                )
                return
            }
        }

        // 3. Check Tableau moves that reveal a face-down card or free up a column for a waiting King
        val hasKingWaiting = (wasteTop?.rank == Rank.KING) || state.tableauPiles.any { pile ->
            pile.firstOrNull()?.isFaceUp == false && pile.any { it.isFaceUp && it.rank == Rank.KING }
        }

        for (srcIdx in state.tableauPiles.indices) {
            val pile = state.tableauPiles[srcIdx]
            val firstFaceUpIdx = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx == -1) continue

            val leadingCard = pile[firstFaceUpIdx]
            val revealsHiddenCard = firstFaceUpIdx > 0
            val freesEmptyColumnForKing = firstFaceUpIdx == 0 && leadingCard.rank != Rank.KING && hasKingWaiting

            if (revealsHiddenCard || freesEmptyColumnForKing) {
                for (dstIdx in state.tableauPiles.indices) {
                    if (srcIdx == dstIdx) continue
                    val targetTop = state.tableauPiles[dstIdx].lastOrNull()
                    if (canMoveToTableau(leadingCard, targetTop)) {
                        _activeHint.value = HintMove(
                            card = leadingCard,
                            messageEs = "Mueve ${leadingCard.rank.symbol}${leadingCard.suit.symbol} de la Columna ${srcIdx + 1} a la Columna ${dstIdx + 1}",
                            sourceCardId = leadingCard.id,
                            targetPileIndex = dstIdx
                        )
                        return
                    }
                }
            }
        }

        // 4. Check if top Waste card can move to any Tableau column
        if (wasteTop != null) {
            for (dstIdx in state.tableauPiles.indices) {
                val targetTop = state.tableauPiles[dstIdx].lastOrNull()
                if (canMoveToTableau(wasteTop, targetTop)) {
                    _activeHint.value = HintMove(
                        card = wasteTop,
                        messageEs = "Coloca ${wasteTop.rank.symbol}${wasteTop.suit.symbol} del Descarte en la Columna ${dstIdx + 1}",
                        sourceCardId = wasteTop.id,
                        targetPileIndex = dstIdx
                    )
                    return
                }
            }
        }

        // 5. If stock or waste has cards, suggest drawing from the stock
        if (state.stockPile.isNotEmpty() || state.wastePile.isNotEmpty()) {
            val dummyCard = state.stockPile.lastOrNull() ?: state.wastePile.last()
            _activeHint.value = HintMove(
                card = dummyCard,
                messageEs = "Toca el Mazo superior izquierdo para robar nuevas cartas",
                isStockTap = true
            )
            return
        }

        _isNoMoreMoves.value = true
    }

    /**
     * Automatically cascades all remaining cards into the 4 foundations when `boardState.canAutoComplete` is true.
     */
    fun autoCompleteGame() {
        if (!_boardState.value.canAutoComplete || _isAutoCompleting.value) return
        _isAutoCompleting.value = true
        AnalyticsManager.logAutoCompleteUsed()

        autoCompleteJob?.cancel()
        autoCompleteJob = viewModelScope.launch {
            while (!_boardState.value.isWon) {
                val state = _boardState.value
                var movedInStep = false

                // Find lowest-rank top card across tableau piles that can move to its foundation
                val candidates = mutableListOf<Card>()
                state.wastePile.lastOrNull()?.let { candidates.add(it) }
                for (pile in state.tableauPiles) {
                    pile.lastOrNull()?.let { candidates.add(it) }
                }

                val sortedCandidates = candidates.sortedBy { it.rank.value }
                for (candidate in sortedCandidates) {
                    val foundation = state.foundationPiles[candidate.suit] ?: emptyList()
                    if (canMoveToFoundation(candidate, foundation)) {
                        moveCardToFoundation(candidate, candidate.suit)
                        movedInStep = true
                        break
                    }
                }

                if (!movedInStep) {
                    _isAutoCompleting.value = false
                    break
                }
                delay(75L)
            }
            _isAutoCompleting.value = false
        }
    }

    /**
     * Evaluates whether any productive move remains on the board or inside the stock/waste cycle.
     */
    private fun evaluateBoardDeadlock() {
        val state = _boardState.value
        if (state.isWon || state.canAutoComplete) {
            _isNoMoreMoves.value = false
            return
        }

        // Check if any face-down top card can be flipped
        if (state.tableauPiles.any { it.isNotEmpty() && !it.last().isFaceUp }) {
            _isNoMoreMoves.value = false
            return
        }

        // Check if any tableau leading face-up card can move to foundation or to another tableau pile usefully
        for (srcIdx in state.tableauPiles.indices) {
            val pile = state.tableauPiles[srcIdx]
            if (pile.isEmpty()) continue
            val topCard = pile.last()
            val foundation = state.foundationPiles[topCard.suit] ?: emptyList()
            if (canMoveToFoundation(topCard, foundation)) {
                _isNoMoreMoves.value = false
                return
            }

            val firstFaceUpIdx = pile.indexOfFirst { it.isFaceUp }
            if (firstFaceUpIdx != -1) {
                val leadingCard = pile[firstFaceUpIdx]
                for (dstIdx in state.tableauPiles.indices) {
                    if (srcIdx == dstIdx) continue
                    val dstPile = state.tableauPiles[dstIdx]
                    val dstTop = dstPile.lastOrNull()
                    // Avoid counting King from empty base to empty base as a useful move
                    if (dstTop == null && firstFaceUpIdx == 0 && leadingCard.rank == Rank.KING) continue
                    if (canMoveToTableau(leadingCard, dstTop)) {
                        // Useful if it reveals a card or allows another card in pile to be exposed
                        if (firstFaceUpIdx > 0 || dstTop != null) {
                            _isNoMoreMoves.value = false
                            return
                        }
                    }
                }
            }
        }

        // Check if any card in stock + waste could potentially be played to foundation or tableau
        val allStockAndWaste = state.stockPile + state.wastePile
        for (card in allStockAndWaste) {
            val foundation = state.foundationPiles[card.suit] ?: emptyList()
            if (canMoveToFoundation(card, foundation)) {
                _isNoMoreMoves.value = false
                return
            }
            for (dstPile in state.tableauPiles) {
                if (canMoveToTableau(card, dstPile.lastOrNull())) {
                    _isNoMoreMoves.value = false
                    return
                }
            }
        }

        _isNoMoreMoves.value = true
    }

    /**
     * Undoes the last move.
     */
    fun undo() {
        if (_boardState.value.isWon) return
        if (historyStack.isNotEmpty()) {
            val previousState = historyStack.removeAt(historyStack.size - 1)
            usedUndoInCurrentGame = true
            _activeHint.value = null
            _isNoMoreMoves.value = false
            _boardState.value = previousState
            prefsRepo?.saveActiveGame(previousState, _secondsElapsed.value)
            AnalyticsManager.logUndoUsed()
        }
    }

    // Player customization & settings actions
    fun selectTableTheme(theme: TableTheme) {
        if (_playerStats.value.level >= theme.requiredLevel) {
            val updated = _playerStats.value.copy(selectedTableTheme = theme)
            _playerStats.value = updated
            prefsRepo?.savePlayerStats(updated)
        }
    }

    fun selectCardBackTheme(cardBack: CardBackTheme) {
        if (_playerStats.value.level >= cardBack.requiredLevel) {
            val updated = _playerStats.value.copy(selectedCardBack = cardBack)
            _playerStats.value = updated
            prefsRepo?.savePlayerStats(updated)
        }
    }

    fun updateGameSettings(
        drawCount: Int = _playerStats.value.preferredDrawCount,
        scoringMode: ScoringMode = _playerStats.value.preferredScoringMode,
        leftHandedMode: Boolean = _playerStats.value.leftHandedMode,
        winningDealsOnly: Boolean = _playerStats.value.winningDealsOnly,
        hapticsEnabled: Boolean = _playerStats.value.hapticsEnabled,
        autoCompleteEnabled: Boolean = _playerStats.value.autoCompleteEnabled,
        startNewDealImmediately: Boolean = false
    ) {
        val updated = _playerStats.value.copy(
            preferredDrawCount = drawCount.coerceIn(1, 3),
            preferredScoringMode = scoringMode,
            leftHandedMode = leftHandedMode,
            winningDealsOnly = winningDealsOnly,
            hapticsEnabled = hapticsEnabled,
            autoCompleteEnabled = autoCompleteEnabled
        )
        _playerStats.value = updated
        prefsRepo?.savePlayerStats(updated)

        if (startNewDealImmediately) {
            startNewGame(
                winningDeal = winningDealsOnly,
                drawCount = drawCount,
                scoringMode = scoringMode
            )
        }
    }

    fun initializeAdMob(context: Context) {
        attachContext(context)
        try {
            MobileAds.initialize(context) { _ -> }
        } catch (_: Exception) {}
    }

    companion object {
        fun getTodayIsoDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getYesterdayIsoDate(): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(cal.time)
        }
    }
}
