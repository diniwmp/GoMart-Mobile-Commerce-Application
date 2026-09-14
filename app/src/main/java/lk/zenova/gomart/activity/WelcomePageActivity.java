package lk.zenova.gomart.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import lk.zenova.gomart.R;
import lk.zenova.gomart.helper.AppPreferences;

public class WelcomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_page);
        Button btnGetStart = findViewById(R.id.btnGetStart);

        btnGetStart.setOnClickListener(v -> {
            new AppPreferences(this).setWelcomeSeen(true);
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });

    }
}