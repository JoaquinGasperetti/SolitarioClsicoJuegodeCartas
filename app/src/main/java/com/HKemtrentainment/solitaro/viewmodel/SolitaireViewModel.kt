package com.HKemtrentainment.solitaro.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HKemtrentainment.solitaro.analytics.AnalyticsManager
import com.HKemtrentainment.solitaro.model.BoardState
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.Deck
import com.HKemtrentainment.solitaro.model.Suit
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val _boardState = MutableStateFlow(deck.createInitialBoard())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _secondsElapsed = MutableStateFlow(0L)
    val secondsElapsed: StateFlow<Long> = _secondsElapsed.asStateFlow()

    private var timerJob: Job? = null
    private val historyStack = mutableListOf<BoardState>()

    init {
        initializeGame()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _secondsElapsed.value = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!_boardState.value.isWon) {
                    _secondsElapsed.value += 1L
                }
            }
        }
    }

    /**
     * Initializes a new game, shuffling the deck and resetting history.
     */
    fun initializeGame() {
        val newState = deck.createInitialBoard()
        historyStack.clear()
        _boardState.value = newState
        startTimer()
        AnalyticsManager.logGameStart()
    }

    fun startNewGame() {
        initializeGame()
    }

    /**
     * Checks whether a card can be moved to a target tableau pile card (or empty pile).
     */
    fun canMoveToTableau(card: Card, targetCard: Card?): Boolean {
        if (targetCard == null) {
            return card.rank.value == 13 // King required for empty column
        }
        return card.color != targetCard.color && targetCard.rank.value == card.rank.value + 1
    }

    /**
     * Checks whether a card can be moved to a foundation pile.
     */
    fun canMoveToFoundation(card: Card, foundationPile: List<Card>): Boolean {
        val topCard = foundationPile.lastOrNull()
        if (topCard == null) {
            return card.rank.value == 1 // Ace required for empty foundation
        }
        return card.suit == topCard.suit && card.rank.value == topCard.rank.value + 1
    }

    /**
     * Taps stock pile to draw cards to waste pile, or recycles waste back to stock.
     */
    fun onStockTapped() {
        val currentState = _boardState.value
        if (currentState.isWon) return
        historyStack.add(currentState)

        val stock = currentState.stockPile.toMutableList()
        val waste = currentState.wastePile.toMutableList()

        if (stock.isNotEmpty()) {
            val card = stock.removeAt(stock.size - 1)
            waste.add(card.copy(isFaceUp = true))
        } else if (waste.isNotEmpty()) {
            stock.addAll(waste.reversed().map { it.copy(isFaceUp = false) })
            waste.clear()
        }

        _boardState.value = currentState.copy(
            stockPile = stock,
            wastePile = waste,
            moves = currentState.moves + 1
        )
    }

    /**
     * Moves a card (or subsequence) from waste or tableau to a target tableau column index.
     */
    fun moveCardToTableau(card: Card, targetColumnIndex: Int) {
        val currentState = _boardState.value
        if (currentState.isWon) return
        val targetPile = currentState.tableauPiles.getOrNull(targetColumnIndex) ?: return
        val targetCard = targetPile.lastOrNull()

        historyStack.add(currentState)

        val newWaste = currentState.wastePile.toMutableList()
        val newTableau = currentState.tableauPiles.map { it.toMutableList() }.toMutableList()

        val movingCards: List<Card>
        val isWasteMove: Boolean

        // Check if card is in waste pile
        if (newWaste.isNotEmpty() && newWaste.last() == card) {
            isWasteMove = true
            movingCards = listOf(card)
        } else {
            isWasteMove = false
            var foundSourcePileIndex = -1
            var cardIndexInPile = -1
            for (i in newTableau.indices) {
                val idx = newTableau[i].indexOfFirst { it.id == card.id }
                if (idx != -1) {
                    foundSourcePileIndex = i
                    cardIndexInPile = idx
                    break
                }
            }

            if (foundSourcePileIndex != -1 && cardIndexInPile != -1) {
                val sourcePile = newTableau[foundSourcePileIndex]
                val subList = sourcePile.subList(cardIndexInPile, sourcePile.size)
                if (subList.all { it.isFaceUp }) {
                    movingCards = subList.toList()
                } else {
                    historyStack.removeAt(historyStack.size - 1)
                    return
                }
            } else {
                historyStack.removeAt(historyStack.size - 1)
                return
            }
        }

        // Validate using the first (bottom) card of the dragged stack against the destination's top card
        if (!canMoveToTableau(movingCards.first(), targetCard)) {
            historyStack.removeAt(historyStack.size - 1)
            return
        }

        if (isWasteMove) {
            newWaste.removeAt(newWaste.size - 1)
            newTableau[targetColumnIndex].addAll(movingCards.map { it.copy(isFaceUp = true) })
        } else {
            var foundSourcePileIndex = -1
            var cardIndexInPile = -1
            for (i in newTableau.indices) {
                val idx = newTableau[i].indexOfFirst { it.id == card.id }
                if (idx != -1) {
                    foundSourcePileIndex = i
                    cardIndexInPile = idx
                    break
                }
            }
            if (foundSourcePileIndex != -1 && cardIndexInPile != -1) {
                val sourcePile = newTableau[foundSourcePileIndex]
                repeat(movingCards.size) { sourcePile.removeAt(sourcePile.size - 1) }
                newTableau[targetColumnIndex].addAll(movingCards)

                if (sourcePile.isNotEmpty() && !sourcePile.last().isFaceUp) {
                    sourcePile[sourcePile.size - 1] = sourcePile.last().copy(isFaceUp = true)
                }
            }
        }

        _boardState.value = currentState.copy(
            wastePile = newWaste,
            tableauPiles = newTableau.map { it.toList() },
            moves = currentState.moves + 1,
            score = currentState.score + 5
        )
    }

    /**
     * Checks if the game has been won.
     * A win is achieved when all 4 foundation piles have exactly 13 cards each.
     */
    private fun checkWinCondition(foundationPiles: Map<Suit, List<Card>>): Boolean {
        return foundationPiles.values.all { it.size == 13 }
    }

    /**
     * Moves a card from waste or tableau to its foundation pile.
     */
    fun moveCardToFoundation(card: Card, suit: Suit) {
        val currentState = _boardState.value
        if (currentState.isWon) return
        val foundationPile = currentState.foundationPiles[suit] ?: emptyList()

        if (!canMoveToFoundation(card, foundationPile)) return

        historyStack.add(currentState)

        val newWaste = currentState.wastePile.toMutableList()
        val newTableau = currentState.tableauPiles.map { it.toMutableList() }.toMutableList()
        val newFoundations = currentState.foundationPiles.toMutableMap()

        var moved = false
        if (newWaste.isNotEmpty() && newWaste.last() == card) {
            newWaste.removeAt(newWaste.size - 1)
            moved = true
        } else {
            for (pile in newTableau) {
                if (pile.isNotEmpty() && pile.last() == card) {
                    pile.removeAt(pile.size - 1)
                    if (pile.isNotEmpty() && !pile.last().isFaceUp) {
                        pile[pile.size - 1] = pile.last().copy(isFaceUp = true)
                    }
                    moved = true
                    break
                }
            }
        }

        if (moved) {
            newFoundations[suit] = foundationPile + card.copy(isFaceUp = true)
            val isWon = checkWinCondition(newFoundations)

            if (isWon) {
                timerJob?.cancel()
                AnalyticsManager.logGameWon(seconds = _secondsElapsed.value, moves = currentState.moves + 1)
            }

            _boardState.value = currentState.copy(
                wastePile = newWaste,
                tableauPiles = newTableau.map { it.toList() },
                foundationPiles = newFoundations,
                moves = currentState.moves + 1,
                score = currentState.score + 10,
                isWon = isWon
            )
        } else {
            historyStack.removeAt(historyStack.size - 1)
        }
    }



    /**
     * Finds the valid auto-move destination for a card if any.
     */
    fun findAutoMoveDestination(card: Card, source: CardSource): AutoMoveDestination? {
        if (_boardState.value.isWon) return null
        val currentState = _boardState.value

        val isSingleCard = when (source) {
            is CardSource.Waste -> true
            is CardSource.Tableau -> {
                val pile = currentState.tableauPiles[source.pileIndex]
                pile.lastOrNull() == card
            }
            else -> false
        }

        // 1. Try Foundation (only if single card)
        if (isSingleCard) {
            val foundationPile = currentState.foundationPiles[card.suit] ?: emptyList()
            if (canMoveToFoundation(card, foundationPile)) {
                return AutoMoveDestination.Foundation(card.suit)
            }
        }

        // 2. Try Tableau piles
        for (i in currentState.tableauPiles.indices) {
            if (source is CardSource.Tableau && source.pileIndex == i) continue
            val targetPile = currentState.tableauPiles[i]
            val targetCard = targetPile.lastOrNull()
            if (canMoveToTableau(card, targetCard)) {
                return AutoMoveDestination.Tableau(i)
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
     * Handles card tap for auto-move (Tap-to-Move) or flipping face-down cards.
     */
    fun onCardTapped(card: Card, source: CardSource) {
        if (_boardState.value.isWon) return
        when (source) {
            is CardSource.Waste -> {
                autoMoveCard(card, source)
            }
            is CardSource.Tableau -> {
                val pile = _boardState.value.tableauPiles[source.pileIndex]
                val cardIndex = pile.indexOfFirst { it.id == card.id }
                if (cardIndex != -1) {
                    if (!card.isFaceUp && cardIndex == pile.size - 1) {
                        val currentState = _boardState.value
                        historyStack.add(currentState)
                        val newTableau = currentState.tableauPiles.mapIndexed { idx, p ->
                            if (idx == source.pileIndex) {
                                p.toMutableList().apply {
                                    if (isNotEmpty()) this[this.size - 1] = this[this.size - 1].copy(isFaceUp = true)
                                }
                            } else {
                                p
                            }
                        }
                        _boardState.value = currentState.copy(tableauPiles = newTableau, moves = currentState.moves + 1)
                    } else if (card.isFaceUp) {
                        autoMoveCard(card, source)
                    }
                }
            }
            else -> {}
        }
    }

    /**
     * Undoes the last move.
     */
    fun undo() {
        if (_boardState.value.isWon) return
        if (historyStack.isNotEmpty()) {
            val previousState = historyStack.removeAt(historyStack.size - 1)
            _boardState.value = previousState
            AnalyticsManager.logUndoUsed()
        }
    }

    fun initializeAdMob(context: Context) {
        try {
            MobileAds.initialize(context) { _ -> }
        } catch (_: Exception) {}
    }
}
