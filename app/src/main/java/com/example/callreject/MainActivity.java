package com.example.callreject;

import androidx.appcompat.app.AppCompatActivity;
import com.example.callreject.CallLogReader;

import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private CallBlocker blocker;
    private CallBlockerPro blockerPro;
    private int mode = 0;
    IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
    CallLogReader callLogReader = new CallLogReader(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameInit();
        InitListener();

        Log.i("myError","准备启动服务");

        // 创建 CallLogReader 实例并读取通话记录
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
        blockerPro = new CallBlockerPro(contacts);

        // 默认模式
        registerReceiver(blocker, filter);
        mode = 0;
        Log.i("myError","开始监听来电");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销 BroadcastReceiver
        if(mode == 0)
            unregisterReceiver(blocker);
        if(mode == 1)
            unregisterReceiver(blockerPro);
        mode = 0;
    }

    private Button flush, pro;
    private TextView display;

    class myClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            // 先注销
            if(mode == 0)
                unregisterReceiver(blocker);
            if(mode == 1)
                unregisterReceiver(blockerPro);

            if(v.getId() == R.id.button){
                Map<String, Integer> callLog = callLogReader.readCallLog();
                Set<String> contacts = callLogReader.readContacts();
                blocker.flushCallLog(callLog, contacts);

                registerReceiver(blocker, filter);
                mode = 0;
                display.setText("普通模式");
                Log.i("myError","普通模式");
            }

            if(v.getId() == R.id.button2){
                Set<String> contacts = callLogReader.readContacts();
                blockerPro.flushCallLog(contacts);

                registerReceiver(blockerPro, filter);
                mode = 1;
                display.setText("强力模式");
                Log.i("myError","强力模式");
            }
        }
    }

    public void nameInit(){
        flush = findViewById(R.id.button);
        pro = findViewById(R.id.button2);

        display = findViewById(R.id.textView2);
    }

    protected void InitListener(){
        myClick mc = new myClick();
        flush.setOnClickListener(mc);
        pro.setOnClickListener(mc);
        Log.i("myError" , "监听器初始化完毕");
    }
}