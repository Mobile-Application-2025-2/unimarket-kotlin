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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.unimarket.R
import com.example.unimarket.controller.auth.WelcomePageController
import com.example.unimarket.controller.auth.WelcomePageViewPort
import com.google.android.material.button.MaterialButton
import kotlin.math.sqrt

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class WelcomePage : AppCompatActivity(), WelcomePageViewPort {

    private lateinit var root: ConstraintLayout
    private lateinit var controller: WelcomePageController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseFirestore.setLoggingEnabled(true)

        val app = FirebaseApp.getInstance()
        val opts = app.options
        Log.e("FIREBASE_APP", "projectId=${opts.projectId}, applicationId=${opts.applicationId}, storageBucket=${opts.storageBucket}")

        val db = FirebaseFirestore.getInstance()
        val settings = db.firestoreSettings
        Log.e("FIRESTORE", "host=${settings.host}, persistence=${settings.isPersistenceEnabled}")

        setContentView(R.layout.welcome_page)

        root = findViewById(R.id.root_welcome)
        val btnSignUp = findViewById<MaterialButton>(R.id.btn_sign_up)
        val tvLogIn   = findViewById<TextView>(R.id.tv_login_action)

        controller = WelcomePageController(this)

        btnSignUp.setOnClickListener { controller.onClickSignUp() }
        tvLogIn.setOnClickListener   { controller.onClickLogin() }

        controller.onInit()
    }

    override fun playIntroOverlay() {
        playIntroOverlay(root)
    }

    override fun navigateToCreateAccount() {
        startActivity(Intent(this, CreateAccountActivity::class.java))
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun navigateToBuyer() {
        startActivity(Intent(this, com.example.unimarket.view.explore.ExploreBuyerActivity::class.java))
        finish()
    }

    override fun navigateToCourier() {
        startActivity(Intent(this, com.example.unimarket.view.home.CourierHomeActivity::class.java))
        finish()
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
            val fade = ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f).apply { duration = 300; startDelay = 150 }

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