package org.thoughtcrime.securesms.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.R;

public class OnboardingActivity extends AppCompatActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, OnboardingActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);


    }
}
