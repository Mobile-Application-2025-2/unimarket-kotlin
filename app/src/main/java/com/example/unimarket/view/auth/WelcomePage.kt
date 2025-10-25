package com.example.unimarket.view.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt
import kotlinx.coroutines.launch

class WelcomePage : AppCompatActivity() {

    private lateinit var root: ConstraintLayout

    // Inyectamos el ViewModel (usa el constructor por defecto que le dimos)
    private val viewModel: AuthViewModel by viewModels()

    // Para no repetir la animación cada vez que se re-emite el mismo estado
    private var introAlreadyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.welcome_page)

        root = findViewById(R.id.root_welcome)
        val btnSignUp = findViewById<MaterialButton>(R.id.btn_sign_up)
        val tvLogIn = findViewById<TextView>(R.id.tv_login_action)

        // Listeners de UI: en vez de llamar controller, llamamos al ViewModel
        btnSignUp.setOnClickListener {
            viewModel.welcome_onClickSignUp()
        }

        tvLogIn.setOnClickListener {
            viewModel.welcome_onClickLogin()
        }

        // Observamos el estado del ViewModel
        observeWelcomeState()

        // Disparamos la lógica de arranque (antes era controller.onInit())
        viewModel.welcome_onInit()
    }

    private fun observeWelcomeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.welcome.collect { ui ->

                    // 1. Animación de intro
                    if (ui.shouldPlayIntro && !introAlreadyPlayed) {
                        introAlreadyPlayed = true
                        playIntroOverlay(root)
                    }

                    // 2. Navegación según el destino que publique el ViewModel
                    when (ui.nav) {
                        AuthNavDestination.ToCreateAccount -> {
                            startActivity(
                                Intent(
                                    this@WelcomePage,
                                    CreateAccountActivity::class.java
                                )
                            )
                            // Resetea nav en el VM para no re-navegar
                            viewModel.welcome_clearNav()
                        }

                        AuthNavDestination.ToLogin -> {
                            startActivity(
                                Intent(
                                    this@WelcomePage,
                                    LoginActivity::class.java
                                )
                            )
                            viewModel.welcome_clearNav()
                        }

                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(
                                Intent(
                                    this@WelcomePage,
                                    com.example.unimarket.view.explore.ExploreBuyerActivity::class.java
                                )
                            )
                            finish()
                            viewModel.welcome_clearNav()
                        }

                        AuthNavDestination.ToCourierHome -> {
                            startActivity(
                                Intent(
                                    this@WelcomePage,
                                    com.example.unimarket.view.home.CourierHomeActivity::class.java
                                )
                            )
                            finish()
                            viewModel.welcome_clearNav()
                        }

                        AuthNavDestination.ToStudentCode -> {
                        }

                        AuthNavDestination.None -> {
                            // no-op
                        }
                    }
                }
            }
        }
    }

    /**
     * Tu misma animación original.
     * La dejo idéntica pero como método privado normal.
     */
    private fun playIntroOverlay(root: ConstraintLayout) {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    this@WelcomePage,
                    R.color.yellowLight
                )
            )
            isClickable = true
            alpha = 1f
        }

        val lp = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(overlay, lp)
        overlay.bringToFront()

        val circleSizeDp = 120f
        val circleSizePx = (circleSizeDp * resources.displayMetrics.density).toInt()
        val START_SCALE = 0.02f

        val circle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            scaleX = START_SCALE
            scaleY = START_SCALE
        }

        val circleLp = FrameLayout.LayoutParams(circleSizePx, circleSizePx).apply {
            gravity = Gravity.CENTER
        }
        overlay.addView(circle, circleLp)

        overlay.post {
            val w = root.width
            val h = root.height
            val diag = sqrt((w * w + h * h).toDouble()).toFloat()
            val targetScale = (diag / circleSizePx) * 1.2f

            val animX = ObjectAnimator.ofFloat(circle, View.SCALE_X, START_SCALE, targetScale)
            val animY = ObjectAnimator.ofFloat(circle, View.SCALE_Y, START_SCALE, targetScale)

            // Desvanezco el overlay después de la expansión
            val fade = ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f).apply {
                duration = 300
                startDelay = 150
            }

            AnimatorSet().apply {
                duration = 830
                playTogether(animX, animY)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fade.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                root.removeView(overlay)
                            }
                        })
                        fade.start()
                    }
                })
                start()
            }
        }
    }
}