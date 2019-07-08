package org.thoughtcrime.securesms.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.dd.CircularProgressButton;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.GenaralUtils.Constants;
import org.thoughtcrime.securesms.util.GenaralUtils.GenaralUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnboardingActivity extends AppCompatActivity {
    public static final String NEXT_INTENT = "next_intent";
    @BindView(R.id.et_specialization)
    EditText etSpecialization;
    @BindView(R.id.et_qualifications)
    EditText etQualifications;
    @BindView(R.id.et_clinic_address)
    EditText etClinicAddress;
    @BindView(R.id.btn_finish)
    CircularProgressButton btnFinish;
    private Intent nextIntent;


    public static void start(Context context) {
        Intent starter = new Intent(context, OnboardingActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        ButterKnife.bind(this);


        this.nextIntent = getIntent().getParcelableExtra(NEXT_INTENT);


    }


    @OnClick({R.id.et_specialization, R.id.btn_finish})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.et_specialization:
                GenaralUtils.showLongToast(this, "Not Completed");
                break;
            case R.id.btn_finish:
                finishClick();
                break;
        }
    }

    private void finishClick() {
        if (GenaralUtils.getText(etSpecialization).isEmpty() || GenaralUtils.getText(etSpecialization).isEmpty() || GenaralUtils.getText(etSpecialization).isEmpty()) {
            GenaralUtils.showLongToast(this, "plz enter all feilds data");
        } else {

            GenaralUtils.cacheString(OnboardingActivity.this, Constants.USER_SPECIALIZATION, GenaralUtils.getText(etSpecialization));
            GenaralUtils.cacheString(OnboardingActivity.this, Constants.USER_QUALIFICATION, GenaralUtils.getText(etQualifications));
            GenaralUtils.cacheString(OnboardingActivity.this, Constants.USER_ID, GenaralUtils.getText(etClinicAddress));


            this.btnFinish.setIndeterminateProgressMode(true);
            this.btnFinish.setProgress(50);
            if (nextIntent != null) startActivity(nextIntent);
            finish();
        }
    }
}
