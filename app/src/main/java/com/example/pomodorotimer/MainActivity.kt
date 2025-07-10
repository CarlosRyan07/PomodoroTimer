package com.example.pomodorotimer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import android.os.Handler
import android.os.Looper
import android.view.View
import android.util.Log // Adicionar import para Log

import com.bumptech.glide.Glide
import android.widget.ImageView

import androidx.core.content.ContextCompat
import android.os.VibratorManager

class MainActivity : AppCompatActivity() {

    // Declaração das Views (elementos da UI)
    private lateinit var timerDisplay: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var modeIndicator: TextView
    private lateinit var breakMessageDisplay: TextView
    private lateinit var animatedImageView: ImageView

    // Declare o botão de configurações
    private lateinit var settingsButton: Button

    // Variáveis de controle do cronômetro
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0L
    private var isRunning: Boolean = false

    // Mude de 'val' para 'var' para que possam ser atualizados pelas configurações
    private var focusTime = 25 * 60 * 1000L
    private var breakTime = 5 * 60 * 1000L
    private var longBreakTime = 15 * 60 * 1000L

    private var isFocusMode: Boolean = true
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

    // Variáveis para controlar feedback de vibração
    private var isVibrationEnabled: Boolean = true

    // SharedPreferences para carregar configurações
    private lateinit var sharedPreferences: SharedPreferences


    @Override
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
        animatedImageView = findViewById(R.id.animatedImageView)

        // Inicializa o botão de configurações
        settingsButton = findViewById(R.id.settingsButton)

        // Inicializa o array de mensagens de pausa a partir de strings.xml
        breakMessages = resources.getStringArray(R.array.break_messages)

        // Inicializa SharedPreferences
        sharedPreferences = getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE)

        // Não chame loadTimerSettings() aqui, onResume cuidará disso para garantir os valores mais recentes.
        // Apenas inicialize timeLeftInMillis com um valor padrão para que o display não fique vazio.
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

        // --- LÓGICA: Ouvinte de clique para avançar imagens/mensagens ---
        // REMOVIDA A CONDIÇÃO 'if (isRunning)'
        animatedImageView.setOnClickListener {
            if (isFocusMode) {
                focusImageHandler.removeCallbacks(focusImageRunnable)
                updateFocusImage()
                focusImageHandler.postDelayed(focusImageRunnable, 10000L)
            } else {
                messageHandler.removeCallbacks(messageRunnable)
                updateBreakMessage()
                messageHandler.postDelayed(messageRunnable, 7000L)
            }
        }

        // REMOVIDA A CONDIÇÃO 'if (isRunning)'
        breakMessageDisplay.setOnClickListener {
            if (!isFocusMode) { // Apenas no modo pausa, já que não temos mensagens de foco
                messageHandler.removeCallbacks(messageRunnable)
                updateBreakMessage()
                messageHandler.postDelayed(messageRunnable, 7000L)
            }
        }
        // --- FIM DA NOVA LÓGICA ---

        // Configura o ouvinte de clique para o botão de configurações
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    @Override
    override fun onResume() {
        super.onResume()
        // SEMPRE recarrega as configurações e ATUALIZA o display aqui.
        // onResume é chamado quando a Activity volta ao foco (ex: após fechar SettingsActivity).
        Log.d("MainActivity", "onResume called. Loading settings.")
        loadTimerSettings()
    }

    // Função para carregar as configurações do timer
    private fun loadTimerSettings() {
        // Carrega os valores em minutos e converte para milissegundos
        val newFocusTime = sharedPreferences.getInt("focusTime", 25) * 60 * 1000L
        val newBreakTime = sharedPreferences.getInt("breakTime", 5) * 60 * 1000L
        val newLongBreakTime = sharedPreferences.getInt("longBreakTime", 15) * 60 * 1000L
        val newVibrationEnabled = sharedPreferences.getBoolean("vibrationEnabled", true)

        // Atualiza as variáveis de classe com os novos valores
        focusTime = newFocusTime
        breakTime = newBreakTime
        longBreakTime = newLongBreakTime
        isVibrationEnabled = newVibrationEnabled

        Log.d("MainActivity", "Loaded settings: Focus=${focusTime/60000}min, Break=${breakTime/60000}min, LongBreak=${longBreakTime/60000}min, Vibrate=$isVibrationEnabled")

        // Se o timer NÃO estiver rodando, atualiza timeLeftInMillis e o display
        // com os novos tempos configurados para o modo atual.
        if (!isRunning) {
            if (isFocusMode) {
                timeLeftInMillis = focusTime
            } else {
                // Se estiver em modo de pausa, o tempo restante será o tempo de pausa configurado
                timeLeftInMillis = if (pomodoroCycles % 4 == 0) longBreakTime else breakTime
            }
            updateCountDownText() // Atualiza o display IMEDIATAMENTE
            Log.d("MainActivity", "Timer not running, updated timeLeftInMillis to ${timeLeftInMillis/1000}s. Display: ${timerDisplay.text}")
        } else {
            Log.d("MainActivity", "Timer is running, new settings will apply next cycle.")
        }
    }


    // Função para iniciar o cronômetro
    private fun startTimer() {
        if (isRunning) return

        isRunning = true
        stopAllDynamicContentRotationHandlers()

        // Cancela qualquer timer existente para garantir um início/retomada limpa
        countDownTimer?.cancel()
        Log.d("MainActivity", "startTimer called. isRunning: $isRunning, timeLeftInMillis before start: ${timeLeftInMillis/1000}s")


        // Lógica para definir o timeLeftInMillis no início do timer
        // Se o timer estava previamente finalizado ou resetado (countDownTimer é null),
        // OU se timeLeftInMillis é 0 (significando que terminou sua contagem),
        // devemos re-inicializar timeLeftInMillis para a duração total do modo atual.
        // Caso contrário, retomamos do timeLeftInMillis atual (se estava pausado).
        if (countDownTimer == null || timeLeftInMillis == 0L) {
            // Esta condição verifica se o tempo atual é um dos tempos "cheios" ou se o timer já terminou.
            // Se sim, significa que estamos começando um NOVO ciclo ou reiniciando após um fim.
            if (isFocusMode) {
                timeLeftInMillis = focusTime
            } else {
                timeLeftInMillis = if (pomodoroCycles % 4 == 0) longBreakTime else breakTime
            }
            Log.d("MainActivity", "Initializing new timer cycle with full time: ${timeLeftInMillis/1000}s")
        } else {
            // Se timeLeftInMillis não é um tempo "cheio" e não é 0, significa que o timer foi pausado
            Log.d("MainActivity", "Resuming paused timer from: ${timeLeftInMillis/1000}s")
        }

        updateCountDownText() // Garante que o display mostre o tempo que o timer vai usar

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                isRunning = false
                countDownTimer = null // Define como null para indicar que o ciclo terminou

                triggerFeedback()

                if (isFocusMode) {
                    // Foco terminou, vai para Pausa
                    pomodoroCycles++
                    isFocusMode = false
                    // Define o timeLeftInMillis para o PRÓXIMO ciclo (pausa)
                    timeLeftInMillis = if (pomodoroCycles % 4 == 0) longBreakTime else breakTime
                    Toast.makeText(this@MainActivity, getString(R.string.focus_ended_message), Toast.LENGTH_SHORT).show()
                    updateModeIndicator()
                    stopFocusImagesRotationOnlyHandlers()
                    startBreakMessagesAndRotation()
                } else {
                    // Pausa terminou, vai para Foco
                    isFocusMode = true
                    // Define o timeLeftInMillis para o PRÓXIMO ciclo (foco)
                    timeLeftInMillis = focusTime
                    Toast.makeText(this@MainActivity, getString(R.string.break_ended_message), Toast.LENGTH_SHORT).show()
                    updateModeIndicator()
                    clearBreakContentViews()
                    startFocusImagesRotation()
                }
                updateCountDownText() // Atualiza o display para mostrar o tempo inicial do novo ciclo
                Log.d("MainActivity", "Cycle finished. Next timeLeftInMillis: ${timeLeftInMillis/1000}s. Mode: ${if(isFocusMode) "Focus" else "Break"}")
                // Opcional: Para iniciar o próximo timer automaticamente:
                // startTimer()
            }
        }.start()

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
        Log.d("MainActivity", "Timer paused. timeLeftInMillis: ${timeLeftInMillis/1000}s")
        // Rotação de mensagens e imagens CONTINUA.
    }

    // Função para reiniciar o cronômetro. CONTEÚDO É LIMPO E ESCONDIDO.
    private fun resetTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isFocusMode = true
        pomodoroCycles = 0 // Zera os ciclos ao resetar completamente

        // Usa o focusTime carregado das configurações para o reset
        timeLeftInMillis = focusTime
        updateCountDownText()
        updateModeIndicator()

        // Limpa e esconde todo o conteúdo dinâmico ao resetar
        clearAllDynamicContentViews()

        // APÓS O RESET, ESTAMOS NO MODO FOCO, ENTÃO REINICIAMOS A ROTAÇÃO DE IMAGENS DE FOCO
        startFocusImagesRotation()
        Log.d("MainActivity", "Timer reset. timeLeftInMillis: ${timeLeftInMillis/1000}s")
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

    // Aciona feedback de vibração
    private fun triggerFeedback() {
        // Verifica se a vibração está ativada nas configurações
        if (isVibrationEnabled) {
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
        }
    }

    // FUNÇÕES PARA GERENCIAR A ROTAÇÃO DE MENSAGENS E IMAGENS DE PAUSA
    private fun startBreakMessagesAndRotation() {
        currentMessageIndex = 0
        currentBreakImageIndex = 0

        // Certifica-se de que a rotação automática seja limpa antes de reiniciar
        messageHandler.removeCallbacks(messageRunnable)
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
        // Certifica-se de que a rotação automática seja limpa antes de reiniciar
        focusImageHandler.removeCallbacks(focusImageRunnable)
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

    @Override
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        clearAllDynamicContentViews() // Limpa e esconde tudo ao destruir a Activity
    }
}