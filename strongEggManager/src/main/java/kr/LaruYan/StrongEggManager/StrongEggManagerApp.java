package kr.LaruYan.StrongEggManager;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class StrongEggManagerApp extends Application {
	public static final boolean isDebug = false;
	private static final boolean didKTsolvedIssue = true;
	public static final String PACKAGE_NAME = "kr.LaruYan.StrongEggManager";
	//UI갱신 요청 방송
	/**기본 지연 시간 0.5초
	 * 500ms 지연
	 */
	public static final long DEFAULT_UI_DELAY = 500;
	/** Intent 정보 교환을 위한 매개인자.
	 * 
	 */
	//public static final String EXTRA_EXPLICIT_WORKING = "SEM_explicitWorking";
	public static final String EXTRA_SERVICE_ISRUNNING = "SEM_serviceIsRunning";
	public static final String EXTRA_SIGNAL_STRENGTH = "SEM_signalStrength";
	public static final String EXTRA_SET_PAGE_MAINACTIVITY= "SEM_CurrentPage_MainAct";
	//public static final String EXTRA_EGG_CONNECTION_CHANGED = "SEM_Egg_Conn";
	
	//intent 정보 교환용 인식자
	//현재 연결상태.

	public static final byte EGG_CONNSTAT_COMPLETED = 0;
	public static final byte EGG_CONNSTAT_LOGIN_FAILED = 1;
	public static final byte EGG_CONNSTAT_UNSUPPORTED = 2;
	public static final byte EGG_CONNSTAT_NOW_RECOGNIZING = -1; 
	public static final byte EGG_CONNSTAT_UNRECOGNIZED = -2;
	public static final byte EGG_CONNSTAT_NO_WIFI = -3;
	
	//와이브로 신호세기 간단
	public static final byte WIBRO_SIGNALSIMPLE_VERYSTRONG = 36;
	public static final byte WIBRO_SIGNALSIMPLE_STRONG = 35;
	public static final byte WIBRO_SIGNALSIMPLE_LESSSTRONG = 34;
	public static final byte WIBRO_SIGNALSIMPLE_NORMAL = 33;
	public static final byte WIBRO_SIGNALSIMPLE_LESSWEAK = 32;
	public static final byte WIBRO_SIGNALSIMPLE_WEAK = 31;
	public static final byte WIBRO_SIGNALSIMPLE_LOST = 30;
	public static final byte WIBRO_SIGNALSIMPLE_UNKNOWN = -31;
	public static final byte WIBRO_SIGNALSIMPLE_WAITING = -32;
	
	//와이브로 신호세기 간단
	public static final byte WIBRO_STATUSSIMPLE_CONNECTING = 42;
	public static final byte WIBRO_STATUSSIMPLE_INITIALIZING = 41;
	public static final byte WIBRO_STATUSSIMPLE_COMPLETED = 40;
	public static final byte WIBRO_STATUSSIMPLE_WAITING = -42;
	public static final byte WIBRO_STATUSSIMPLE_OUTOFRANGE = -44;
	public static final byte WIBRO_STATUSSIMPLE_UNKNOWN = -41;
	
	//에그 배터리 단게
	public static final byte EGG_BATTERYLEVEL_CHARGING = 54;
	public static final byte EGG_BATTERYLEVEL_ENOUGH = 53;
	public static final byte EGG_BATTERYLEVEL_FINE = 52;
	public static final byte EGG_BATTERYLEVEL_INSUFFICIENT = 51;
	public static final byte EGG_BATTERYLEVEL_BOTTOMUP = 50;
	public static final byte EGG_BATTERYLEVEL_UNKNOWN = 50;
	
	//앱 설정 
	private static final String PREF_STR_EGG_BSSID="EggBssid";// 에그 BSSID를 파악해서 설정된 에그인지 확인
	private static final String PREF_INT_EGG_TYPE = "EggType"; 
	private static final String PREF_STR_EGG_AUTH = "EggWAuth";
	private static final String PREF_BIN_HTTP_CLIENT = "useHttpClient";
	private static final String PREF_BIN_DARK_ICON = "useDarkIcon";
	private static final String PREF_BIN_PERSISTANT_MONITORING = "PsstntMntrn"; 
	private static final String PREF_BIN_MINI_AUTOREFRESH = "useMiniPsst";
	private static final String PREF_LON_POLLING_RATE = "pollingRate";

	//기본값
	private static final String DEF_STR_EGG_AUTH = "dXNlcjowMDAw";//user 0000 의 기본값.
	private static final boolean DEF_BIN_HTTP_CLIENT = false;
	private static final boolean DEF_BIN_DARK_ICON = false;
	private static final boolean DEF_BIN_PERSISTANT_MONITORING = true; 
	private static final boolean DEF_BIN_MINI_AUTOREFRESH = false;
	private static final long DEF_LON_POLLING_RATE = 5000L;
	
	//에그 설정 항목 기본값
	//public static byte MINIMUM_CREDS_CHARS = 4;
	
	
	private static final String USER_STR_WIBRO_PLAN = "WbPlan";
	private static final String USER_STR_WIBRO_PACKETS_PROVIDED = "WbPPvd";
	private static final String USER_STR_WIBRO_PACKETS_USED = "WbPUsd";
	private static final String USER_STR_WIBRO_FEE_BASIC = "WbFBsc";
	private static final String USER_STR_WIBRO_FEE_CURRENT = "WbFCnt";
	private static final String USER_STR_WIBRO_ADDITIONAL_CHARGES = "WbFAdd";
	private static final String USER_STR_WIBRO_LAST_OBTAINED = "WbLObt";
	
//
	private static final String TAG_LOG = "SEM_CORE";
	public Context ApplicationContext;
	private Thread mainThread;
	//private Intent updateUiIntent;
	
	public Handler mainThreadHandler;
	public Handler uiCompositionThreadHandler;
	public TaskThreadHandler taskThreadHandler;
	public Handler broadcastThreadHandler;
	private static final Object settingsSyncObject = new Object();
	private static final Object statusSyncObject = new Object();
	//private Thread nonUiPleaseThread;
	//private Looper nonUiThreadLooper; 
	private WifiManager wifiManager;
	private SharedPreferences sharedPrefs;
	private ConnectivityManager connManager;
	
	//일처리, 주기적인 정보 갱신을 위한 서비스
	//private StrongEggManagerService EggService;
	//WiFi 상태 메시지를 수신, BroadcastReceiver.
	//public WifiStateReceiver wifiStateReceiver;
	private boolean isMonitorServiceRunning = false;
		
	//현재는 스트롱에그 v1640 위주 개발, 컴팩트에그 v2294 지원 완료.
	//공통 문자열 지정..을 했다.
	public static final String COMMON_STR_EMPTY = "";
	public static final String COMMON_STR_SLASHWITHSPACES = " / ";
	public static final String COMMON_STR_CRLF = "\n";
	private static final String COMMON_STR_COMMA = ",";
	
	public static final String COMMON_STR_NUM_2N = "-2";
	public static final String COMMON_STR_NUM_1N = "-1";
	public static final String COMMON_STR_NUM_0 = "0";
	public static final String COMMON_STR_NUM_1 = "1";
	public static final String COMMON_STR_NUM_2 = "2";
	public static final String COMMON_STR_NUM_3 = "3";
	public static final String COMMON_STR_NUM_4 = "4";
	public static final String COMMON_STR_NUM_5 = "5";
	public static final String COMMON_STR_NUM_6 = "6";
	public static final String COMMON_STR_NUM_7 = "7";
	public static final String COMMON_STR_NUM_8 = "8";
	public static final String COMMON_STR_NUM_9 = "9";
	
	public static final String COMMON_UNIT_PACKETS_GB = " GB";
	public static final String COMMON_UNIT_PACKETS_MB = " MB";
	public static final String COMMON_UNIT_PACKETS_KB = " KB";
	public static final String COMMON_UNIT_PACKETS_BYTE = " Byte";

	public static final String VERSION_STRING_KWDB2600_1760 = "1.7.6.0";
	public static final String VERSION_STRING_KWDB2600_1740 = "1.7.4.0";
	public static final String VERSION_STRING_KWDB2600_1640 = "1.6.4.0";
	public static final String VERSION_STRING_KWDB2600_1250 = "1.2.5.0";
	
	public static final String VERSION_STRING_KWFB2700_2294 = "2.2.9.4";
	public static final String VERSION_STRING_KWFB2700_2234 = "2.2.3.4";
	
	//public static final String COLON = ": ";
	//private int STRONG_EGG_WBINFOARRAY = 11;
	private static final byte EGG_NOT_FOUND = -21;
	private static final byte EGG_KWDB2600_MOBILE_PAGE = 25;
	private static final byte EGG_KWDB2600_PC_PAGE = 23;
	public  static final byte EGG_KWDB2600_GENERIC = 21;
	private static final byte EGG_KWFB2700_v2234 = 24;
	public  static final byte EGG_KWFB2700_GENERIC = 22;
//TODO
	//컴팩트에그2, 스트롱에그2 지원 추가.
	private static final byte EGG_KWFB3000_v1234 = 27;
	public  static final byte EGG_KWFB3000_GENERIC = 26;
	private static final byte EGG_KWDB2800_v1234 = 29;
	public  static final byte EGG_KWDB2800_GENERIC = 28;
	
	
	//private enum DEVICE_TYPE {NOT_FOUND , EGG_KWDB2600_v1640, EGG_KWDB2600_v1250, EGG_KWDB2600_vGENERIC, EGG_KWFB2700_v2234, EGG_KWFB2700_vGENERIC};
	//private DEVICE_TYPE Egg_TYPE;
	private byte Egg_Type;
	private String Egg_BSSID;
	
	//에그-앱 접속에 필요
	private String Auth;////YjI2MDA6d2JlZ2cwMzMxeTU1";//deprecated! 결국은 설정을 이용케해야한다.
//	07-31 10:08:26.876: V/SEM_CORE(444): New Encoded: YjI2MDA6d2JlZ2cwMzMxeTU1 개인화 성공

	//private String Egg_ID;
	//private String Egg_PW;
	private byte Egg_ConnectionStatus = EGG_CONNSTAT_NOW_RECOGNIZING;
	//private String Egg_BSSID;
	private String Egg_IP;// = "192.168.1.254";//deprecated! >> WifiManager로 게이트웨이 주소를 가져오게하자.
	//에그의 와이브로 정보.
	private String Wibro_IP;
	private String Wibro_Gateway;
	private String Wibro_DnsPrimary;
	private String Wibro_DnsSecondary;
	private String Wibro_SignalSimple;
	//private String Wibro_SignalSimpleNonParsed;
	private String Wibro_Status;
	private String Wibro_ElapsedTime;
//	private String Wibro_Channel;
	private String Wibro_BatteryLevel;
	private String Wibro_SignalDetailed;
	//와이브로 사용량 정보
	private String Wibro_UsedMB_UP_currentSession;
	private String Wibro_UsedMB_DN_currentSession;
//	private int Wibro_UsedMB_currentSession = -1;
//	private int Wibro_UsedMB_KTserver = -1;
	
	//개인화: 앱 설정
	private boolean App_useDarkIconSet = DEF_BIN_DARK_ICON;
	private boolean App_HttpClientPreferred = DEF_BIN_HTTP_CLIENT;
	private boolean App_enablePersistantMonitoring = DEF_BIN_PERSISTANT_MONITORING;
	private boolean App_useMiniAutoRefresh = DEF_BIN_MINI_AUTOREFRESH;
	public long App_pollingRate = DEF_LON_POLLING_RATE; 
	
	//앱 상태 설정
	private boolean isPaused = false;
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		if(isDebug)
		{
			Log.v(TAG_LOG,"onCreate()");
			//if(Debug.isDebuggerConnected()){
			//	Log.i(TAG_LOG,"DebuggerConnected");
			//}
		}
		
		ApplicationContext = getApplicationContext();
		
		//서비스
		//wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		mainThread = Looper.getMainLooper().getThread();

		//updateUiIntent = new Intent();
		mainThreadHandler = new Handler(Looper.getMainLooper()); //
		
		HandlerThread uiCompositionThread = new HandlerThread(TAG_LOG+"_UI_Compositor",
				Process.THREAD_PRIORITY_DISPLAY);
		uiCompositionThread.start();
		
		uiCompositionThreadHandler = new Handler(uiCompositionThread.getLooper());
		
		HandlerThread broadcastThread = new HandlerThread(TAG_LOG+"_Broadcast",Process.THREAD_PRIORITY_BACKGROUND);
		broadcastThread.start();
		
		broadcastThreadHandler = new Handler(broadcastThread.getLooper());
		
		//registerWifiStatusReceiver();
		
		
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		//넘어온 메시지를 별도 스레드에 실행하기 위해 핸들러를 둡니다.
		//그런데 이 핸들러, main에서 실행준비를 해요. ..#망.. 
		HandlerThread taskThread = new HandlerThread(TAG_LOG+"_Task_Handler",
					Process.THREAD_PRIORITY_BACKGROUND);
		taskThread.start();
		
		// Get the HandlerThread's Looper and use it for our Handler
		//nonUiThreadLooper = nonUiThread.getLooper();
		taskThreadHandler = new TaskThreadHandler(taskThread.getLooper(),this);
		
		taskThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				if(isDebug)
				{
					Log.v(TAG_LOG,"initialize App on Thread: "+Thread.currentThread().getName());
				}
				//다른 스레드에 넣어서 하도록 했더니 NullpointerException이 나타났다.
				if(wifiManager == null)
				{
					wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				}
				//wifiStateReceiver = new WifiStateReceiver();
				initializeApp();//WifiStateReceiver에 있는걸 참고하세요. 곧 손봅니다.
				//음.. 자꾸 세 번이나 실행된다.
			}
		});

		if(isDebug)
		{
			Log.v(TAG_LOG,"onCreate() finished");
		}
	}
	
	//스레드가 순서대로 진행되고 있어요.
	//lint 경고(Leak 문제)가 있어 static으로 변경했다가 WeakRefenence만 남겨두었어요.
	//http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
	// Handler that receives messages from the thread
	public static final class TaskThreadHandler extends Handler {
		private final WeakReference<StrongEggManagerApp> EggCore;
		
		public TaskThreadHandler(Looper looper, StrongEggManagerApp app ) {
			super(looper);
			EggCore = new WeakReference<StrongEggManagerApp>(app);
		}
		
		@Override
		public void handleMessage(final Message msg) {
			if(msg != null)
			{
				final StrongEggManagerApp app = EggCore.get();
				switch(msg.what)
				{
				case StrongEggManagerService.SERVICE_START:
				case StrongEggManagerService.SERVICE_INTERVAL_CHANGED:
				case StrongEggManagerService.SERVICE_STOP:
				case StrongEggManagerService.SERVICE_TERMINATE:
				//new Thread(new Runnable(){
				//	@Override
				//	public void run() {
						app._launchService(msg.what);						
				//	}
				//}).start();
				break;
				default:
					super.handleMessage(msg);//상위 핸들러로 떠넘기기
					break;
				}
			}
		}
		
		
	}
	
	/**
	 * Set OnAppStateChangedListener for the activity/widget
	 * 
	 * @param 이부분이 있어야하는구낭..
	 * 
	 * @author LaruYan
	 *
	 */
	public interface OnAppStateChangedListener {
        public void onConnectionChanged(byte connectionStat);
        public void onServiceStateChanged(boolean isRunning);
	}
	private OnAppStateChangedListener mOnAppStateChangedListener;
	//MAp은 Iterate할 수 없다..
	/**
	 * Set OnEggConnectionChangedListener for the activity/widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	//갈수록 메모리 먹는 문제가 생길 수도 있으려나.. 일단 생존여부 확인한걸넣었으니 될까..
	//안된다.. 새 액티비티라 이것도 새롭게 만들어진다.. 앙ㄷ..

	public final void setOnAppStateChangedListener(OnAppStateChangedListener listener) {
		if( listener != null){
			mOnAppStateChangedListener = listener;
		}
	}
	
	//Map을 써서 Key가 있는 경우 안넣도록 했는데 작동을 안한다. 유통 기한이 지난것 같다.
	//기존에 있던걸 지우고 새로 넣는 걸 택해야지.
	//단순히 NullPointerException 때문에 스레드가 맛갔을 뿐이라면.. 이렇게 안해도 될텐뎅..
	/** contains가 통하지 않으니 이것도 무용지물.
	*	이었다가 일단은 고쳐봐야죵
	*/
	public final void removeOnAppStateChangedListener(OnAppStateChangedListener listener) {
		if(mOnAppStateChangedListener == listener){
			mOnAppStateChangedListener = null;
		}
	}
	private final void sendConnectionHasChangedEvent(final byte connStat){
		if(mOnAppStateChangedListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: connection status changed >> "+connStat);
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnAppStateChangedListener.onConnectionChanged(connStat);
				}});
		}
	}

	private final void sendServiceStateChangedEvent(final boolean isRunning){
		if(mOnAppStateChangedListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Service State Changed. Now, it is "+(isRunning?"Running":"Stopped"));
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnAppStateChangedListener.onServiceStateChanged(isRunning);
				}
			});
		}
	}
	

	/**
	 * Set OnAppStateChangedListener for the activity/widget
	 * 
	 * @param 이부분이 있어야하는구낭..
	 * 
	 * @author LaruYan
	 *
	 */
	public interface OnWiFiAPChangedListener {
        public void onWiFiAPChanged(String ssid,boolean isHidden, String bssid, String clientIPA);
	}
	private OnWiFiAPChangedListener mOnWiFiAPChangedListener;
	//MAp은 Iterate할 수 없다..
	/**
	 * Set OnEggConnectionChangedListener for the activity/widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	//갈수록 메모리 먹는 문제가 생길 수도 있으려나.. 일단 생존여부 확인한걸넣었으니 될까..
	//안된다.. 새 액티비티라 이것도 새롭게 만들어진다.. 앙ㄷ..

	public final void setOnWiFiAPChangedListener(OnWiFiAPChangedListener listener) {
		if( listener != null){
			mOnWiFiAPChangedListener = listener;
		}
	}
	
	//Map을 써서 Key가 있는 경우 안넣도록 했는데 작동을 안한다. 유통 기한이 지난것 같다.
	//기존에 있던걸 지우고 새로 넣는 걸 택해야지.
	//단순히 NullPointerException 때문에 스레드가 맛갔을 뿐이라면.. 이렇게 안해도 될텐뎅..
	/** contains가 통하지 않으니 이것도 무용지물.
	*	이었다가 일단은 고쳐봐야죵
	*/
	public final void removeOnWiFiAPChangedListener(OnWiFiAPChangedListener listener) {
		if(mOnWiFiAPChangedListener == listener){
			mOnWiFiAPChangedListener = null;
		}
	}
	private final void sendWiFiStateChangedEvent(){
		if(mOnWiFiAPChangedListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Device's WiFi State has Changed.");
			}
			WifiInfo wifi_details = wifiManager.getConnectionInfo();
			final String AP_BSSID = Egg_BSSID;
			final String AP_SSID = wifi_details.getSSID();
			final String Local_IPAddress = IPv4Input.intLsbToIPv4String(wifi_details.getIpAddress());
			final boolean AP_isHidden = wifi_details.getHiddenSSID();
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnWiFiAPChangedListener.onWiFiAPChanged(AP_SSID,AP_isHidden,AP_BSSID,Local_IPAddress);
				}
			});
			wifi_details = null;
		}
	}


	
	/**
	 * Set OnLocalRefreshEggInfoListener for the activity/widget
	 * 
	 * @param 이부분이 있어야하는구낭..
	 * 
	 * @author LaruYan
	 *
	 */
	public interface OnLocalRefreshEggInfoListener{
		public void onLocalRefreshed();
		public void onRefreshedWibroStatus();
		public void onRefreshedWibroUsageSession();
	}
	private OnLocalRefreshEggInfoListener mOnLocalRefreshEggInfoListener;
	/**
	 * Set OnRefreshEggInfoListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final boolean setOnLocalRefreshEggInfoListener(OnLocalRefreshEggInfoListener listener) {
		if(listener != null){
			mOnLocalRefreshEggInfoListener = listener;
			return true;
		}else{
			return false;
		}
	}
	public final void removeOnLocalRefreshEggInfoListener(OnLocalRefreshEggInfoListener listener) {
		if(mOnLocalRefreshEggInfoListener == listener){
			mOnLocalRefreshEggInfoListener = null;
		}
	}

	
	private final void sendLocalRefreshHasCompletedEvent(){
		if(mOnLocalRefreshEggInfoListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Egg's Local info Refreshed.");
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnLocalRefreshEggInfoListener.onLocalRefreshed();
				}});
		}
	}
	private final void sendRefreshWibroStatusHasCompletedEvent(){
		if(mOnLocalRefreshEggInfoListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Egg's Local info-(WibroStatus) Refreshed.");
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnLocalRefreshEggInfoListener.onRefreshedWibroStatus();
				}
			});
		}			
	}
	private final void sendRefreshWibroUsageSessionHasCompletedEvent(){
		if(mOnLocalRefreshEggInfoListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Egg's Local info-(WibroUsageSession) Refreshed.");
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnLocalRefreshEggInfoListener.onRefreshedWibroUsageSession();
				}
			});
		}
	}
	
	
	/**
	 * Set OnRemoteRefreshEggInfoListener for the activity/widget
	 * 
	 * @param 이부분이 있어야하는구낭..
	 * 
	 * @author LaruYan
	 *
	 */
	public interface OnUsageKtRefreshEggInfoListener{
		public void onRefreshFailedWibroUsageKT();
		public void onRefreshedWibroUsageKT(long lastObtained,String wibroPlan, String providedPackets, String usedPackets, String basicFee, String usedFee, String additionalCharges);
	}
	private OnUsageKtRefreshEggInfoListener mOnUsageKtRefreshEggInfoListener;
	/**
	 * Set OnUsageKtRefreshEggInfoListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final void setOnUsageKtRefreshEggInfoListener(OnUsageKtRefreshEggInfoListener listener) {
		if(listener != null){
			mOnUsageKtRefreshEggInfoListener = listener;
		}
	}
	public final void removeOnUsageKtRefreshEggInfoListener(OnUsageKtRefreshEggInfoListener listener) {
		if(mOnUsageKtRefreshEggInfoListener == listener){
			mOnUsageKtRefreshEggInfoListener = null;
		}
	}

	private final void sendRefreshWibroUsageKtHasFailedEvent(){
		if(mOnUsageKtRefreshEggInfoListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Egg's Usage Status(from KT) NOT retrieved");
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnUsageKtRefreshEggInfoListener.onRefreshFailedWibroUsageKT();
				}
			});
		}
		showShortToast(R.string.error_usage_cannot_retrieve);
	}
	private final void sendRefreshWibroUsageKtHasCompletedEvent(final long lastObtained, final String wibroPlan, final String providedPackets,final String usedPackets, final String basicFee,final String usedFee,final String additionalCharges){
		if(mOnUsageKtRefreshEggInfoListener != null){
			if(isDebug)
			{
				Log.v(TAG_LOG,"event: Egg's Usage Status(from KT) Obtained");
			}
			broadcastThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					mOnUsageKtRefreshEggInfoListener.onRefreshedWibroUsageKT(lastObtained,wibroPlan,providedPackets,usedPackets,basicFee,usedFee,additionalCharges);
				}
			});
		}
	}
	
	/**
	 * 애플리케이션에서 기준으로 사용할 상태를 설정합니다.
	 * 애플리케이션 전반에서 사용되고, 이 값에 따라 작동이 크게 변경될 수 있습니다.
	 * 
	 * 상태 변경하자마자 바로 실행되는 것도 있습니다.
	 * 
	 * @param connStat 설정하고픈 기준 연결 상태. EGG_CONNSTAT_가 붙은 상수를 참고해주세요.
	 */
	public void setConnectionStatus(byte connStat){
		if(Egg_ConnectionStatus != connStat){
			synchronized(statusSyncObject){
				Egg_ConnectionStatus = connStat;
			}
			if(isDebug){
				Log.i(TAG_LOG,"connection status changed >> "+Egg_ConnectionStatus);
			}
			sendConnectionHasChangedEvent(Egg_ConnectionStatus);
			
			//서비스를 사용하도록 되어있다면 바로 켜자.
			switch(Egg_ConnectionStatus)
			{
			case EGG_CONNSTAT_COMPLETED:				
				if(App_enablePersistantMonitoring || isMonitorServiceRunning){
					LaunchServiceSti();
					if(isDebug){
						Log.i(TAG_LOG,"auto refresh is on << "+(App_enablePersistantMonitoring?"always ":"")+"#"+(isMonitorServiceRunning?"turned on previously":""));
					}
				}
				break;
			case EGG_CONNSTAT_UNSUPPORTED:
			case EGG_CONNSTAT_LOGIN_FAILED:
			
			// 일단 연결상태 변수에 신경을 쓰게 바꾸어보았다. 아예 SEM_HttpRequest GET / POST 함수는 실행도 안한다.
			//	TerminateServiceSti();
			//	break;
			
			case EGG_CONNSTAT_UNRECOGNIZED://아니면 바로 끄자.. if(Egg_ConnectionStatus == EGG_CONNSTAT_NO_WIFI_AVAILABLE)
			
				//권외 중단 관련 기능에 버그가 있어서 끄라는 와이파이는 안끄고  IP만 꺼트린다.. #망..
				//WIFI ON/OFF시간 만큼 된다..
				TerminateServiceSti();
				if(isDebug){
					Log.i(TAG_LOG,"auto refresh will be off.");
				}
				//setEggIP(SEM_HttpResponse.HTTP_LOOPBACK_ADDRESS);// 127.0.0.1을 무시하는건 좋겠다.
				break;
			//case EGG_CONNSTAT_NOW_RECOGNIZING:
			case EGG_CONNSTAT_NO_WIFI:// 일단 WifiStateReceiver에서 500ms범위 내에서는 무시할 수 있게 해두었다.
			case EGG_CONNSTAT_NOW_RECOGNIZING:
			default:
				Egg_BSSID = null;
				break;
			}
		}else{
			if(isDebug)	{
				Log.i(TAG_LOG,"connection status already set. << "+Egg_ConnectionStatus);
			}
		}
	}
	
	/**
	 * 현재 애플리케이션의 기준 연결상태를 반환 합니다.
	 * 애플리케이션 클래스 내부에선 이 함수를 부르지 마세요.
	 * 
	 * @return 기준 연결 상태.
	 */
	public byte getConnectionStatus(){
		if(isDebug){
			Log.v(TAG_LOG,"Sending Connection Status to other components << " + Egg_ConnectionStatus);
		}
		return Egg_ConnectionStatus;
	}
	
	/**
	 * 현재 연결된 WiFi AP의 SSID 이름을 줍니다.
	 * 가져올 수  없는 경우, 와이파이에 연결하라는 문자열을 보냅니다.
	 * 
	 * @return SSID.
	 * 
	 */
	public String getSSID() {
		if(isDebug){
			Log.v(TAG_LOG,"getSSID()");
		}
		try{
			switch(Egg_ConnectionStatus){
			case EGG_CONNSTAT_NOW_RECOGNIZING:
			case EGG_CONNSTAT_NO_WIFI:
				if(isDebug){
					Log.w(TAG_LOG,"requesting SSID before WiFi fully connected.");
				}
				return getString(R.string.error_nna_nowifiavailable_ssid);
			default:
				return wifiManager.getConnectionInfo().getSSID();
			}
		} catch(NullPointerException npe) {
			if(isDebug){
				Log.w(TAG_LOG,".getConnectionInfo().getSSID() failed. notifying connect wifi first");
				npe.printStackTrace();
			}
			return getString(R.string.error_nna_nowifiavailable_ssid);
		}
	}
	
	/**
	 * /data에 저장된 애플리케이션 설정을 불러온 후
	 * 애플리케이션 실행준비를 작업용 스레드에서 실행합니다.
	 */
	private void initializeApp() {
		//앱 사용준비
		if(isDebug){
			Log.v(TAG_LOG,"initializeApp()");
		}
		loadAppSetting(true,true);
		
		taskThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				initialize();
			}});
	}
	
	/**
	 * 와이파이 인식 작업.
	 * BSSID를 비교하여 이전에 연결'중'이던 BSSID와
	 * 같은 경우 이것을 무시합니다.
	 */
	private final Runnable Run_WifiChangedEvent = new Runnable(){
		@Override
		public void run() {
			if(isDebug){
				Log.v(TAG_LOG,"recognizeWiFi()");
			}
			if(wifiManager == null){
				if(isDebug){
					Log.v(TAG_LOG,"WifiManager is NULL. try to get system service");
				}
				wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			}
			if(Egg_BSSID != null){	
				if(Egg_BSSID.equals(wifiManager.getConnectionInfo().getBSSID())){
					if(isDebug){
						Log.i(TAG_LOG,"current AP is same as previous. BSSID << "+Egg_BSSID);
					}
					return ;
				}
			}
			
			if(wifiManager.getConnectionInfo().getBSSID() != null){
				if(isDebug){
					Log.i(TAG_LOG,"new AP << "+wifiManager.getConnectionInfo().getBSSID());
				}
				taskThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						initialize();
					}});
			}
		}
	};
	/**
	 * 지정된 시간 후에 WiFi 인식 요청을 보냅니다.
	 * 
	 * @param delay 지연시간 (단위 ms)
	 */
	public void sendCheckWifiEvent(long delay){
		if(isDebug){
			Log.i(TAG_LOG,"requesting Check Wifi within "+delay+"ms");
		}
		broadcastThreadHandler.postDelayed(Run_WifiChangedEvent, delay);
	}
	
	/**
	 * 모든 WiFi 인식 요청을 취소합니다.
	 * 
	 * @param delay 지연시간 (단위 ms)
	 */
	public void cancelCheckWifiEvent(){
		if(isDebug){
			Log.i(TAG_LOG,"cancelling Pending Check Wifi events");
		}
		broadcastThreadHandler.removeCallbacks(Run_WifiChangedEvent);
	}
	
	/**
	 * 와이파이에 연결되어있는지 확인하고
	 * 연결되어있다면 장치를 인식합니다.
	 * 연결되어있지 않다면 와이파이가 없단 기준 상태만 설정합니다.
	 */
	private void initialize(){
		if(isDebug){
			Log.v(TAG_LOG,"initialize()");
		}
		//http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
		//First, unlike other broad casted intents, for Intent.ACTION_SCREEN_OFF and Intent.ACTION_SCREEN_ON you CANNOT declare them in your Android Manifest! I’m not sure exactly why, but they must be registered in an IntentFilter in your JAVA code.
		IntentFilter screenOnOffFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenOnOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver screenStateReceiver = new ScreenStateReceiver();
        getApplicationContext().registerReceiver(screenStateReceiver, screenOnOffFilter);
        //http://stackoverflow.com/questions/1588061/android-how-to-receive-broadcast-intents-action-screen-on-off/1588267#1588267
        isPaused = false; // 서비스 중단 도중 새 AP가 끼어들었고, 새AP이므로 이전 AP에서의 상태를 이어올 필요가 없다.
		if(isWifiConnected()){
			if(isDebug){
				Log.i(TAG_LOG,"WiFi is connected.");
			}	
			recognizeEgg();
		}else{
			if(isDebug){
				Log.i(TAG_LOG,"WiFi is NOT connected.");
			}
			setConnectionStatus(EGG_CONNSTAT_NO_WIFI);
		}
	}
	
	/**
	 * 에그를 인식합니다.
	 * /data 에 저장된 BSSID와 장치 형식이 있다면 사용합니다.
	 * 현재 연결된 AP와 BSSID가 일치하는 경우 경우 장치 버전이 일치하는지만 확인합니다.
	 */
	private void recognizeEgg(){
		if(isDebug){
			Log.v(TAG_LOG,"recognizeEgg()");
		}

		if(sharedPrefs == null){
			if(isDebug){
				Log.v(TAG_LOG,"DefaultSharedPreferences is NULL. try to get again");
			}
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		
		try{
			//BSSID 를 확인하여 기존에 설정된 에그인지 확인.
			if(isDebug){
				Log.v(TAG_LOG,"Known Egg was BSSID << "+ sharedPrefs.getString(PREF_STR_EGG_BSSID, COMMON_STR_EMPTY));
				Log.v(TAG_LOG,"Current AP has BSSID << "+ Egg_BSSID);
			}
			
			//default 값을 쓸 수 있는 경우 되도록 써야겠당.
			if( ! (sharedPrefs.getString(PREF_STR_EGG_BSSID, COMMON_STR_EMPTY)).equals(Egg_BSSID) ){
				if(isDebug){
					Log.v(TAG_LOG,"New Device Detected << "+Egg_BSSID);
				}
				//boolean isAvailable; 
				//기존에 설정된 에그가 아니네요
				//에그인지 확인부터..
				if(isSupportedDevice()){
					if(isDebug){
						Log.i(TAG_LOG,"Supported New Device.");
					}
					tryLogin();
				}else{
					if(isDebug){
						Log.i(TAG_LOG,"UNSUPPORTED New Device.");
					}
					//에그가 아니다
					//로그인 못하게 막자.
					setConnectionStatus(EGG_CONNSTAT_UNRECOGNIZED);
				}
			} else {
				if(isDebug){
					Log.i(TAG_LOG,"Supported and Known Device.");
				}
				//이전에 쓰던 에그가 맞다.
				//EggType만 불러오자.
				//loadAppSetting(true,true);
				Egg_Type = (byte)(sharedPrefs.getInt(PREF_INT_EGG_TYPE,EGG_NOT_FOUND));
				if(isDebug){
					Log.v(TAG_LOG,"Known Egg was Type << "+Egg_Type);
				}
				tryLogin(false);
				//로그인만 통과하면 OK!
				//setConnectionStatus(EGG_CONNSTAT_COMPLETED);
			}
			//Egg_BSSID = null;
		}catch(NullPointerException npe){
			if(isDebug){
				Log.e(TAG_LOG,"NullPointerException initializing Egg.");
				npe.printStackTrace();
			}
			if(sharedPrefs != null){
				if(isDebug){
					Log.e(TAG_LOG,"Fortunetely, reload settings PARTIALLY possible.");
				}
				loadAppSetting(true,true);
			}
		}
	}

	/**
	 * 미리 정해둔 Authorize 값으로 웹CM에 접근해봅니다.
	 * 에그 기기나 버전 판단도 같이 합니다. 
	 * 성공하면 이 Authorize 값을 /data에 등록합니다.
	 * @see tryLogin(boolean isCommit)
	 * @return 웹CM 페이지 접근 가능 << true || 불가능 (UNAUTHORIZED 등) << false
	 */
	private boolean tryLogin()
	{
		return tryLogin(true);
	}
	/**
	 * 미리 정해둔 Authorize 값으로 웹CM에 접근해봅니다.
	 * 에그 기기나 버전 판단도 같이 합니다. 이 때 애플리케이션 기준 연결 상태를 변경합니다.
	 * @param isCommit Authorize값 /data에 등록하려면 true
	 * @return 웹CM 페이지 접근 가능 << true || 불가능 (UNAUTHORIZED 등) << false
	 */
	private boolean tryLogin(boolean isCommit){
		if(isSupportedVersion()){
			//에그이다. user 0000 또는 이전에 저장된 것으로 로그인이 가능하다.
			//버전 확인이 되었다. Yes!
			if(isDebug){
				Log.i(TAG_LOG,"This device IS ready for use.");
			}
			if(isCommit){
				commitThisEgg();
				//BSSID 갱신. 이 장치를 기본으로 인식합니다.
				//무조건 갱신하게 하면 번잡할것 같아서 기존 장치인 경우엔 괜찮다고 말하게하고 싶었는데 잘 안되네.
			}
			
			setConnectionStatus(EGG_CONNSTAT_COMPLETED);
			return true;
		}else{
			//지원버전이 아니거나 로그인 정보가 맞지 않거나
			if(Egg_ConnectionStatus == EGG_CONNSTAT_LOGIN_FAILED){
				if(isDebug){
					Log.i(TAG_LOG,"Failed to authenticate on this device.");
				}
				//에그이다. 하지만 기본값 user 0000 조합 또는 이전에 저장된 것으론 안되서,
				//버전확인을 위해 로그인 하도록 요청
				//setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
				return false;
			}else{
				if(isDebug){
					Log.i(TAG_LOG,"Update is neccessary for this device.");
				}
				//에그이다. 하지만 버전이 낮당.
				if(isCommit){
					commitThisEgg();
					//BSSID 갱신. 이 장치를 기본으로 인식합니다.
					//무조건 갱신하게 하면 번잡할것 같아서 기존 장치인 경우엔 괜찮다고 말하게하고 싶었는데 잘 안되네.
				}
				setConnectionStatus(EGG_CONNSTAT_UNSUPPORTED);
				return true;
			}
		}
			
		/*
		//로그인 성공시
		if(isSupportedVersion())
		{
		
			//if (Egg_BSSID == EMPTY_STRING)
			//{
				// 첫 실행.
				
				commitNewEgg(wifiManager.getConnectionInfo().getBSSID());
				setConnectionStatus(EGG_CONNSTAT_COMPLETED);
				return true;
			//}
			//else
			//{
				//기존 설정된 에그가 아니네요.
				//설정을 적용할까요..?로 보여주는게 좋겠어요.
			//	return false;
			//}
		}else{
			setConnectionStatus(EGG_CONNSTAT_UNSUPPORTED);
			return false;
		}*/
	}
	
	/**
	 * 시스템 전체의 서비스들을 훑어 실행중인 서비스 중 해당하는 이름이 있는지 확인합니다.
	 * 
	 * @param context
	 * @param serviceName 찾고 싶은 서비스의 이름 (클래스)
	 * @return
	 */
	public static final boolean initialCheckServiceIsRunning(Context context, String serviceName)
	{
		if(serviceName != null){
			//시스템의 서비스를 훑어보는
			//느린 작업이므로 되도록 실행시키지 않아야해요...
			ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
		    for (RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
		        if (serviceName.equals(runningServiceInfo.service.getClassName())) {
		        	if(isDebug){
		    			Log.v(TAG_LOG,"service is running << "+serviceName);
		    		}
		        	 return true;
		        }
		    }
		}
		if(isDebug){
			Log.v(TAG_LOG,"service not running << "+serviceName);
		}
	    return false;
	}
	

	/**
	 * 자동 새로고침이 실행중인지 확인하여 애플리캐이션 FLAG에 반영. 
	 */
	public void initialCheckAutoRefreshIsRunning(){
		isMonitorServiceRunning = initialCheckServiceIsRunning(this,StrongEggManagerService.class.getName());
	}
	
	/**
	 * 자동 새로고침이 실행중인지 FLAG 반환. 
	 * @return 자동 새로고침 실행여부 (FLAG, 실제와 다를 수 있다)
	 */
	public boolean isAutoRefreshRunning(){
		return isMonitorServiceRunning;
	}
	
	/**
	 * 자동 새로고침 실행중인지 FLAG를 설정하고 서비스 실행상태가 바뀌었다고 알린다.
	 * @param isRunning 자동 새로고침 실행여부(FLAG)
	 */
	public void setIsAutoRefreshRunning(boolean isRunning){
		isMonitorServiceRunning = isRunning;
		sendServiceStateChangedEvent(isRunning);
	}
	
	//DEBUG용 임시기능
	/**
	 * 지정된 명령을 서비스 핸들러로 전송합니다. 
	 *  
	 * @param Message 메시지
	 */
	public void commandService(final int Message)
	{
		taskThreadHandler.post(new Runnable(){

			@Override
			public void run() {
				if(isDebug)
				{
					Log.v(TAG_LOG,"commandService handled on: "+Thread.currentThread().getName());
				}
				Message msg = taskThreadHandler.obtainMessage();
				msg.what=Message;
				taskThreadHandler.sendMessage(msg);
			}});
		/*if(isDebug)
		{
			Log.v(TAG_LOG,"commandService handled on: "+Thread.currentThread().getName());
		}
		Message msg = taskThreadHandler.obtainMessage();
		msg.what=Message;
		taskThreadHandler.sendMessage(msg);//.sendMessageAtFrontOfQueue(msg);
		/*
		nonUiThreadHandler.post(new Runnable(){

			@Override
			public void run() {
				_launchService(Message);
			}});*/
	}
	
	/**
	 * 서비스 실행 요청
	 */
	public void LaunchServiceSti()
	{
		if(!isPaused){
			commandService(StrongEggManagerService.SERVICE_START);
		}else{
			if(isDebug){
				Log.w(TAG_LOG,"Screen is off, will work if Screen is ON");
			}
		}
	}
	
	/**
	 * 서비스 중지 요청
	 */
	public void StopServiceSti()
	{
		commandService(StrongEggManagerService.SERVICE_STOP);
	}
	
	/**
	 *  서비스 긴급중지 요청
	 */
	public void TerminateServiceSti()
	{
		commandService(StrongEggManagerService.SERVICE_TERMINATE);
	}
	
	/**
	 * 해당 메시지대로 서비스 실행
	 * @param Message
	 */
	private void _launchService(int Message){
		Intent intent = new Intent(ApplicationContext, StrongEggManagerService.class);
		if(Message == StrongEggManagerService.SERVICE_TERMINATE){
			setIsAutoRefreshRunning(false);
			stopService(intent);
			//setFlag_MonitorServiceRunning(false);
		}else{
			intent.putExtra(StrongEggManagerService.EXTRA_SERVICE_DO, Message);
			//intent.putExtra(StrongEggManagerService.EXTRA_SERVICE_JOB_ID, Message);
			startService(intent);
			//setFlag_MonitorServiceRunning(true);
			setIsAutoRefreshRunning(true);
		}
		//EXTRA_SERVICE_STATE_CHANGED
	}
	/*
	public void setFlag_MonitorServiceRunning(boolean isTurnedOn)
	{
		//not thread-safe operation
		isMonitorServiceRunning = isTurnedOn;
	}
	*/
	//HTTP 요청은 스레드에서 처리, 텍스트 파싱도 같이 할 수 있을까..? 그러면 같은 기능을 여러개 만들어야되서 귀찮은데..
	//함수로 분리하면 되려나
	
	
	//근본기능 1234

	//0. WiFi 상태 확인
	/**
	 * 와이파이가 켜져있는지 알아보고,
	 * 켜져있다면 연결이 완료(할당,인증 절차 모두 끝나 IP 연결 가능)되었을 때
	 * 자신의 IP를 확인 후 DHCP 서버 IP(대개 게이트웨이)를 받아오려고 시도합니다.
	 * 
	 * 만약 자신의 IP를 받지 못했(DHCP작업이 끝나지 않았)다면 매 DEFAULT_UI_DELAY ms 만큼 기다립니다.
	 * (주의: 6~7초 걸리기도 합니다. DHCP서버 IP를 받지 못한 채 작업하지는 못할 목적으로 기다리는 동안 해당 스레드가 잠깁니다.)
	 * 
	 * @return 와이파이 접속 성공, IP 연결 가능여부.
	 */
	private boolean isWifiConnected(){
		boolean isWifiConnected = false;
		if(isDebug){
			Log.v(TAG_LOG,"checking Wifi status.");
		}
		setConnectionStatus(EGG_CONNSTAT_NOW_RECOGNIZING);
		
		if(wifiManager == null){
			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		}
		
	
		switch(wifiManager.getWifiState()){
		case WifiManager.WIFI_STATE_ENABLING:
		case WifiManager.WIFI_STATE_ENABLED:
		
		//String Egg_BSSID = EMPTY_STRING;
		//if(!isDebug)//애석하게도 와이브로 권외 지역이라.. 개발을 위해선 무시해야되려나..
		//{
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			SupplicantState wifiSupplicantState = wifiInfo.getSupplicantState();

			//안드로이드는 little endian으로 IP주소를 기록한단말인가..
			//이게 192.168.0.1 이어야할게 1.0.168.192로 나온다.. ㅠ
			//Egg_IP = IPv4Input.intToIPv4String(wifiManager.getDhcpInfo().gateway);
			//this.setEggIP(IPv4Input.intLsbToIPv4String(wifiManager.getDhcpInfo().gateway));
			
			if(wifiSupplicantState == SupplicantState.COMPLETED){
				isWifiConnected = false;
				//연결됨
				//showShortToast(wifiManager.getConnectionInfo().getMacAddress());//자신의MAC주소확인
				//showShortToast("BSSID: "+wifiInfo.getBSSID());//BSSID:
				//showShortToast("SSID: "+wifiInfo.getSSID());//SSID
				//작은쪽부터 먼저쓰다니 너무행.
				
				// 문제는 왜 이거 예전 것을 먼저 쓰는거야...
				//COMPLETED 이후에 DHCP 작업을 하기 때문에 시간차가 난다. ㅠㅠ
				//그래서 재시도 기능을 넣어볼까 생각중..
				//IP 못받아온 경우 계속 확인하게 해서 제대로 된 녀석을 받아오는데 성공☆
				while(!isWifiConnected){
					wifiInfo = wifiManager.getConnectionInfo();
					
					//lollipop workaround begin
					//롤리팝에서는 와이파이 최초 연결시 인터넷 연결여부를 확인합니다. 연결된 그 후는 신경 안쓰는듯
					//그러므로 인식중에서 무한 대기시킵시다 어쩔수 없지만 이게 가장 쉬운 우회방법인것 같네요 ㅠㅠ
					if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
						if (connManager == null){
							connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
						}
						if(!connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()){
							try {
								Log.v(TAG_LOG,"waiting Lollipop Internet Check.");
								Thread.sleep(App_pollingRate);//앱 설정에서 정보를 가져오는 주기에 따름
							} catch (InterruptedException ie) {
								if(isDebug){
									Log.v(TAG_LOG,"waiting Lollipop Internet Check operation interrupted.");
									ie.printStackTrace();
								}
							}
							continue;
						};
					}
					//lollipop workaround end
					
					
					if(wifiInfo.getIpAddress() != 0){
						if(isDebug){
							Log.i(TAG_LOG,"Finished DHCP Operation. Local IP << "+IPv4Input.intLsbToIPv4String(wifiInfo.getIpAddress()));
						}
						isWifiConnected = true;
						break;
					}
					//아직 IP 못받아온 경우라 잠시 기다리게 하자.
					if(isDebug){
						Log.v(TAG_LOG,"DHCP Operation is not completed. maybe Wi-Fi failure?");
					}
					wifiSupplicantState = wifiInfo.getSupplicantState();
					if(wifiSupplicantState == SupplicantState.COMPLETED){
						try {
							Thread.sleep(DEFAULT_UI_DELAY);
						} catch (InterruptedException ie) {
							if(isDebug){
								Log.v(TAG_LOG,"waiting DHCP operation interrupted.");
								ie.printStackTrace();
							}
						}
						//continue;
					} else if (wifiSupplicantState == SupplicantState.DISCONNECTED){
						if(isDebug){
							Log.w(TAG_LOG,"Wi-Fi disconnected while doing DHCP Operation.");
						}
						setConnectionStatus(EGG_CONNSTAT_NO_WIFI);
						break;
					} else {
						if(isDebug){
							Log.w(TAG_LOG,"Wi-Fi state unknown while doing DHCP Operation.");
						}
						break;
					}
				}
				Egg_BSSID = wifiInfo.getBSSID();
				setEggIP(IPv4Input.intLsbToIPv4String(wifiManager.getDhcpInfo().gateway));
				//setEggIP(IPv4Input.intLsbToIPv4String();
				//setEggIP(IPv4Input.intLsbToIPv4String(wifiManager.getDhcpInfo().gateway));
				sendWiFiStateChangedEvent();
				
				//서비스가 작동되도록 설정되었다면.. 서비스 시작.
				
				/* 안드로이드에서 gateway subnetmask 정보 확인하기.
				 * http://stackoverflow.com/questions/5387036/how-to-get-gateway-and-subnet-mask-details-in-android-programmatically
				 	super.onCreate(savedInstanceState);
			        setContentView(R.layout.main);
			        wifii= (WifiManager) getSystemService(Context.WIFI_SERVICE);
			        d=wifii.getDhcpInfo();
			
			        s_dns1="DNS 1: "+String.valueOf(d.dns1);
			        s_dns2="DNS 2: "+String.valueOf(d.dns2);    
			        s_gateway="Default Gateway: "+String.valueOf(d.gateway);    
			        s_ipAddress="IP Address: "+String.valueOf(d.ipAddress); 
			        s_leaseDuration="Lease Time: "+String.valueOf(d.leaseDuration);     
			        s_netmask="Subnet Mask: "+String.valueOf(d.netmask);    
			        s_serverAddress="Server IP: "+String.valueOf(d.serverAddress);
			
			        //dispaly them
			        info= (TextView) findViewById(R.id.infolbl);
			        info.setText("Network Info\n"+s_dns1+"\n"+s_dns2+"\n"+s_gateway+"\n"+s_ipAddress+"\n"+s_leaseDuration+"\n"+s_netmask+"\n"+s_serverAddress);

				 */
			}
		//}
		
		//showShortToast("Wifi NO");
			
			if(isDebug){
				Log.v(TAG_LOG,"finished checking Wifi status.");
			}
			//nullify itself
			wifiSupplicantState = null;
			wifiInfo = null;
			break;
		case WifiManager.WIFI_STATE_DISABLING:
		case WifiManager.WIFI_STATE_DISABLED:
		default:
			break;
		}
		
		return isWifiConnected;
	}
		
	//0. MAC 주소 확인
	/*private String getRemoteBSSID() {
	    //WiFiManager에서 받아오면 되어서 이거 필요없어요.
	    return "TESTING";
	}*/
	
	public String getClientIP() {
		//WiFiManager.
		return IPv4Input.intLsbToIPv4String(wifiManager.getConnectionInfo().getIpAddress());
	}
	
	public String getBSSID(){
		return Egg_BSSID;
	}
	
	
	//0. HTTP POST & RECEIVE
	//Runnable 안에서 실행해야 렉이 덜 걸려요
	/**
	 * WiFi에 연결되어있고 장치가 Egg 일 때
	 * HTTP GET 요청을 수행합니다.
	 * 
	 * 앱의 Http Client 사용 설정에 따라 HTTP URLConnection이거나 HTTP Client 일 수도 있습니다.
	 * 
	 * @param URL 주소
	 * @param DoAuthenticate 인증키 사용 여부
	 * 
	 * @return HTTP Response 일부(상태 코드, WWW-Authenticate 내용, HTTP Response 내용);
	 */
	private SEM_HttpResponse httpRequestGET(String URL, boolean DoAuthenticate){
		switch(Egg_ConnectionStatus){
		case EGG_CONNSTAT_NO_WIFI:
		case EGG_CONNSTAT_UNRECOGNIZED:
			if(isDebug){
				Log.w(TAG_LOG + "(HttpGET)","URL may not be unreachable. request actively refused on app. returning >> 410 GONE.");
			}
			return new SEM_HttpResponse(HttpURLConnection.HTTP_GONE, null, null);
		default:
			if(isDebug){
				//HTTP 작업을 어느 스레드에서 하고 있을까..
				Log.v(TAG_LOG + "(HttpGET)","http operation on Thread: "+Thread.currentThread().getName());
			}
		
			if(App_HttpClientPreferred){
				return SEM_HttpResponse.httpRequestGET_HttpClient(URL,(DoAuthenticate? Auth : null));	
			}else{
				return SEM_HttpResponse.httpRequestGET_HttpURLConn(URL,(DoAuthenticate? Auth : null));
			}
		}
	}
	/**
	 * WiFi에 연결되어있고 장치가 Egg 일 때
	 * HTTP POST 요청을 수행합니다.
	 * 인증 키는 항상 사용합니다.
	 * 
	 * 앱의 Http Client 사용 설정에 따라 HTTP URLConnection이거나 HTTP Client 일 수도 있습니다.
	 * 
 	 * @param URL 주소
	 * @param httpPOSTParameters HTTP POST매개변수(nameValuePair 따를 필요 없음)
	 * 
	 * @return HTTP Response 일부(상태 코드, WWW-Authenticate 내용, HTTP Response 내용);
	 */
	private SEM_HttpResponse httpRequestPOST(String URL, String httpPOSTParameters){//List<NameValuePair> httpPOSTParameters)
		switch(Egg_ConnectionStatus){
		case EGG_CONNSTAT_NO_WIFI:
		case EGG_CONNSTAT_UNRECOGNIZED:
			if(isDebug){
				Log.w(TAG_LOG + "(HttpPOST)","URL may not be unreachable. request actively refused on app. returning >> 410 GONE.");
			}
			return new SEM_HttpResponse(HttpURLConnection.HTTP_GONE, null, null);
		default:
			if(isDebug){
				//HTTP 작업을 어느 스레드에서 하고 있을까..
				Log.v(TAG_LOG + "(HttpPOST)","http operation on Thread: "+Thread.currentThread().getName());
			}
	
			//TODO httpPOST_URLccopnnection 구현이 되지 않았다. 그런데 쓸 때가 많지 않아서 그냥 둬도 될 듯. (..)
			if(App_HttpClientPreferred){
				return SEM_HttpResponse.httpRequestPOST_httpClient(URL,httpPOSTParameters,Auth);
			}else{
				return SEM_HttpResponse.httpRequestPOST_httpClient(URL,httpPOSTParameters,Auth);
			}
		}
	}
		
	//1.지원대상 에그인지 아닌지 확인
	//와이파이에 진입할 때 마다 한 번만 실행.
	/**
	 * 지원 대상에 든 에그인지 확인합니다.
	 * 시중에는 스트롱에그를 포함, 컴팩트에그, Egg2 등 여러 기종들이 출시되어 있습니다.
	 * 현재 앱은 스트롱에그 만 지원합니다.
	 * 
	 * @return 지원대상 에그 >> true
	 */
	public boolean isSupportedDevice(){
		if(isDebug){
			Log.v(TAG_LOG,"isSupportedDevice()");
		}
		//DEVICE_TYPE Egg_Type = DEVICE_TYPE.NOT_FOUND;
		Egg_Type = checkDeviceSpecificAuthRealm(httpRequestGET(Egg_IP,false));
		//admin/admin.asp 페이지가 프레임셋인 관계로 그냥 인증 확인을 안하는건가 왜이러지. 
		
		//여기서 판독되는건 장치 종류 뿐이라, GENERIC으로 나오는게 정상.
		switch(Egg_Type){
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_PC_PAGE:
		case EGG_KWDB2600_GENERIC:
			//스트롱에그
			if(isDebug){
				Log.v(TAG_LOG,"This may StrongEgg");
			}
			return true;
		case EGG_KWFB2700_v2234:
		case EGG_KWFB2700_GENERIC:
			//컴팩트에그
			if(isDebug){
				Log.v(TAG_LOG,"This may CompactEgg");
			}
			return true;
		case EGG_NOT_FOUND:
		default:
			if(isDebug){
				Log.v(TAG_LOG,"This may NOT an Egg");
			}
			return false;
		}
	}
	/**
	 * 수행한 HTTP Response에서 WWW-Authenticate 값만 뽑아
	 * 장치별로 지정된 고유값과 같은지 확인합니다. 
	 * 별도 버전 확인이 필요합니다.
	 * 
	 * @param 올바른 인증 키 없이 진행한 HTTP Response 일부.
	 * @return 인식된 장치 형식. 항상 GENERIC.  HTTP Response 가 NULL이거나 인식하지 못했다면 EGG_NOT_FOUND
	 */
	private static final byte checkDeviceSpecificAuthRealm(SEM_HttpResponse httpRESPONSE){
		if ( httpRESPONSE != null){
			//switch(httpRESPONSE.getStatusLine().getStatusCode())
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_UNAUTHORIZED: //접근할 수 있지만 인증정보가 맞지 않다.
				//String httpAUTHrealm = httpRESPONSE.getFirstHeader(HTTP_HEADER_KEY_REALM).getValue();
				String httpAUTHrealm = httpRESPONSE.getHttpResponseAuthRealm();
				
				if(isDebug){
					Log.v(TAG_LOG,"HTTP AuthRealm << "+httpAUTHrealm);
				}
				
				if(httpAUTHrealm.contains("KWD-B2600 Web")){
					if(isDebug){
						Log.i(TAG_LOG,"This device MIGHT BE << KWD-B2600. (스트롱에그) ");
					}
				
					//nullify response itself
					httpRESPONSE = null;
					return EGG_KWDB2600_GENERIC;
				}else if(httpAUTHrealm.contains("user")){
					if(isDebug){
						Log.i(TAG_LOG,"This device MIGHT BE << KWF-B2700. (컴팩트에그) ");
					}
					//컴팩트에그 [미지원]
					
					//nullify response itself
					httpRESPONSE = null;
					return EGG_KWFB2700_GENERIC;
				}else{
					//nullify response itself
					httpRESPONSE = null;
					return EGG_NOT_FOUND;
				}
				//WWW-Authenticate: Basic realm="KWD-B2600 Web"
				//break;
			case HttpURLConnection.HTTP_OK: //Basic Auth 없이 접근 가능한 것은 스트롱에그,컴팩트에그가 아니다.
				//break;
			default: //접근할 수 없다.
				return EGG_NOT_FOUND;
				//break;
			}
		}else{
			//실패..를 표현할 다른 값이 있으면 좋겠는데 아직 모르겠다.
			return EGG_NOT_FOUND;
		}
	}
	
	/**
	 * 각 기종에 맞춰 현재 기기의 펌웨어 버전을 확인하여 설정합니다.
	 * 
	 * 인증키가 올바르지 않다면 로그인 실패 기준 상태로 설정됩니다.
	 * 인식하지 못한 경우 GENERIC 그대로이며,
	 * HTTP Response 가 NULL일 때 EGG_NOT_FOUND로 설정됩니다.
	 * (주의: 개발 FLAG가 세워져 있다면(isDebug=true) EGG_NOT_FOUND 인 상태로 장치 버전 확인시 NullPointerException이 나타납니다.)   
	 * 
	 * @return 해당 펌웨어가 앱에서 지원되는 경우 >> true
	 */
	private boolean isSupportedVersion(){
		if(isDebug){
			Log.v(TAG_LOG,"isSupportedVersion()");
		}
		if(isDebug){
			Log.v(TAG_LOG,"Device Type << "+Egg_Type);
		}
		SEM_HttpResponse httpRESPONSE = null;

		//상세 버전 확인
		switch(Egg_Type){
		//버전을 다시 확인하게 한다는건 렉먹을 부분일텐데.. 
		// 못본 사이에 에그 버전이 달라졌다고 한다면 이게 맞는데
		// known 인걸 무시하는 격이잖아.
		case EGG_KWDB2600_PC_PAGE:
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_GENERIC: //스트롱에그
			//스트롱에그 버전 확인
			httpRESPONSE = httpRequestGET(Egg_IP+"/admin/RS_getWiMAXInfo.asp",true);
			if(isDebug){
				Log.v(TAG_LOG,"checking StrongEgg Version. httpRESPONSE NULL?"+(httpRESPONSE==null ? "true":"false"));
			}
			break;
		case EGG_KWFB2700_v2234:
		case EGG_KWFB2700_GENERIC://컴팩트에그
			httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_version_info",true);
			if(isDebug){
				Log.v(TAG_LOG,"checking CompactEgg Version. httpRESPONSE NULL?"+(httpRESPONSE==null ? "true":"false"));
			}
			break;
		default:
			if(isDebug){
				Log.wtf(TAG_LOG,"Correct Device Type is ESSENTIAL checking Egg Version << "+Egg_Type);
				throw new NullPointerException();
			}
			break;
		}
		
		if(httpRESPONSE != null){
			//if(httpRESPONSE.getStatusLine().getStatusCode() == 200)
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				switch(Egg_Type){
				//버전을 다시 확인하게 한다는건 렉먹을 부분일텐데.. 
				// 못본 사이에 에그 버전이 달라졌다고 한다면 이게 맞는데
				// known 인걸 무시하는 격이잖아.
				case EGG_KWDB2600_PC_PAGE:
				case EGG_KWDB2600_MOBILE_PAGE:
				case EGG_KWDB2600_GENERIC: //스트롱에그
					//스트롱에그 버전 확인
					if(isDebug){
						Log.v(TAG_LOG,"parsing StrongEgg Version");
					}
					Egg_Type = parseStrongEggVersion(httpRESPONSE.getHttpResponseBody());
					break;
				case EGG_KWFB2700_v2234:
				case EGG_KWFB2700_GENERIC://컴팩트에그
					if(isDebug){
						Log.v(TAG_LOG,"parsing CompactEgg Version");
					}
					Egg_Type = parseCompactEggVersion(httpRESPONSE.getHttpResponseBody());
					break;
				default:
					break;
				}
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				// 버전 정보를 가져오지 못한경우.
				break;//Egg_recognizedAs = EGG_NOT_FOUND;//Egg_recognizedAs = EGG_KWDB2600_vGENERIC;
			}
		}else{
			if(isDebug){
				Log.w(TAG_LOG,"Egg Type Not Found. http response NULL?");
			}
			Egg_Type = EGG_NOT_FOUND;
		}
		
		//nullify response itself
		httpRESPONSE = null;
			
		if(isDebug){
			Log.v(TAG_LOG,"Device Type /w version << "+Egg_Type);
		}
		
		switch(Egg_Type){
		/////////////
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
			//1.6.4.0, 웹CM 및 모바일CM 지원
			if(isDebug){
				Log.i(TAG_LOG,"This device IS << KWD-B2600(스트롱에그). Version: 1.6.4.0");
			}
			return true;
		case EGG_KWDB2600_PC_PAGE:
			//1.2.5.0, 웹CM 전용 지원.
			if(isDebug){
				Log.i(TAG_LOG,"This device IS << KWD-B2600(스트롱에그). Version: 1.2.5.0");
			}
			return false;
		//////////////
		//컴팩트 에그
		case EGG_KWFB2700_v2234:
			if(isDebug){
				Log.i(TAG_LOG,"This device IS << KWF-B2700(컴팩트에그). Version: 2.2.3.4");
			}
			return true;
		////////
		//미지원
		default:
			return false;
		//case EGG_NOT_FOUND:
		//	return false;
		}
	}
	
	/**
	 * 현재 기준 장치 형식에 따라 사람이 읽을 수 있는 문자열로 버전을 표현합니다.
	 * 
	 * @return 표준화된 펌웨어 버전 문자열 모르는 경우 "unknown".
	 */
	/*
	public String switchString_DeviceVersion(){
		return switchString_DeviceVersion(Egg_Type);
	}
*/
	/**
	 * 입력된 장치 형식에 따라 사람이 읽을 수 있는 문자열로 버전을 표현합니다. 
	 * 
	 * @param eggType 장치 형식
	 * @return 표준화된 펌웨어 버전 문자열 모르는 경우 "unknown".
	 */
	/*
	private static final String switchString_DeviceVersion(byte eggType){
		switch(eggType){
		case StrongEggManagerApp.EGG_KWDB2600_MOBILE_PAGE:
			return StrongEggManagerApp.VERSION_STRING_KWDB2600_1640;
		case StrongEggManagerApp.EGG_KWDB2600_PC_PAGE:
			return StrongEggManagerApp.VERSION_STRING_KWDB2600_1250;
		case StrongEggManagerApp.EGG_KWDB2600_GENERIC:
			return "HCSTR_ unknown StrongEgg";
		case StrongEggManagerApp.EGG_KWFB2700_v2234:
			return "HCSTR_ 2.2.3.4";
		case StrongEggManagerApp.EGG_KWFB2700_GENERIC:
			return "HCSTR_ unknown CompactEgg";
		default:
			if(isDebug){
				Log.wtf(TAG_LOG+ " switchString_DeviceVersion()", "this App is working for unrecognized device! this should not happen!!");
			}
			return "HCSTR_ wrong device";
		}
	}*/
	
	/**
	 * 스트롱에그 버전 확인. 지정된 HTTP Response 내용에서 버전 정보를 확인합니다.
	 * 
	 * @param EggInfoString 스트롱에그가 제공하는 버전 정보 페이지
	 * @return 확인된 스트롱에그 형식
	 */
	private static final byte parseStrongEggVersion(String EggInfoString){
		if(EggInfoString != null){
			try{
				//HTTP 200 OK
				//String EggInfoString = getHttpResponseBody(httpRESPONSE);
				EggInfoString = EggInfoString.substring(EggInfoString.indexOf("var arrVerInfos = ["));
				EggInfoString = EggInfoString.substring(0, EggInfoString.indexOf(";"));
				
				if(isDebug){
					Log.v(TAG_LOG,"Parsed Version info << " +EggInfoString);
				}
				
				//불안정한 방법. contains 라서 그다지 내키지 않는다. 어떢하지.
				if(EggInfoString.contains(VERSION_STRING_KWDB2600_1760)){
					EggInfoString = null;
					return EGG_KWDB2600_MOBILE_PAGE;
				}else if(EggInfoString.contains(VERSION_STRING_KWDB2600_1740)){
					EggInfoString = null;
					return EGG_KWDB2600_MOBILE_PAGE;
				}else if(EggInfoString.contains(VERSION_STRING_KWDB2600_1640)){
					EggInfoString = null;
					return EGG_KWDB2600_MOBILE_PAGE;
				}else if(EggInfoString.contains(VERSION_STRING_KWDB2600_1250)){
					EggInfoString = null;
					return EGG_KWDB2600_PC_PAGE;
				}
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.e(TAG_LOG,"parsed string doesn't fit to process.");
					ioobe.printStackTrace();
				}
			}
			finally{
				EggInfoString = null;
			}
		}
		return EGG_KWDB2600_GENERIC;
	}
	
	/**
	 * 컴팩트에그 버전 확인. 지정된 HTTP Response 내용에서 버전 정보를 확인합니다.
	 * 
	 * @param EggInfoString 스트롱에그가 제공하는 버전 정보 페이지
	 * @return 확인된 컴팩트에그 형식
	 */
	private static final byte parseCompactEggVersion(String EggInfoString){
		if(EggInfoString != null){
			try{
				//HTTP 200 OK
				//String EggInfoString = getHttpResponseBody(httpRESPONSE);
				EggInfoString = EggInfoString.substring(EggInfoString.indexOf("VPOS_VERSION"));
				EggInfoString = EggInfoString.substring(0, EggInfoString.indexOf(","));
				
				if(isDebug){
					Log.v(TAG_LOG,"Parsed Version info << " +EggInfoString);
					Log.v(TAG_LOG,"parsed is 2.2.9.4? "+EggInfoString.contains(VERSION_STRING_KWFB2700_2294));
				}
				
				//불안정한 방법. contains 라서 그다지 내키지 않는다. 어떢하지.
				if(EggInfoString.contains(VERSION_STRING_KWFB2700_2294)){
					EggInfoString = null;
					return EGG_KWFB2700_v2234;
				}else if(EggInfoString.contains(VERSION_STRING_KWFB2700_2234)){
					EggInfoString = null;
					return EGG_KWFB2700_v2234;
				}
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.e(TAG_LOG,"parsed string doesn't fit to process.");
					ioobe.printStackTrace();
				}
			}
			finally{
				EggInfoString = null;
			}
		}
		return EGG_KWFB2700_GENERIC;
	}
	
	//Toast 메시지를 서비스에서 보이는 방법.
	//The problem is, that the toast has to be created in the main ui thread.
	//To get a handler for this thread you have to override onStartCommand method and save a reference to a handler.
	//Then you can post a Runnable in the onHandleIntent method to create and show a toast in the main ui thread.
	//http://www.jjoe64.com/2011/09/show-toast-notification-from-service.html

	/**
	 * 지정된 문자열을 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param ToastMessage 보일 문자열
	 * @param isLong 오래 머무르게 << true
	 */
	private void showToast(final String ToastMessage, final boolean isLong){
		if(Thread.currentThread() != mainThread){
			//메인스레드가 아니다
			mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(ApplicationContext, ToastMessage, isLong ? Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
				}
			});
		}else{
			//그냥 메인스레드다.
			Toast.makeText(ApplicationContext, ToastMessage, isLong ? Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
		}
	}
	/**
	 * 지정된 문자열을 Toast.SHORT 만큼 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param ToastMessage 보일 문자열
	 */
	public void showShortToast(String ToastMessage) {
		showToast(ToastMessage,false);
	}
	/**
	 * 지정된 문자열을 Toast.LONG 만큼 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param ToastMessage 보일 문자열
	 */
	public void showLongToast(String ToastMessage) {
		showToast(ToastMessage,true);
	}
	
	/**
	 * 지정된 문자열을 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param stringResourceId 보일 문자열의 자원 ID
	 * @param isLong 오래 머무르게 << true
	 */
	private void showToast(final int stringResourceId, final boolean isLong){
		if(Thread.currentThread() != mainThread){
			//메인스레드가 아니다
			mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(ApplicationContext, stringResourceId, isLong ? Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
				}
			});
		}else{
			//그냥 메인스레드다.
			Toast.makeText(ApplicationContext, stringResourceId, isLong ? Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
		}
	
	}
	/**
	 * 지정된 문자열을 Toast.SHORT 만큼 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param stringResourceId 보일 문자열의 자원 ID
	 */
	public void showShortToast(int stringResourceId) {
		showToast(stringResourceId,false);
	}
	/**
	 * 지정된 문자열을 Toast.LONG 만큼 화면에 머무르게 합니다.
	 * 메인 스레드에서 실행하여 타 스레드에서 실행할 때 겪는 지연을 막습니다.
	 * 
	 * @param stringResourceId 보일 문자열의 자원 ID
	 */
	public void showLongToast(int stringResourceId) {
		showToast(stringResourceId,true);
	}
	
	/**
	 * 지정된 계정이름, 비밀번호를 이용하여
	 * HTTP Basic 인증키("계정이름:비밀번호"를 Base64 인코딩)를 구성합니다. 
	 * 항상 /data 에 변경사항이 저장됩니다.
	 * 
	 * @param Egg_ID 에그 웹CM 계정 이름
	 * @param Egg_PW 에그 웹CM 계정 비밀번호
	 */
	public void renewAuthKey(String Egg_ID,String Egg_PW){
		//설정에 저장하도록 옮겨야겠지만..;
		renewAuthKey(Base64.encodeToString((Egg_ID+":"+Egg_PW).getBytes(),Base64.NO_WRAP),true);
		//설정된 값으로 로그인 다시 하기
		if(!tryLogin()){
			showShortToast(R.string.error_login_failed);
		}
	}
	
	/**
	 * 지정된 인증키를 앱의 기준 인증키로 사용합니다.
	 * HTTP Basic 형식을 따라주세요.
	 * 
	 * @param authKey 새 인증키
	 * @param isCommit /data 에 저장하려면 >> true
	 */
	private void renewAuthKey(String authKey,boolean isCommit){
		synchronized(settingsSyncObject){
			Auth = authKey;
			if(isDebug){
				Log.i(TAG_LOG,"Renewed AuthKey.");
			}
			
			if(isCommit){
				//설정 파일을 변경하도록 준비합니다.
				if(sharedPrefs == null){
					sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);//getApplicationContext());
				}
				SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
				
				//실제 변경할 부분 만들기
				sharedPrefsEditor.putString(PREF_STR_EGG_AUTH, Auth);
				
				
				//설정 파일 변경을 '저장'합니다.
				sharedPrefsEditor.commit();
				sharedPrefsEditor = null;
				
				if(isDebug){
					Log.i(TAG_LOG,"Committed renewed AuthKey to prefs.");
				}
			}
		}
	}
	
	/**
	 * 현재 사용중인 기준 BSSID 및 기준 장치 형식을
	 * /data 에 반영합니다.
	 */
	private void commitThisEgg(){//(String? BSSID)
		if(isDebug){
			Log.v(TAG_LOG,"Committing new Egg Type#BSSID >> "+Egg_Type +" # " + Egg_BSSID);
		}
		if(sharedPrefs == null){
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);//getApplicationContext());
		}
		synchronized(settingsSyncObject){
			//설정 파일을 변경하도록 준비합니다.
			SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
			
			//실제 변경할 부분 만들기
			sharedPrefsEditor.putString(PREF_STR_EGG_BSSID, Egg_BSSID);
			sharedPrefsEditor.putInt(PREF_INT_EGG_TYPE,Egg_Type);
			
			//설정 파일 변경을 '저장'합니다.
			sharedPrefsEditor.commit();
			sharedPrefsEditor =null;
		}
	}
	
	/**
	 * 앱 기준 설정을 바꿉니다.
	 * (주의: /data 엔 반영되지 않습니다.) 
	 * 
	 * @param useHttpClient HTTP Client 사용 >> true || false << HTTP URL Connection 사용
	 * @param useDarkIconSet 어두운 아이콘 사용 >> true || false << 밝은 아이콘 사용
	 * @param usePersistantMonitoring 항상 자동 새로고침 사용 >> true
	 * @param useMiniAutorefresh 간소화된 새로고침 사용 >> true
	 * @param pollingRate 자동 새로고침 주기
	 */
	public void setPreferredNow(boolean useHttpClient,boolean useDarkIconSet, boolean usePersistantMonitoring, boolean useMiniAutorefresh, long pollingRate) {
		synchronized(settingsSyncObject){
			App_HttpClientPreferred = useHttpClient;
			App_useDarkIconSet = useDarkIconSet;
			App_enablePersistantMonitoring = usePersistantMonitoring;
			App_useMiniAutoRefresh = useMiniAutorefresh;
			//500~15000 범위를 감지해야한다. 감지했을 경우 알림메시지와 함께 기본값인 5초로 되돌린다
			if(pollingRate >= 500L && pollingRate <= 15000L){
				App_pollingRate = pollingRate;
			}else{
				App_pollingRate = 5000L;
				showShortToast(R.string.app_pref_err_outofbounds);
			}
			
		}
	}
	
	/**
	 * HTTP Client를 쓰고 있는지를 반환합니다.
	 * 현재 앱 기준 설정을 따르므로, /data 에 저장된 것과 다를 수 있습니다.
	 * 
	 * @return HTTP Client 사용 << true || false >> HTTP URL Connection 사용
	 */
	public boolean isHttpClientPreferred() {
		return App_HttpClientPreferred;
	}
	/**
	 * HTTP Client를 쓰고 있는지를 설정합니다.
	 * (주의: /data 에 저장되지는 않습니다.)
	 * 
	 * @deprecated
	 * @param isPreferred HTTP Client >> true || false << HTTP URL Connection
	 */
	/*private void setHttpClientPreferred(boolean isPreferred) {
		synchronized(settingsSyncObject)
		{
			App_HttpClientPreferred = isPreferred;
		}
	}*/
	/**
	 * 어두운 아이콘을 쓰고 있는지를 반환합니다.
	 * 현재 앱 기준 설정을 따르므로, /data 에 저장된 것과 다를 수 있습니다.
	 * 
	 * @return 어두운 아이콘 사용 << true || false >> 밝은 아이콘 사용
	 */
	public boolean isDarkIconSetPreferred() {
		return App_useDarkIconSet;
	}
	/**
	 * 어두운 아이콘을 쓰고 있는지를 설정합니다.
	 * (주의: /data 에 저장되지는 않습니다.)
	 * 
	 * @deprecated
	 * @param isPreferred 어두운 아이콘 사용 >> true || false << 밝은 아이콘 사용
	 */
	/*private void setDarkIconSetPreferred(boolean isPreferred) {
		synchronized(settingsSyncObject)
		{
			App_useDarkIconSet = isPreferred;
		}
	}*/
	/**
	 * 항상 자동 새로고침을 쓰고 있는지를 반환합니다.
	 * 현재 앱 기준 설정을 따르므로, /data 에 저장된 것과 다를 수 있습니다.
	 * 
	 * @return 항상 자동 새로고침 사용 << true || false >> 사용자가 켜야한다.
	 */
	public boolean isPersistantMonitoringAlwaysPreferred() {
		return App_enablePersistantMonitoring;
	}
	/**
	 * 항상 자동 새로고침을 쓰고 있는지를 설정합니다.
	 * (주의: /data 에 저장되지는 않습니다.)
	 * 
	 * @deprecated
	 * @param isPreferred 항상 자동 새로고침 사용 >> true || false << 사용자가 켜야한다.
	 */
	/*private void setPersistantMonitoringAlwaysPreferred(boolean isPreferred) {
		synchronized(settingsSyncObject)
		{
			App_enablePersistantMonitoring = isPreferred;
		}
	}*/
	
	/**
	 * 간소화 새로고침을 쓰고 있는지를 반환합니다.
	 * 현재 앱 기준 설정을 따르므로, /data 에 저장된 것과 다를 수 있습니다.
	 * 
	 * @return 간소화 자동 새로고침 사용 << true || false >> 모든 정보 새로고침
	 */
	public boolean isMiniAutoRefreshPreferred() {
		return App_useMiniAutoRefresh;
	}
	

	/**
	 * 
	 * 
	 * @return
	 */
	public int getPositionPollingRate() {
		// TODO 입력받은 값을 updateIntervalValues값과 대조한 다음 updateInterval에 맞는 배열로 배출.
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.updateIntervalValues, android.R.layout.simple_spinner_item);
		return adapter.getPosition(App_pollingRate+"");
		
		//return 15001;//Specify Custom 
	}
	
	/**
	 * 다음의 앱 기준 설정의 현재값을 /data에 저장합니다:
	 * HTTPClient 사용 여부,
	 * 어두운 색 아이콘 사용 여부,
	 * 항상 자동새로고침 사용 여부,
	 * 간소화 자동새로고침 사용 여부
	 */
	public void commitAppSetting() {
		if(sharedPrefs == null){
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);//getApplicationContext());
		}
		synchronized(settingsSyncObject){
			if(sharedPrefs != null){
				SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
				
				//실제 변경할 부분 만들기
				sharedPrefsEditor.putBoolean(PREF_BIN_HTTP_CLIENT, App_HttpClientPreferred); //HttpClient VS httpURLconnection
				sharedPrefsEditor.putBoolean(PREF_BIN_DARK_ICON, App_useDarkIconSet); //검은색 알림막대 아이콘
				sharedPrefsEditor.putBoolean(PREF_BIN_PERSISTANT_MONITORING, App_enablePersistantMonitoring); //실시간 확인 항상 사용
				sharedPrefsEditor.putBoolean(PREF_BIN_MINI_AUTOREFRESH, App_useMiniAutoRefresh);//간소화된 자동새로고침
				sharedPrefsEditor.putLong(PREF_LON_POLLING_RATE, App_pollingRate);//간소화된 자동새로고침
				
				//설정 파일 변경을 '저장'합니다.
				sharedPrefsEditor.commit();
				sharedPrefsEditor = null;
				//loadAppSetting();
				if(isDebug){
					Log.v(TAG_LOG,"Commiting App Settings. >> "+(App_HttpClientPreferred? "HttpClient ":"HttpURLConnection ")+(App_useDarkIconSet ?"DarkIcon ":"LightIcon ")+(App_enablePersistantMonitoring ?"Always_Monitoring ":"Manual_Monitoring ")+(App_useMiniAutoRefresh ? "mini autorefresh ":"full autorefresh ")+(App_pollingRate));
				}
			} else {
				if(isDebug){
					Log.e(TAG_LOG,"Error Saving preference. sharedPrefs is NULL");
				}
			}
		}
		
		//실시간 확인 항상 표시는 즉각 반영토록 해보자.
		if(App_enablePersistantMonitoring){
			LaunchServiceSti();
		}else{
			TerminateServiceSti();
		}
	}

	/**
	 * /data 에 저장된 앱 설정을 불러옵니다.
	 * 인증 키나 장치 형식은 불러오지 않습니다.
	 */
	public void loadAppSetting(){
		loadAppSetting(false,false);
	}
	
	/**
	 * /data 에 저장된 앱 설정을 불러옵니다.
	 * 
	 * @param withCreds 저장된 인증키 불러오기 >> true
	 * @param withType 저장된 장치 형식 불러오기 >> true
	 */
	private void loadAppSetting(boolean withCreds,boolean withType){
		if(isDebug){
			Log.v(TAG_LOG,"LoadSetting()");
		}
		
		//설정에 저장된 값을 불러오기
		if(sharedPrefs == null){
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);//getApplicationContext());
		}

		//07-31 11:29:41.116: V/SEM_CORE(601): New Encoded: dXNlcjowMDAw
		if(withCreds){
			//인증값을 불러오게하기.
			renewAuthKey(sharedPrefs.getString(PREF_STR_EGG_AUTH, DEF_STR_EGG_AUTH),false);
		}
		if(withType){
			Egg_Type = (byte)(sharedPrefs.getInt(PREF_INT_EGG_TYPE, EGG_NOT_FOUND));
		}
		
		//Gingerbread부턴 HttpURLconnection 을 권장합니다. 그렇기에 HttpClient는 사장시킵시다.
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO)
		{
			App_HttpClientPreferred = sharedPrefs.getBoolean(PREF_BIN_HTTP_CLIENT, true);
		}
		App_HttpClientPreferred = sharedPrefs.getBoolean(PREF_BIN_HTTP_CLIENT, DEF_BIN_HTTP_CLIENT);
		App_useDarkIconSet =sharedPrefs.getBoolean(PREF_BIN_DARK_ICON, DEF_BIN_DARK_ICON);
		App_enablePersistantMonitoring = sharedPrefs.getBoolean(PREF_BIN_PERSISTANT_MONITORING, DEF_BIN_PERSISTANT_MONITORING);
		App_useMiniAutoRefresh = sharedPrefs.getBoolean(PREF_BIN_MINI_AUTOREFRESH, DEF_BIN_MINI_AUTOREFRESH);
		App_pollingRate = sharedPrefs.getLong(PREF_LON_POLLING_RATE, DEF_LON_POLLING_RATE);
		
		if(isDebug){
			Log.v(TAG_LOG,"Prepared App Settings. << "+(App_HttpClientPreferred? "HttpClient ":"HttpURLConnection ")+(App_useDarkIconSet ?"DarkIcon ":"LightIcon ")+(App_enablePersistantMonitoring ?"Always_Monitoring ":"Manual_Monitoring ")+(App_useMiniAutoRefresh ?"mini autorefresh ":"full autorefresh ")+(App_pollingRate));
		}
	}
	
	/**
	 * /data에 저장된 사용량 정보를 반환합니다.
	 * 
	 * @return (모두 갱신 당시 기준)
	 * 인간이 읽을 수 있는 시각(기기 시간대)
	 * 인간이 읽을 수 있는 날짜(기기 시간대)
	 * 제공량(MB)
	 * 사용량(MB)
	 * 기본료(원)
	 * 사용료(원)
	 * 정보이용료(원)
	 * 순으로 이루어진 8원소 문자열 배열.
	 */
	public String[] loadLastObtainedUsage(){
		if(sharedPrefs == null){
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		}
		String[] usageStringfromUserData = new String[8];
		usageStringfromUserData[0] = getDateTime_inCurrentTimeZone(sharedPrefs.getLong(USER_STR_WIBRO_LAST_OBTAINED, 0L));
		usageStringfromUserData[1] = getDate_inCurrentTimeZone(sharedPrefs.getLong(USER_STR_WIBRO_LAST_OBTAINED, 0L));
		usageStringfromUserData[2] = sharedPrefs.getString(USER_STR_WIBRO_PLAN, getString(R.string.wb_status_unknown));
		usageStringfromUserData[3] = sharedPrefs.getString(USER_STR_WIBRO_PACKETS_PROVIDED, StrongEggManagerApp.COMMON_STR_NUM_0);
		usageStringfromUserData[4] = sharedPrefs.getString(USER_STR_WIBRO_PACKETS_USED, StrongEggManagerApp.COMMON_STR_NUM_0);
		usageStringfromUserData[5] = sharedPrefs.getString(USER_STR_WIBRO_FEE_BASIC, StrongEggManagerApp.COMMON_STR_NUM_0);
		usageStringfromUserData[6] = sharedPrefs.getString(USER_STR_WIBRO_FEE_CURRENT,StrongEggManagerApp.COMMON_STR_NUM_0);
		usageStringfromUserData[7] = sharedPrefs.getString(USER_STR_WIBRO_ADDITIONAL_CHARGES, StrongEggManagerApp.COMMON_STR_NUM_0);
		
		return usageStringfromUserData;
	}
	/**
	 * 지정된 밀리초를 인간이 읽을 수 있는 날짜 및 시간으로 반환합니다.
	 * 기기의 시간대 및 표기법을 지킵니다.
	 * 
	 * @param UTCinMillis (ms, 밀리초) 1970년 1월 1일 00:00:00 UTC 를 처음으로, 흐른 시간.
	 * @return 인간이 읽을 수 있는 시각. (기기 시간대)
	 */
	public static final String getDateTime_inCurrentTimeZone(long UTCinMillis){
		return getTime_inCurrentTimeZone(UTCinMillis,false);
	}
	/**
	 * 지정된 밀리초를 인간이 읽을 수 있는 날짜로 반환합니다. 
	 * 기기의 시간대 및 표기법을 지킵니다.
	 * 
	 * @param UTCinMillis (ms, 밀리초) 1970년 1월 1일 00:00:00 UTC 를 처음으로, 흐른 시간.
	 * @return 인간이 읽을 수 있는 날짜. (기기 시간대)
	 */
	public static final String getDate_inCurrentTimeZone(long UTCinMillis){
		return getTime_inCurrentTimeZone(UTCinMillis,true);
	}
	
	/**
	 * 지정된 밀리초를 인간이 읽을 수 있는 날짜 또는 시간으로 반환합니다.
	 * 기기의 시간대 및 표기법을 지킵니다.
	 * 
	 * @param UTCinMillis (ms, 밀리초) 1970년 1월 1일 00:00:00 UTC 를 처음으로, 흐른 시간.
	 * @param onlyDate 날짜만 표기 >> true
	 * @return 인간이 읽을 수 있는 날짜 또는 시각. (기기 시간대)
	 */
	public static final String getTime_inCurrentTimeZone(final long UTCinMillis,final boolean onlyDate){
		if(UTCinMillis > 0){
			Calendar calenderPlane = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			calenderPlane.setTimeInMillis(UTCinMillis);
			calenderPlane.setTimeZone(TimeZone.getDefault());

			//java.text.DateFormat simpleDateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT,Locale.getDefault());
			/*
			if(onlyDate){
				simpleDateFormatter.applyPattern(simpleDateFormatter.toLocalizedPattern()
						.replace("yy", "yyyy")
						.replace("h", "")
						.replace("k", "")
						.replace("m", "")
						.replace("s", "")
						.replace("a", "")
						.replace("H", "")
						.replace("K", "")
						.replace("S", "")
						.replace(":", "")
						.trim()
						);
			}else{
				simpleDateFormatter.applyPattern(simpleDateFormatter.toLocalizedPattern().replace("yy", "yyyy"));*/
			//}
			if(onlyDate){
				return (SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT,Locale.getDefault())).format(calenderPlane.getTime());
			}else{
				//http://developer.android.com/reference/java/text/SimpleDateFormat.html
				//The most useful non-localized pattern is "yyyy-MM-dd HH:mm:ss.SSSZ",
				//which corresponds to the ISO 8601 international standard date format.
				SimpleDateFormat simpleDateFormatter = new SimpleDateFormat();
				simpleDateFormatter.applyPattern(simpleDateFormatter.toLocalizedPattern().replace("yy", "yyyy"));
				return simpleDateFormatter.format(calenderPlane.getTime());
			}
		}else{
			return null;//"HCSTR_ not yet refreshed";
		}
	}

	//2.[앱] 에그 접근 주소 설정
	/**
	 * 앱에서 사용할 기준 게이트웨이 IP를 설정합니다.
	 * 이론상 IPv4, IPv6 모두 가능할 것입니다.
	 * 
	 * @param Address 앱이 접근하도록 할 IP주소. (NULL 가능)
	 */
	public void setEggIP(String Address){
		//WifiManager 게이트웨이 주소로 뽑아온 걸 단순 캐싱시킬 생각이에요.
		synchronized (settingsSyncObject){
			Egg_IP = Address;//
			
			if(isDebug){
				Log.i(TAG_LOG,"settings on thread-safe change, using this as gateway's >> "+Egg_IP);
			}
		}
	}
	
	/**
	 * 앱에서 기준삼고 있는 에그 형식을 반환합니다. 
	 * 
	 * @return
	 */
	public byte getEggType(){
		return Egg_Type;
	}
	/**
	 * 앱에서 기준삼고 있는 게이트웨이 IP를 반환합니다. 
	 * (주의: NULL일 수 있습니다.)
	 * 
	 * @return
	 */
	public String getEggIP(){
		return Egg_IP;
	}
	
	//기능 ABCD
	//0. 새로고침
	/**
	 * 에그에서 받아올 수 있는 와이브로 상태, 현재 세션 사용량을 가져옵니다.
	 * 개별 알림은 보내지 않으며, 두 알림을 묶어서 보냅니다.
	 */
	public void LocalRefresh(){
		//신호세기, 와이브로 사용량(현재세션) 받아오기
		obtainWibroStatus(false);
		obtainCurrentSessionWibroUsed(false);
		
		//UI새로고침 요청
		sendLocalRefreshHasCompletedEvent();
	}
	
	/**
	 * HTTP POST 요청을 수행합니다.
	 * 401 UNAUTHORIZED 인 경우 로그인 실패 알림을 보입니다.
	 * 
	 * @param targetAddress 주소
	 * @param httpPOSTbody HTTP POST 내용
	 * @return 요청 성공 여부.
	 */
	private boolean httpPOST_withTrackAuthValid(String targetAddress,String httpPOSTbody){
		switch(httpRequestPOST(targetAddress,httpPOSTbody).getHttpResponseStatusCode()){
		case HttpURLConnection.HTTP_UNAUTHORIZED:
			setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			return false;
		default:
			return true;
		}
	}

	//A. 에그 끄기
	/**
	 * 에그의 전원을 끄도록 요청합니다.
	 * 기기마다 구현이 따로 되어있어야합니다.
	 * 
	 * @return 요청 성공 여부.
	 * (주의: 지원되지 않는 기기일 때엔 항상 false가 반환됩니다)
	 */
	public boolean PowerOff(){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
			return httpPOST_withTrackAuthValid(Egg_IP+"/goform/mobile_submit","pwof");
		case EGG_KWDB2600_PC_PAGE:
			break;
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			//return httpPOST_withTrackAuthValid(Egg_IP+"??????????????????","????????????????");
			return httpPOST_withTrackAuthValid(Egg_IP+"/cgi-bin/upgrademain.cgi","act=act_system_reboot&param=POWEROFF");
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" PowerOff()","the App is working for Unrecognized Device!! This should not happen!");
			}
		}
		showShortToast(R.string.operation_not_supported_on_this_device);
		return false;
	}
	

	//B. 에그 최대절전모드
	/**
	 * 에그가 최대절전모드로 들어가도록 요청합니다.
	 * 기기마다 구현이 따로 되어있어야합니다.
	 * 
	 * @return 요청 성공 여부
	 * (주의: 지원되지 않는 기기일 때엔 항상 false가 반환됩니다)
	 */
	public boolean Hibernate(){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
			return httpPOST_withTrackAuthValid(Egg_IP+"/goform/deep_sleep","dummy=");
		case EGG_KWDB2600_PC_PAGE:
			break;
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			//return httpPOST_withTrackAuthValid(Egg_IP+"????????????","????????????");
			break;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" Hibernate()","the App is working for Unrecognized Device!! This should not happen!");
			}
		}
		showShortToast(R.string.operation_not_supported_on_this_device);
		return false;
	}
	
	//C. 에그 재시작
	/**
	 * 에그가 재시작하도록 요청합니다.
	 * 기기마다 구현이 따로 되어있어야합니다.
	 * 
	 * @return 요청 성공 여부
	 * (주의: 지원되지 않는 기기일 때엔 항상 false가 반환됩니다)
	 */
	public boolean Reboot(){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
			return httpPOST_withTrackAuthValid(Egg_IP+"/goform/mobile_submit","reboot");
		case EGG_KWDB2600_PC_PAGE:
			return httpPOST_withTrackAuthValid(Egg_IP+"/goform/reboot_system","dummy=");
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			//return httpPOST_withTrackAuthValid(Egg_IP+"???????????????","????????");
			return httpPOST_withTrackAuthValid(Egg_IP+"/cgi-bin/upgrademain.cgi","act=act_system_reboot&param=REBOOT");
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" Reboot()","the App is working for Unrecognized Device!! This should not happen!");
			}
		}
		showShortToast(R.string.operation_not_supported_on_this_device);
		return false;
	}
	
	//D. 에그 켜진시간 확인
	/**
	 * 에그의 와이브로 사용 시간이 들어있는 문자열을 반환합니다.
	 * 기기에 따라 값이 다를 수 있습니다.
	 * 
	 * @see switchString_WibroElapsedTime(~)
	 * 
	 * @return 기기에서 미리 가져온 와이브로 사용시간 문자열. (초기엔 NULL일 가능성이 높다)
	 */
	public String getRaw_WibroElapsedTime(){
		return Wibro_ElapsedTime;
	}
	public String getProcessed_WibroElapsedTime(){
		if(Wibro_ElapsedTime != null)
		{
			if(Wibro_ElapsedTime.equals("N/A")){
				return getString(R.string.wb_status_notconnected);
			}
			if(Wibro_ElapsedTime.equals("00:00:00 sec")){
				 return getString(R.string.wb_status_notconnected);
			}
			if(Wibro_ElapsedTime.length() > 8){
				Wibro_ElapsedTime = Wibro_ElapsedTime.substring(0, 8);
			}	
			return Wibro_ElapsedTime+getString(R.string.item_postfix_elapsed);
		}
		return getString(R.string.wb_status_notconnected);
	}
	
	
	
	//E. 신호세기 불러오기
	/**
	 * 에그에 접속하여 에그의 와이브로 상태 를 불러옵니다.
	 * 기기마다 구현이 따로 되어있어야합니다.
	 * 
	 * @param isIndividual 새로고침 알림을 요청하려면 >> true
	 * @return 받아오는데 성공했다면 << true
	 */
	public boolean obtainWibroStatus(boolean isIndividual){
		boolean wasSuccessful = false;
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_PC_PAGE:
			wasSuccessful = obtainWibroStatus_StrongEgg();

			break;
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			wasSuccessful = obtainWibroStatus_CompactEgg();
			//page4ObtainSignalStrength = Egg_IP+???
			break;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" obtainSignalStrength()","the App is working for Unrecognized Device!! This should not happen!");
			}
		}
		if(isIndividual && wasSuccessful){
			sendRefreshWibroStatusHasCompletedEvent();
		}
		return wasSuccessful;
	}

	public boolean obtainWibroSignalOnly(boolean isIndividual){
		boolean wasSuccessful = false;
		switch(Egg_Type){
		case EGG_KWFB2700_v2234:
			wasSuccessful = obtainWibroSignalOnly_CompactEgg();
			//page4ObtainSignalStrength = Egg_IP+???
			break;
		default://스트롱에그1 은 한번에 해결됬으므로 그냥 쓴다.
			wasSuccessful = obtainWibroStatus(isIndividual);
		}
		return wasSuccessful;
	}
	
	
	//스트롱에그 1640을 기준으로 제작된 와이브로 정보 받아오기.
	/**
	 * (스트롱에그 전용)
	 * 스트롱에그에 접속하여 스트롱에그의 와이브로 상태 를 불러옵니다.
	 * 
	 * 얻어올 수 있는 정보는 다음과 같습니다(10개):
	 * 와이브로 IP,
	 * 와이브로 게이트웨이,
	 * 와이브로 DNS(주),
	 * 와이브로 DNS(부),
	 * 와이브로 수신상태(간단),
	 * 와이브로 상태,
	 * 와이브로 이용시간,
	 * 와이브로 채널,
	 * 에그 배터리(간단),
	 * 와이브로 수신상태(상세, CINR / RSSI)
	 * 
	 * @return 받아오는데 성공했다면 << true
	 */
	public boolean obtainWibroStatus_StrongEgg(){
		SEM_HttpResponse httpRESPONSE = httpRequestGET(Egg_IP+"/admin/RS_getWiMAXInfo.asp",true);
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				String[] EggWibroInfoStrings = parsePage_WibroInfo_StrongEgg(httpRESPONSE.getHttpResponseBody());
				if(EggWibroInfoStrings != null){
					synchronized(statusSyncObject){
							Wibro_IP = EggWibroInfoStrings[1];
							Wibro_Gateway = EggWibroInfoStrings[2];
							Wibro_DnsPrimary = EggWibroInfoStrings[3];
							Wibro_DnsSecondary = EggWibroInfoStrings[4];
							Wibro_SignalSimple = EggWibroInfoStrings[5];
							Wibro_Status = EggWibroInfoStrings[6];
							Wibro_ElapsedTime = EggWibroInfoStrings[7];
//							Wibro_Channel = EggWibroInfoStrings[8];
							Wibro_BatteryLevel = EggWibroInfoStrings[9];
							Wibro_SignalDetailed = EggWibroInfoStrings[10];
					}
					EggWibroInfoStrings = null;
					return true;
				}else{
					return false;
				}
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
				return false;
			}
		}
		return false;
	}
	
	public boolean obtainWibroStatus_CompactEgg(){
		boolean wasSuccessful = false;
		SEM_HttpResponse httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_network_info&param=DEV_NAME,DEV_MF,MAC_ADDR,DEV_STATUS,WI_IP_ADDR,WI_SUB_MASK,WI_DEF_GATEWAY,WI_FST_DNS,WI_SEC_DNS,",true);
		String[] EggWibroInfo_network_Strings = null;
		String[] EggWibroInfo_battery_Strings = null;
		String[] EggWibroInfo_physical_Strings = null;
		String EggWibroInfo_usetime_String = null;
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				EggWibroInfo_network_Strings = parsePage_WibroInfo_network_CompactEgg(httpRESPONSE.getHttpResponseBody());
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
			}
		}
		
		httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_battery_status&TYPE=BISCUIT",true);
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				EggWibroInfo_battery_Strings = parsePage_WibroInfo_battery_CompactEgg(httpRESPONSE.getHttpResponseBody());
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
			}
		}
		
		httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_wimax_status&param=WIMAX_PHY_STATUS,WIMAX_LINK_STATUS",true);
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				EggWibroInfo_physical_Strings = parsePage_WibroInfo_physical_CompactEgg(httpRESPONSE.getHttpResponseBody());
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
			}
		}
		
		httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_wimax_conn_data&param=use_time",true);
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				EggWibroInfo_usetime_String = parsePage_WibroInfo_usetime_CompactEgg(httpRESPONSE.getHttpResponseBody());
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
			}
		}
		
		
		//CompactEgg는 방식이 달라서 따로 반영해야한다.
		if(EggWibroInfo_network_Strings != null &&
				EggWibroInfo_battery_Strings != null &&
				EggWibroInfo_physical_Strings != null &&
				EggWibroInfo_usetime_String != null){
						synchronized(statusSyncObject){
								Wibro_IP = EggWibroInfo_network_Strings[0];
								Wibro_Gateway = EggWibroInfo_network_Strings[2];
								Wibro_DnsPrimary = EggWibroInfo_network_Strings[3];
								Wibro_DnsSecondary = EggWibroInfo_network_Strings[4];
								Wibro_SignalSimple = parseSignal_CompactEgg(EggWibroInfo_physical_Strings[2],EggWibroInfo_physical_Strings[1])+"";
								Wibro_Status = EggWibroInfo_network_Strings[7];
								Wibro_ElapsedTime = EggWibroInfo_usetime_String;
//								Wibro_Channel = EggWibroInfo_physical_Strings[3];
								Wibro_BatteryLevel = EggWibroInfo_battery_Strings[1];
								Wibro_SignalDetailed = EggWibroInfo_physical_Strings[2] + COMMON_STR_SLASHWITHSPACES + EggWibroInfo_physical_Strings[1];
						}
						wasSuccessful = true;
		}
		EggWibroInfo_usetime_String = null;
		EggWibroInfo_physical_Strings = null;
		EggWibroInfo_battery_Strings = null;
		EggWibroInfo_network_Strings = null;
		
		return wasSuccessful;
	}
	
	public boolean obtainWibroSignalOnly_CompactEgg(){
		boolean wasSuccessful = false;
		SEM_HttpResponse httpRESPONSE = httpRequestGET(Egg_IP+"/cgi-bin/webmain.cgi?act=act_wimax_status&param=WIMAX_PHY_STATUS,WIMAX_LINK_STATUS",true);
	
		String[] EggWibroInfo_physical_Strings = null;
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				EggWibroInfo_physical_Strings = parsePage_WibroInfo_physical_CompactEgg(httpRESPONSE.getHttpResponseBody());
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
			}
		}

		//CompactEgg는 방식이 달라서 따로 반영해야한다.
		if(EggWibroInfo_physical_Strings != null){

			synchronized(statusSyncObject){
					Wibro_SignalSimple = parseSignal_CompactEgg(EggWibroInfo_physical_Strings[2],EggWibroInfo_physical_Strings[1])+"";
					Wibro_SignalDetailed = EggWibroInfo_physical_Strings[2] + COMMON_STR_SLASHWITHSPACES + EggWibroInfo_physical_Strings[1];
			}
			wasSuccessful = true;
		}
		EggWibroInfo_physical_Strings = null;
		
		return wasSuccessful;
	}
	
	//스트롱에그 1640을 기준으로 제작된 와이브로 정보 받아오기.
	/**
	 * (스트롱에그 전용)
	 * 스트롱에그에서 얻어온 페이지를 분석하여 와이브로 상태를 읽어냅니다.
	 * 
	 * @param planetextStatus_StrongEgg 스트롱에그가 제공한 HTTP Response 내용
	 * @return
	 * (주의: 분석 도중 문제가 발생한 경우 NULL이 반환됩니다.)
	 * 스트롱 에그가 제공한 와이브로 상태 정보가 들어있는 10원소 문자열 배열:
	 * 와이브로 IP,
	 * 와이브로 게이트웨이,
	 * 와이브로 DNS(주),
	 * 와이브로 DNS(부),
	 * 와이브로 수신상태(간단),
	 * 와이브로 상태,
	 * 와이브로 이용시간,
	 * 와이브로 채널,
	 * 에그 배터리(간단),
	 * 와이브로 수신상태(상세, CINR / RSSI)
	 */
	private static final String[] parsePage_WibroInfo_StrongEgg(String planetextStatus_StrongEgg){
		if(planetextStatus_StrongEgg != null){
			try{
				planetextStatus_StrongEgg = planetextStatus_StrongEgg.substring(planetextStatus_StrongEgg.indexOf("callback_SetWiMAXInfo( ["),planetextStatus_StrongEgg.indexOf("] );"));
				String[] str_StrongEggWibroStatus = planetextStatus_StrongEgg.split(",");
				planetextStatus_StrongEgg = null;
							
				//EggInfoString.substring(EggInfoString.indexOf("[")+1, EggInfoString.indexOf("\"]")-1);
				//EggWiBroInfoStrings = EggInfoString.split(",");
				//기기 시간은 중요하지 않아! 서.. 1부터 시작..(..)
		
				byte currentIndexOfInfoStrings = 0; 
				
				for(String EggWiBroInfoString : str_StrongEggWibroStatus) {//int i = 1;i < EggWiBroInfoStrings.length; i++)
					if(currentIndexOfInfoStrings > 0 ){
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] get << "+EggWiBroInfoString);
						}
						if(EggWiBroInfoString.endsWith("\"\"")){
							str_StrongEggWibroStatus[currentIndexOfInfoStrings] = COMMON_STR_EMPTY;
						}else{
							str_StrongEggWibroStatus[currentIndexOfInfoStrings] = EggWiBroInfoString.substring(EggWiBroInfoString.indexOf("\"")+1,EggWiBroInfoString.length()-1);// 이 방법. indexOutOfBoundsException 가 나타나서 안된다.아쉽네
		
							//http://stackoverflow.com/questions/2088037/trim-characters-in-java
							//
							//String b = a.replaceAll("y$|^x", ""); // will remove all the y from the end and x from the beggining
						}
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] Parsed >> "+str_StrongEggWibroStatus[currentIndexOfInfoStrings]);//+EggWiBroInfoString);
						}
					}
					currentIndexOfInfoStrings++;
				}
				currentIndexOfInfoStrings = -1;
		
				return str_StrongEggWibroStatus;
				//신호세기 확인
				//window.parent.callback_SetWiMAXInfo( ["2011-03-17 12:16:33 PM", "",
				//                                  	"", "", "", "0",
				//                                  	"2", "00:00:00 sec", "0", "5", "0 / 0"] );
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.w(TAG_LOG,"IndexOutOfBoundsException while parseWibroStatus_StrongEgg(). returning NULL");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}
	
	private static final String[] parsePage_WibroInfo_network_CompactEgg(String planetextStatus_CompactEgg){
		if(planetextStatus_CompactEgg != null){
			try{
				//                                                                    0                                 1                                    2                               3                               4                             5                        6                           7                          8  
				//planetextStatus_CompactEgg="{\"result\":\"0\",\"data\":{\"WI_IP_ADDR\":\"***.***.***.***\",\"WI_SUB_MASK\":\"255.255.255.0\",\"WI_DEF_GATEWAY\":\"***.***.***.1\",\"WI_FST_DNS\":\"168.126.63.1\",\"WI_SEC_DNS\":\"168.126.63.2\",\"DEV_NAME\":\"KWF-B2700\",\"DEV_MF\":\"INFOMARK\",\"DEV_STATUS\":\"CONNECTED\",\"MAC_ADDR\":\"**:**:**:**:**:**\",\"dummy09\":\"XX\"}";
				planetextStatus_CompactEgg = planetextStatus_CompactEgg.substring(planetextStatus_CompactEgg.indexOf("\"data\":{")+9,planetextStatus_CompactEgg.indexOf("\",\"dummy09\""));
				String[] str_CompactEggWibroStatus = planetextStatus_CompactEgg.split("\",\"");
				planetextStatus_CompactEgg = null;
							
				//EggInfoString.substring(EggInfoString.indexOf("[")+1, EggInfoString.indexOf("\"]")-1);
				//EggWiBroInfoStrings = EggInfoString.split(",");
				//기기 시간은 중요하지 않아! 서.. 1부터 시작..(..)
		
				byte currentIndexOfInfoStrings = 0; 
				
				str_CompactEggWibroStatus[1] = null;
				str_CompactEggWibroStatus[5] = null;
				str_CompactEggWibroStatus[6] = null;
				str_CompactEggWibroStatus[8] = null;
				
				for(String EggWiBroInfoString : str_CompactEggWibroStatus) {//int i = 1;i < EggWiBroInfoStrings.length; i++)
					if(EggWiBroInfoString != null){
						
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] get << "+EggWiBroInfoString);
						}
						
						str_CompactEggWibroStatus[currentIndexOfInfoStrings] = EggWiBroInfoString.substring(EggWiBroInfoString.indexOf("\":\"")+3);
							//http://stackoverflow.com/questions/2088037/trim-characters-in-java
							//
							//String b = a.replaceAll("y$|^x", ""); // will remove all the y from the end and x from the beggining
						
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] Parsed >> "+str_CompactEggWibroStatus[currentIndexOfInfoStrings]);//+EggWiBroInfoString);
						}
					}
					currentIndexOfInfoStrings++;
				}
				currentIndexOfInfoStrings = -1;
		
				return str_CompactEggWibroStatus;
				
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.w(TAG_LOG,"IndexOutOfBoundsException while parseWibroStatus_CompactEgg(). returning NULL");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}
	
	private static final String[] parsePage_WibroInfo_physical_CompactEgg(String planetextStatus_CompactEgg){
		if(planetextStatus_CompactEgg != null){
			try{
				//planetextStatus_CompactEgg="{\"result\":\"0\",\"data\":{\"tx_power\":\"1\",\"rssi\":\"-70\",\"cinr\":\"25\",\"cf\":\"2355000\",\"bsid\":\"**:**:**:**:**:**\",\"WIMAX_LINK_STATUS\":\"SUCCESS\",\"psbsid\":\"**-**-**-**-**-**\",\"psbw\":\"10000\",\"psfreq\":\"2355000\",\"frmnum\":\"12980784\",\"fch\":\"0xfca160\",\"ttg\":\"296\",\"rtg\":\"168\",\"nds\":\"29\",\"nus\":\"18\",\"pix\":\"91\",\"mac_state\":\"3\",\"ppix\":\"4\",\"maca\":\"**:**:**:**:**:**\",\"cme\":\"25\",\"csd\":\"15\",\"rssm\":\"-70\",\"rsd\":\"-80\",\"cam\":\"0\",\"cbm\":\"0\",\"cinrmain\":\"22\",\"cinrdiver\":\"22\",\"cinr_p1\":\"26\",\"cinr_p3\":\"27\",\"rssimain\":\"-79\",\"rssidiver\":\"-71\",\"pmode\":\"open loop(retention)\",\"atp\":\"2\",\"asp\":\"-11\",\"ltp\":\"1\",\"lsp\":\"-12\",\"maxtp\":\"0\",\"maxsp\":\"-13\",\"mintp\":\"10\",\"minsp\":\"-7\",\"WIMAX_PHY_STATUS\":\"SUCCESS\",\"dummy09\":\"XX\"}";
				planetextStatus_CompactEgg = planetextStatus_CompactEgg.substring(planetextStatus_CompactEgg.indexOf("\"data\":{")+9,planetextStatus_CompactEgg.indexOf("\",\"dummy09\""));
				String[] str_CompactEggWibroStatus = planetextStatus_CompactEgg.split("\",\"",5);
				planetextStatus_CompactEgg = null;
							
				//EggInfoString.substring(EggInfoString.indexOf("[")+1, EggInfoString.indexOf("\"]")-1);
				//EggWiBroInfoStrings = EggInfoString.split(",");
				//기기 시간은 중요하지 않아! 서.. 1부터 시작..(..)
		
				byte currentIndexOfInfoStrings = 0; 
				
				str_CompactEggWibroStatus[0] = null;
				str_CompactEggWibroStatus[4] = null;
				
				for(String EggWiBroInfoString : str_CompactEggWibroStatus) {//int i = 1;i < EggWiBroInfoStrings.length; i++)
					if(EggWiBroInfoString != null){
						if(EggWiBroInfoString.contains("rssi") || EggWiBroInfoString.contains("cinr")){
							if(isDebug){
								Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] get << "+EggWiBroInfoString);
							}
							
							str_CompactEggWibroStatus[currentIndexOfInfoStrings] = EggWiBroInfoString.substring(EggWiBroInfoString.indexOf("\":\"")+3);
								//http://stackoverflow.com/questions/2088037/trim-characters-in-java
								//
								//String b = a.replaceAll("y$|^x", ""); // will remove all the y from the end and x from the beggining
							
							if(isDebug){
								Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] Parsed >> "+str_CompactEggWibroStatus[currentIndexOfInfoStrings]);//+EggWiBroInfoString);
							}
						}else{
							str_CompactEggWibroStatus[currentIndexOfInfoStrings] = COMMON_STR_NUM_0;
						}
					
					}
					currentIndexOfInfoStrings++;
				}
				currentIndexOfInfoStrings = -1;
		
				return str_CompactEggWibroStatus;
				
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.w(TAG_LOG,"IndexOutOfBoundsException while parseWibroStatus_CompactEgg(). returning NULL");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}
	
	private static final String[] parsePage_WibroInfo_battery_CompactEgg(String planetextStatus_CompactEgg){
		if(planetextStatus_CompactEgg != null){
			try{
				//planetextStatus_CompactEgg="{\"result\":\"0\",\"data\":{\"STATUS\":\"charging\",\"LEVEL\":\"0\",\"dummy09\":\"XX\"},";
				planetextStatus_CompactEgg = planetextStatus_CompactEgg.substring(planetextStatus_CompactEgg.indexOf("\"data\":{")+9,planetextStatus_CompactEgg.indexOf("\",\"dummy09\""));
				String[] str_CompactEggWibroStatus = planetextStatus_CompactEgg.split("\",\"");
				planetextStatus_CompactEgg = null;
				
				//EggInfoString.substring(EggInfoString.indexOf("[")+1, EggInfoString.indexOf("\"]")-1);
				//EggWiBroInfoStrings = EggInfoString.split(",");
				//기기 시간은 중요하지 않아! 서.. 1부터 시작..(..)
				
				byte currentIndexOfInfoStrings = 0; 
				str_CompactEggWibroStatus[0] = null;

				for(String EggWiBroInfoString : str_CompactEggWibroStatus) {//int i = 1;i < EggWiBroInfoStrings.length; i++)
					if(EggWiBroInfoString != null){
						
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] get << "+EggWiBroInfoString);
						}
						
						str_CompactEggWibroStatus[currentIndexOfInfoStrings] = EggWiBroInfoString.substring(EggWiBroInfoString.indexOf("\":\"")+3);
							//http://stackoverflow.com/questions/2088037/trim-characters-in-java
							//
							//String b = a.replaceAll("y$|^x", ""); // will remove all the y from the end and x from the beggining
						
						if(isDebug){
							Log.v(TAG_LOG, "EggWibroInfoStrings["+currentIndexOfInfoStrings+"] Parsed >> "+str_CompactEggWibroStatus[currentIndexOfInfoStrings]);//+EggWiBroInfoString);
						}
					}
					currentIndexOfInfoStrings++;
				}
				currentIndexOfInfoStrings = -1;
		
				return str_CompactEggWibroStatus;
				//신호세기 확인
				
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.w(TAG_LOG,"IndexOutOfBoundsException while parseWibroStatus_CompactEgg(). returning NULL");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}
	
	private static final String parseSignal_CompactEgg(String strCinr, String strRssi){
		if(strCinr != null && strRssi != null){
			if(strRssi == COMMON_STR_NUM_0)// && strCinr == COMMON_STR_NUM_1N)
			{//RSSI가 0이 될 때는 시그널 없을 때 뿐이므로..!
				return COMMON_STR_NUM_0;
			}
			//컴팩트 에그 웹CM의 것을 그대로 가져와씁니다.
			byte rssi = Byte.parseByte(strRssi);
			byte cinr = Byte.parseByte(strCinr);
			
			strRssi = null;
			strCinr = null;
			
			if (rssi > -55)
			{
				if		(cinr > 15) 	return COMMON_STR_NUM_5;
				else if (cinr > 10) 	return COMMON_STR_NUM_5;
				else if (cinr >  3) 	return COMMON_STR_NUM_3;
				else if (cinr >  0) 	return COMMON_STR_NUM_2;
				else if (cinr > -3) 	return COMMON_STR_NUM_1;
				else					return COMMON_STR_NUM_0;
			}
			else if (rssi > -65)
			{
				if		(cinr > 15) 	return COMMON_STR_NUM_5;
				else if (cinr > 10) 	return COMMON_STR_NUM_4;
				else if (cinr >  3) 	return COMMON_STR_NUM_2;
				else if (cinr >  0) 	return COMMON_STR_NUM_1;
				else if (cinr > -3) 	return COMMON_STR_NUM_1;
				else					return COMMON_STR_NUM_0;
			}
			else if (rssi > -75)
			{
				if		(cinr > 15) 	return COMMON_STR_NUM_4;
				else if (cinr > 10) 	return COMMON_STR_NUM_3;
				else if (cinr >  3) 	return COMMON_STR_NUM_1;
				else if (cinr >  0) 	return COMMON_STR_NUM_1;
				else if (cinr > -3) 	return COMMON_STR_NUM_0;
				else					return COMMON_STR_NUM_0;
			}
			else if (rssi > -84)
			{
				if		(cinr > 15) 	return COMMON_STR_NUM_2;
				else if (cinr > 10) 	return COMMON_STR_NUM_1;
				else if (cinr >  3) 	return COMMON_STR_NUM_1;
				else if (cinr >  0) 	return COMMON_STR_NUM_1;
				else if (cinr > -3) 	return COMMON_STR_NUM_0;
				else					return COMMON_STR_NUM_0;
			}
			else
				return COMMON_STR_NUM_0;
		}
		return COMMON_STR_NUM_1N;
	}
	
	private static final String parsePage_WibroInfo_usetime_CompactEgg(String planetextStatus_CompactEgg){
		if(planetextStatus_CompactEgg != null){
			try{
				//planetextStatus_CompactEgg="{\"result\":\"0\",\"data\":{\"use_time\":\"00:46:13\",\"dummy09\":\"XX\"}";
				planetextStatus_CompactEgg = planetextStatus_CompactEgg.substring(planetextStatus_CompactEgg.indexOf("\"data\":{")+9,planetextStatus_CompactEgg.indexOf("\",\"dummy09\""));
				
				return planetextStatus_CompactEgg.substring(planetextStatus_CompactEgg.indexOf("\":\"")+3);
				
			}catch(IndexOutOfBoundsException ioobe){
				if(isDebug){
					Log.w(TAG_LOG,"IndexOutOfBoundsException while parseWibroStatus_CompactEgg(). returning NULL");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}
	
	
	
	
	/**
	 * 에그의 와이브로 수신 상태(간단)이 들어있는 문자열을 반환합니다.
	 * 기기에 따라 값이 다를 수 있습니다.
	 * 
	 * @see switchString_SignalStrength(String signalStrength)
	 * 
	 * @return 기기에서 미리 가져온 와이브로 사용시간 문자열. (초기엔 NULL일 가능성이 높다)
	 */
	public String getRaw_WibroSignalStrengthSimple(){
		return Wibro_SignalSimple;
	}
	
	/**
	 * 현재 마지막으로 받아온 것을 기준으로
	 * 와이브로 수신 상태(간단) 문자열을 반환합니다.
	 * 기기에 맞춰 구현해야한다.
	 * 
	 * @return 기기 중립적인 와이브로 수신 상태(간단) 문자열.
	 */
	public String switchString_SignalStrength(){
		return switchString_SignalStrength(Wibro_SignalSimple);
	}
	/**
	 * 와이브로 수신 상태(간단) 문자열을 반환합니다.
	 * 기기에 맞춰 구현해야한다.
	 * 
	 * @param signalStrength 와이브로 수신 상태(간단)이 들어있는 문자열. (null일 수 있다)
	 * @return 기기 중립적인 와이브로 수신 상태(간단) 문자열.
	 */
	public String switchString_SignalStrength(String signalStrength){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
	        //break;
		case EGG_KWDB2600_PC_PAGE:
			return switchString_SignalStrength(parseSignalStrength_StrongEgg(signalStrength));
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			return switchString_SignalStrength(parseSignalStrength_CompactEgg(signalStrength));
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" switchString_SignalStrength()","the App is working for Unrecognized Device!! This should not happen!");
			}
			break;
		}
		return getString(R.string.wb_signal_unknown);
	}
	
	/**
	 * 와이브로 수신 상태(간단) 문자열을 반환합니다.
	 * 
	 * @param signalStrength 기기 중립적인 와이브로 수신 상태(간단).
	 * @return 기기 중립적인 와이브로 수신 상태(간단) 문자열.
	 */
	private final String switchString_SignalStrength(byte signalStrength){
		//Wibro_SignalSimpleNonParsed
		switch(signalStrength){
		case WIBRO_SIGNALSIMPLE_VERYSTRONG:
			return getString(R.string.wb_signal_very_strong);
		case WIBRO_SIGNALSIMPLE_STRONG:
			return getString(R.string.wb_signal_strong);
		case WIBRO_SIGNALSIMPLE_LESSSTRONG:
			return getString(R.string.wb_signal_less_strong);
		case WIBRO_SIGNALSIMPLE_NORMAL:
			return getString(R.string.wb_signal_normal);
		case WIBRO_SIGNALSIMPLE_LESSWEAK:
			return getString(R.string.wb_signal_less_weak);
		case WIBRO_SIGNALSIMPLE_WEAK:
			return getString(R.string.wb_signal_weak);
		case WIBRO_SIGNALSIMPLE_LOST:
			return getString(R.string.wb_signal_lost);
		default:
			return getString(R.string.wb_signal_unknown);
		}
	}
	
	public int switchResId_SignalStrength(boolean forceDark){
		return switchResId_SignalStrength(Wibro_SignalSimple,forceDark);
	}
	/**
	 * 와이브로 수신 상태(간단) 아이콘 ID를 반환합니다.
	 * 어두운 아이콘 사용 설정을 지킵니다.
	 * 기기에 맞춰 구현해야한다.
	 * 
	 * @param signalStrength 와이브로 수신 상태(간단)이 들어있는 문자열. (null일 수 있다)
	 * @return 기기 중립적인 와이브로 수신 상태 아이콘 ID.
	 */
	
	public int switchResId_SignalStrength(String signalStrength,boolean forceDark){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
	        //break;
		case EGG_KWDB2600_PC_PAGE:
			return switchResId_SignalStrength(parseSignalStrength_StrongEgg(signalStrength),App_useDarkIconSet||forceDark);
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			return switchResId_SignalStrength(parseSignalStrength_CompactEgg(signalStrength),App_useDarkIconSet||forceDark);
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" switchResId_SignalStrength()","the App is working for Unrecognized Device!! This should not happen!");
			}
			return (forceDark||App_useDarkIconSet) ? (R.drawable.ic_stat_notify_warning_dark):(R.drawable.ic_stat_notify_warning);
		}
	}
	
	/**
	 * 와이브로 수신 상태(간단) 아이콘 ID를 반환합니다.
	 * 
	 * @param signalStrength 기기 중립적인 와이브로 상태.
	 * @param isDark 어두운 아이콘 >> true || false << 밝은 아이콘
	 * @return 기기 중립적인 와이브로 수신 상태 아이콘 ID.
	 */
	private static final int switchResId_SignalStrength(byte signalStrength,boolean isDark){
		//아이콘 받기
		switch(signalStrength){
		case WIBRO_SIGNALSIMPLE_VERYSTRONG:
			return isDark ? (R.drawable.ic_stat_notify_signal_4_dark):(R.drawable.ic_stat_notify_signal_4);
		case WIBRO_SIGNALSIMPLE_STRONG:
			return isDark ? (R.drawable.ic_stat_notify_signal_3_dark):(R.drawable.ic_stat_notify_signal_3);
		case WIBRO_SIGNALSIMPLE_LESSSTRONG:
			return isDark ? (R.drawable.ic_stat_notify_signal_3_dark):(R.drawable.ic_stat_notify_signal_3);
		case WIBRO_SIGNALSIMPLE_NORMAL:
			return isDark ? (R.drawable.ic_stat_notify_signal_2_dark):(R.drawable.ic_stat_notify_signal_2);
		case WIBRO_SIGNALSIMPLE_LESSWEAK:
			return isDark ? (R.drawable.ic_stat_notify_signal_1_dark):(R.drawable.ic_stat_notify_signal_1);
		case WIBRO_SIGNALSIMPLE_WEAK:
			return isDark ? (R.drawable.ic_stat_notify_signal_0_dark):(R.drawable.ic_stat_notify_signal_0);
		case WIBRO_SIGNALSIMPLE_LOST:
			return isDark ? (R.drawable.ic_stat_notify_signal_lost_dark):(R.drawable.ic_stat_notify_signal_lost);
		case WIBRO_SIGNALSIMPLE_WAITING:
			return isDark ? (R.drawable.ic_stat_notify_waiting_dark):(R.drawable.ic_stat_notify_waiting);
		default:
			return isDark ? (R.drawable.ic_stat_notify_warning_dark):(R.drawable.ic_stat_notify_warning);
		}
	}
	/*
	private static final int switchResId_SignalStrength(byte signalStrength){
		//아이콘 받기
		switch(signalStrength){
		case WIBRO_SIGNALSIMPLE_VERYSTRONG:
			return R.drawable.ic_stat_notify_signal_4_gray;
		case WIBRO_SIGNALSIMPLE_STRONG:
			return R.drawable.ic_stat_notify_signal_3_gray;
		case WIBRO_SIGNALSIMPLE_LESSSTRONG:
			return R.drawable.ic_stat_notify_signal_3_gray;
		case WIBRO_SIGNALSIMPLE_NORMAL:
			return R.drawable.ic_stat_notify_signal_2_gray;
		case WIBRO_SIGNALSIMPLE_LESSWEAK:
			return R.drawable.ic_stat_notify_signal_1_gray;
		case WIBRO_SIGNALSIMPLE_WEAK:
			return R.drawable.ic_stat_notify_signal_0_gray;
		case WIBRO_SIGNALSIMPLE_LOST:
			return R.drawable.ic_stat_notify_signal_lost_gray;
		case WIBRO_SIGNALSIMPLE_WAITING:
			return R.drawable.ic_stat_notify_waiting_dark;
		default:
			return R.drawable.ic_stat_notify_warning_dark;
		}
	}*/
	
	
	/**
	 * 스트롱에그의 와이브로 수신 상태(간단)가 들어있는 문자열을 분석하여
	 * 기기 중립적 와이브로 수신상태(간단)으로 반환합니다.
	 * 
	 * @param signalStrength 스트롱에그의 와이브로 수신 상태(간단)이 들어있는 문자열
	 * @return 기기 중립적인 와이브로 수신 상태(간단).
	 */
	private static final byte parseSignalStrength_StrongEgg(String signalStrength){
		//Wibro_SignalSimpleNonParsed
		if(signalStrength != null){
			if(signalStrength.equals(COMMON_STR_NUM_6)){
				return WIBRO_SIGNALSIMPLE_VERYSTRONG;
			}else if(signalStrength.equals(COMMON_STR_NUM_5)){
				return WIBRO_SIGNALSIMPLE_STRONG;
			}else if(signalStrength.equals(COMMON_STR_NUM_4)){
				return WIBRO_SIGNALSIMPLE_LESSSTRONG;
			}else if(signalStrength.equals(COMMON_STR_NUM_3)){
				return WIBRO_SIGNALSIMPLE_NORMAL;
			}else if(signalStrength.equals(COMMON_STR_NUM_2)){
				return WIBRO_SIGNALSIMPLE_LESSWEAK;
			}else if(signalStrength.equals(COMMON_STR_NUM_1)){
				return WIBRO_SIGNALSIMPLE_WEAK;
			}else if(signalStrength.equals(COMMON_STR_NUM_0)){
				return WIBRO_SIGNALSIMPLE_LOST;
			}else if(signalStrength.equals(COMMON_STR_NUM_2N)){
				return WIBRO_SIGNALSIMPLE_WAITING;
			}
		}
		return WIBRO_SIGNALSIMPLE_UNKNOWN;
	}
	
	private static final byte parseSignalStrength_CompactEgg(String signalStrength){
		//Wibro_SignalSimpleNonParsed
		if(signalStrength != null){
			if(signalStrength.equals(COMMON_STR_NUM_5)){
				return WIBRO_SIGNALSIMPLE_VERYSTRONG;
			}else if(signalStrength.equals(COMMON_STR_NUM_4)){
				return WIBRO_SIGNALSIMPLE_STRONG;
			}else if(signalStrength.equals(COMMON_STR_NUM_3)){
				return WIBRO_SIGNALSIMPLE_NORMAL;
			}else if(signalStrength.equals(COMMON_STR_NUM_2)){
				return WIBRO_SIGNALSIMPLE_LESSWEAK;
			}else if(signalStrength.equals(COMMON_STR_NUM_1)){
				return WIBRO_SIGNALSIMPLE_WEAK;
			}else if(signalStrength.equals(COMMON_STR_NUM_0)){
				return WIBRO_SIGNALSIMPLE_LOST;
			}else if(signalStrength.equals(COMMON_STR_NUM_2N)){
				return WIBRO_SIGNALSIMPLE_WAITING;
			}
		}
		return WIBRO_SIGNALSIMPLE_UNKNOWN;
	}
	
	/**
	 * 현재 마지막으로 받아온 것을 기준으로
	 * 기기 중립적인 와이브로 상태 문자열을 반환합니다.
	 * 기기에 맞춰 구현해야한다.
	 * 
	 * @return 기기 중립적인 와이브로 상태 문자열.
	 */
	public String switchWibroStatus(){
		return switchWibroStatus(Wibro_Status);
	}
	/**
	 * 기기 중립적인 와이브로 상태 문자열을 반환합니다.
	 * 기기에 맞춰 구현해야한다.
	 * 
	 * @param wibroStatus 와이브로 상태가 들어있는 문자열.
	 * @return 기기 중립적인 와이브로 상태 문자열.
	 */
	public String switchWibroStatus(String wibroStatus){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
	        //break;
		case EGG_KWDB2600_PC_PAGE:
			return switchString_WibroStatus(parseWibroStatus_StrongEgg(wibroStatus));
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			return switchString_WibroStatus(parseWibroStatus_CompactEgg(wibroStatus));
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" switchString_WibroStatus()","the App is working for Unrecognized Device!! This should not happen!");
			}
			break;
		}
		return getString(R.string.wb_signal_unknown);
	}
	
	/**
	 * 기기 중립적인 와이브로 상태 문자열을 반환합니다.
	 * 
	 * @param wibroStatus 기기 중립적인 와이브로 상태.
	 * @return 기기 중립적인 와이브로 상태 문자열.
	 */
	public final String switchString_WibroStatus(byte wibroStatus){
		switch(wibroStatus){
		case WIBRO_STATUSSIMPLE_COMPLETED:
			return getString(R.string.wb_status_completed);
		case WIBRO_STATUSSIMPLE_CONNECTING:
			return getString(R.string.wb_status_connecting);
		case WIBRO_STATUSSIMPLE_WAITING:
			return getString(R.string.wb_status_waiting);
		case WIBRO_STATUSSIMPLE_INITIALIZING:
			return getString(R.string.wb_status_initializing);
		case WIBRO_STATUSSIMPLE_OUTOFRANGE:
			return getString(R.string.wb_status_outofrange);
		default:
			return getString(R.string.wb_status_unknown);
		}
	}
		
	/**
	 * 스트롱에그의 와이브로 상태가 들어있는 문자열을 분석하여
	 * 기기 중립적 와이브로 상태으로 반환합니다.
	 * 
	 * @param wibroStatus 와이브로 상태가 들어있는 문자열.
	 * @return 기기 중립적인 와이브로 상태.
	 */
	private static final byte parseWibroStatus_StrongEgg(String wibroStatus){
		if(wibroStatus != null){
			if(wibroStatus.equals(COMMON_STR_NUM_0)){
				return WIBRO_STATUSSIMPLE_INITIALIZING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_1)){
				return WIBRO_STATUSSIMPLE_WAITING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_2)){
				return WIBRO_STATUSSIMPLE_OUTOFRANGE;
			}else if(wibroStatus.equals(COMMON_STR_NUM_3)){
				return WIBRO_STATUSSIMPLE_COMPLETED;
			}else if(wibroStatus.equals(COMMON_STR_NUM_4)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_5)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_6)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_7)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_8)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}else if(wibroStatus.equals(COMMON_STR_NUM_9)){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}
		}
		return WIBRO_STATUSSIMPLE_UNKNOWN;
	}
	
	//컴팩트에그가 제공하는 문자열들을 모두 대조해볼 필요가 있다. 걸려든 건 네가지
	//READY
	//SCANNING
	//CONNECTING
	//CONNECTED
	//SUCCESS??는 WIFI-PHY-STATUS

	private static final byte parseWibroStatus_CompactEgg(String wibroStatus){
		if(wibroStatus != null){
			if(wibroStatus.equals("SCANNING")){
				return WIBRO_STATUSSIMPLE_WAITING;
			}else if(wibroStatus.equals("READY")){
				return WIBRO_STATUSSIMPLE_OUTOFRANGE;
			}else if(wibroStatus.equals("CONNECTED")){
				return WIBRO_STATUSSIMPLE_COMPLETED;
			}else if(wibroStatus.equals("CONNECTING")){
				return WIBRO_STATUSSIMPLE_CONNECTING;
			}
		}
		return WIBRO_STATUSSIMPLE_UNKNOWN;
	}

	//사용량
	/**
	 * 에그에 접속하여 에그의 와이브로 사용량(현재 세션)을 불러옵니다.
	 * 기기에 따라 값이 다를 수 있습니다.
	 * 
	 * @param isIndividual 새로고침 알림을 요청하려면 >> true
	 * @return 받아오는데 성공했다면 << true
	 */
	public boolean obtainCurrentSessionWibroUsed(boolean isIndividual){
		boolean wasSuccessful = false;
		
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_PC_PAGE:
			wasSuccessful = obtainCurrentSessionWibroUsed_StrongEgg();
			break;
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			//미지원 기능이지만..
			wasSuccessful = true;
			break;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" obtainCurrentSessionWibroUsed()","the App is working for Unrecognized Device!! This should not happen!");
			}
			//break;
		}
		if(isIndividual && wasSuccessful){
			sendRefreshWibroUsageSessionHasCompletedEvent();
		}
		return wasSuccessful;
	}
	
	/**
	 * 문자열을 끝부터 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 빈 문자열이 반환됩니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromEnd 끝부터 잘려나갈 문자 수.
	 * @return 뒤가 잘려나간 문자열 또는 빈 문자열""
	 */
	public static final String subStringEnd(String text, int trimCharsFromEnd){
		return subStringSides(text,0,trimCharsFromEnd);
	}
	/**
	 * 문자열을 끝부터 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 대체 문자열을 지정하실 수 있습니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromEnd 끝부터 잘려나갈 문자 수.
	 * @param defaultValue 기본 문자열, 문자 수가 모자라 모두 잘려나간 경우 사용합니다.
	 * @return 뒤가 잘려나간 문자열 또는 기본 문자열.
	 */
	public static final String subStringEnd(String text, int trimCharsFromEnd, String defaultValue){
		return subStringSides(text,0,trimCharsFromEnd,defaultValue);
	}
	/**
	 * 문자열을 처음부터 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 빈 문자열이 반환됩니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromStart 처음부터 잘려나갈 문자 수.
	 * @return 앞이 잘려나간 문자열 또는 빈 문자열""
	 */
	public static final String subStringBegin(String text, int trimCharsFromStart){
		return subStringSides(text,trimCharsFromStart,0);
	}
	/**
	 * 문자열을 처음부터 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 대체 문자열을 지정하실 수 있습니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromStart 처음부터 잘려나갈 문자 수.
	 * @param defaultValue 기본 문자열, 문자 수가 모자라 모두 잘려나간 경우 사용합니다.
	 * @return 앞이 잘려나간 문자열 또는 기본 문자열.
	 */
	public static final String subStringBegin(String text, int trimCharsFromStart, String defaultValue){
		return subStringSides(text,trimCharsFromStart,0,defaultValue);
	}
	/**
	 * 문자열을 앞 또는 뒤 또는 앞뒤 모두 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 빈 문자열이 반환됩니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromStart 처음부터 잘려나갈 문자 수.
	 * @param trimCharsFromEnd 끝부터 잘려나갈 문자 수.
	 * @return 앞뒤가 잘려나간 문자열 또는 빈 문자열""
	 */
	public static final String subStringSides(String text, int trimCharsFromStart, int trimCharsFromEnd){
		return subStringSides(text,trimCharsFromStart,trimCharsFromEnd,StrongEggManagerApp.COMMON_STR_EMPTY);
	}
	/**
	 * 문자열을 앞 또는 뒤 또는 앞뒤 모두 지정된 문자 수 만큼 잘라냅니다.
	 * 문자수가 모자랄 때 대체 문자열을 지정하실 수 있습니다.
	 * 
	 * @param text 자를 문자열.
	 * @param trimCharsFromStart 처음부터 잘려나갈 문자 수.
	 * @param trimCharsFromEnd 끝부터 잘려나갈 문자 수.
	 * @param defaultValue 기본 문자열, 문자 수가 모자라 모두 잘려나간 경우 사용합니다.
	 * @return 앞뒤가 잘려나간 문자열 또는 기본값.
	 */
	public static final String subStringSides(String text, int trimCharsFromStart, int trimCharsFromEnd,String defaultValue){
		if(text != null){
			if((trimCharsFromStart+trimCharsFromStart)>0){
				if(text.length() > (trimCharsFromStart+trimCharsFromEnd)){
					return text.substring(trimCharsFromStart, text.length()-trimCharsFromEnd-1);
				}
			}else{
				return text;
			}
		}
		return defaultValue;
	}
	
	/**
	 * 스트롱에그에 접속하여 현재 세션 와이브로 사용량을 읽어옵니다.
	 * 업로드/다운로드 따로입니다.
	 * 
	 * @return 받아오는데 성공했다면 << true
	 */
	public boolean obtainCurrentSessionWibroUsed_StrongEgg(){
		SEM_HttpResponse httpRESPONSE = httpRequestGET(Egg_IP+"/admin/adm/statistic.asp",true);
		
		if(httpRESPONSE != null){
			switch(httpRESPONSE.getHttpResponseStatusCode()){
			case HttpURLConnection.HTTP_OK:
				//HTTP 200 OK
				String[] EggWiBroUsageStrings = parsePage_CurrentSessionWibroUsed_StrongEgg(httpRESPONSE.getHttpResponseBody());
				if(EggWiBroUsageStrings != null){
					EggWiBroUsageStrings[1] = subStringEnd(EggWiBroUsageStrings[1],3,StrongEggManagerApp.COMMON_STR_NUM_1);
					EggWiBroUsageStrings[3] = subStringEnd(EggWiBroUsageStrings[3],3,StrongEggManagerApp.COMMON_STR_NUM_1);
					synchronized(statusSyncObject) {
						//Egg_WibroUsedBytes = "Rx: "+rxBytesString+"B, Tx: "+txBytesString+"B";
						Wibro_UsedMB_DN_currentSession = EggWiBroUsageStrings[1];
						Wibro_UsedMB_UP_currentSession = EggWiBroUsageStrings[3];
					}
					return true;
				}else{
					return false;
				}
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			default:
				httpRESPONSE = null;
				return false;
			}
		}else{
			return false;
		}
				//사용량 확인
				//<td class="head">WiBro Rx 패킷</td>
//				<td>0&nbsp;</td>
//			</tr>
//			<tr>
//				<td class="head">WiBro Rx 바이트</td>
//				<td>0&nbsp;</td>
//			</tr>
//			<tr>
//				<td class="head">WiBro Tx 패킷</td>
//				<td>0&nbsp;</td>
//			</tr>
//			<tr>
//				<td class="head">WiBro Tx 바이트</td>
//				<td>0&nbsp;</td>
	}
	
	
	/**
	 * 
	 * @param planetextStatistics_StrongEgg
	 * @return
	 */
	private static final String[] parsePage_CurrentSessionWibroUsed_StrongEgg(String planetextStatistics_StrongEgg) {
		if(planetextStatistics_StrongEgg != null){
			try{
				planetextStatistics_StrongEgg = planetextStatistics_StrongEgg.substring(
						planetextStatistics_StrongEgg.indexOf("WiBro Rx 패킷</td>"));
				String[] str_StrongEggWibroCurrentSessionUsage =
						planetextStatistics_StrongEgg.substring(0,planetextStatistics_StrongEgg.indexOf("Wi-Fi 사용정보</td>"))
													.split("</td>", 9);
				planetextStatistics_StrongEgg = null;
				
				str_StrongEggWibroCurrentSessionUsage[0] = null;
				str_StrongEggWibroCurrentSessionUsage[2] = null;
				str_StrongEggWibroCurrentSessionUsage[4] = null;
				str_StrongEggWibroCurrentSessionUsage[6] = null;
				str_StrongEggWibroCurrentSessionUsage[8] = null;//[str_StrongEggWibroCurrentSessionUsage.length-1] = null;
		
				byte currentIndexOfUsageStrings = 0;
				for(String parseWiBroUsageString : str_StrongEggWibroCurrentSessionUsage){//int i = 1;i < EggWiBroUsageStrings.length; i=i+2)
					if(parseWiBroUsageString != null){//홀수 번째 문자열일때만.  != 0 붙일것 없이 0이면 짝수..이지만  java....
						if(isDebug){
							Log.v(TAG_LOG, "EggWiBroUsageStrings["+currentIndexOfUsageStrings+"] get << "+parseWiBroUsageString);
						}
		
						str_StrongEggWibroCurrentSessionUsage[currentIndexOfUsageStrings] = parseWiBroUsageString.substring(parseWiBroUsageString.indexOf("td>")+3,parseWiBroUsageString.length()-6);
						
						if(isDebug){
							Log.v(TAG_LOG, "EggWiBroUsageStrings["+currentIndexOfUsageStrings+"] parsed >> "+str_StrongEggWibroCurrentSessionUsage[currentIndexOfUsageStrings]);//+EggWiBroUsageStrings[currentIndexOfUsageStrings]);
						}
						
						currentIndexOfUsageStrings++;
					}
					
				}
				
				//Log.v(TAG_LOG,"Method D: "+(System.currentTimeMillis()-startTime));
				/*for(String w : EggWiBroUsageStrings){
					Log.v(TAG_LOG,"Method D: parsed >> "+ w);
				}*/
				//1 3 5 7 배열이 D방법에선 바뀌어서 0 1 2 3 순으로 가지런히 들어간다.
				return str_StrongEggWibroCurrentSessionUsage;
			} catch (IndexOutOfBoundsException ioobe){
				//파싱 및 분석 실패. indexof에서 -1을 뱉었어요 ㅠㅠ..
				if(isDebug){
					Log.e(TAG_LOG,"parsed string doesn't fit to process.");
					ioobe.printStackTrace();
				}
				return null;
			}
		}else{
			return null;
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getLastObtainedUsedWibroBytes(){
		//
		if(Wibro_UsedMB_DN_currentSession != null && Wibro_UsedMB_UP_currentSession != null){
			if(Wibro_UsedMB_DN_currentSession.length() > 9 || Wibro_UsedMB_UP_currentSession.length() > 9 ){
				return Long.parseLong(Wibro_UsedMB_DN_currentSession,10)+Long.parseLong(Wibro_UsedMB_UP_currentSession,10)+"";
			}else{
				return Integer.parseInt(Wibro_UsedMB_DN_currentSession,10)+Integer.parseInt(Wibro_UsedMB_UP_currentSession,10)+"";
			}
		}
		return StrongEggManagerApp.COMMON_STR_NUM_0;//Egg_WibroUsedBytes;
	}/*
	private void setLastObtainedUsedWibroBytes(String rxBytesString, String txBytesString)
	{
		try{
			rxBytesString = rxBytesString.substring(0, rxBytesString.length()-3);
		}catch(IndexOutOfBoundsException ioobe){
			rxBytesString = "HCSTR_ 1";
		}
		
		try{
			txBytesString = txBytesString.substring(0, txBytesString.length()-3);
		}catch(IndexOutOfBoundsException ioobe){
			txBytesString = "HCSTR_ 1";
		}
		
		//Alert: now will be thread-safe operation;
		synchronized(statusSyncObject) {
			//Egg_WibroUsedBytes = "Rx: "+rxBytesString+"B, Tx: "+txBytesString+"B";

			Wibro_UsedMB_UP_currentSession = rxBytesString;
			Wibro_UsedMB_DN_currentSession = txBytesString;
			if(isDebug)
			{
				//Log.v(TAG_LOG,"status changed on threa-safe operation: Wibro used "+Egg_WibroUsedBytes);
			}
		}
	}*/
		
	//F. 배터리 잔량 불러오기
	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroSignalDetailed() {
		return Wibro_SignalDetailed;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroIP() {
		return Wibro_IP;
	}
	public String getChecked_WibroIP() {
		if( Wibro_IP != null ){
			if(Wibro_IP.length() < 8){
				return null;
			}
			return Wibro_IP;
		}else{
			return null;
		}
					
	}

	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroGateway() {
		return Wibro_Gateway;
	}

	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroDnsPrimary() {
		return Wibro_DnsPrimary;
	}

	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroDnsSecondary() {
		return Wibro_DnsSecondary;
	}

	/**
	 * 
	 * @return
	 */
	public String getRaw_EggBatteryLevel() {
		return Wibro_BatteryLevel;
	}
	
	/**
	 * 
	 * @return
	 */
	public String switchString_BatteryLevel(){
		return switchString_BatteryLevel(Wibro_BatteryLevel);
	}
	
	public int switchResId_BatteryLevel(){
		return switchResId_BatteryLevel(Wibro_BatteryLevel);
	}
	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroCurrentSessionUsedMB_DN() {
		return Wibro_UsedMB_DN_currentSession;
	}
	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroCurrentSessionUsedMB_UP() {
		return Wibro_UsedMB_UP_currentSession;
	}
	/**
	 * 
	 * @return
	 */
	public String getRaw_WibroStatus() {

		return Wibro_Status;
	}
	
	/**
	 * 
	 * @param battLevel
	 * @return
	 */
	public String switchString_BatteryLevel(String battLevel){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_PC_PAGE:
			return switchString_BatteryLevel(parseBatteryLevel_StrongEgg(battLevel));
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			return switchString_BatteryLevel(parseBatteryLevel_CompactEgg(battLevel));
//			break;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" switchEgg_BatteryLevel()","the App is working for Unrecognized Device!! This should not happen!");
			}
			//break;
			return null;
		}
	}
	
	public int switchResId_BatteryLevel(String battLevel){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
		case EGG_KWDB2600_PC_PAGE:
			return switchResId_BatteryLevel(parseBatteryLevel_StrongEgg(battLevel));
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			return switchResId_BatteryLevel(parseBatteryLevel_CompactEgg(battLevel));
//			break;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" switchEgg_BatteryLevel()","the App is working for Unrecognized Device!! This should not happen!");
			}
			//break;
			return R.drawable.ic_battery_1;//fallback
		}
	}
	
	/**
	 * 
	 * @param battLevel
	 * @return
	 */
	private static final byte parseBatteryLevel_StrongEgg(String battLevel){
		if(battLevel != null){
			if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_5)){
				return EGG_BATTERYLEVEL_ENOUGH;
			}else if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_4)){
				return EGG_BATTERYLEVEL_FINE;
			}else if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_3)){
				return EGG_BATTERYLEVEL_INSUFFICIENT;
			}else{
				return EGG_BATTERYLEVEL_BOTTOMUP;
			}
		}else{
			return EGG_BATTERYLEVEL_UNKNOWN;
		}
	}
	
	private static final byte parseBatteryLevel_CompactEgg(String battLevel) {
		if(battLevel != null){
			if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_3)){
				return EGG_BATTERYLEVEL_ENOUGH;
			}else if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_2)){
				return EGG_BATTERYLEVEL_FINE;
			}else if(battLevel.equals(StrongEggManagerApp.COMMON_STR_NUM_1)){
				return EGG_BATTERYLEVEL_INSUFFICIENT;
			}else{
				return EGG_BATTERYLEVEL_CHARGING;
			}
		}else{
			return EGG_BATTERYLEVEL_UNKNOWN;
		}
	}
	
	/**
	 * 
	 * @param battLevel
	 * @return
	 */
	public String switchString_BatteryLevel(byte battLevel) {
		switch(battLevel){
		case EGG_BATTERYLEVEL_ENOUGH:
			return getString(R.string.battery_enough);
		case EGG_BATTERYLEVEL_FINE:
			return getString(R.string.battery_fine);
		case EGG_BATTERYLEVEL_INSUFFICIENT:
			return getString(R.string.battery_insufficient);
		case EGG_BATTERYLEVEL_BOTTOMUP:
			return getString(R.string.battery_bottomup);
		case EGG_BATTERYLEVEL_CHARGING:
			return getString(R.string.battery_charging);
		default:
			return getString(R.string.battery_unknown);
		}
	}

	private static final int switchResId_BatteryLevel(byte battLevel){
		//아이콘 받기
		switch(battLevel){
		case EGG_BATTERYLEVEL_ENOUGH:
			return R.drawable.ic_battery_4;
		case EGG_BATTERYLEVEL_FINE:
			return R.drawable.ic_battery_3;
		case EGG_BATTERYLEVEL_INSUFFICIENT:
			return R.drawable.ic_battery_2;
		case EGG_BATTERYLEVEL_BOTTOMUP:
			return R.drawable.ic_battery_0;
		case EGG_BATTERYLEVEL_CHARGING:
			return R.drawable.ic_battery_charging;/// anim으로 변환
		default:
			return R.drawable.ic_battery_1;// anim으로 변환할까 그냥 빈칸으로 둘까
		}
	}

	/**
	 * KT 서버에 접속하여 전산에 입력된 사용량 정보를 불러옵니다.
	 * 기기마다 구현이 다릅니다.
	 * 
	 */
	public void checkWibroUsageFromKT(){
		switch(Egg_Type){
		//스트롱에그
		case EGG_KWDB2600_MOBILE_PAGE:
			checkWibroUsageFromKT_StrongEgg1640();
			return ;
		case EGG_KWDB2600_PC_PAGE:
			break;
		//컴팩트에그
		case EGG_KWFB2700_v2234:
			checkWibroUsageFromKT_CompactEgg();
			return ;
		//미지원이 있을리는 없지만 만약으로.(..)
		default:
			if(isDebug){
				Log.e(TAG_LOG+" obtainCurrentSessionWibroUsed()","the App is working for Unrecognized Device!! This should not happen!");
			}
			//break;
		}
		showShortToast(R.string.operation_not_supported_on_this_device);
	}
	
	/**
	 * 스트롱에그에 구현된 기능으로 KT 전산에서 와이브로 사용량 정보를 읽어옵니다.
	 * 성공할 때 /data에 저장 및 UI를 갱신요청합니다.  
	 * 실패할 땐 UI를 원래대로 둡니다.
	 */
	public void checkWibroUsageFromKT_StrongEgg1640() {
		//사용량 새로고침 KT에서 받아옵니다.
		//
		//와이브로 사용량 정보를 받아온 경우.
		if(( httpRequestPOST(Egg_IP+"/goform/updateWBUsage", "n/a").getHttpResponseBody(true) ).equals("running")){
			taskThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					taskThreadHandler.postDelayed(this, DEFAULT_UI_DELAY);
					if( ! httpRequestPOST(Egg_IP+"/goform/checkWBUsageDone","n/a").getHttpResponseBody(true).equals("running")){
						taskThreadHandler.removeCallbacks(this);
						//KT서버에서 받아온 와이브로 정보.
						commitLastObtainedUsageFromKT(
								parseWibroUsageFromKT_StrongEgg1640(
										httpRequestPOST(Egg_IP+"/goform/getWBUsage","n/a").getHttpResponseBody()
										)
										);
					}
				}});
		}else{
			sendRefreshWibroUsageKtHasFailedEvent();
		}
	}
	
	public void checkWibroUsageFromKT_CompactEgg() {
		//사용량 새로고침 KT에서 받아옵니다.
		//
		//와이브로 사용량 정보를 받아온 경우.
		commitLastObtainedUsageFromKT(parseWibroUsageFromKT_CompactEgg(
				httpRequestPOST(Egg_IP+"/cgi-bin/webmain.cgi", "act=act_get_uicc_info&param=USE_INFO").getHttpResponseBody(true)
										));
	}

	public static final String printStackTraceToString(StackTraceElement[] stkTrcElem){
		String trace = "";
		for(StackTraceElement elem : stkTrcElem){
			trace = trace+elem.getClassName()+"("+elem.getLineNumber()+")"+StrongEggManagerApp.COMMON_STR_CRLF;
		}
		return trace;
	}
	public static final String stringArrayContent(String[] strArr){
		String content = "";
		int i = 0; 
		for (String str : strArr){
			content = content+"["+(i++)+"]"+str+StrongEggManagerApp.COMMON_STR_CRLF;
		}
		return content;
	}
	/**
	 * 스트롱에그가 KT 전산에서 받아온 와이브로 사용량 정보를 분석합니다.
	 * 
	 * @param checkedFromKT 와이브로 사용량 정보가 들어있는 문자열.
	 * @return
	 * 와이브로 사용량 정보가 들어있는 6원소 문자열배열, 순서대로:
	 * wibroPlan 요금제
	 * providedPackets 제공량(MB)
	 * usedPackets 사용량(MB)
	 * basicFee 기본료(원)
	 * usedFee 사용료(원)
	 * additionalCharges 정보이용료(원)
	 */
	private final String[] parseWibroUsageFromKT_StrongEgg1640(String checkedFromKT) {
		if(checkedFromKT != null){
			//checkedFromKT="notfail";//dummy
			if( ! checkedFromKT.equals("failed")){
				//checkedFromKT="프로30;30720;30;10000;684;0";///dummy
				return checkedFromKT.split(";");
								
				//프로30;30720;67;10000;684;0
				//objProdName.nodeValue = res[0] + " ";요금제
				//objBasicPacketVol.nodeValue = res[1] + " MB"; 기본량
				//objPacketTotalVol.nodeValue = res[2] + " MB"; 사용량
				//objBasicPacketRate.nodeValue = res[3] + " 원"; 기본요
				//objPacketTotalUsed.nodeValue = res[4] + " 원"; 사용요
				//objContTotalUsed.nodeValue = res[5] + " 원"; 정보료
				
				//그런데 System.currentTimeMillis()도 UTC(표준시)다.. 그럼 이거 써도 되겠네 와아 >_<
				//commitLastObtainedUsageFromKT((Calendar.getInstance(TimeZone.getTimeZone(COMMON_STR_TIMEZONE_UTC))).getTimeInMillis(),splittedUsage[0],splittedUsage[1],splittedUsage[2],splittedUsage[3],splittedUsage[4],splittedUsage[5]);
				//commitLastObtainedUsageFromKT(System.currentTimeMillis(),splittedUsage[0],splittedUsage[1],splittedUsage[2],splittedUsage[3],splittedUsage[4],splittedUsage[5]);
			}
		}
		return null;
	}
	
	private final String[] parseWibroUsageFromKT_CompactEgg(String checkedFromKT) {
		//checkedFromKT = "notfail";//dummy
		if(checkedFromKT != null ){
			if(checkedFromKT != ""){
				//임시 시험용으로 실제 메시지를 소화시켜보겠습니다.
				//checkedFromKT = "{\"result\":\"0\",\"data\":{\"auth\":\"YES\",\"prodname\":\"신표준요금 30G\",\"basicpacketvol\":\"30720.000000\",\"packettotalval\":\"1083.634766\",\"basicpacketrate\":\"30000\",\"contotalrate\":\"0\",\"AAA\":\"0\",\"BBB\":\"10869\",\"CCC\":\"New Rate 30G\",\"DDD\":\"201309\",\"EEE\":\"0\",\"serverdate\":\"20130909123954769\",\"dummy09\":\"XX\"}";
				//checkedFromKT = "{\"result\":\"0\",\"data\":{\"auth\":\"NO\",\"dummy09\":\"XX\"}";
				try{
					if(isDebug){
						Log.v(TAG_LOG, "checkedFromKT get << "+ checkedFromKT);				
					}
					checkedFromKT = checkedFromKT.substring(checkedFromKT.indexOf("{\"auth"),checkedFromKT.indexOf(",\"dummy"));
					
					if(isDebug){
						Log.v(TAG_LOG, "checkedFromKT parsed >> "+ checkedFromKT);				
					}
					
					if( checkedFromKT.startsWith("{\"auth\":\"YES\"")){ 
		
						String[] str_checkedFromKT = checkedFromKT.split("\",\"", 7);
						checkedFromKT = null;
						
						str_checkedFromKT[0] = null;
						str_checkedFromKT[6] = null;
						
						byte currentIndexOfUsageStrings = 0;
						for(String parseWiBroUsageString : str_checkedFromKT){//int i = 1;i < EggWiBroUsageStrings.length; i=i+2)
							if(parseWiBroUsageString != null){//홀수 번째 문자열일때만.  != 0 붙일것 없이 0이면 짝수..이지만  java....
								if(isDebug){
									Log.v(TAG_LOG, "EggWiBroUsageStrings["+currentIndexOfUsageStrings+"] get << "+parseWiBroUsageString);
								}
								
								str_checkedFromKT[currentIndexOfUsageStrings] = parseWiBroUsageString.substring(parseWiBroUsageString.indexOf("\":\"")+3);
								
								if(isDebug){
									Log.v(TAG_LOG, "EggWiBroUsageStrings["+currentIndexOfUsageStrings+"] parsed >> "+str_checkedFromKT[currentIndexOfUsageStrings]);//+EggWiBroUsageStrings[currentIndexOfUsageStrings]);
								}
								
								currentIndexOfUsageStrings++;
							}
						}
						if(didKTsolvedIssue){
						//소숫점이 있는 경우 과감히 버립니다. 1MB 차이 그렇게 크지 않아요
						//if(str_checkedFromKT[1].indexOf(".") != -1){
							str_checkedFromKT[1] = str_checkedFromKT[1].substring(0,str_checkedFromKT[1].indexOf("."));
						//}
						//if(str_checkedFromKT[2].indexOf(".") != -1){
							str_checkedFromKT[2] = str_checkedFromKT[2].substring(0,str_checkedFromKT[2].indexOf("."));
						//}
						}else{
							int posDot1 = str_checkedFromKT[1].indexOf(".");
							//if(str_checkedFromKT[1].substring(0,posDot1).length()>=5){
								str_checkedFromKT[1] = str_checkedFromKT[1].substring(0,posDot1);
							//}else{
							//TODO 요금제 변경 이후 한 자리 적게 제공량이 표시되는 현상 수정 -박 찬님 요청. 20140107
							//	str_checkedFromKT[1] = str_checkedFromKT[1].substring(0,posDot1) + str_checkedFromKT[1].substring(posDot1 +1, posDot1 +1+1);
							//}
								
							//KT전산이 맛가는 틈을 타 새로운 방법으로 파싱
							//int posDot1 = str_checkedFromKT[1].indexOf(".");
							int posDot2 = str_checkedFromKT[2].indexOf(".");
							
							//str_checkedFromKT[1] = str_checkedFromKT[1].substring(0,posDot1) + str_checkedFromKT[1].substring(posDot1 +1,posDot1 +1+3);
							str_checkedFromKT[2] = str_checkedFromKT[2].substring(0,posDot2) + str_checkedFromKT[2].substring(posDot2 +1, posDot2 +1+3);
						}
						
						//정보료 항목을 제 위치에 넣기 위해 사용료 부분과 바꿔치기합니다.
						str_checkedFromKT[6] = str_checkedFromKT[5];
						str_checkedFromKT[5] = StrongEggManagerApp.COMMON_STR_NUM_0;
						
						return str_checkedFromKT;
		
						//{"result":"0","data":{"auth":"NO","dummy09":"XX"},
						//{"result":"0","data":{"auth":"YES","prodname":"신표준요금 30G","basicpacketvol":"30720.000000","packettotalval":"1083.634766","basicpacketrate":"30000","contotalrate":"0","AAA":"0","BBB":"10869","CCC":"New Rate 30G","DDD":"201309","EEE":"0","serverdate":"20130909123954769","dummy09":"XX"}
					}
				}catch(StringIndexOutOfBoundsException sioobe){
					if(isDebug){
						Log.w(TAG_LOG,"parseWibroUsageFromKT_CompactEgg failed. failed to retrieve");
						sioobe.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	//workaround for KT's ill-working 전산
		//남은 패킷량 GB로 표시
	public String showString_WibroRemainingPacket(String providedPackets, String usedPackets){
		int provided = Integer.parseInt(providedPackets,10);
		int used = Integer.parseInt(usedPackets,10);
		int remaining = 0;
		if(didKTsolvedIssue || Egg_Type == EGG_KWFB2700_v2234){
			remaining = provided - used;
			if(remaining<0){//초과 사용중
				return getString(R.string.item_header_packets_exceeded)+StrongEggManagerApp.putCommaNumeric(remaining*-1)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB;
			}else{
				return getString(R.string.item_header_packets_remaining)+StrongEggManagerApp.putCommaNumeric(remaining)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB;
			}
		}else{
			provided = provided/1024;
			remaining = provided - used;
			if(remaining<=0){//초과 사용중: ~ 미만이므로 실제 값보다 1 많게 표시
				return getString(R.string.item_header_packets_exceeded)+getString(R.string.item_header_less_than, ((remaining*-1)+1)+StrongEggManagerApp.COMMON_UNIT_PACKETS_GB);
			//}else if (remaining == 0){//0 이하므로 초과사용되었다고 알림 30-30= 0이니까 초과사용이다.
			//	return getString(R.string.item_header_packets_exceeded)+getString(R.string.item_header_less_than, (1)+StrongEggManagerApp.COMMON_UNIT_PACKETS_GB);
			}else{//~ 이상이므로 실제 값보다 1 적게 표시
				return getString(R.string.item_header_packets_remaining)+getString(R.string.item_header_more_than, (remaining-1)+StrongEggManagerApp.COMMON_UNIT_PACKETS_GB);
			}
		}
	}
	
	//사용 패킷량 GB로 표시
	public String showString_WibroUsedPacket(String usedPackets){
		if(didKTsolvedIssue || Egg_Type == EGG_KWFB2700_v2234){
			return StrongEggManagerApp.putCommaNumeric(usedPackets)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB;
		}else{
			return getString(R.string.item_header_less_than,
					Integer.parseInt(usedPackets,10)+1+StrongEggManagerApp.COMMON_UNIT_PACKETS_GB);
		}
	}
	
	//end of workaround KT's ill-working 전산
	
	
	
	//너무한건가 이정도로 나눴단건.
	/**
	 * 받아온 와이브로 사용량 정보를 /data 에 저장합니다.
	 * 
	 * @param splittedUsageStrings 와이브로 사용량 정보가 들어있는 6원소 이상의 문자열 배열.
	 */
	private void commitLastObtainedUsageFromKT(String[] splittedUsageStrings){
		if(splittedUsageStrings != null){
			if(splittedUsageStrings.length > 5){// 6원소 이상인경우
				//try{
					commitLastObtainedUsageFromKT(System.currentTimeMillis(),splittedUsageStrings[0],splittedUsageStrings[1],splittedUsageStrings[2],splittedUsageStrings[3],splittedUsageStrings[4],splittedUsageStrings[5]);
					return ;
				//}catch(IndexOutOfBoundsException ioobe){
				//	if(isDebug){
				//		showShortToast("MCM_IOOBE: "+printStackTraceToString(ioobe.getStackTrace())+stringArrayContent(splittedUsageStrings));
				//	}
				//}catch(Exception E){
				//	showShortToast("MCM_E: "+printStackTraceToString(E.getStackTrace()));
				//}
			}
		}
		sendRefreshWibroUsageKtHasFailedEvent();
	}
	
	/**
	 * 받아온 와이브로 사용량 정보를
	 * /data 에 저장합니다.
	 * 
	 * @param lastObtained 정보를 앱에 저장할 때의 시각(ms, 밀리초). 1970년 1월 1일 00:00:00 UTC 를 처음으로, 흐른 시간. 
	 * @param wibroPlan 요금제
	 * @param providedPackets 제공량(MB)
	 * @param usedPackets 사용량(MB)
	 * @param basicFee 기본료(원)
	 * @param usedFee 사용료(원)
	 * @param additionalCharges 정보이용료(원)
	 */
	private void commitLastObtainedUsageFromKT(long lastObtained, String wibroPlan,String providedPackets,String usedPackets,String basicFee,String usedFee,String additionalCharges){
		if(isDebug){
			Log.v(TAG_LOG,"Committing last obtained Wibro Usage >> "+wibroPlan+providedPackets+usedPackets+basicFee+usedFee+additionalCharges);
		}
		if(sharedPrefs == null){
			sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);//getApplicationContext());
		}
		
		synchronized(settingsSyncObject){
			//설정 파일을 변경하도록 준비합니다.
			SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
			
			//실제 변경할 부분 만들기
			sharedPrefsEditor.putString(USER_STR_WIBRO_PLAN, wibroPlan);
			sharedPrefsEditor.putString(USER_STR_WIBRO_PACKETS_PROVIDED, providedPackets);
			sharedPrefsEditor.putString(USER_STR_WIBRO_PACKETS_USED, usedPackets);
			sharedPrefsEditor.putString(USER_STR_WIBRO_FEE_BASIC, basicFee);
			sharedPrefsEditor.putString(USER_STR_WIBRO_FEE_CURRENT, usedFee);
			sharedPrefsEditor.putString(USER_STR_WIBRO_ADDITIONAL_CHARGES, additionalCharges);
			sharedPrefsEditor.putLong(USER_STR_WIBRO_LAST_OBTAINED, lastObtained);

			//http://stackoverflow.com/questions/5369682/android-get-current-time-and-date
			//Calendar c = Calendar.getInstance(); 
			//int seconds = c.get(Calendar.SECOND)

			//The Time class is a faster replacement for the java.util.Calendar and java.util.GregorianCalendar classes. An instance of the Time class represents a moment in time, specified with second precision.
			//Time now = new Time();
			//now.setToNow();
			
			//설정 파일 변경을 '저장'합니다.
			sharedPrefsEditor.commit();
			sharedPrefsEditor = null;
		}
		sendRefreshWibroUsageKtHasCompletedEvent(lastObtained,wibroPlan,providedPackets,usedPackets,basicFee,usedFee,additionalCharges);
	}
	
	
	//화면 꺼졌을 때 일시정지 만들기
	public boolean isPaused(){
		return isPaused;
	}
	public void setPaused(boolean setPause){
		isPaused = setPause;
	}
	
	public void pause(){
		if(isMonitorServiceRunning){
			if(isPaused){
				if(isDebug){
					Log.w(TAG_LOG,"Already Paused");
				}
			} else {
				isPaused = true;
				if(isDebug){
					Log.v(TAG_LOG,"pause() service, eggstat:"+Egg_ConnectionStatus);
				}
				TerminateServiceSti();
			}
		} else {
			if(isDebug){
				Log.v(TAG_LOG,"Monitoring isn't running");
			}
		}
	}
	
	public void resume(){
		if(isPaused){
			if(Egg_ConnectionStatus == EGG_CONNSTAT_COMPLETED){
				if(isDebug){
					Log.v(TAG_LOG,"resume() service, eggstat:"+Egg_ConnectionStatus);
				}
				isPaused = false;
				LaunchServiceSti();
				
			}else{
				if(isDebug){
					Log.i(TAG_LOG, "Not to start a autorefresh unless it is secure");
				}
			}
		} else {
			if(isDebug){
				Log.w(TAG_LOG,"Already Resumed or not affected");
			}
		}
	}

	//TODO API 11 이상에서 상태바 꺼짐도 확인하기.
	
	//TODO MiniAutorefresh에 대응되는 알림막대 표시
	String getNotiTitle(){
		if(!App_useMiniAutoRefresh){
			return switchString_SignalStrength()+", "+getProcessed_WibroElapsedTime();
		}else{
			return switchString_SignalStrength();
		}
		
	}
	
	String getNotiText(){
		if(!App_useMiniAutoRefresh){
			return getString(R.string.item_title_used_packets)+StrongEggManagerApp.putCommaNumeric(getLastObtainedUsedWibroBytes())+StrongEggManagerApp.COMMON_UNIT_PACKETS_BYTE;
		} else {
			return getString(R.string.guide_mini_refresh);
		}
	}
	
	//3개 숫자 단위로 콤마 넣기, 테라까지 쓸 사람은 없을테니 콤마 3개, 즉 9자리 이상만 하면 될것 같다. 
	public static String putCommaNumeric(int intNumeric){
		return putCommaNumeric(intNumeric+"");
	}
	public static String putCommaNumeric(String strNumeric){
		if(strNumeric.startsWith("0") && strNumeric.length()>1){
			strNumeric = strNumeric.substring(1);
		}
		
		if(strNumeric.length() > 9){
			//strNumeric = "9,999,999,999";
			strNumeric = strNumeric.substring(0,strNumeric.length()-9) + StrongEggManagerApp.COMMON_STR_COMMA + strNumeric.substring(strNumeric.length()-9,strNumeric.length()-6) + StrongEggManagerApp.COMMON_STR_COMMA +strNumeric.substring(strNumeric.length()-6,strNumeric.length()-3) + StrongEggManagerApp.COMMON_STR_COMMA +strNumeric.substring(strNumeric.length()-3,strNumeric.length());
		}else if(strNumeric.length() > 6){
			//strNumeric = "6,666,666";
			strNumeric = strNumeric.substring(0,strNumeric.length()-6) + StrongEggManagerApp.COMMON_STR_COMMA +strNumeric.substring(strNumeric.length()-6,strNumeric.length()-3) + StrongEggManagerApp.COMMON_STR_COMMA +strNumeric.substring(strNumeric.length()-3,strNumeric.length());
		}else if(strNumeric.length() > 3){
			//strNumeric = "3,333";
			strNumeric = strNumeric.substring(0,strNumeric.length()-3) + StrongEggManagerApp.COMMON_STR_COMMA +strNumeric.substring(strNumeric.length()-3,strNumeric.length());
		}
		return strNumeric;
		
	}
	
}