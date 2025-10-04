package com.example.unimarket.ui.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.unimarket.R
import com.google.android.material.button.MaterialButton
import kotlin.math.sqrt //

class WelcomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_page)
        val root = findViewById<ConstraintLayout>(R.id.root_welcome)
        val btnSignUp = findViewById<MaterialButton>(R.id.btn_sign_up)
        val tvLogIn   = findViewById<TextView>(R.id.tv_login_action)

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
        tvLogIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        playIntroOverlay(root)
    }

    private fun playIntroOverlay(root: ConstraintLayout) {
        // Contenedor a pantalla completa por encima del layout
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@WelcomePage, R.color.yellowLight))
            isClickable = true   // mientras dura la animación, captura toques
            alpha = 1f
        }
        val lp = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(overlay, lp)
        overlay.bringToFront()

        // Círculo blanco creado en código (sin drawable externo)
        val circleSizeDp = 120f
        val circleSizePx = (circleSizeDp * resources.displayMetrics.density).toInt()
        val circle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            scaleX = 0.1f
            scaleY = 0.1f
        }
        val circleLp = FrameLayout.LayoutParams(circleSizePx, circleSizePx).apply {
            gravity = Gravity.CENTER
        }
        overlay.addView(circle, circleLp)

        // Esperamos a que el root tenga medidas para calcular la escala
        overlay.post {
            val w = root.width
            val h = root.height

            // Diagonal en píxeles (sin toDouble(); usamos sqrt)
            val diag = sqrt((w * w + h * h).toDouble()).toFloat()
            val targetScale = (diag / circleSizePx) * 1.2f

            val animX = ObjectAnimator.ofFloat(circle, View.SCALE_X, targetScale)
            val animY = ObjectAnimator.ofFloat(circle, View.SCALE_Y, targetScale)

            AnimatorSet().apply {
                duration = 650
                playTogether(animX, animY)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Quitamos el overlay para que no tape los botones
                        overlay.animate()
                            .alpha(0f)
                            .setDuration(250)
                            .withEndAction { root.removeView(overlay) }
                            .start()
                    }
                })
                start()
            }
        }
    }
}