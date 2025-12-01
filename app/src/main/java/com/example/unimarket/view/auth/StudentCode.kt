package com.example.unimarket.view.auth

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.databinding.ActivityStudentCodeBinding
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BusinessAccountActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.example.unimarket.viewmodel.OnbStep
import kotlinx.coroutines.launch

class StudentCodeActivity : AppCompatActivity() {

    private lateinit var b: ActivityStudentCodeBinding
    private val viewModel: AuthViewModel by viewModels()

    // Preview de la foto
    private var photoPreview: ImageView? = null

    // Launcher de cámara
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                val iv = ensurePhotoPreview()
                iv.setImageBitmap(bitmap)
                iv.visibility = View.VISIBLE
                viewModel.student_onCameraResult(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityStudentCodeBinding.inflate(layoutInflater)
        (b.root.parent as? ViewGroup)?.removeView(b.root)
        setContentView(b.root)

        viewModel.student_init()

        // Documento / código
        b.etStudentId.doAfterTextChanged {
            // Si quieres, puedes seguir informando al VM, pero ya no es obligatorio:
            // viewModel.student_onInputChanged(it?.toString().orEmpty())
            updateSubmitEnabled()
        }

        // Dirección
        b.etAddress.doAfterTextChanged {
            updateSubmitEnabled()
        }

        // Logo (solo business)
        b.etLogo.doAfterTextChanged {
            updateSubmitEnabled()
        }

        // Campo de categorías: solo lectura; abre el selector
        b.etCategories.setOnClickListener {
            maybeOpenCategoriesPicker()
        }

        // End icon del documento -> cámara
        b.tilStudentId.setEndIconOnClickListener {
            openCamera()
        }

        // End icon de categorías -> selector
        b.tilCategories.setEndIconOnClickListener {
            maybeOpenCategoriesPicker()
        }

        // Botón principal
        b.btnGetStarted.setOnClickListener {
            val ui = viewModel.student.value

            // Toda la operación de submit requiere red
            if (!isOnline()) {
                showTopToast("Sin conexión a internet. No se puede continuar.")
                return@setOnClickListener
            }

            val document = b.etStudentId.text?.toString().orEmpty()
            val address  = b.etAddress.text?.toString().orEmpty()
            val logo     = if (ui.role == "business") b.etLogo.text?.toString().orEmpty() else null
            val selectedCats = if (ui.role == "business") ui.selectedCats else emptyList()

            viewModel.student_submitSingle(document, address, logo, selectedCats)
        }

        observeStudentUi()
    }

    private fun updateSubmitEnabled() {
        val ui = viewModel.student.value
        val document = b.etStudentId.text?.toString()?.trim().orEmpty()
        val address  = b.etAddress.text?.toString()?.trim().orEmpty()
        val logo     = b.etLogo.text?.toString()?.trim().orEmpty()

        val enabled = if (ui.role == "business") {
            document.length >= 3 && address.isNotEmpty() && logo.isNotEmpty()
        } else {
            document.length >= 3 && address.isNotEmpty()
        }

        b.btnGetStarted.isEnabled = enabled
    }

    private fun observeStudentUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.student.collect { ui ->

                    // 1) Título oculto siempre
                    b.title.visibility = View.GONE

                    // 2) Subtítulo fijo
                    b.subtitle.text =
                        "Antes de empezar, necesitamos esta información para crear tu cuenta."

                    // 3) Hint del campo documento según rol
                    b.tilStudentId.hint = if (ui.role == "business") {
                        "NIT o documento"
                    } else {
                        "Código estudiantil"
                    }

                    // Si quieres, el texto interno del input también:
                    // val currentDoc = b.etStudentId.text?.toString().orEmpty()
                    // if (currentDoc != ui.textValue) {
                    //     b.etStudentId.setText(ui.textValue)
                    //     b.etStudentId.setSelection(b.etStudentId.text?.length ?: 0)
                    // }

                    val isBusiness = ui.role == "business"

                    // Mostrar/ocultar campos extra negocio
                    b.businessExtraContainer.visibility =
                        if (isBusiness) View.VISIBLE else View.GONE

                    if (isBusiness) {
                        val display = if (ui.selectedCats.isEmpty())
                            ""
                        else
                            ui.selectedCats.joinToString(", ")
                        if (b.etCategories.text?.toString() != display) {
                            b.etCategories.setText(display)
                        }
                    } else {
                        b.etCategories.setText("")
                    }

                    // Habilitar botón según lo que haya escrito
                    updateSubmitEnabled()

                    ui.errorMessage?.let { msg ->
                        Toast.makeText(this@StudentCodeActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.student_clearNavAndErrors()
                    }

                    when (ui.nav) {
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(
                                Intent(
                                    this@StudentCodeActivity,
                                    HomeBuyerActivity::class.java
                                )
                            )
                            finish()
                            viewModel.student_clearNavAndErrors()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(
                                Intent(
                                    this@StudentCodeActivity,
                                    BusinessAccountActivity::class.java
                                )
                            )
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
        if (ui.role != "business") return

        if (ui.categories.isEmpty()) {
            viewModel.student_loadCategories()
            if (ui.categories.isEmpty()) {
                Toast.makeText(this, "No hay categorías para mostrar.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val items = ui.categories.toTypedArray()
        val currentSelection = ui.selectedCats.firstOrNull()
        val selectedIndex = items.indexOf(currentSelection).let { if (it >= 0) it else -1 }

        var chosen: String? = currentSelection

        AlertDialog.Builder(this)
            .setTitle("Selecciona una categoría")
            .setSingleChoiceItems(items, selectedIndex) { _, which ->
                chosen = items[which]
            }
            .setPositiveButton("OK") { _, _ ->
                val list = chosen?.let { listOf(it) } ?: emptyList()
                viewModel.student_onCategoriesPicked(list)
                // Si sigues usando etStudentId para mostrar las categorías:
                b.etStudentId.setText(list.joinToString(", "))
                // Si estás usando un campo separado (etCategories), cambia por:
                // b.etCategories.setText(list.joinToString(", "))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------- Cámara y preview --------
    private fun openCamera() { cameraLauncher.launch(null) }

    /** Crea (si hace falta) un ImageView pequeño centrado debajo del campo. */
    private fun ensurePhotoPreview(): ImageView {
        photoPreview?.let { return it }

        val root = b.root as ViewGroup
        val sizePx = dp(80)

        val iv = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }

        root.addView(iv)

        root.post {
            val marginTop = dp(8)
            val centerX = b.tilStudentId.x + (b.tilStudentId.width - sizePx) / 2f
            val topY = b.tilStudentId.y + b.tilStudentId.height + marginTop
            iv.x = centerX
            iv.y = topY
            iv.bringToFront()
        }

        photoPreview = iv
        return iv
    }

    // -------- Conectividad + toast superior --------

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showTopToast(message: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.personajesingup)
            val s = dp(20)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { rightMargin = dp(8) }
        }

        val text = TextView(this).apply {
            this.text = message
            setTextColor(Color.BLACK)
            textSize = 14f
        }

        container.addView(icon)
        container.addView(text)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(24))
            show()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}