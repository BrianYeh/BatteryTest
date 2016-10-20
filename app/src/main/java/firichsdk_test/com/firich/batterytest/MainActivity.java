package firichsdk_test.com.firich.batterytest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView mTextViewBatteryCapacity;
    private TextView mTextViewBatteryStatus;
    private TextView mTextViewThermalTempStatus;
    TextView mtxtLog;

    private Button mBtnEverCharged;
    private Button mBtnEverDisChargeToRange;

    private HandlerThread mCheckMsgThread;
    private Handler mCheckMsgHandler;
    private boolean isUpdateInfo = false;
    private Handler mHandler;
    private Handler mHandlerUI;
    private Handler mHandlerUIButton;

    private static final int MSG_UPDATE_INFO = 0x110;
    String mBatteryCapacity="";
    String mBatterystatus="";
    String mTemperature="";
    boolean mIsEverCharged= false;
    boolean mIsDisChargeToRange = false;
    boolean mCanShutdown= false;
    int mUpdateFrequency=5;
    int mUpperLimit=70;
    int mLowerLimit=65;
    boolean mWriteLog=true;

    configItemsUIUtil g_configUIItemsFile;
    configItemsUIUtil.configItemUI g_configItemUIObject=null;

    private boolean bDebugOn = true;
    String strTagUtil = "MainActivity.";

    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";

    private void dump_trace( String bytTrace)
    {
        if (bDebugOn)
            Log.d(strTagUtil, bytTrace);
    }

    private void Load_and_Set_Config()
    {
        g_configUIItemsFile = new configItemsUIUtil();
        g_configUIItemsFile.dom4jXMLParser();
        if (!g_configUIItemsFile.getParseOK())
            return;
        g_configItemUIObject = g_configUIItemsFile.getConfigItemUI("BatteryTest");
        if (!g_configUIItemsFile.IsConfigExist()) {
            return;
        }
        //set config
        mUpdateFrequency = g_configItemUIObject.update_frequency;
        mWriteLog = g_configItemUIObject.write_log;
        mUpperLimit = g_configItemUIObject.discharging_upper_limit;
        mLowerLimit = g_configItemUIObject.discharging_lower_limit;
    }
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context paramAnonymousContext, Intent intent) {
            dump_trace("onReceive:Begin");
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                dump_trace("onReceive:ACTION_BATTERY_CHANGED");
                dump_trace("onReceive:isUpdateInfo="+isUpdateInfo);
                Bundle bundle = intent.getExtras();
                // 获取当前电量
                int current = bundle.getInt("level");
                // 获取总电量
                int total = bundle.getInt("scale");

                //sb.append("当前电量为：" + current * 100 / total + "%" + "  ");
                mBatteryCapacity = (current * 100 / total + "%" + "  ");
                MainActivity.this.mTextViewBatteryCapacity.setText(mBatteryCapacity);

                /*
                  電池充電狀態（ BATTERY_STATUS_CHARGING ）
                 ·電池放電狀態（ BATTERY_STATUS_DISCHARGING ）
                 ·電池滿電狀態（ BATTERY_STATUS_FULL ）
                 ·電池不充電狀態（ BATTERY_STATUS_NOT_CHARGING ）
                 ·電池未知狀態（ BATTERY_STATUS_UNKNOWN ）
                 */
                // Are we charging / charged?
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                String strStatus = "";
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        strStatus = "BATTERY_STATUS_UNKNOWN";
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        strStatus = "BATTERY_STATUS_CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        strStatus = "BATTERY_STATUS_DISCHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        strStatus = "BATTERY_STATUS_NOT_CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        strStatus = "BATTERY_STATUS_FULL";
                       // mBtnEverCharged.setBackgroundColor(Color.GREEN);
                       // PostUIUpdateButton(mBtnEverCharged);
                        mIsEverCharged = true;
                        break;
                }
                mBatterystatus = strStatus;
                /*
                boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
                if (isCharging){
                    mBatterystatus = "Charging";
                }
                boolean isCharged =  (status == BatteryManager.BATTERY_STATUS_FULL);
                if (isCharged){
                    mBatterystatus = "Charged(FULL)";
                    mBtnEverCharged.setBackgroundColor(Color.GREEN);
                    mIsEverCharged = true;
                }
                boolean isDisCharging =  (status == BatteryManager.BATTERY_STATUS_DISCHARGING);
                if (isDisCharging){
                    mBatterystatus = "DisCharging";
                }
                */
                MainActivity.this.mTextViewBatteryStatus.setText(mBatterystatus);
                mTemperature = get_Thermal_Temperature();
                MainActivity.this.mTextViewThermalTempStatus.setText(mTemperature);
                int Capacity = (current*100)/total;

                if (mIsEverCharged){
                    if ( (Capacity >= mLowerLimit) && (Capacity <= mUpperLimit)){
                     //   mBtnEverDisChargeToRange.setBackgroundColor(Color.GREEN);
                        mIsDisChargeToRange = true;
                    }
                }
                boolean mCanShutdown= false;
                if ( mIsEverCharged && mIsDisChargeToRange && (Capacity < mLowerLimit)){
                    //shutdown
                    mCanShutdown = true;
                    set_Pre_Power_Off_Status(true);
                }
                //set_Pre_Power_Off_Status(); //test only
                //mCanShutdown = true;//test only
                //shutdown_now(); //test only
                if (mCanShutdown){
                    shutdown_now();
                }

            }
        }
    };

    private void shutdown_now()
    {
        Intent i = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        i.putExtra("android.intent.extra.KEY_CONFIRM",false);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity.this.startActivity(i);
    }
    protected void onPause()
    {
        super.onPause();
        //unregisterReceiver(this.mBroadcastReceiver);
    }

    protected void onResume()
    {
        super.onResume();
        //isUpdateInfo = true;
        //mCheckMsgHandler.sendEmptyMessage(MSG_UPDATE_INFO);
/*
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.mBroadcastReceiver, localIntentFilter);
        */
    }
    /** Called just before the activity is destroyed. */

    @Override
    public void onDestroy() {
        super.onDestroy();
        dump_trace("onDestroy");
        unregisterReceiver(this.mBroadcastReceiver);
        //释放资源
        isUpdateInfo = false;
        mCheckMsgHandler.removeMessages(MSG_UPDATE_INFO);
        mCheckMsgThread.quit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dump_trace("onCreate:");
        isUpdateInfo = false;
        mHandler = new Handler();
        mHandlerUI = new Handler();
        mHandlerUIButton = new Handler();
        initLog_and_UIUpdate_BackThread();
        Load_and_Set_Config();

        mtxtLog =(TextView) findViewById(R.id.textViewLog);
        mtxtLog.setText("Log:\n");

        Check_Pre_Power_Off();

        mTextViewBatteryCapacity= (TextView)findViewById(R.id.PercentageContent_text);
        mTextViewBatteryStatus= (TextView)findViewById(R.id.BatteryStatusContent_text);
        mTextViewThermalTempStatus= (TextView)findViewById(R.id.ThermalTempContent_text);

        mBtnEverCharged = (Button) findViewById(R.id.btnEverCharged);
        mBtnEverDisChargeToRange = (Button) findViewById(R.id.btnEverDischarge);

        mBtnEverCharged.setBackgroundColor(Color.RED);
        mBtnEverDisChargeToRange.setBackgroundColor(Color.RED);

        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.mBroadcastReceiver, localIntentFilter);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        dump_trace("onConfigurationChanged:");
    }

    private void Check_Pre_Power_Off()
    {
        boolean IsPowerOff = get_Pre_Power_Off_Status();
        TextView mTextViewPrePowerOff = (TextView) findViewById(R.id.PowerOff_text);
        if (IsPowerOff) {
            mTextViewPrePowerOff.setText("Pre Power Off");
        }
    }

    /*
    http://stackoverflow.com/questions/38036861/get-android-thermal-zone-temperature
There are system folders reflecting all temperature sensors: /sys/class/thermal_zone/thermal_zoneXX,
where XX=number. Inside each folder you can find 2 files:

temp: current sensor value, usually in millidegree Celsius, like 54000
type: contains string description of the sensor, like "ac", "battery",...
     */
    String mLogFileName="";
    public void Start_Test_click(View view)
    {
        mLogFileName = get_log_file_name();
        isUpdateInfo = true;
        mIsEverCharged = false;
        mIsDisChargeToRange = false;
        mCanShutdown = false;
        set_Pre_Power_Off_Status(false);
        mBtnEverCharged.setBackgroundColor(Color.RED);
        mBtnEverDisChargeToRange.setBackgroundColor(Color.RED);
        mTextViewBatteryStatus.setText("");
        mTextViewThermalTempStatus.setText("");
        mTextViewBatteryCapacity.setText("");
        mtxtLog.setText("Log:\n");

        unregisterReceiver(this.mBroadcastReceiver);
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        registerReceiver(this.mBroadcastReceiver, localIntentFilter);

        mCheckMsgHandler.sendEmptyMessage(MSG_UPDATE_INFO);

    }

    private void initLog_and_UIUpdate_BackThread()
    {
        mCheckMsgThread = new HandlerThread("check-message-coming");
        mCheckMsgThread.start();
        mCheckMsgHandler = new Handler(mCheckMsgThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {

                dump_trace("handleMessage:begin:isUpdateInfo="+isUpdateInfo);
                if (isUpdateInfo)
                {
                    WriteToLog();
                    mCheckMsgHandler.sendEmptyMessageDelayed(MSG_UPDATE_INFO, 1000);
                }
            }
        };
    }

    private void PostUIUpdateButton(final Button btnUI)
    {
        this.mHandlerUIButton.post(new Runnable()
        {
            public void run()
            {
                //final Button btnUI = (Button) findViewById(btnID);
                btnUI.setBackgroundColor(Color.GREEN);

            }
        });
    }
    private void PostUIUpdateLog(final String msg)
    {
        this.mHandlerUI.post(new Runnable()
        {
            public void run()
            {
                //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                final TextView testViewlog = (TextView) findViewById(R.id.textViewLog);
                testViewlog.append(msg.toString());

            }
        });
    }
    private void WriteToLog()
    {
        try
        {
            //模拟耗时
            int UpdateFrequency = mUpdateFrequency-1;
            if (UpdateFrequency <0){
                UpdateFrequency=4;
            }
            Thread.sleep(UpdateFrequency*1000);
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {

                    //Write log file
                    Date newdate = new Date();
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                    String date = format.format(newdate);

                    String LogString ="";
                    LogString = "["+ date +"]" + "; Capacity="+ mBatteryCapacity+ "; Status="+ mBatterystatus +"; Thermal= "+ mTemperature+ "\n";
                    PostUIUpdateLog(LogString);
                    if (mIsEverCharged) {
                        PostUIUpdateButton(mBtnEverCharged);
                    }
                    if (mIsDisChargeToRange){
                        PostUIUpdateButton(mBtnEverDisChargeToRange);
                    }
                    if (mWriteLog) {
                        write_Log_to_storage(LogString);
                    }
                }
            });

        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    public void write_Log_to_storage(String log)
    {
        String LogFileName="BatteryLog.txt";
        if (mLogFileName.isEmpty()){
            return;
        }
        File dir = new File ("/storage/emulated/legacy");
        String LogString ="";
        LogString = log;
        String strVersion = Build.DISPLAY;
        boolean contains_androidHTC = strVersion.contains("MRA58K");
        if (contains_androidHTC){
            /*
            Android added new permission model for Android 6.0 (Marshmallow).

http://www.captechconsulting.com/blogs/runtime-permissions-best-practices-and-how-to-gracefully-handle-permission-removal

             */
            return;
            /*
            java.io.File file = new java.io.File( Environment.getExternalStorageDirectory().getAbsolutePath()+"/"
                    + mLogFileName);

            try {
                //File secondFile = new File(dir, mLogFileName);
                //file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write(LogString.getBytes());
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            */

        }
        try {
            File secondFile = new File(dir, mLogFileName);
            //secondFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(secondFile, true);
            fos.write(LogString.getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    String get_log_file_name()
    {
        //Initialize your Date however you like it.
        Date newdate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        String date = "FEC_Battery_Log_"+ Build.SERIAL +"_" + format.format(newdate)+".txt";;
        return date;
    }
    String get_Thermal_Temperature()
    {
        String temperature1="";
        String temperature2="";
        String temp="";
        //Debug on Android 4.4
        String strVersion = Build.DISPLAY;
        boolean contains_android4 = strVersion.contains("4.4.3 2.0.0-rc2.");
        try {
            if (!contains_android4) {
                temperature1 = "soc_dts0:" + loadFileAsString("/sys/class/thermal/thermal_zone1/temp");
                temperature2 = "soc_dts1:" + loadFileAsString("/sys/class/thermal/thermal_zone2/temp");
                temp = temperature1 + temperature2;
            }
            if (contains_android4){
                temp="imx_thermal_zone:"+ loadFileAsString("/sys/class/thermal/thermal_zone0/temp");
            }
            return temp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /*
     * Load file content to String
     */
    public static String loadFileAsString(String filePath) throws java.io.IOException{
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
    private void set_Pre_Power_Off_Status(boolean statusPrePowerOfff)
    {
        SharedPreferences spref = getPreferences(MODE_PRIVATE);

        //由 SharedPreferences 中取出 Editor 物件，透過 Editor 物件將資料存入
        SharedPreferences.Editor editor = spref.edit();
        //清除 SharedPreferences 檔案中所有資料
        editor.clear();
        //儲存 boolean 型態的資料
        editor.putBoolean("KEY_PRE_POWER_OFF", statusPrePowerOfff);
        //將目前對 SharedPreferences 的異動寫入檔案中
        //如果沒有呼叫 commit()，則異動的資料不會生效
        editor.commit();
    }
    private boolean get_Pre_Power_Off_Status()
    {
        SharedPreferences spref = getPreferences(MODE_PRIVATE);
        //回傳 KEY_STRING 是否在在 SharedPreferences 檔案中
        boolean exists = spref.contains("KEY_PRE_POWER_OFF");
        if (exists){
            //透過 KEY_BOOL key 取出 boolean 型態的資料，若資料不存在則回傳 true
            boolean IsPowerOff = spref.getBoolean("KEY_PRE_POWER_OFF", false);
            return IsPowerOff;
        }else{
            return false;
        }
    }
}
