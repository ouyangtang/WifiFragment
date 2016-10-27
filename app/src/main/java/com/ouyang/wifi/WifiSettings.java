/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ouyang.wifi;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 */
public class WifiSettings extends Fragment
        implements DialogInterface.OnClickListener  {
    private static final String TAG = "WifiSettings";
    private static final int MENU_ID_WPS_PBC = Menu.FIRST;
    private static final int MENU_ID_WPS_PIN = Menu.FIRST + 1;
    private static final int MENU_ID_P2P = Menu.FIRST + 2;
    private static final int MENU_ID_ADD_NETWORK = Menu.FIRST + 3;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;

    private static final int WIFI_DIALOG_ID = 1;
    private static final int WPS_PBC_DIALOG_ID = 2;
    private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WIFI_SKIPPED_DIALOG_ID = 4;
    private static final int WIFI_AND_MOBILE_SKIPPED_DIALOG_ID = 5;

    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;

    // Instance state keys
    private static final String SAVE_DIALOG_EDIT_MODE = "edit_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    // Activity result when pressing the Skip button
    private static final int RESULT_SKIP = Activity.RESULT_FIRST_USER;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;

    private WifiManager mWifiManager;
    /*  private WifiManager.ActionListener mConnectListener;
      private WifiManager.ActionListener mSaveListener;
      private WifiManager.ActionListener mForgetListener;*/
    private boolean mP2pSupported;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private DetailedState mLastState;
    private WifiInfo mLastInfo;

    private final AtomicBoolean mConnected = new AtomicBoolean(false);

    private WifiDialog mDialog;

   // private WifiPswDialog mPswDialog;

    private TextView mEmptyView;

    /* Used in Wifi Setup context */

    // this boolean extra specifies whether to disable the Next button when not connected
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    // this boolean extra specifies whether to auto finish when connection is established
    private static final String EXTRA_AUTO_FINISH_ON_CONNECT = "wifi_auto_finish_on_connect";

    // this boolean extra shows a custom button that we can control
    protected static final String EXTRA_SHOW_CUSTOM_BUTTON = "wifi_show_custom_button";

    // show a text regarding data charges when wifi connection is required during setup wizard
    protected static final String EXTRA_SHOW_WIFI_REQUIRED_INFO = "wifi_show_wifi_required_info";

    // this boolean extra is set if we are being invoked by the Setup Wizard
    private static final String EXTRA_IS_FIRST_RUN = "firstRun";

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // should activity finish once we have a connection?
    private boolean mAutoFinishOnConnection;

    // Save the dialog details
    private boolean mDlgEdit;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;

    // the action bar uses a different set of controls for Setup Wizard

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        mScanner = new Scanner();
    }

    @Override
    public void onCreate(Bundle icicle) {
        // Set this flag early, as it's needed by getHelpResource(), which is called by super

        super.onCreate(icicle);
    }

    private LinearLayout listView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
            View view =inflater.inflate(R.layout.fragment_wifi2,null);
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            view.setLayoutParams(params);
            listView= (LinearLayout) view.findViewById(R.id.wifi_list_container);
            return view;
    }


    private WifiManager.ActionListener mConnectListener;

    private WifiManager.ActionListener mSaveListener;

    private WifiManager.ActionListener mForgetListener;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mP2pSupported = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mConnectListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
                                   }
                                   @Override
                                   public void onFailure(int reason) {
                                       Activity activity = getActivity();
                                       if (activity != null) {
                                          /* Toast.makeText(activity,
                                               "connect failed",
                                                Toast.LENGTH_SHORT).show();*/
                                       }
                                   }
                               };

        mSaveListener = new WifiManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                }
                                @Override
                                public void onFailure(int reason) {
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                       /* Toast.makeText(activity,
                                            "failed to save",
                                            Toast.LENGTH_SHORT).show();*/
                                    }
                                }
                            };

        mForgetListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
                                   }
                                   @Override
                                   public void onFailure(int reason) {
                                       Activity activity = getActivity();
                                       if (activity != null) {
                                         /*  Toast.makeText(activity,
                                               "failed forget",
                                               Toast.LENGTH_SHORT).show();*/
                                       }
                                   }
                               };
        if (savedInstanceState != null
                && savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
            mDlgEdit = savedInstanceState.getBoolean(SAVE_DIALOG_EDIT_MODE);
            mAccessPointSavedState = savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
        }

        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();

        // first if we're supposed to finish once we have a connection
        mAutoFinishOnConnection = intent.getBooleanExtra(EXTRA_AUTO_FINISH_ON_CONNECT, false);

        if (mAutoFinishOnConnection) {
            // Hide the next button
            final ConnectivityManager connectivity = (ConnectivityManager)
                    activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null
                    && connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                activity.setResult(Activity.RESULT_OK);
                activity.finish();
                return;
            }
        }

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (mEnableNextOnConnection) {
        }

        // On/off switch is hidden for Setup Wizard

            Switch actionBarSwitch = (Switch) getView().findViewById(R.id.wifi_oper2);

          //  actionBarSwitch.setTrackResource(R.drawable.switch_statue_bg);
          //  actionBarSwitch.setThumbResource(R.drawable.switch_thumb);
            /*actionBarSwitch.setSwitchMinWidth(150);
            actionBarSwitch.setTextOff("");
            actionBarSwitch.setTextOn("");
            actionBarSwitch.setThumbTextPadding(40);
            actionBarSwitch.setSwitchPadding(0);*/

         //   actionBarSwitch.setTrackDrawable(R.drawable.switch_off);

            if (activity instanceof PreferenceActivity) {
                PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
                if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                    final int padding = 0;
                    //actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
                    activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                            ActionBar.DISPLAY_SHOW_CUSTOM);
                  /*  activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.END));*/
                }
            }
           // FrameLayout switchContainer= (FrameLayout) getView().findViewById(R.id.wifi_switch_container);
           /* switchContainer.addView(actionBarSwitch,new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.RIGHT|Gravity.CENTER_VERTICAL));*/
        mWifiEnabler = new WifiEnabler(activity, actionBarSwitch);
        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        //getListView().setEmptyView(mEmptyView);
        setHasOptionsMenu(true);
    }


    @Override
    public void onResume() {
        super.onResume();
        SupplicantState suppState=mWifiManager.getConnectionInfo().getSupplicantState();
        mLastState =mWifiManager.getConnectionInfo().getDetailedStateOf(suppState);
        mLastInfo=mWifiManager.getConnectionInfo();
        if (mWifiEnabler != null) {
            mWifiEnabler.resume();
        }
        getActivity().registerReceiver(mReceiver, mFilter);
        updateAccessPoints();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
        mScanner.pause();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
       /* if (mDialog != null && mDialog.isShowing()) {
            outState.putBoolean(SAVE_DIALOG_EDIT_MODE, mDlgEdit);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }*/
    }


    private void showDialog(AccessPoint accessPoint, boolean edit,int type){
        if(mDialog!=null&&mDialog.isShowing()){
           mDialog.dismiss();
            mDialog=null;
        }
        mDlgAccessPoint=accessPoint;
        AccessPoint ap = mDlgAccessPoint; // For manual launch
        if (ap == null) { // For re-launch from saved state
            if (mAccessPointSavedState != null) {
                ap = new AccessPoint(getActivity(), mAccessPointSavedState,inflateBindView());
                // For repeated orientation changes
                mDlgAccessPoint = ap;
                // Reset the saved access point data
                mAccessPointSavedState = null;
            }
        }
        // If it's still null, fine, it's for Add Network
        mSelectedAccessPoint = ap;
        mDlgEdit = edit;
        mDialog = new WifiDialog(getActivity(), wifiBtnListener, ap, mDlgEdit,type);
        if(mDialog!=null){
            mDialog.show();
        }
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;
        mDlgEdit = edit;
        showDialog(WIFI_DIALOG_ID);
    }

    private  void showDialog(int id){
       // mDialog= (WifiDialog) createDialog(id);
      //  mDialog.show();
    }

    private void removeDialog(int type){

    }


    private OnClickListener wifiBtnListener=new OnClickListener(){

        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.wifi_dialog_dismis:
                    closeDialog();
                    break;
                case R.id.dialog_wifi_forget:
                    closeDialog();
                    forget();
                    break;
                case R.id.wifi_dialog_poss:
                       WifiConfigController configController = mDialog.getController();
                       // submitConfig(configController);
                       closeDialog();
                       submit(configController);
                    break;
            }
        }
    };


    private void closeDialog(){
        if(mDialog!=null&&mDialog.isShowing()){
            mDialog.dismiss();
            mDialog=null;
        }
    }


    void submitConfig(WifiConfigController configController) {
        final WifiConfiguration config = configController.getConfig();
        if (config == null) {
            Log.i(TAG," config==null:");
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.networkId != -1) {
                  /*  mWifiManager.connect(mSelectedAccessPoint.networkId,
                     mConnectListener);
                  */
                Log.i(TAG," config==null networkid:"+mSelectedAccessPoint.networkId);
                int id=mWifiManager.addNetwork(config);
                mWifiManager.enableNetwork(id,true);
            }
        } else if (config.networkId != -1) {
            Log.i(TAG, " networkid !=-1 point:"+(mSelectedAccessPoint==null));
            if (mSelectedAccessPoint != null) {
                //mWifiManager.save(config, mSaveListener);
                mWifiManager.saveConfiguration();
            }
        } else {
            Log.i(TAG," isEdit:"+(configController.isEdit()));
            if (configController.isEdit()) {
                // mWifiManager.save(config, mSaveListener);
                int id=mWifiManager.addNetwork(config);
                mWifiManager.enableNetwork(id,true);
                mWifiManager.saveConfiguration();
            } else {
                //  mWifiManager.connect(config, mConnectListener);
                int id=mWifiManager.addNetwork(config);
                mWifiManager.enableNetwork(id,true);
            }
        }
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

   /* public Dialog createDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                AccessPoint ap = mDlgAccessPoint; // For manual launch
                if (ap == null) { // For re-launch from saved state
                    if (mAccessPointSavedState != null) {
                        ap = new AccessPoint(getActivity(), mAccessPointSavedState,inflateBindView());
                        // For repeated orientation changes
                        mDlgAccessPoint = ap;
                        // Reset the saved access point data
                        mAccessPointSavedState = null;
                    }
                }
                // If it's still null, fine, it's for Add Network
                mSelectedAccessPoint = ap;
                mDialog = new WifiDialog(getActivity(), this, ap, mDlgEdit);
                return mDialog;
            default:
                return null;
        }
    }*/

    /**
     * Shows the latest access points available with supplimental information like
     * the strength of network and the security for it.
     */
    private List<AccessPoint> scanResults=new ArrayList<AccessPoint>();

    private void updateAccessPoints() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) return;
        final int wifiState = mWifiManager.getWifiState();
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                // AccessPoints are automatically sorted with TreeSet.
                final Collection<AccessPoint> accessPoints = constructAccessPoints();
                //  getPreferenceScreen().removeAll();
                scanResults.clear();
                scanResults.addAll(accessPoints);

                removeAllItemView();
                if(accessPoints.size() == 0) {
                    Log.i(TAG,"seraching for wifi");
                    //addMessagePreference(R.string.wifi_empty_list_wifi_on);
                }
             //   Log.i("ouyang"," *****************************************");
                for (AccessPoint accessPoint : accessPoints) {
                    if(accessPoint.getLevel()!=-1){
                        if(accessPoint.getState()!=null&&false){

                        }else {
                            addWifiItemView(accessPoint);
                        }
                       // Log.i("ouyang","  wifi ssid:"+accessPoint.ssid+"  level:"+accessPoint.getLevel());
                    }
                }
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                // getPreferenceScreen().removeAll();
                removeAllItemView();
                break;

            case WifiManager.WIFI_STATE_DISABLING:
               // Toast.makeText(getActivity(),"Turning off Wi",Toast.LENGTH_SHORT).show();
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                break;
        }
    }


    private void addWifiItemView(AccessPoint point){
        point.onBindView();
        point.getBindView().setTag(point);
        point.getBindView().setOnClickListener(wifiItemClicklintener);
        point.getBindView().setOnLongClickListener(longClickListener);
        listView.addView(point.getBindView());
    }

    private View inflateBindView(){
        View view =getActivity().getLayoutInflater().inflate(R.layout.layout_wifi_item,null);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,88);
        params.gravity=Gravity.CENTER_HORIZONTAL;
        view.setLayoutParams(params);
        return view;
    }



    private void removeAllItemView(){
        listView.removeAllViews();
    }

    private View.OnLongClickListener longClickListener=new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            mSelectedAccessPoint = (AccessPoint) view.getTag();
            if(mSelectedAccessPoint.getConfig()!=null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.setting_wifi_ssid_delete_ask_info));
                builder.setPositiveButton(getString(R.string.dialog_button_confir), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //  boolean flag = mWifiUtil.removeWifi(wifiID);
                        mWifiManager.forget(mSelectedAccessPoint.networkId, mForgetListener);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
            return true;
        }
    };

    private OnClickListener wifiItemClicklintener=new OnClickListener(){

        @Override
        public void onClick(View view) {
            mSelectedAccessPoint = (AccessPoint) view.getTag();
            /** Bypass dialog for unsecured, unsaved networks */
            if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE &&
                    mSelectedAccessPoint.networkId == -1) {
                mSelectedAccessPoint.generateOpenNetworkConfig();
                mWifiManager.connect(mSelectedAccessPoint.getConfig(), mConnectListener);
            } else {
                if(mSelectedAccessPoint.getConfig()==null) {
                    showDialog(mSelectedAccessPoint,false, 1);
                }else{
                   showDialog(mSelectedAccessPoint,false,2);
                 /*   if(mSelectedAccessPoint.getConfig().networkId!=-1){
                        mWifiManager.connect(mSelectedAccessPoint.getConfig().networkId,mConnectListener);
                    }*/
                    Log.i("setting","  conntectId:"+mSelectedAccessPoint.getConfig().networkId);
                }
            }
        }
    };

    private void setOffMessage() {
        if (mEmptyView != null) {
          /*  mEmptyView.setText(R.string.wifi_empty_list_wifi_off);
            if (Settings.Global.getInt(getActivity().getContentResolver(),
                    Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1) {
                mEmptyView.append("\n\n");
                int resId;
                if (Settings.Secure.isLocationProviderEnabled(getActivity().getContentResolver(),
                        LocationManager.NETWORK_PROVIDER)) {
                    resId = R.string.wifi_scan_notify_text_location_on;
                } else {
                    resId = R.string.wifi_scan_notify_text_location_off;
                }
                CharSequence charSeq = getText(resId);
                mEmptyView.append(charSeq);
            }*/
        }
        //  getPreferenceScreen().removeAll();
        removeAllItemView();
    }

    private void addMessagePreference(int messageId) {
        if (mEmptyView != null) mEmptyView.setText(messageId);
        // getPreferenceScreen().removeAll();
        removeAllItemView();
    }

    /** Returns sorted list of access points */
    private List<AccessPoint> constructAccessPoints() {
        ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        /** Lookup table to more quickly update AccessPoints by only considering objects with the
         * correct SSID.  Maps SSID -> List of AccessPoints with the given SSID.  */
        Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = new AccessPoint(getActivity(), config,inflateBindView());
                accessPoint.update(mLastInfo, mLastState);
                accessPoints.add(accessPoint);
                apMap.put(accessPoint.ssid, accessPoint);
               // Log.i("ouyang","  config ssid:"+accessPoint.ssid);
            }
        }

        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {

                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }
               // Log.i("ouyang","  scanResult ssid:"+result.SSID);
                boolean found = false;
                for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
                    if (accessPoint.update(result))
                        found = true;
                }
                if (!found) {
                    AccessPoint accessPoint = new AccessPoint(getActivity(), result,inflateBindView());
                    accessPoints.add(accessPoint);
                    apMap.put(accessPoint.ssid, accessPoint);
                }
            }
        }
        // Pre-sort accessPoints to speed preference insertion
        Collections.sort(accessPoints);
        return accessPoints;
    }

    /** A restricted multimap for use in constructAccessPoints */
    private class Multimap<K,V> {
        private final HashMap<K,List<V>> store = new HashMap<K,List<V>>();
        /** retrieve a non-null list of values with key K */
        List<V> getAll(K key) {
            List<V> values = store.get(key);
            return values != null ? values : Collections.<V>emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<V>(3);
                store.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    public static final String CONFIGURED_NETWORKS_CHANGED_ACTION =
            "android.net.wifi.CONFIGURED_NETWORKS_CHANGE";
    public static final String LINK_CONFIGURATION_CHANGED_ACTION =
            "android.net.wifi.LINK_CONFIGURATION_CHANGED";

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG," wifi Action:"+action);
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            //Ignore supplicant state changes when network is connected
            //TODO: we should deprecate SUPPLICANT_STATE_CHANGED_ACTION and
            //introduce a broadcast that combines the supplicant and network
            //network state change events so the apps dont have to worry about
            //ignoring supplicant state change when network is connected
            //to get more fine grained information.
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(
                    WifiManager.EXTRA_NEW_STATE);
            if (!mConnected.get() && isHandshakeState(state)) {
                updateConnectionState(WifiInfo.getDetailedStateOf(state));
            } else {
                // During a connect, we may have the supplicant
                // state change affect the detailed network state.
                // Make sure a lost connection is updated as well.
                updateConnectionState(null);
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            updateAccessPoints();
            updateConnectionState(info.getDetailedState());
            if (mAutoFinishOnConnection && info.isConnected()) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
                }
                return;
            }
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        }
    }

    public static boolean isHandshakeState(SupplicantState state) {
        switch(state) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                return true;
            case COMPLETED:
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    private void updateConnectionState(DetailedState state) {
        /*sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }
        if (state == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }
        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }
       Log.i("setting","  updateConnectionState***************");
        for(int i=0;i<scanResults.size();i++){
            AccessPoint point=scanResults.get(i);
            point.update(mLastInfo,mLastState);
        }
       /* for (int i = getPreferenceScreen().getPreferenceCount() - 1; i >= 0; --i) {
            // Maybe there's a WifiConfigPreference
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof AccessPoint) {
                final AccessPoint accessPoint = (AccessPoint) preference;
                accessPoint.update(mLastInfo, mLastState);
            }
        }*/
    }



    private void updateWifiState(int state) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
                mScanner.resume();
                return; // not break, to avoid the call to pause() below

            case WifiManager.WIFI_STATE_ENABLING:
                addMessagePreference(R.string.wifi_starting);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                break;
        }
        mLastInfo = null;
        mLastState = null;
        mScanner.pause();
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Activity activity = getActivity();
                if (activity != null) {
                  /*  Toast.makeText(activity, "Can not scan for networks",
                            Toast.LENGTH_LONG).show();*/
                }
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            forget();
        } else if (button == WifiDialog.BUTTON_SUBMIT) {
            if (mDialog != null) {
              // submit(mDialog.getController());
            }
        }
    }

    void submit(WifiConfigController configController) {
        final WifiConfiguration config = configController.getConfig();
        if (config == null) {
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.networkId != WifiConfiguration.INVALID_NETWORK_ID) {
                mWifiManager.connect(mSelectedAccessPoint.networkId,
                        mConnectListener);
                Log.i(TAG,"  config==null networkid:"+mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != WifiConfiguration.INVALID_NETWORK_ID) {
            if (mSelectedAccessPoint != null) {
                mWifiManager.save(config, mSaveListener);
                Log.i(TAG, "  config.networkId" + config.networkId);
            }
        } else {
            if (configController.isEdit()) {
                mWifiManager.save(config, mSaveListener);
                Log.i(TAG, "  configController.isEdit" );
            } else {
                mWifiManager.connect(config, mConnectListener);
                Log.i(TAG, " not    configController.isEdit ssid:"+config.SSID+" key:"+config.preSharedKey);
            }
        }
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

   /* *//* package */
   void forget() {
        if (mSelectedAccessPoint.networkId == -1) {
            // Should not happen, but a monkey seems to triger it
            Log.e(TAG, "Failed to forget invalid network " + mSelectedAccessPoint.getConfig());
            return;
        }
        mWifiManager.forget(mSelectedAccessPoint.networkId, mForgetListener);
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
        // We need to rename/replace "Next" button in wifi setup context.
    }

    /**
     * Refreshes acccess points and ask Wifi module to scan networks again.
     */
    /* package */ void refreshAccessPoints() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        // getPreferenceScreen().removeAll();
        removeAllItemView();
    }
    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showDialog(null, true);
    }

    /**
     * Requests wifi module to pause wifi scan. May be ignored when the module is disabled.
     */
    /* package */ void pauseWifiScan() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.pause();
        }
    }
    /**
     * Requests wifi module to resume wifi scan. May be ignored when the module is disabled.
     */
    /* package */ void resumeWifiScan() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
    }

}