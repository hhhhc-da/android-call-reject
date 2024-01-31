package com.example.callreject;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CallLogReader{

    private Context mContext;
    private Map<String, Integer> callLog;

    public CallLogReader(Context context) {
        // 捕捉上下文
        mContext = context;
        callLog = new HashMap<>();
        Log.i("myError","创建读取实例");
    }

    public Map<String, Integer> readCallLog() {
        Log.i("myError","开始执行readCallLog函数");
        ContentResolver contentResolver = mContext.getContentResolver();

        // 查询通话记录
        Cursor cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,   // 访问通话记录的 URI
                null,                        // 返回所有列
                null,                        // 无选择条件
                null,                        // 无选择条件参数
                CallLog.Calls.DATE + " DESC" // 按日期降序排序
        );

        int counter = 0;
        Log.i("myError","循环读取通话记录中");
        // 读取每个通话记录
        while (cursor.moveToNext()) {
            cursor.getColumnIndex(CallLog.Calls.NUMBER);
            // 获取电话号码和通话时间
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));;
            long timeInMillis = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));

            // 将时间戳转换为可读的时间格式
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(timeInMillis));

            long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));

            // 在控制台输出电话号码和通话时间
            // Log.i("myError", time + "\t" + duration + "s\t" + number);

            // 如果通话时间小于8s
            if(duration < 8){
                if(callLog.containsKey(number)) {
                    int count = callLog.get(number);
                    callLog.put(number, count + 1);
                }
                else{
                    callLog.put(number,  1);
                }
            }

            if(++counter>100){
                Log.i("myError","超过访问上限，停止读取");
                break;
            }
        }

        cursor.close();

        return callLog;
    }

    public Set<String> readContacts(){
        ContentResolver contentResolver = mContext.getContentResolver();
        Set<String> phoneSet = new HashSet<>();

// 定义要从通讯录中检索的字段
        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };

// 查询联系人数据
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
        );

// 将查询结果存储在 Set 中
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                // 不处理的电话号码是有空格的 xxx xxxx xxxx
                phoneSet.add(phoneNumber.replace(" ",""));
            }
            cursor.close();
        }

        return phoneSet;
    }
}

class CallBlocker extends BroadcastReceiver {
    private static final String TAG = "CallBlocker";
    private static final String ACTION_INCOMING_CALL = "android.intent.action.PHONE_STATE";

    private Map<String, Integer> callLog;
    private Set<String> contacts;
    private int status = 0;

    public CallBlocker(Map<String, Integer> callLog, Set<String> contacts) {
        this.callLog = callLog;
        this.contacts = contacts;

//        for(String i: this.callLog.keySet()){
//            Log.d("myError","屏蔽号码: " + i);
//        }

        Log.i("myError","初始化挂断服务");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(this.status == 1){
            this.status = 0;
            return;
        }
        // 人工延时
        for(int i=0;i<100000;i++);

        if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i("myError","来电: " + phoneNumber + ",status: " + this.status);

            if (phoneNumber != null) {
                // 检查号码是否是骚扰电话
                if (callLog.containsKey(phoneNumber) && this.status == 0) {
                    this.status = 1;

                    if(contacts.contains(phoneNumber)){
                        Log.i("myError","检测到通讯录");
                        return;
                    }

                    endCall(context);
                }
            }
        }
    }

    // 挂断电话
    private void endCall(Context context) {
        Log.i("myError","挂断电话函数启动");

        TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if(tm != null){
            tm.endCall();
        }

//        Log.e("myError", "无法挂断电话: ", e);
    }
}
