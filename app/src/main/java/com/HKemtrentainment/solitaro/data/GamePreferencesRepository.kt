package com.HKemtrentainment.solitaro.data

import android.content.Context
import android.content.SharedPreferences
import com.HKemtrentainment.solitaro.model.BoardState
import com.HKemtrentainment.solitaro.model.Card
import com.HKemtrentainment.solitaro.model.CardBackTheme
import com.HKemtrentainment.solitaro.model.PlayerStats
import com.HKemtrentainment.solitaro.model.Rank
import com.HKemtrentainment.solitaro.model.ScoringMode
import com.HKemtrentainment.solitaro.model.Suit
import com.HKemtrentainment.solitaro.model.TableTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight, zero-overhead persistence manager backed by SharedPreferences and org.json
 * to ensure instant startup (optimal Android Vitals cold start time) and zero ANRs.
 */
class GamePreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPlayerStats(): PlayerStats {
        val completedDates = prefs.getStringSet(KEY_COMPLETED_DAILIES, emptySet())?.toSet() ?: emptySet()
        val achievements = prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet())?.toSet() ?: emptySet()
        val scoringName = prefs.getString(KEY_PREFERRED_SCORING, ScoringMode.STANDARD.name) ?: ScoringMode.STANDARD.name
        val scoringMode = try {
            ScoringMode.valueOf(scoringName)
        } catch (_: Exception) {
            ScoringMode.STANDARD
        }

        return PlayerStats(
            gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0),
            gamesWon = prefs.getInt(KEY_GAMES_WON, 0),
            bestScore = prefs.getInt(KEY_BEST_SCORE, 0),
            bestTimeSeconds = prefs.getLong(KEY_BEST_TIME, 0L),
            fewestMoves = prefs.getInt(KEY_FEWEST_MOVES, 0),
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            streakFreezesAvailable = prefs.getInt(KEY_STREAK_FREEZES, 1),
            lastWinDateIso = prefs.getString(KEY_LAST_WIN_DATE, "") ?: "",
            completedDailyDates = completedDates,
            xp = prefs.getInt(KEY_XP, 0),
            vegasBankroll = prefs.getInt(KEY_VEGAS_BANKROLL, 0),
            unlockedAchievements = achievements,
            selectedTableTheme = TableTheme.fromId(
                prefs.getString(KEY_TABLE_THEME, TableTheme.CLASSIC_EMERALD.id) ?: TableTheme.CLASSIC_EMERALD.id
            ),
            selectedCardBack = CardBackTheme.fromId(
                prefs.getString(KEY_CARD_BACK, CardBackTheme.ROYAL_BLUE.id) ?: CardBackTheme.ROYAL_BLUE.id
            ),
            preferredDrawCount = prefs.getInt(KEY_PREFERRED_DRAW_COUNT, 1).coerceIn(1, 3),
            preferredScoringMode = scoringMode,
            leftHandedMode = prefs.getBoolean(KEY_LEFT_HANDED, false),
            winningDealsOnly = prefs.getBoolean(KEY_WINNING_DEALS_ONLY, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS_ENABLED, true),
            autoCompleteEnabled = prefs.getBoolean(KEY_AUTO_COMPLETE_ENABLED, true)
        )
    }

    fun savePlayerStats(stats: PlayerStats) {
        prefs.edit().apply {
            putInt(KEY_GAMES_PLAYED, stats.gamesPlayed)
            putInt(KEY_GAMES_WON, stats.gamesWon)
            putInt(KEY_BEST_SCORE, stats.bestScore)
            putLong(KEY_BEST_TIME, stats.bestTimeSeconds)
            putInt(KEY_FEWEST_MOVES, stats.fewestMoves)
            putInt(KEY_CURRENT_STREAK, stats.currentStreak)
            putInt(KEY_BEST_STREAK, stats.bestStreak)
            putInt(KEY_STREAK_FREEZES, stats.streakFreezesAvailable)
            putString(KEY_LAST_WIN_DATE, stats.lastWinDateIso)
            putStringSet(KEY_COMPLETED_DAILIES, stats.completedDailyDates)
            putInt(KEY_XP, stats.xp)
            putInt(KEY_VEGAS_BANKROLL, stats.vegasBankroll)
            putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, stats.unlockedAchievements)
            putString(KEY_TABLE_THEME, stats.selectedTableTheme.id)
            putString(KEY_CARD_BACK, stats.selectedCardBack.id)
            putInt(KEY_PREFERRED_DRAW_COUNT, stats.preferredDrawCount)
            putString(KEY_PREFERRED_SCORING, stats.preferredScoringMode.name)
            putBoolean(KEY_LEFT_HANDED, stats.leftHandedMode)
            putBoolean(KEY_WINNING_DEALS_ONLY, stats.winningDealsOnly)
            putBoolean(KEY_HAPTICS_ENABLED, stats.hapticsEnabled)
            putBoolean(KEY_AUTO_COMPLETE_ENABLED, stats.autoCompleteEnabled)
            apply()
        }
    }

    fun saveActiveGame(state: BoardState, secondsElapsed: Long) {
        if (state.isWon) {
            clearSavedGame()
            return
        }
        try {
            val root = JSONObject().apply {
                put("score", state.score)
                put("moves", state.moves)
                put("drawCount", state.drawCount)
                put("scoringMode", state.scoringMode.name)
                put("stockRecycleCount", state.stockRecycleCount)
                put("isDailyChallenge", state.isDailyChallenge)
                put("dailyChallengeDate", state.dailyChallengeDate ?: "")
                put("isWinningDeal", state.isWinningDeal)
                put("secondsElapsed", secondsElapsed)
                put("stockPile", serializeCardList(state.stockPile))
                put("wastePile", serializeCardList(state.wastePile))

                val foundationsJson = JSONObject()
                for ((suit, pile) in state.foundationPiles) {
                    foundationsJson.put(suit.name, serializeCardList(pile))
                }
                put("foundationPiles", foundationsJson)

                val tableauArray = JSONArray()
                for (pile in state.tableauPiles) {
                    tableauArray.put(serializeCardList(pile))
                }
                put("tableauPiles", tableauArray)
            }
            prefs.edit().putString(KEY_SAVED_GAME_JSON, root.toString()).apply()
        } catch (_: Exception) {
            // Ignore serialization errors in restricted test environments
        }
    }

    fun loadSavedGame(): Pair<BoardState, Long>? {
        val raw = prefs.getString(KEY_SAVED_GAME_JSON, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val stockPile = deserializeCardList(root.getJSONArray("stockPile"))
            val wastePile = deserializeCardList(root.getJSONArray("wastePile"))

            val foundationsJson = root.getJSONObject("foundationPiles")
            val foundationPiles = Suit.entries.associateWith { suit ->
                if (foundationsJson.has(suit.name)) {
                    deserializeCardList(foundationsJson.getJSONArray(suit.name))
                } else {
                    emptyList()
                }
            }

            val tableauArray = root.getJSONArray("tableauPiles")
            val tableauPiles = mutableListOf<List<Card>>()
            for (i in 0 until tableauArray.length()) {
                tableauPiles.add(deserializeCardList(tableauArray.getJSONArray(i)))
            }

            val dailyDate = root.optString("dailyChallengeDate", "").ifEmpty { null }
            val scoringMode = try {
                ScoringMode.valueOf(root.optString("scoringMode", ScoringMode.STANDARD.name))
            } catch (_: Exception) {
                ScoringMode.STANDARD
            }

            val boardState = BoardState(
                stockPile = stockPile,
                wastePile = wastePile,
                foundationPiles = foundationPiles,
                tableauPiles = tableauPiles,
                score = root.optInt("score", 0),
                moves = root.optInt("moves", 0),
                isWon = false,
                drawCount = root.optInt("drawCount", 1).coerceIn(1, 3),
                scoringMode = scoringMode,
                stockRecycleCount = root.optInt("stockRecycleCount", 0),
                isDailyChallenge = root.optBoolean("isDailyChallenge", false),
                dailyChallengeDate = dailyDate,
                isWinningDeal = root.optBoolean("isWinningDeal", false)
            )
            val seconds = root.optLong("secondsElapsed", 0L)
            boardState to seconds
        } catch (_: Exception) {
            null
        }
    }

    fun clearSavedGame() {
        prefs.edit().remove(KEY_SAVED_GAME_JSON).apply()
    }

    private fun serializeCardList(cards: List<Card>): JSONArray {
        val array = JSONArray()
        for (c in cards) {
            val obj = JSONObject().apply {
                put("s", c.suit.name)
                put("r", c.rank.value)
                put("u", c.isFaceUp)
            }
            array.put(obj)
        }
        return array
    }

    private fun deserializeCardList(array: JSONArray): List<Card> {
        val list = mutableListOf<Card>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val suit = Suit.valueOf(obj.getString("s"))
            val rank = Rank.fromValue(obj.getInt("r"))
            val isFaceUp = obj.getBoolean("u")
            list.add(Card(suit = suit, rank = rank, isFaceUp = isFaceUp))
        }
        return list
    }

    companion object {
        private const val PREFS_NAME = "solitario_clasico_prefs"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_GAMES_WON = "games_won"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_BEST_TIME = "best_time"
        private const val KEY_FEWEST_MOVES = "fewest_moves"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_STREAK_FREEZES = "streak_freezes"
        private const val KEY_LAST_WIN_DATE = "last_win_date"
        private const val KEY_COMPLETED_DAILIES = "completed_dailies"
        private const val KEY_XP = "player_xp"
        private const val KEY_VEGAS_BANKROLL = "vegas_bankroll"
        private const val KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements"
        private const val KEY_TABLE_THEME = "table_theme"
        private const val KEY_CARD_BACK = "card_back"
        private const val KEY_PREFERRED_DRAW_COUNT = "preferred_draw_count"
        private const val KEY_PREFERRED_SCORING = "preferred_scoring"
        private const val KEY_LEFT_HANDED = "left_handed"
        private const val KEY_WINNING_DEALS_ONLY = "winning_deals_only"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_AUTO_COMPLETE_ENABLED = "auto_complete_enabled"
        private const val KEY_SAVED_GAME_JSON = "saved_game_json"
    }
}
