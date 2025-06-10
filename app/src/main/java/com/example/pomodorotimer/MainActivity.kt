package com.example.pomodorotimer

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.os.Handler // Importe Handler para agendar tarefas
import android.os.Looper // Importe Looper para obter o Looper da thread principal
import android.view.View // Importe View para View.VISIBLE/GONE

import com.bumptech.glide.Glide // Importe para a biblioteca Glide
import android.widget.ImageView // Revertido para ImageView padrão

import androidx.core.content.ContextCompat
import android.os.VibratorManager // Importe para API 31+ (Android 12)

class MainActivity : AppCompatActivity() {

    // Declaração das Views (elementos da UI)
    private lateinit var timerDisplay: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var modeIndicator: TextView
    private lateinit var breakMessageDisplay: TextView
    // REVERTIDO: Tipo da variável para ImageView
    private lateinit var animatedImageView: ImageView

    // Variáveis de controle do cronômetro
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0L
    private var isRunning: Boolean = false

    // Tempos padrão do Pomodoro
    // Este `focusTime = 0 * 60 * 1000L` está na sua versão, vou mantê-lo, mas é 0 segundos
    private val focusTime = 0 * 60 * 1000L
    private val breakTime = 1 * 60 * 1000L
    private val longBreakTime = 15 * 60 * 1000L

    private var isFocusMode: Boolean = true // Começa no modo foco
    private var pomodoroCycles: Int = 0

    // VARIÁVEIS E LÓGICA PARA AS MENSAGENS DE PAUSA
    private lateinit var breakMessages: Array<String>
    private var currentMessageIndex = 0
    private val messageHandler = Handler(Looper.getMainLooper())
    private val messageRunnable = object : Runnable {
        override fun run() {
            updateBreakMessage()
            messageHandler.postDelayed(this, 7000L) // Repete a cada 7 segundos
        }
    }

    // Arrays de recursos para imagens/GIFs (ATENÇÃO AOS NOMES DOS ARQUIVOS EM res/drawable/)
    private val focusImages = intArrayOf(
        R.drawable.foco,
        R.drawable.estudando,
        R.drawable.corpo_e_mente,
        R.drawable.xp
    )
    private val breakImages = intArrayOf(
        R.drawable.ouvindo_musica,
        R.drawable.lanchinho,
        R.drawable.beber_agua,
        R.drawable.entretenimento,
        R.drawable.cachorro,
        R.drawable.ar_livre,
        R.drawable.gatinho
    )
    private var currentBreakImageIndex = 0

    // VARIÁVEIS PARA A ROTAÇÃO DE IMAGENS NO MODO FOCO
    private var currentFocusImageIndex = 0
    private val focusImageHandler = Handler(Looper.getMainLooper())
    private val focusImageRunnable = object : Runnable {
        override fun run() {
            updateFocusImage()
            focusImageHandler.postDelayed(this, 10000L) // Tempo de rotação do foco: 10 segundos
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa as Views
        timerDisplay = findViewById(R.id.timerDisplay)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)
        modeIndicator = findViewById(R.id.modeIndicator)
        breakMessageDisplay = findViewById(R.id.breakMessageDisplay)
        // REVERTIDO: Inicializando como ImageView normal
        animatedImageView = findViewById(R.id.animatedImageView)

        // Inicializa o array de mensagens de pausa a partir de strings.xml
        breakMessages = resources.getStringArray(R.array.break_messages)

        // Define o tempo inicial como o tempo de foco e atualiza o display
        timeLeftInMillis = focusTime
        updateCountDownText()
        updateModeIndicator()

        // Inicia a rotação de imagens de foco imediatamente ao iniciar o app,
        // pois o app começa no modo foco por padrão.
        if (isFocusMode) {
            startFocusImagesRotation()
        }

        // Garante que o display de mensagens de pausa esteja limpo e invisível no início.
        breakMessageDisplay.text = ""
        breakMessageDisplay.visibility = View.GONE


        // Configura os ouvintes de clique para os botões
        startButton.setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { pauseTimer() }
        resetButton.setOnClickListener { resetTimer() }
    }

    // Função para iniciar o cronômetro
    private fun startTimer() {
        if (isRunning) return

        isRunning = true
        // Para todas as rotações anteriores para evitar conflitos, mas sem limpar/esconder o conteúdo VISÍVEL.
        stopAllDynamicContentRotationHandlers()


        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                isRunning = false
                countDownTimer = null

                triggerFeedback()

                if (isFocusMode) {
                    // Foco terminou, vai para Pausa
                    pomodoroCycles++
                    timeLeftInMillis = if (pomodoroCycles % 4 == 0) longBreakTime else breakTime
                    isFocusMode = false
                    Toast.makeText(this@MainActivity, getString(R.string.focus_ended_message), Toast.LENGTH_SHORT).show()
                    updateModeIndicator() // "Modo: Pausa"
                    stopFocusImagesRotationOnlyHandlers() // Para a rotação de foco
                    startBreakMessagesAndRotation() // Inicia mensagens e imagens de pausa
                } else {
                    // Pausa terminou, vai para Foco
                    timeLeftInMillis = focusTime
                    isFocusMode = true
                    Toast.makeText(this@MainActivity, getString(R.string.break_ended_message), Toast.LENGTH_SHORT).show()
                    updateModeIndicator() // "Modo: Foco"
                    clearBreakContentViews() // Limpa e esconde o conteúdo de PAUSA
                    startFocusImagesRotation() // Inicia rotação de imagens de foco
                }
                updateCountDownText()
                // Opcional: Para iniciar o próximo timer automaticamente:
                // startTimer()
            }
        }.start()

        // Aciona a rotação de conteúdo baseada no modo atual ao INICIAR (ou RESUMIR) o timer
        if (isFocusMode) {
            startFocusImagesRotation()
        } else {
            startBreakMessagesAndRotation()
        }
    }

    // Função para pausar o cronômetro. CONTEÚDO VISUAL CONTINUA RODANDO.
    private fun pauseTimer() {
        countDownTimer?.cancel() // Para o timer principal (o tempo vai parar)
        isRunning = false
        // Rotação de mensagens e imagens CONTINUA.
    }

    // Função para reiniciar o cronômetro. CONTEÚDO É LIMPO E ESCONDIDO.
    private fun resetTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isFocusMode = true
        timeLeftInMillis = focusTime
        pomodoroCycles = 0
        updateCountDownText()
        updateModeIndicator()

        // Limpa e esconde todo o conteúdo dinâmico ao resetar
        clearAllDynamicContentViews()

        // APÓS O RESET, ESTAMOS NO MODO FOCO, ENTÃO REINICIAMOS A ROTAÇÃO DE IMAGENS DE FOCO
        startFocusImagesRotation()
    }

    // Função para formatar e atualizar o texto do cronômetro (MM:SS)
    private fun updateCountDownText() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftInMillis) -
                TimeUnit.MINUTES.toSeconds(minutes)
        val timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        timerDisplay.text = timeLeftFormatted
    }

    // Função para atualizar o texto do indicador de modo (apenas "Modo: Foco" ou "Modo: Pausa")
    private fun updateModeIndicator() {
        modeIndicator.text = if (isFocusMode) getString(R.string.mode_focus) else getString(R.string.mode_break)
    }

    // Aciona feedback de vibração e som
    private fun triggerFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ContextCompat.getSystemService(this, VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            ContextCompat.getSystemService(this, Vibrator::class.java)
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)) // 1 segundo de vibração
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(1000) // 1 segundo de vibração
            }
        }

        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // FUNÇÕES PARA GERENCIAR A ROTAÇÃO DE MENSAGENS E IMAGENS DE PAUSA
    private fun startBreakMessagesAndRotation() {
        currentMessageIndex = 0
        currentBreakImageIndex = 0

        breakMessageDisplay.visibility = View.VISIBLE
        animatedImageView.visibility = View.VISIBLE

        updateBreakMessage() // Mostra a primeira mensagem E IMAGEM imediatamente
        messageHandler.postDelayed(messageRunnable, 7000L) // Começa a agendar as próximas mensagens e imagens
    }

    // Função para parar apenas os handlers da rotação de mensagens/imagens (sem limpar/esconder)
    private fun stopBreakMessagesOnlyHandlers() {
        messageHandler.removeCallbacks(messageRunnable)
    }

    // updateBreakMessage() faz a limpeza interna quando não há mensagens ou não é modo pausa
    private fun updateBreakMessage() {
        if (!isFocusMode && breakMessages.isNotEmpty() && breakImages.isNotEmpty()) {
            breakMessageDisplay.text = breakMessages[currentMessageIndex]
            currentMessageIndex = (currentMessageIndex + 1) % breakMessages.size

            Glide.with(this@MainActivity).load(breakImages[currentBreakImageIndex]).into(animatedImageView)
            currentBreakImageIndex = (currentBreakImageIndex + 1) % breakImages.size
        } else {
            // Se o modo de pausa não estiver ativo ou não houver conteúdo, esconde as views.
            // Isso acontece se updateBreakMessage() for chamado em um contexto inesperado.
            breakMessageDisplay.text = ""
            breakMessageDisplay.visibility = View.GONE
            animatedImageView.setImageDrawable(null)
            animatedImageView.visibility = View.GONE
        }
    }

    // FUNÇÕES PARA GERENCIAR A ROTAÇÃO DE IMAGENS NO MODO FOCO
    private fun startFocusImagesRotation() {
        currentFocusImageIndex = 0
        animatedImageView.visibility = View.VISIBLE
        updateFocusImage() // Mostra a primeira imagem imediatamente
        focusImageHandler.postDelayed(focusImageRunnable, 10000L) // Tempo de rotação do foco: 10 segundos
    }

    // Função para parar apenas os handlers da rotação de imagens de foco (sem limpar/esconder)
    private fun stopFocusImagesRotationOnlyHandlers() {
        focusImageHandler.removeCallbacks(focusImageRunnable)
    }

    // updateFocusImage() faz a limpeza interna quando não há imagens ou não é modo foco
    private fun updateFocusImage() {
        if (isFocusMode && focusImages.isNotEmpty()) {
            Glide.with(this@MainActivity).load(focusImages[currentFocusImageIndex]).into(animatedImageView)
            currentFocusImageIndex = (currentFocusImageIndex + 1) % focusImages.size
        } else {
            // Se o modo foco não estiver ativo ou não houver imagens, esconde a view.
            // Isso acontece se updateFocusImage() for chamado em um contexto inesperado.
            animatedImageView.setImageDrawable(null)
            animatedImageView.visibility = View.GONE
        }
    }

    // Função auxiliar para limpar e esconder APENAS o conteúdo de PAUSA
    private fun clearBreakContentViews() {
        stopBreakMessagesOnlyHandlers() // Para o handler de mensagens de pausa
        breakMessageDisplay.text = ""
        breakMessageDisplay.visibility = View.GONE
        // animatedImageView.setImageDrawable(null) - Não é mais necessário aqui.
        // animatedImageView.visibility = View.GONE - Não é mais necessário aqui.
        // A imagem de foco será carregada por startFocusImagesRotation() logo em seguida
    }


    // Função auxiliar para limpar e esconder todo o conteúdo dinâmico (para reset e destroy)
    private fun clearAllDynamicContentViews() {
        messageHandler.removeCallbacks(messageRunnable)
        breakMessageDisplay.text = ""
        breakMessageDisplay.visibility = View.GONE

        focusImageHandler.removeCallbacks(focusImageRunnable)
        animatedImageView.setImageDrawable(null)
        animatedImageView.visibility = View.GONE
    }

    // Função auxiliar para parar apenas os handlers de rotação, mantendo visibilidade e conteúdo
    private fun stopAllDynamicContentRotationHandlers() {
        messageHandler.removeCallbacks(messageRunnable)
        focusImageHandler.removeCallbacks(focusImageRunnable)
    }

    // Garante que cronômetros e handlers sejam cancelados quando a Activity é destruída
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        clearAllDynamicContentViews() // Limpa e esconde tudo ao destruir a Activity
    }
}