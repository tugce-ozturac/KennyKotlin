package com.tugce.kennykotlin

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tugce.kennykotlin.databinding.ActivityMainBinding
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var countDownTimer: CountDownTimer? = null
    private var score = 0
    private val gameTime = 15000L
    private lateinit var imageViews: List<ImageView>

    private val prefs by lazy { getSharedPreferences("kenny_prefs", MODE_PRIVATE) }
    private var bestScore = 0
    private var lastVisibleIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageViews = listOf(
            binding.imageView1,
            binding.imageView2,
            binding.imageView3,
            binding.imageView4,
            binding.imageView5,
            binding.imageView6,
            binding.imageView7,
            binding.imageView8,
            binding.imageView9
        )

        // Başlangıç durumu
        imageViews.forEach { it.visibility = View.INVISIBLE }
        binding.startButton.visibility = View.VISIBLE
        binding.timeText.text = "Time: ${gameTime / 1000}"
        binding.scoreText.text = "Score: 0"

        // Best score
        bestScore = prefs.getInt("best_score", 0)
        binding.bestScoreText.text = "Best: $bestScore"

        // ProgressBar ayarı
        binding.progressBar.max = (gameTime / 1000).toInt()
        binding.progressBar.progress = binding.progressBar.max
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity kapanınca handler’ı durdur
        runnable?.let { handler.removeCallbacks(it) }
        countDownTimer?.cancel()
    }

    fun inCreaseScore(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        showSparkleEffectOver(view)
        score += 1
        binding.scoreText.text = "Score: $score"
    }

    private fun showSparkleEffectOver(anchorView: View) {
        val root = binding.main

        val rootLoc = IntArray(2)
        val anchorLoc = IntArray(2)
        root.getLocationOnScreen(rootLoc)
        anchorView.getLocationOnScreen(anchorLoc)

        val centerX = (anchorLoc[0] - rootLoc[0] + anchorView.width / 2f)
        val centerY = (anchorLoc[1] - rootLoc[1] + anchorView.height / 2f)

        val particleCount = 10
        val sizePx = dpToPx(16f)
        val half = sizePx / 2f

        repeat(particleCount) {
            val star = ImageView(this).apply {
                setImageResource(android.R.drawable.star_big_on)
                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(sizePx.toInt(), sizePx.toInt())
                alpha = 1f
                scaleX = 0.8f
                scaleY = 0.8f
            }

            root.addView(star)
            star.x = centerX - half
            star.y = centerY - half

            val angle = ThreadLocalRandom.current().nextDouble(0.0, 360.0)
            val distance = dpToPx(ThreadLocalRandom.current().nextInt(60, 130).toFloat())
            val rad = Math.toRadians(angle)
            val dx = (cos(rad) * distance).toFloat()
            val dy = (sin(rad) * distance).toFloat()

            star.animate()
                .x(star.x + dx)
                .y(star.y + dy)
                .alpha(0f)
                .setDuration(550)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { root.removeView(star) }
                .start()
        }
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    fun startGame(view: View) {
        // Oyunu başlat
        binding.startButton.visibility = View.GONE
        score = 0
        binding.scoreText.text = "Score: 0"

        // Progress bar’ı resetle
        binding.progressBar.progress = binding.progressBar.max

        // Sayaç
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(gameTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.timeText.text = "Time: $secondsLeft"

                // Smooth progress update
                val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", binding.progressBar.progress, secondsLeft)
                animator.duration = 400
                animator.start()

                // Renk değişimi: yeşil > sarı > kırmızı
                val progressFraction = secondsLeft.toFloat() / binding.progressBar.max
                binding.progressBar.progressTintList = when {
                    progressFraction > 0.5 -> ColorStateList.valueOf(Color.GREEN)
                    progressFraction > 0.2 -> ColorStateList.valueOf(Color.YELLOW)
                    else -> ColorStateList.valueOf(Color.RED)
                }
            }

            override fun onFinish() {
                binding.progressBar.progress = 0
                endGame()
            }
        }
        countDownTimer?.start()

        // Görselleri rastgele gösteren döngü
        runnable?.let { handler.removeCallbacks(it) }
        runnable = object : Runnable {
            override fun run() {
                imageViews.forEach { it.visibility = View.INVISIBLE }

                var randomIndex: Int
                do {
                    randomIndex = (imageViews.indices).random()
                } while (randomIndex == lastVisibleIndex)
                lastVisibleIndex = randomIndex

                imageViews[randomIndex].visibility = View.VISIBLE
                handler.postDelayed(this, 700)
            }
        }
        handler.post(runnable!!)
    }

    private fun endGame() {
        binding.timeText.text = "Time: 0"
        runnable?.let { handler.removeCallbacks(it) }
        imageViews.forEach { it.visibility = View.INVISIBLE }

        // Best score kontrolü ve kaydetme
        if (score > bestScore) {
            bestScore = score
            prefs.edit().putInt("best_score", bestScore).apply()
        }
        binding.bestScoreText.text = "Best: $bestScore"

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Oyun Bitti!")
        builder.setMessage("Score: $score\nBest: $bestScore\nTekrar oynamak ister misiniz?")
        builder.setPositiveButton("Evet") { _, _ ->
            startGame(binding.startButton)
        }
        builder.setNegativeButton("Hayır") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        binding.startButton.visibility = View.VISIBLE
    }
}
