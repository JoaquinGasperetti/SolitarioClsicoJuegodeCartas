# Guía de Optimización ASO y Android Vitals para Google Play Store
**Proyecto:** Solitario Clásico: Cartas (`com.HKemtrentainment.solitaro`)

Esta guía contiene la ficha lista para copiar y pegar en [Google Play Console](https://play.google.com/console), estructurada según los factores de posicionamiento del algoritmo de Google Play ([Android Vitals](https://developer.android.com/google/play/vitals), [Engage and Retain Users](https://google.play/business/guides/engage-and-retain-your-users/) y [Google Play In-App Reviews API](https://developer.android.com/guide/playcore/in-app-review)).

---

## 1. Metadatos de Ficha de Tienda (Español - `es-419` / `es-ES`)

### Título de la aplicación (Máx. 30 caracteres — Campo de mayor peso semántico)
`Solitario Clásico: Cartas` *(25 caracteres)*

### Descripción breve (Máx. 80 caracteres)
`Solitario Klondike sin internet: retos diarios, rachas, cartas grandes y temas.` *(79 caracteres)*

### Descripción completa (Optimizada para NLP semántico de Google Play, sin keyword stuffing)
Disfruta del auténtico **Solitario Clásico (Klondike)** diseñado para ofrecer partidas fluidas, cartas de alta legibilidad y desafíos nuevos cada día, con o sin conexión a internet.

Tanto si buscas relajar la mente durante unos minutos como si quieres poner a prueba tu estrategia en el modo **Casino Vegas** o en el **Robo de 3 cartas**, esta edición combina las reglas tradicionales del juego de cartas de paciencia con un sistema de progresión moderno.

**CARACTERÍSTICAS PRINCIPALES:**
• **Reglas Oficiales de Klondike:** Elige entre **Robar 1 carta** (relajado) o **Robar 3 cartas** (desafío estratégico clásico).
• **Desafíos Diarios y Rachas con Escudo:** Supera un reto único garantizado cada día en el calendario, mantén tu racha diaria encendida y gana escudos protectores de racha.
• **Manos Ganadoras Garantizadas:** Activa la opción de repartos resolubles para jugar manos donde siempre existe una solución estratégica.
• **Cartas Grandes y Alta Visibilidad:** Índices extra claros y figuras reales estilizadas que facilitan la lectura en cualquier tamaño de pantalla.
• **Subida de Nivel y Temas VIP Desbloqueables:** Gana puntos de experiencia (XP) y desbloquea 10 medallas de logros para personalizar tu mesa con tapetes (*Casino Esmeralda*, *Zafiro Real*, *Terciopelo Carmesí*, *Modo Oscuro OLED Ahorro de Batería* y *Palacio Imperial*) y dorsos de cartas exclusivos.
• **Ayudas Inteligentes sin Fricción:**
  - **Pistas Inteligentes:** Sugerencias estratégicas que priorizan destapar cartas ocultas.
  - **Auto-Completar en Cascada:** Resuelve automáticamente la partida con un solo toque cuando todas las cartas están descubiertas.
  - **Deshacer Ilimitado y Guardado Automático:** Continúa tu partida exactamente donde la dejaste en cualquier momento.
  - **Modo Mano Izquierda (Zurdo):** Invierte el mazo y las fundaciones para jugar cómodamente con una sola mano.
• **Puntuación Clásica y Modo Casino Vegas:** Acumula tu banca histórica en dólares virtuales o supera tu récord de tiempo y movimientos.

---

## 2. Metadatos de Ficha de Tienda (Inglés - `en-US`)

### App Title (Max 30 chars)
`Classic Solitaire: Card Game` *(28 chars)*

### Short Description (Max 80 chars)
`Offline Klondike Solitaire with Daily Challenges, Streaks, Big Cards & Themes!` *(78 chars)*

---

## 3. Etiquetas (Tags) Oficiales en Google Play Console
Selecciona exactamente estas 5 etiquetas en **Configuración de la tienda > Categorización**:
1. **Categoría:** Juegos > Cartas (`GAME_CARD`)
2. **Etiquetas:**
   - `Solitario` (Solitaire)
   - `Juegos de cartas` (Card)
   - `Casual`
   - `Sin conexión` (Offline)
   - `Un jugador` (Single player)

---

## 4. Checklist Técnico de Android Vitals Implementado en el Repositorio
| Métrica del Algoritmo de Google Play | Umbral Crítico Oficial | Optimización Aplicada en el Código |
| :--- | :--- | :--- |
| **User-perceived ANR Rate** | `< 0.47%` ([Android Vitals](https://developer.android.com/google/play/vitals)) | Inicialización de `MobileAds` movida a `Dispatchers.IO` en `SolitaireApplication.kt` y eliminada la llamada en cada recomposición dentro de `MainActivity.kt`. |
| **User-perceived Crash Rate** | `< 1.09%` ([Android Vitals](https://developer.android.com/google/play/vitals)) | Bloques seguros en persistencia JSON, AdMob y Firebase Analytics; validación de índices en listas de cartas. |
| **Tamaño de Descarga / Conversión** | Reducción de ~15 MB | Eliminadas 15 dependencias fantasma (`androidx.camera`, `play-services-location`, `retrofit`, `okhttp`, `room`, `moshi`, `coil`) en `app/build.gradle.kts`. |
| **Tasa de Desinstalación a 7 Días (D7 Uninstall)** | Factor negativo #1 ([Vmobify](https://vmobify.com/blog/google-play-algorithm-2026)) | Implementado `showInterstitialIfEligible` en `AdMobInterstitialHelper.kt` con enfriamiento mínimo de 120s y mínimo de 12 movimientos jugados para no penalizar reinicios rápidos. |
| **Valoración Media y Velocidad de Reseñas** | `>= 4.0★` ([ApsteQ](https://apsteq.com/blog/app-store-optimization-google-play/)) | Integrado `InAppReviewHelper.kt` con la [Google Play In-App Review API](https://developer.android.com/guide/playcore/in-app-review) que solicita valoración tras la 2.ª, 6.ª y 15.ª victoria o al lograr una racha de 3 días. |
| **Retención D1 / D7 / D30** | Top Quartile Category Benchmark | Añadidos Desafíos Diarios por semilla de fecha, Racha Diaria con Escudo Protector (`Streak Freeze`), Niveles/XP, 10 Logros, 5 Tapetes, 5 Dorsos y Guardado Automático de partida. |
