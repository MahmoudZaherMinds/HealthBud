/**
 * Copyright (C) 2011 Whisper Systems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.os.AsyncTask;
import android.os.Bundle;

import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.VersionTracker;

/**
 * Activity for creating a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseCreateActivity extends PassphraseActivity {

    public PassphraseCreateActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_passphrase_activity);

        initializeResources();
    }

    private void initializeResources() {
        new SecretGenerator().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
    }

    @Override
    protected void cleanup() {
        System.gc();
    }

    private class SecretGenerator extends AsyncTask<String, Void, Void> {
        private MasterSecret masterSecret;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(String... params) {
            String passphrase = params[0];
            masterSecret = MasterSecretUtil.generateMasterSecret(PassphraseCreateActivity.this, passphrase);

            MasterSecretUtil.generateAsymmetricMasterSecret(PassphraseCreateActivity.this, masterSecret);
            IdentityKeyUtil.generateIdentityKeys(PassphraseCreateActivity.this);
            VersionTracker.updateLastSeenVersion(PassphraseCreateActivity.this);

            TextSecurePreferences.setLastExperienceVersionCode(PassphraseCreateActivity.this, Util.getCanonicalVersionCode());
            TextSecurePreferences.setPasswordDisabled(PassphraseCreateActivity.this, true);
            TextSecurePreferences.setReadReceiptsEnabled(PassphraseCreateActivity.this, true);
            TextSecurePreferences.setTypingIndicatorsEnabled(PassphraseCreateActivity.this, true);
            TextSecurePreferences.setHasSeenWelcomeScreen(PassphraseCreateActivity.this, false);

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            setMasterSecret(masterSecret);
        }
    }
}
