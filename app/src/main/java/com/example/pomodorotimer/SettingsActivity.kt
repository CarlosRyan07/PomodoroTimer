package com.example.pomodorotimer

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var editFocusTime: EditText
    private lateinit var editBreakTime: EditText
    private lateinit var editLongBreakTime: EditText
    private lateinit var switchVibration: SwitchCompat
    // switchSound foi removido

    private lateinit var btnSaveSettings: Button

    private lateinit var sharedPreferences: SharedPreferences

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inicializa as Views
        editFocusTime = findViewById(R.id.edit_focus_time)
        editBreakTime = findViewById(R.id.edit_break_time)
        editLongBreakTime = findViewById(R.id.edit_long_break_time)
        switchVibration = findViewById(R.id.switch_vibration)
        btnSaveSettings = findViewById(R.id.btn_save_settings)

        // Inicializa SharedPreferences para salvar e carregar configurações
        sharedPreferences = getSharedPreferences("PomodoroSettings", Context.MODE_PRIVATE)

        // Carrega as configurações salvas (ou valores padrão)
        loadSettings()

        // Configura o listener para o botão Salvar
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val focus = sharedPreferences.getInt("focusTime", 25)
        val breakS = sharedPreferences.getInt("breakTime", 5)
        val longBreak = sharedPreferences.getInt("longBreakTime", 15)
        val vibrate = sharedPreferences.getBoolean("vibrationEnabled", true)

        editFocusTime.setText(focus.toString())
        editBreakTime.setText(breakS.toString())
        editLongBreakTime.setText(longBreak.toString())
        switchVibration.isChecked = vibrate
    }

    private fun saveSettings() {
        val focusInput = editFocusTime.text.toString()
        val breakInput = editBreakTime.text.toString()
        val longBreakInput = editLongBreakTime.text.toString()

        val focus = focusInput.toIntOrNull() ?: 25
        val breakS = breakInput.toIntOrNull() ?: 5
        val longBreak = longBreakInput.toIntOrNull() ?: 15
        val vibrate = switchVibration.isChecked

        // Validação básica para tempos mínimos
        if (focus <= 0 || breakS <= 0 || longBreak <= 0) {
            Toast.makeText(this, "Os tempos devem ser maiores que zero.", Toast.LENGTH_SHORT).show()
            return
        }

        val editor = sharedPreferences.edit()
        editor.putInt("focusTime", focus)
        editor.putInt("breakTime", breakS)
        editor.putInt("longBreakTime", longBreak)
        editor.putBoolean("vibrationEnabled", vibrate)
        val savedSuccessfully = editor.commit() // Mantido commit() para garantir a gravação imediata

        if (savedSuccessfully) {
            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()
            finish() // Fecha a SettingsActivity e retorna para a MainActivity
        } else {
            Toast.makeText(this, "Erro ao salvar configurações.", Toast.LENGTH_SHORT).show()
        }
    }
}