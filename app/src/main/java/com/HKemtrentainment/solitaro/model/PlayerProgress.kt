package com.HKemtrentainment.solitaro.model

enum class TableTheme(
    val id: String,
    val titleEs: String,
    val requiredLevel: Int,
    val feltColorHex: Long,
    val headerColorHex: Long,
    val accentColorHex: Long
) {
    CLASSIC_EMERALD(
        id = "classic_emerald",
        titleEs = "Casino Esmeralda",
        requiredLevel = 1,
        feltColorHex = 0xFF1B4D3E,
        headerColorHex = 0xFF0F3B2E,
        accentColorHex = 0xFFFFD54F
    ),
    ROYAL_SAPPHIRE(
        id = "royal_sapphire",
        titleEs = "Zafiro Real",
        requiredLevel = 2,
        feltColorHex = 0xFF102A43,
        headerColorHex = 0xFF0A1929,
        accentColorHex = 0xFF64B5F6
    ),
    CRIMSON_VELVET(
        id = "crimson_velvet",
        titleEs = "Terciopelo Carmesí",
        requiredLevel = 3,
        feltColorHex = 0xFF4A1525,
        headerColorHex = 0xFF2E0B16,
        accentColorHex = 0xFFFFB74D
    ),
    OBSIDIAN_OLED(
        id = "obsidian_oled",
        titleEs = "Noche OLED (Ahorro Batería)",
        requiredLevel = 4,
        feltColorHex = 0xFF121417,
        headerColorHex = 0xFF08090A,
        accentColorHex = 0xFF81C784
    ),
    GOLDEN_PALACE(
        id = "golden_palace",
        titleEs = "Palacio Imperial VIP",
        requiredLevel = 6,
        feltColorHex = 0xFF2D2318,
        headerColorHex = 0xFF1A140E,
        accentColorHex = 0xFFFFD700
    );

    companion object {
        fun fromId(id: String): TableTheme = entries.firstOrNull { it.id == id } ?: CLASSIC_EMERALD
    }
}

enum class CardBackTheme(
    val id: String,
    val titleEs: String,
    val requiredLevel: Int,
    val primaryColorHex: Long,
    val innerColorHex: Long,
    val centerSymbol: String
) {
    ROYAL_BLUE(
        id = "royal_blue",
        titleEs = "Azul Clásico",
        requiredLevel = 1,
        primaryColorHex = 0xFF1565C0,
        innerColorHex = 0xFF0D47A1,
        centerSymbol = "♠"
    ),
    CRIMSON_CROWN(
        id = "crimson_crown",
        titleEs = "Corona Real",
        requiredLevel = 2,
        primaryColorHex = 0xFFB71C1C,
        innerColorHex = 0xFF7F0000,
        centerSymbol = "♛"
    ),
    EMERALD_LUXE(
        id = "emerald_luxe",
        titleEs = "Trébol Imperial",
        requiredLevel = 3,
        primaryColorHex = 0xFF2E7D32,
        innerColorHex = 0xFF1B5E20,
        centerSymbol = "♣"
    ),
    CYBER_GOLD(
        id = "cyber_gold",
        titleEs = "Oro de Montecarlo",
        requiredLevel = 5,
        primaryColorHex = 0xFF8D6E63,
        innerColorHex = 0xFF4E342E,
        centerSymbol = "♦"
    ),
    MINIMAL_ONYX(
        id = "minimal_onyx",
        titleEs = "Ónix Minimalista",
        requiredLevel = 7,
        primaryColorHex = 0xFF263238,
        innerColorHex = 0xFF11171A,
        centerSymbol = "★"
    );

    companion object {
        fun fromId(id: String): CardBackTheme = entries.firstOrNull { it.id == id } ?: ROYAL_BLUE
    }
}

enum class Achievement(
    val id: String,
    val titleEs: String,
    val descriptionEs: String,
    val xpReward: Int,
    val badgeEmoji: String
) {
    FIRST_WIN(
        id = "first_win",
        titleEs = "Primera Victoria",
        descriptionEs = "Completa tu primera partida de Solitario.",
        xpReward = 100,
        badgeEmoji = "🏆"
    ),
    SPEED_DEMON(
        id = "speed_demon",
        titleEs = "Relámpago del Tapete",
        descriptionEs = "Gana una partida en menos de 180 segundos.",
        xpReward = 150,
        badgeEmoji = "⚡"
    ),
    SHARPSHOOTER(
        id = "sharpshooter",
        titleEs = "Estratega Preciso",
        descriptionEs = "Gana una partida en 90 movimientos o menos.",
        xpReward = 150,
        badgeEmoji = "🎯"
    ),
    DRAW_THREE_MASTER(
        id = "draw_three_master",
        titleEs = "Maestro del Robo 3",
        descriptionEs = "Gana una partida en el modo clásico de Robo de 3 cartas.",
        xpReward = 200,
        badgeEmoji = "🃏"
    ),
    PURE_SKILL(
        id = "pure_skill",
        titleEs = "Sin Marcha Atrás",
        descriptionEs = "Gana una partida sin usar el botón Deshacer ni una sola vez.",
        xpReward = 175,
        badgeEmoji = "💎"
    ),
    DAILY_CHAMPION(
        id = "daily_champion",
        titleEs = "Campeón del Día",
        descriptionEs = "Supera un Desafío Diario del calendario.",
        xpReward = 150,
        badgeEmoji = "📅"
    ),
    STREAK_3(
        id = "streak_3",
        titleEs = "En Racha (3 Días)",
        descriptionEs = "Mantén una racha de victorias durante 3 días seguidos.",
        xpReward = 200,
        badgeEmoji = "🔥"
    ),
    STREAK_7(
        id = "streak_7",
        titleEs = "Imparable (7 Días)",
        descriptionEs = "Alcanza una racha de 7 días consecutivos jugando y ganando.",
        xpReward = 400,
        badgeEmoji = "👑"
    ),
    VEGAS_HIGH_ROLLER(
        id = "vegas_high_roller",
        titleEs = "Magnate de Las Vegas",
        descriptionEs = "Termina una partida en modo Casino Vegas con saldo positivo.",
        xpReward = 250,
        badgeEmoji = "🎰"
    ),
    CENTURION(
        id = "centurion",
        titleEs = "Leyenda del Solitario",
        descriptionEs = "Acumula 10 victorias totales.",
        xpReward = 500,
        badgeEmoji = "🌟"
    )
}

data class HintMove(
    val card: Card,
    val messageEs: String,
    val sourceCardId: String? = null,
    val targetSuit: Suit? = null,
    val targetPileIndex: Int? = null,
    val isStockTap: Boolean = false
)

data class PlayerStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestScore: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val fewestMoves: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakFreezesAvailable: Int = 1,
    val lastWinDateIso: String = "",
    val completedDailyDates: Set<String> = emptySet(),
    val xp: Int = 0,
    val vegasBankroll: Int = 0,
    val unlockedAchievements: Set<String> = emptySet(),
    val selectedTableTheme: TableTheme = TableTheme.CLASSIC_EMERALD,
    val selectedCardBack: CardBackTheme = CardBackTheme.ROYAL_BLUE,
    val preferredDrawCount: Int = 1,
    val preferredScoringMode: ScoringMode = ScoringMode.STANDARD,
    val leftHandedMode: Boolean = false,
    val winningDealsOnly: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoCompleteEnabled: Boolean = true
) {
    /**
     * Every 250 XP advances the player by 1 level (Level 1 starts at 0 XP).
     */
    val level: Int
        get() = (xp / 250) + 1

    val currentLevelXp: Int
        get() = xp % 250

    val xpForNextLevel: Int
        get() = 250

    val xpProgressFraction: Float
        get() = (currentLevelXp.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)

    val winRatePercent: Int
        get() = if (gamesPlayed > 0) ((gamesWon * 100.0) / gamesPlayed).toInt() else 0

    val playerTitleEs: String
        get() = when {
            level >= 10 -> "Gran Maestro del Solitario"
            level >= 7 -> "Leyenda del Casino"
            level >= 5 -> "Estratega Real"
            level >= 3 -> "Crupié Experto"
            level >= 2 -> "Jugador Habitual"
            else -> "Aprendiz de Cartas"
        }
}
