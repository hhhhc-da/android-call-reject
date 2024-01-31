package com.example.callreject;

import androidx.appcompat.app.AppCompatActivity;
import com.example.callreject.CallLogReader;

import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private CallBlocker blocker;
    IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.i("myError","准备启动服务");

        // 创建 CallLogReader 实例并读取通话记录
        CallLogReader callLogReader = new CallLogReader(this);
        Map<String, Integer> callLog = callLogReader.readCallLog();
        Set<String> contacts = callLogReader.readContacts();

        Log.i("myError","共发现条目: " + callLog.size());

//        for(String key: callLog.keySet()){
//            if(callLog.get(key) < 3){
//                Log.i("myError","发现疑似骚扰电话: "+ key);
//            }
//        }

//        for(String key: contacts){
//                Log.i("myError","联系人: "+ key);
//        }

        blocker = new CallBlocker(callLog, contacts);

        registerReceiver(blocker, filter);
        Log.i("myError","开始监听来电");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销 BroadcastReceiver
        unregisterReceiver(blocker);
    }
}