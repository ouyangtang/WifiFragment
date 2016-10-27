package com.ouyang.wifi;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.test.Contact;
import com.test.NativeProxy;

import java.util.ArrayList;

/**
 * Created by sisuo on 2016/10/24.
 */
public class MainActivity extends Activity {

    private WifiSettings mWifiSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWifiSetting=new WifiSettings();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container,mWifiSetting).commit();

        new Thread(){
            @Override
            public void run() {
                super.run();
                NativeProxy proxy=new NativeProxy();
                ArrayList<Contact> list=new ArrayList<Contact>();
                proxy.getList(list);
                for(Contact c:list){
                    Log.i("ouyang"," name:"+c.name+" age:"+c.age);
                }
                Log.i("ouyang"," listSize:"+list.size());
            }
        }.start();
    }
}
