package lk.zenova.gomart.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import lk.zenova.gomart.R;
import lk.zenova.gomart.helper.AppPreferences;

public class SplashActivity extends AppCompatActivity {


    ProgressBar progressBar;
    TextView loadingText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splashProgressBar);
        loadingText = findViewById(R.id.loadingText);



        progressBar.setProgress(0);

        Handler handler = new Handler(Looper.getMainLooper());

        Runnable runnable = new Runnable() {
            int progress = 0;

            @Override
            public void run() {

                progress++;

                progressBar.setProgress(progress);
                loadingText.setText("Loading " + progress + "%");

                if (progress < 100) {
                    handler.postDelayed(this, 20);
                } else {
                    AppPreferences prefs = new AppPreferences(SplashActivity.this);

                    Intent intent;
                    if (!prefs.isWelcomeSeen()) {
                        intent = new Intent(SplashActivity.this,
                                WelcomePageActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this,
                                MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }
            }
        };

        handler.postDelayed(runnable, 800);
    }
}