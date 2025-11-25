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

        // Init del flujo (define rol y primer paso)
        viewModel.student_init()

        // Inputs -> VM (para pasos CODE / ADDRESS / LOGO)
        b.etStudentId.doAfterTextChanged { text ->
            viewModel.student_onInputChanged(text?.toString().orEmpty())
        }

        // CTA con estrategia de conectividad
        b.btnGetStarted.setOnClickListener {
            val ui = viewModel.student.value

            // Pasos que requieren red:
            // 1) LOGO -> next carga categorías desde backend (CategoryService.listAll())
            val requiresFetchCategories = (ui.step == OnbStep.LOGO)

            // 2) Finalizar Buyer: en ADDRESS cuando rol != business
            val finishingBuyer = (ui.step == OnbStep.ADDRESS && ui.role != "business")

            // 3) Finalizar Business: en CATEGORIES
            val finishingBusiness = (ui.step == OnbStep.CATEGORIES)

            val needsNetwork = requiresFetchCategories || finishingBuyer || finishingBusiness

            if (needsNetwork && !isOnline()) {
                showTopToast("Sin conexión a internet. No se puede continuar.")
                return@setOnClickListener
            }

            viewModel.student_next()
        }

        // End icon:
        // - CODE -> cámara
        // - CATEGORIES -> selector categorías
        b.tilStudentId.setEndIconOnClickListener {
            val ui = viewModel.student.value
            when (ui.step) {
                OnbStep.CODE -> openCamera()
                OnbStep.CATEGORIES -> maybeOpenCategoriesPicker()
                else -> Unit
            }
        }

        // Tap sobre el campo abre el selector SOLO en CATEGORIES
        b.etStudentId.setOnClickListener {
            val ui = viewModel.student.value
            if (ui.step == OnbStep.CATEGORIES) {
                maybeOpenCategoriesPicker()
            }
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
                            b.etStudentId.isEnabled = true
                            b.etStudentId.isFocusable = true
                            b.etStudentId.isFocusableInTouchMode = true
                            b.etStudentId.isClickable = true
                            b.tilStudentId.isEndIconVisible = true
                        }
                        OnbStep.CATEGORIES -> {
                            b.etStudentId.isEnabled = false
                            b.etStudentId.isFocusable = false
                            b.etStudentId.isFocusableInTouchMode = false
                            b.etStudentId.isClickable = true
                            b.tilStudentId.isEndIconVisible = true

                            val display =
                                if (ui.selectedCats.isEmpty()) "" else ui.selectedCats.joinToString(", ")
                            if (b.etStudentId.text?.toString() != display) {
                                b.etStudentId.setText(display)
                            }
                        }
                    }

                    // Sincroniza texto en pasos NO-CATEGORIES
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

                viewModel.student_onCategoriesPicked(current)
            }
            .setPositiveButton("OK") { _, _ ->
                viewModel.student_onCategoriesPicked(current)
                b.etStudentId.setText(current.joinToString(", "))
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