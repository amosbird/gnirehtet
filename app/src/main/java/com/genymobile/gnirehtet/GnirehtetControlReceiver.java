/*
 * Copyright (C) 2017 Genymobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.genymobile.gnirehtet;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver to expose {@link #ACTION_GNIREHTET_START START} and {@link #ACTION_GNIREHTET_STOP}
 * actions.
 * <p>
 * Since {@link GnirehtetService} extends {@link VpnService}, it requires the clients to have the
 * system permission {@code android.permission.BIND_VPN_SERVICE}, which {@code shell} have not. As a
 * consequence, we cannot expose our own actions intended to be called from {@code shell} directly
 * in {@link GnirehtetService}.
 * <p>
 * Starting the VPN requires authorization from the user. If the authorization is not granted yet,
 * an {@code Intent}, returned by the system, must be sent <strong>from an Activity</strong>
 * (through {@link android.app.Activity#startActivityForResult(Intent, int)
 * startActivityForResult()}. However, if the authorization is already granted, it is better to
 * avoid starting an {@link android.app.Activity Activity} (which would be useless), since it may
 * cause (minor) side effects (like closing any open virtual keyboard). Therefore, this {@link
 * GnirehtetControlReceiver} starts an {@link android.app.Activity Activity} only when necessary.
 * <p>
 * Stopping the VPN just consists in delegating the request to {@link GnirehtetService} (which is
 * accessible from here).
 */
public class GnirehtetControlReceiver extends BroadcastReceiver {

    public static final String ACTION_GNIREHTET_CLIP_SET = "com.genymobile.gnirehtet.CLIP_SET";
    public static final String ACTION_GNIREHTET_CLIP_GET = "com.genymobile.gnirehtet.CLIP_GET";

    private static final String TAG = GnirehtetControlReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received request " + action);
        if (ACTION_GNIREHTET_CLIP_SET.equals(action)) {
            String text = intent.getStringExtra("text");
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(null, text);
            if (clipboard == null) {
                return;
            }
            clipboard.setPrimaryClip(clip);
        } else if (ACTION_GNIREHTET_CLIP_GET.equals(action)) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            Log.d(TAG, "Getting text from clipboard");
            CharSequence clip = clipboard.getText();
            if (clip != null) {
                Log.d(TAG, String.format("Clipboard text: %s", clip));
                setResultCode(Activity.RESULT_OK);
                setResultData(clip.toString());
            } else {
                setResultCode(Activity.RESULT_CANCELED);
                setResultData("");
            }
        }
    }
}
