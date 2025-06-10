package com.example.pomodorotimer // <<<<< VERIFIQUE SE O NOME DO SEU PACOTE ESTÁ CORRETO AQUI

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit // Importe TimeUnit

// Classe de testes unitários para funções utilitárias do timer
class TimerUtilsTest {

    // Função auxiliar para testar formatTime, pois ela é privada em MainActivity
    private fun formatTimeHelper(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    @Test
    fun formatTime_handlesZeroSeconds() {
        assertEquals("00:00", formatTimeHelper(0L))
    }

    @Test
    fun formatTime_handlesSingleDigitSeconds() {
        assertEquals("00:05", formatTimeHelper(5 * 1000L))
    }

    @Test
    fun formatTime_handlesDoubleDigitSeconds() {
        assertEquals("00:15", formatTimeHelper(15 * 1000L))
    }

    @Test
    fun formatTime_handlesFullMinutes() {
        assertEquals("01:00", formatTimeHelper(60 * 1000L))
        assertEquals("25:00", formatTimeHelper(25 * 60 * 1000L))
    }

    @Test
    fun formatTime_handlesMinutesAndSeconds() {
        assertEquals("01:30", formatTimeHelper(90 * 1000L))
        assertEquals("05:07", formatTimeHelper(5 * 60 * 1000L + 7 * 1000L))
    }
}