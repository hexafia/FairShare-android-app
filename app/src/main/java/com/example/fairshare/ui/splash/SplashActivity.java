package com.example.fairshare.ui.splash;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fairshare.MainActivity;
import com.example.fairshare.R;
import com.example.fairshare.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 800; // ms – will be tied to Firebase later

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the splash truly full-screen (hide status & nav bars)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_splash);

        ConstraintLayout root = findViewById(R.id.splash_root);
        ImageView splashIcon = findViewById(R.id.splash_icon);

        // ── Build a 4-stop horizontal gradient programmatically ──
        applyFourColorGradient(root);

        // ── Pulsing fade animation on the icon ──
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(splashIcon, "alpha", 1f, 0.3f);
        pulseAnimator.setDuration(400);                        // one half-cycle
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.start();

        // ── After SPLASH_DURATION, fade out and navigate ──
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pulseAnimator.cancel();

            // Fade out the entire splash screen
            root.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        Intent intent;
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        } else {
                            intent = new Intent(SplashActivity.this, LoginActivity.class);
                        }
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    })
                    .start();
        }, SPLASH_DURATION);
    }

    /**
     * Creates a left-to-right linear gradient with the 4 user-specified colors
     * and sets it as the background of the given view.
     */
    private void applyFourColorGradient(final View view) {
        final int[] colors = {
                Color.parseColor("#fdb86b"),  // left
                Color.parseColor("#c7f0ec"),
                Color.parseColor("#f8d7ad"),
                Color.parseColor("#7bd4c9")   // right
        };
        final float[] positions = {0f, 0.33f, 0.66f, 1f};

        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(
                        0, 0, width, 0,         // left → right
                        colors, positions,
                        Shader.TileMode.CLAMP
                );
            }
        };

        PaintDrawable paintDrawable = new PaintDrawable();
        paintDrawable.setShape(new RectShape());
        paintDrawable.setShaderFactory(shaderFactory);
        view.setBackground(paintDrawable);
    }
}
