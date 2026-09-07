package com.luis.alhendinfc.domain.model

/**
 * Resuelve etiquetas de evento en español para UI y PDF.
 * Evita mostrar códigos tipo SAMPLE_SHOT_ON_TARGET.
 */
object EventLabels {

    private val sampleFallback: Map<String, String> by lazy {
        SampleCustomStats.createTypes(0).associate { it.code to it.label }
    }

    fun resolve(event: MatchEvent, customLabels: Map<String, String> = emptyMap()): String =
        event.type?.label
            ?: customLabels[event.typeCode]
            ?: sampleFallback[event.typeCode]
            ?: event.typeCode

    /**
     * Minuto de partido continuo (1ª parte 0–44, 2ª 45–…),
     * a partir del minuto relativo a la parte guardado en el evento.
     */
    fun displayMinute(event: MatchEvent, durationPerPart: Int): Int {
        val partLen = durationPerPart.coerceAtLeast(1)
        val periodIndex = (event.period - 1).coerceAtLeast(0)
        return periodIndex * partLen + event.minute.coerceAtLeast(0)
    }
}
