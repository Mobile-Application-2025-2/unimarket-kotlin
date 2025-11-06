package com.example.unimarket.view.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.databinding.ActivityStudentCodeBinding
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BusinessAccountActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.example.unimarket.viewmodel.OnbStep
import kotlinx.coroutines.launch

class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        // Init del flujo (define rol y primer paso)
        viewModel.student_init()

        // Inputs -> VM (para pasos CODE / ADDRESS / LOGO)
        b.etStudentId.doAfterTextChanged { text ->
            viewModel.student_onInputChanged(text?.toString().orEmpty())
        }

        // CTA
        b.btnGetStarted.setOnClickListener { viewModel.student_next() }

        // Icono al final (antiguamente cámara). Lo reusamos en CATEGORIES como "abrir selector"
        b.tilStudentId.setEndIconOnClickListener {
            maybeOpenCategoriesPicker()
        }

        // tap sobre el campo abre el selector cuando estamos en CATEGORIES
        b.etStudentId.setOnClickListener {
            maybeOpenCategoriesPicker()
        }

        observeStudentUi()
    }

    private fun observeStudentUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->
                    // Bind de textos
                    b.title.text = ui.title
                    b.subtitle.text = ui.subtitle
                    b.tilStudentId.hint = ui.hint
                    b.btnGetStarted.text = ui.cta

                    // Config UI según paso
                    when (ui.step) {
                        OnbStep.CODE, OnbStep.ADDRESS, OnbStep.LOGO -> {
                            // Campo editable normal
                            b.etStudentId.isEnabled = true
                            b.etStudentId.isFocusable = true
                            b.etStudentId.isFocusableInTouchMode = true
                            b.etStudentId.isClickable = true
                            b.tilStudentId.isEndIconVisible = true // mantiene icono (por si lo usas)
                        }
                        OnbStep.CATEGORIES -> {
                            // Campo como "display" de la selección (solo lectura)
                            b.etStudentId.isEnabled = false
                            b.etStudentId.isFocusable = false
                            b.etStudentId.isFocusableInTouchMode = false
                            b.etStudentId.isClickable = true // para abrir el diálogo al tocar
                            b.tilStudentId.isEndIconVisible = true
                            // Texto visible = categorías escogidas
                            val display = if (ui.selectedCats.isEmpty()) "" else ui.selectedCats.joinToString(", ")
                            if (b.etStudentId.text?.toString() != display) {
                                b.etStudentId.setText(display)
                            }
                        }
                    }

                    // Sincroniza texto en pasos NO-CATEGORIES (lo maneja VM)
                    if (ui.step != OnbStep.CATEGORIES) {
                        val current = b.etStudentId.text?.toString().orEmpty()
                        if (current != ui.textValue) {
                            b.etStudentId.setText(ui.textValue)
                            b.etStudentId.setSelection(b.etStudentId.text?.length ?: 0)
                        }
                    }

                    // Habilitar/Deshabilitar CTA
                    b.btnGetStarted.isEnabled = ui.canProceed

                    // Error one-shot
                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.student_clearNavAndErrors()
                    }

                    // Navegación final
                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(Intent(this@StudentCodeActivity, HomeBuyerActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(Intent(this@StudentCodeActivity, BusinessAccountActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@StudentCodeActivity, CourierHomeActivity::class.java))
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun maybeOpenCategoriesPicker() {
        val ui = viewModel.student.value
        if (ui.step != OnbStep.CATEGORIES) return
        if (ui.categories.isEmpty()) {
            Toast.makeText(this, "No hay categorías para mostrar.", Toast.LENGTH_SHORT).show()
            return
        }

        val items = ui.categories.toTypedArray()
        val checked = BooleanArray(items.size) { idx -> ui.selectedCats.contains(items[idx]) }

        // 👇 Buffer vivo durante toda la vida del diálogo
        val current = ui.selectedCats.toMutableList()

        AlertDialog.Builder(this)
            .setTitle("Selecciona hasta 3 categorías")
            .setMultiChoiceItems(items, checked) { dialog, which, isChecked ->
                val name = items[which]

                if (isChecked) {
                    if (current.size >= 3 && !current.contains(name)) {
                        (dialog as AlertDialog).listView.setItemChecked(which, false)
                        Toast.makeText(this, "Máximo 3 categorías", Toast.LENGTH_SHORT).show()
                        return@setMultiChoiceItems
                    }
                    if (!current.contains(name)) current.add(name)
                } else {
                    current.remove(name)
                }

                // Opcional: refleja en vivo
                viewModel.student_onCategoriesPicked(current)
            }
            .setPositiveButton("OK") { _, _ ->
                // ✅ Confirmar TODA la selección acumulada
                viewModel.student_onCategoriesPicked(current)
                b.etStudentId.setText(current.joinToString(", "))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}