package com.HKemtrentainment.solitaro.model

data class BoardState(
    val stockPile: List<Card> = emptyList(),
    val wastePile: List<Card> = emptyList(),
    val foundationPiles: Map<Suit, List<Card>> = Suit.values().associateWith { emptyList() },
    val tableauPiles: List<List<Card>> = emptyList(),
    val score: Int = 0,
    val moves: Int = 0,
    val isWon: Boolean = false
) {
    /**
     * FUTURE INTERACTION HOOKS (Drag-and-Drop & Touch):
     * 
     * 1. Stock Pile Interaction:
     *    - Hook up `onStockClick()` in GameViewModel / UI to draw cards from `stockPile` to `wastePile`.
     * 
     * 2. Drag & Drop Gestures:
     *    - Hook up `pointerInput(Unit) { detectDragGestures(...) }` or Compose Draggable modifiers on card composables.
     *    - When dragging starts (`onDragStart`), track dragged card(s) (either single waste card or tableau subsequence of face-up cards).
     * 
     * 3. Drop Target Validation:
     *    - Tableau Drop: Validate that dropped card(s) can be placed on a target tableau pile (must be alternating color and rank == target.rank - 1, or King on an empty tableau).
     *    - Foundation Drop: Validate that a single dropped card can be placed on a target foundation pile (must be same suit and rank == topCard.rank + 1, or Ace on empty foundation).
     * 
     * 4. Double Tap / Auto-Move:
     *    - Hook up `combinedClickable(onTap = { ... }, onDoubleClick = { autoMoveToFoundation() })` for quick card movement.
     */
}
