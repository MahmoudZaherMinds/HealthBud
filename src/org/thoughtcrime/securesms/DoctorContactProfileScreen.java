package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.thoughtcrime.securesms.conversation.ConversationActivity;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DoctorContactProfileScreen extends AppCompatActivity {

    @BindView(R.id.chatBtn)
    Button chatBtn;
    @BindView(R.id.callBtn)
    Button callBtn;


    String address, number, txt, threadId, typeExtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_contact_profile_screen);
        ButterKnife.bind(this);

        // to get signal id to use their data to make call and chat
        Bundle extras = getIntent().getExtras();
        assert extras != null;
        if (extras.containsKey(ConversationActivity.PHONE)) {
            Object extrasData = extras.get(ConversationActivity.PHONE);
            assert extrasData != null;
            number = extrasData.toString();
        }


    }

    @OnClick({R.id.chatBtn, R.id.callBtn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.chatBtn:
                callConversationActivity();
                break;
            case R.id.callBtn:
                Toast.makeText(this, "not completed", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void callConversationActivity() {
        Recipient recipient = Recipient.from(this, Address.fromExternal(this, number), true);

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
        intent.setDataAndType(getIntent().getData(), getIntent().getType());

        long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipient);

        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        startActivity(intent);
    }
}
