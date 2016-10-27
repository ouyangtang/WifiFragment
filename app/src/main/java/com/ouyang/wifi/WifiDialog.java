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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

class WifiDialog extends Dialog implements WifiConfigUiBase {

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

    private final boolean mEdit;
    private final View.OnClickListener mListener;
    private final AccessPoint mAccessPoint;
    private WifiConfigController mController;
    private int type=1;
    private Button cancelButton;
    private Button postButton;
    private Button forgetButton;
    private EditText editText;
    private TextView titleText;
    private Context mContext;



    public WifiDialog(Context context, View.OnClickListener listener,
            AccessPoint accessPoint, boolean edit,int type) {
        super(context);
        mContext=context;
        mEdit = edit;
        mListener = listener;
        mAccessPoint = accessPoint;
        this.type=type;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    @Override
    public boolean isEdit() {
        return false;
    }

    @Override
    public void setSubmitButton(CharSequence text) {

    }

    @Override
    public void setForgetButton(CharSequence text) {

    }

    @Override
    public void setCancelButton(CharSequence text) {

    }

    @Override
    public Button getSubmitButton() {
        return null;
    }

    @Override
    public Button getForgetButton() {
        return null;
    }

    @Override
    public Button getCancelButton() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
       // getWindow().setBackgroundDrawableResource(R.drawable.wifi_dialog_bg);
        setContentView(R.layout.wifi_config_dialog);
        mController = new WifiConfigController(this, getWindow().getDecorView(), mAccessPoint, mEdit,type);
        editText = (EditText) findViewById(R.id.wifi_dialog_pass);
        editText.addTextChangedListener(textWatcher);
        cancelButton = (Button) findViewById(R.id.wifi_dialog_dismis);
        postButton = (Button) findViewById(R.id.wifi_dialog_poss);
        cancelButton.setOnClickListener(mListener);
        postButton.setOnClickListener(mListener);
        forgetButton= (Button) findViewById(R.id.dialog_wifi_forget);
        forgetButton.setOnClickListener(mListener);
        if(type==1){
            forgetButton.setVisibility(View.GONE);
        }else if(type==2){
            forgetButton.setVisibility(View.VISIBLE);
        }
        titleText= (TextView) findViewById(R.id.dialog_wifi_title_ssid);
        titleText.setText(mAccessPoint.ssid);
    }



    @Override
    public void dismiss() {
        super.dismiss();
    }

    public EditText getPasswordInput(){
        return editText;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // isPressedOK=false;
           // isPassTextChange=true;
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };



    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
