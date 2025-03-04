/*
 * Copyright (C) 2016 Open Whisper Systems
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

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.thoughtcrime.securesms.components.webrtc.WebRtcAnswerDeclineButton;
import org.thoughtcrime.securesms.components.webrtc.WebRtcCallControls;
import org.thoughtcrime.securesms.components.webrtc.WebRtcCallScreen;
import org.thoughtcrime.securesms.crypto.storage.TextSecureIdentityKeyStore;
import org.thoughtcrime.securesms.events.WebRtcViewModel;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.service.WebRtcCallService;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.ViewUtil;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

public class WebRtcCallActivity extends Activity {

    public static final int BUSY_SIGNAL_DELAY_FINISH = 5500;
    public static final String ANSWER_ACTION = WebRtcCallActivity.class.getCanonicalName() + ".ANSWER_ACTION";
    public static final String DENY_ACTION = WebRtcCallActivity.class.getCanonicalName() + ".DENY_ACTION";
    public static final String END_CALL_ACTION = WebRtcCallActivity.class.getCanonicalName() + ".END_CALL_ACTION";
    private static final String TAG = WebRtcCallActivity.class.getSimpleName();
    private static final int STANDARD_DELAY_FINISH = 1000;
    private WebRtcCallScreen callScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.webrtc_call_activity);

        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        initializeResources();

        processIntent(getIntent());
    }


    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
        initializeScreenshotSecurity();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent");
        processIntent(intent);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void processIntent(@NonNull Intent intent) {
        if (ANSWER_ACTION.equals(intent.getAction())) {
            handleAnswerCall();
        } else if (DENY_ACTION.equals(intent.getAction())) {
            handleDenyCall();
        } else if (END_CALL_ACTION.equals(intent.getAction())) {
            handleEndCall();
        }
    }

    private void initializeScreenshotSecurity() {
        if (TextSecurePreferences.isScreenSecurityEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void initializeResources() {
        callScreen = ViewUtil.findById(this, R.id.callScreen);
        callScreen.setHangupButtonListener(new HangupButtonListener());
        callScreen.setIncomingCallActionListener(new IncomingCallActionListener());
        callScreen.setAudioMuteButtonListener(new AudioMuteButtonListener());
        callScreen.setVideoMuteButtonListener(new VideoMuteButtonListener());
        callScreen.setCameraFlipButtonListener(new CameraFlipButtonListener());
        callScreen.setSpeakerButtonListener(new SpeakerButtonListener());
        callScreen.setBluetoothButtonListener(new BluetoothButtonListener());
    }

    private void handleSetMuteAudio(boolean enabled) {
        Intent intent = new Intent(this, WebRtcCallService.class);
        intent.setAction(WebRtcCallService.ACTION_SET_MUTE_AUDIO);
        intent.putExtra(WebRtcCallService.EXTRA_MUTE, enabled);
        startService(intent);
    }

    private void handleSetMuteVideo(boolean muted) {
        Intent intent = new Intent(this, WebRtcCallService.class);
        intent.setAction(WebRtcCallService.ACTION_SET_MUTE_VIDEO);
        intent.putExtra(WebRtcCallService.EXTRA_MUTE, muted);
        startService(intent);
    }

    private void handleFlipCamera() {
        Intent intent = new Intent(this, WebRtcCallService.class);
        intent.setAction(WebRtcCallService.ACTION_FLIP_CAMERA);
        startService(intent);
    }

    private void handleAnswerCall() {
        WebRtcViewModel event = EventBus.getDefault().getStickyEvent(WebRtcViewModel.class);

        if (event != null) {
            Permissions.with(this)
                    .request(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                    .ifNecessary()
                    .withRationaleDialog(getString(R.string.WebRtcCallActivity_to_answer_the_call_from_s_give_signal_access_to_your_microphone, event.getRecipient().toShortString()),
                            R.drawable.ic_mic_white_48dp, R.drawable.ic_videocam_white_48dp)
                    .withPermanentDenialDialog(getString(R.string.WebRtcCallActivity_signal_requires_microphone_and_camera_permissions_in_order_to_make_or_receive_calls))
                    .onAllGranted(() -> {
                        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_answering));

                        Intent intent = new Intent(this, WebRtcCallService.class);
                        intent.setAction(WebRtcCallService.ACTION_ANSWER_CALL);
                        startService(intent);
                    })
                    .onAnyDenied(this::handleDenyCall)
                    .execute();
        }
    }

    private void handleDenyCall() {
        WebRtcViewModel event = EventBus.getDefault().getStickyEvent(WebRtcViewModel.class);

        if (event != null) {
            Intent intent = new Intent(this, WebRtcCallService.class);
            intent.setAction(WebRtcCallService.ACTION_DENY_CALL);
            startService(intent);

            callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_ending_call));
            delayedFinish();
        }
    }

    private void handleEndCall() {
        Log.i(TAG, "Hangup pressed, handling termination now...");
        Intent intent = new Intent(WebRtcCallActivity.this, WebRtcCallService.class);
        intent.setAction(WebRtcCallService.ACTION_LOCAL_HANGUP);
        startService(intent);
    }

    private void handleIncomingCall(@NonNull WebRtcViewModel event) {
        callScreen.setIncomingCall(event.getRecipient());
    }

    private void handleOutgoingCall(@NonNull WebRtcViewModel event) {
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_dialing));
    }

    private void handleTerminate(@NonNull Recipient recipient /*, int terminationType */) {
        Log.i(TAG, "handleTerminate called");

        callScreen.setActiveCall(recipient, getString(R.string.RedPhone_ending_call));
        EventBus.getDefault().removeStickyEvent(WebRtcViewModel.class);

        delayedFinish();
    }

    private void handleCallRinging(@NonNull WebRtcViewModel event) {
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_ringing));
    }

    private void handleCallBusy(@NonNull WebRtcViewModel event) {
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_busy));

        delayedFinish(BUSY_SIGNAL_DELAY_FINISH);
    }

    private void handleCallConnected(@NonNull WebRtcViewModel event) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_connected), "", event.getLocalRenderer(), event.getRemoteRenderer());
    }

    private void handleRecipientUnavailable(@NonNull WebRtcViewModel event) {
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_recipient_unavailable));
        delayedFinish();
    }

    private void handleServerFailure(@NonNull WebRtcViewModel event) {
        callScreen.setActiveCall(event.getRecipient(), getString(R.string.RedPhone_network_failed));
        delayedFinish();
    }

    private void handleNoSuchUser(final @NonNull WebRtcViewModel event) {
        if (isFinishing())
            return; // XXX Stuart added this check above, not sure why, so I'm repeating in ignorance. - moxie
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.RedPhone_number_not_registered);
        dialog.setIconAttribute(R.attr.dialog_alert_icon);
        dialog.setMessage(R.string.RedPhone_the_number_you_dialed_does_not_support_secure_voice);
        dialog.setCancelable(true);
        dialog.setPositiveButton(R.string.RedPhone_got_it, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WebRtcCallActivity.this.handleTerminate(event.getRecipient());
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                WebRtcCallActivity.this.handleTerminate(event.getRecipient());
            }
        });
        dialog.show();
    }

    private void handleUntrustedIdentity(@NonNull WebRtcViewModel event) {
        final IdentityKey theirIdentity = event.getIdentityKey();
        final Recipient recipient = event.getRecipient();

        callScreen.setUntrustedIdentity(recipient, theirIdentity);
        callScreen.setAcceptIdentityListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (SESSION_LOCK) {
                    TextSecureIdentityKeyStore identityKeyStore = new TextSecureIdentityKeyStore(WebRtcCallActivity.this);
                    identityKeyStore.saveIdentity(new SignalProtocolAddress(recipient.getAddress().serialize(), 1), theirIdentity, true);
                }

                Intent intent = new Intent(WebRtcCallActivity.this, WebRtcCallService.class);
                intent.putExtra(WebRtcCallService.EXTRA_REMOTE_ADDRESS, recipient.getAddress());
                intent.setAction(WebRtcCallService.ACTION_OUTGOING_CALL);
                startService(intent);
            }
        });

        callScreen.setCancelIdentityButton(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleTerminate(recipient);
            }
        });
    }

    private void delayedFinish() {
        delayedFinish(STANDARD_DELAY_FINISH);
    }

    private void delayedFinish(int delayMillis) {
        callScreen.postDelayed(new Runnable() {
            public void run() {
                WebRtcCallActivity.this.finish();
            }
        }, delayMillis);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final WebRtcViewModel event) {
        Log.i(TAG, "Got message from service: " + event);

        switch (event.getState()) {
            case CALL_CONNECTED:
                handleCallConnected(event);
                break;
            case NETWORK_FAILURE:
                handleServerFailure(event);
                break;
            case CALL_RINGING:
                handleCallRinging(event);
                break;
            case CALL_DISCONNECTED:
                handleTerminate(event.getRecipient());
                break;
            case NO_SUCH_USER:
                handleNoSuchUser(event);
                break;
            case RECIPIENT_UNAVAILABLE:
                handleRecipientUnavailable(event);
                break;
            case CALL_INCOMING:
                handleIncomingCall(event);
                break;
            case CALL_OUTGOING:
                handleOutgoingCall(event);
                break;
            case CALL_BUSY:
                handleCallBusy(event);
                break;
            case UNTRUSTED_IDENTITY:
                handleUntrustedIdentity(event);
                break;
        }

        callScreen.setRemoteVideoEnabled(event.isRemoteVideoEnabled());
        callScreen.updateAudioState(event.isBluetoothAvailable(), event.isMicrophoneEnabled());
        callScreen.setControlsEnabled(event.getState() != WebRtcViewModel.State.CALL_INCOMING);
        callScreen.setLocalVideoState(event.getLocalCameraState());
    }

    private class HangupButtonListener implements WebRtcCallScreen.HangupButtonListener {
        public void onClick() {
            handleEndCall();
        }
    }

    private class AudioMuteButtonListener implements WebRtcCallControls.MuteButtonListener {
        @Override
        public void onToggle(boolean isMuted) {
            WebRtcCallActivity.this.handleSetMuteAudio(isMuted);
        }
    }

    private class VideoMuteButtonListener implements WebRtcCallControls.MuteButtonListener {
        @Override
        public void onToggle(boolean isMuted) {
            WebRtcCallActivity.this.handleSetMuteVideo(isMuted);
        }
    }

    private class CameraFlipButtonListener implements WebRtcCallControls.CameraFlipButtonListener {
        @Override
        public void onToggle() {
            WebRtcCallActivity.this.handleFlipCamera();
        }
    }

    private class SpeakerButtonListener implements WebRtcCallControls.SpeakerButtonListener {
        @Override
        public void onSpeakerChange(boolean isSpeaker) {
            AudioManager audioManager = ServiceUtil.getAudioManager(WebRtcCallActivity.this);
            audioManager.setSpeakerphoneOn(isSpeaker);

            if (isSpeaker && audioManager.isBluetoothScoOn()) {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
        }
    }

    private class BluetoothButtonListener implements WebRtcCallControls.BluetoothButtonListener {
        @Override
        public void onBluetoothChange(boolean isBluetooth) {
            AudioManager audioManager = ServiceUtil.getAudioManager(WebRtcCallActivity.this);

            if (isBluetooth) {
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
            } else {
                audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
            }
        }
    }

    private class IncomingCallActionListener implements WebRtcAnswerDeclineButton.AnswerDeclineListener {
        @Override
        public void onAnswered() {
            WebRtcCallActivity.this.handleAnswerCall();
        }

        @Override
        public void onDeclined() {
            WebRtcCallActivity.this.handleDenyCall();
        }
    }

}