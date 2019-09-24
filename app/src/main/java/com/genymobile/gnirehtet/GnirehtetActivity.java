package com.genymobile.gnirehtet;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;

/**
 * This (invisible) activity receives the {@link #ACTION_GNIREHTET_START START} and
 * {@link #ACTION_GNIREHTET_STOP} actions from the command line.
 * <p>
 * Recent versions of Android refuse to directly start a {@link android.app.Service Service} or a
 * {@link android.content.BroadcastReceiver BroadcastReceiver}, so actions are always managed by
 * this activity.
 */
public class GnirehtetActivity extends Activity {

    private static final String TAG = GnirehtetActivity.class.getSimpleName();

    public static final String ACTION_GNIREHTET_START = "com.genymobile.gnirehtet.START";
    public static final String ACTION_GNIREHTET_STOP = "com.genymobile.gnirehtet.STOP";
    public static final String ACTION_GNIREHTET_CLIP_SET = "com.genymobile.gnirehtet.CLIP_SET";
    public static final String ACTION_GNIREHTET_CLIP_GET = "com.genymobile.gnirehtet.CLIP_GET";


    public static final String EXTRA_DNS_SERVERS = "dnsServers";
    public static final String EXTRA_ROUTES = "routes";

    private static final int VPN_REQUEST_CODE = 0;

    private VpnConfiguration config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        GnirehtetControlReceiver receiver = new GnirehtetControlReceiver();
        this.registerReceiver(receiver, new IntentFilter(ACTION_GNIREHTET_CLIP_SET));
        this.registerReceiver(receiver, new IntentFilter(ACTION_GNIREHTET_CLIP_GET));
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received request " + action);
        boolean finish = true;
        if (ACTION_GNIREHTET_START.equals(action)) {
            VpnConfiguration config = createConfig(intent);
            finish = startGnirehtet(config);
        } else if (ACTION_GNIREHTET_STOP.equals(action)) {
            stopGnirehtet();
        }

        if (finish) {
            finish();
        }
    }

    private static VpnConfiguration createConfig(Intent intent) {
        String[] dnsServers = intent.getStringArrayExtra(EXTRA_DNS_SERVERS);
        if (dnsServers == null) {
            dnsServers = new String[0];
        }
        String[] routes = intent.getStringArrayExtra(EXTRA_ROUTES);
        if (routes == null) {
            routes = new String[0];
        }
        return new VpnConfiguration(Net.toInetAddresses(dnsServers), Net.toCIDRs(routes));
    }

    private boolean startGnirehtet(VpnConfiguration config) {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent == null) {
            Log.d(TAG, "VPN was already authorized");
            // we got the permission, start the service now
            GnirehtetService.start(this, config);
            return true;
        }

        Log.w(TAG, "VPN requires the authorization from the user, requesting...");
        requestAuthorization(vpnIntent, config);
        return false; // do not finish now
    }

    private void stopGnirehtet() {
        GnirehtetService.stop(this);
    }

    private void requestAuthorization(Intent vpnIntent, VpnConfiguration config) {
        this.config = config;
        startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            GnirehtetService.start(this, config);
        }
        config = null;
        finish();
    }
}
