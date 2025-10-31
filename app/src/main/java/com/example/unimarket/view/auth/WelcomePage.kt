package com.example.unimarket.view.auth

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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.unimarket.R
import com.example.unimarket.view.home.CourierHomeActivity
import com.example.unimarket.view.home.HomeBuyerActivity
import com.example.unimarket.view.profile.BusinessAccountActivity
import com.example.unimarket.viewmodel.AuthNavDestination
import com.example.unimarket.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class WelcomePage : AppCompatActivity() {

    private lateinit var root: ConstraintLayout
    private val viewModel: AuthViewModel by viewModels()
    private var introAlreadyPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_page)

        root = findViewById(R.id.root_welcome)
        val btnSignUp = findViewById<MaterialButton>(R.id.btn_sign_up)
        val tvLogIn = findViewById<TextView>(R.id.tv_login_action)

        btnSignUp.setOnClickListener { viewModel.welcome_onClickSignUp() }
        tvLogIn.setOnClickListener { viewModel.welcome_onClickLogin() }

        observeWelcomeState()
        viewModel.welcome_onInit()
    }

    private fun observeWelcomeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.welcome.collect { ui ->
                    if (ui.shouldPlayIntro && !introAlreadyPlayed) {
                        introAlreadyPlayed = true
                        playIntroOverlay(root)
                    }

                    when (ui.nav) {
                        AuthNavDestination.ToCreateAccount -> {
                            startActivity(Intent(this@WelcomePage, CreateAccountActivity::class.java))
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.ToLogin -> {
                            startActivity(Intent(this@WelcomePage, LoginActivity::class.java))
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.ToBuyerHome -> {
                            startActivity(Intent(this@WelcomePage, HomeBuyerActivity::class.java))
                            finish()
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.ToCourierHome -> {
                            startActivity(Intent(this@WelcomePage, CourierHomeActivity::class.java))
                            finish()
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.ToBusinessProfile -> {
                            startActivity(Intent(this@WelcomePage, BusinessAccountActivity::class.java))
                            finish()
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.ToStudentCode -> {
                            // Si algún flujo te lleva a StudentCode, manéjalo aquí si lo necesitas.
                            viewModel.welcome_clearNav()
                        }
                        AuthNavDestination.None -> Unit
                    }
                }
            }
        }
    }

    private fun playIntroOverlay(root: ConstraintLayout) {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@WelcomePage, R.color.yellowLight))
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
