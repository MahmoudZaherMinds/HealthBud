/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.thoughtcrime.securesms.conversation.ConversationActivity;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.Objects;

/**
 * Activity container for starting a new conversation.
 *
 * @author Moxie Marlinspike
 */
public class NewConversationActivity extends ContactSelectionActivity {

    @SuppressWarnings("unused")
    private static final String TAG = NewConversationActivity.class.getSimpleName();
    private static final String TAB_NAME = null;
    public static String tabName = "";


    public static void start(Context context, String tab) {
        Intent starter = new Intent(context, NewConversationActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle b = new Bundle();
        b.putString(TAB_NAME, tab);
        starter.putExtras(b);
        context.startActivity(starter);
    }


    @Override
    public void onCreate(Bundle bundle, boolean ready) {
        super.onCreate(bundle, ready);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        if (Objects.equals(extras.getString(TAB_NAME), "doctors_tab")) {
            tabName = extras.getString(TAB_NAME);
        }
    }

    @Override
    public void onContactSelected(String number) {
        if (tabName.equals("doctors_tab")) {
            Recipient recipient = Recipient.from(this, Address.fromExternal(this, number), true);

            Intent intent = new Intent(this, DoctorContactProfileScreen.class);
            intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
            intent.putExtra(ConversationActivity.PHONE, recipient.getAddress());
            intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
            intent.setDataAndType(getIntent().getData(), getIntent().getType());

            long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipient);

            intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
            intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
            startActivity(intent);
            //    finish();
        } else {
            Recipient recipient = Recipient.from(this, Address.fromExternal(this, number), true);

            Intent intent = new Intent(this, ConversationActivity.class);
            //Intent intent = new Intent(this, DoctorContactProfileScreen.class);
            intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
            intent.putExtra(ConversationActivity.PHONE, recipient.getAddress());
            intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
            intent.setDataAndType(getIntent().getData(), getIntent().getType());

            long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipient);

            intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
            intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                if (tabName.equals("doctors_tab")) {
                    ConversationListActivity.start(this);
                } else
                    super.onBackPressed();
                return true;
            case R.id.menu_refresh:
                handleManualRefresh();
                return true;
            case R.id.menu_new_group:
                handleCreateGroup();
                return true;
            case R.id.menu_invite:
                handleInvite();
                return true;
        }

        return false;
    }

    private void handleManualRefresh() {
        contactsFragment.setRefreshing(true);
        onRefresh();
    }

    private void handleCreateGroup() {
        startActivity(new Intent(this, GroupCreateActivity.class));
    }

    private void handleInvite() {
        startActivity(new Intent(this, InviteActivity.class));
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.new_conversation_activity, menu);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (tabName.equals("doctors_tab")) {
            ConversationListActivity.start(this);
        } else
            super.onBackPressed();

    }
}
