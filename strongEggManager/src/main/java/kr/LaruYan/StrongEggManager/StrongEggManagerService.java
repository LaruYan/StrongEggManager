package kr.LaruYan.StrongEggManager;

import java.lang.ref.WeakReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class StrongEggManagerService extends Service {
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_SERVICE";
	private StrongEggManagerApp EggCore;

	//intent로 정보교환을 위한 키
	public static final String EXTRA_SERVICE_DO = "ServiceCode";
	public static final String EXTRA_SERVICE_JOB_ID = "JobID";
	public static final String EXTRA_SERVICE_STATE_CHANGED = "SEM_Svc_Stat";
	
	
	//서비스 제어 메시지
	public static final int SERVICE_START = 0;
	public static final int SERVICE_RUNNING = 1;
	public static final int SERVICE_INTERVAL_CHANGED = 2;
	public static final int SERVICE_STOP = -1;
	public static final int SERVICE_TERMINATE = -2;
	public static final int SERVICE_PAUSED = -3;

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private NotificationManager mNotificationManager;
	private boolean _isEntireServiceRunning = false;
	private static final Object serviceSyncObject = new Object();
	
	//핸들러 스레드가 처리해주는건 순서대로일 분이니까.. volatile은 없어도 되겠지..
	
	// 서비스 생성
	// http://developer.android.com/reference/android/app/Service.html
	// 단순히 네트워크 처리가 필요한 알림만 하니까.. IntentService로 전향시키는것도 좋겠다.
	// http://developer.android.com/guide/components/services.html

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION = R.string.app_service_started;
	//사용량 액티비티.
	private Intent notificationIntent;
	private PendingIntent contentIntent;
	
	private static final byte MAX_RETRY_ATTMPTS = 5;
	private volatile byte retryAttemptRemaining = MAX_RETRY_ATTMPTS;
	
	
	// private Timer doRefreshTimer = new Timer();
	//private static final long INITIAL_DELAY = 3000;
	private static final long INITIAL_DELAY = 500;
	//private static final long UPDATE_INTERVAL = 5000;// 설정에서 불러오게 해야한다.
	//private long initialUpdateInterval = EggCore.App_pollingRate;// EggCore에 상주하는 무언가로 바꿔야한다.
	private long updateInterval = INITIAL_DELAY; //UPDATE_INTERVAL; 어차피 초기값이고 바꿀 수 있으니 
	//private Runnable pollItOnceRunnable;
	private Runnable checkItOnceRunnable;
	
	
	//스레드가 순서대로 진행되고 있어요.
	//lint 경고(Leak 문제)가 있어 static으로 변경했다가 WeakRefenence만 남겨두었어요.
	//http://stackoverflow.com/questions/11407943/this-handler-class-should-be-static-or-leaks-might-occur-incominghandler
	// Handler that receives messages from the thread
	private static final class ServiceHandler extends Handler {
		private final WeakReference<StrongEggManagerService> mService;
		
		public ServiceHandler(Looper looper, StrongEggManagerService service ) {
			super(looper);
			mService = new WeakReference<StrongEggManagerService>(service);
			//EggCore = (StrongEggManagerApp) mService.EggCore;
		}

		@Override
		public void handleMessage(final Message msg) {
			if( msg != null)
			{
				final StrongEggManagerService service = mService.get();
		         if (service != null) {
		        	switch(msg.what)
	        	 	{
		        	case StrongEggManagerService.SERVICE_START:
			        	sendEmptyMessage(StrongEggManagerService.SERVICE_RUNNING);
			        	break;
	        	 	case StrongEggManagerService.SERVICE_RUNNING:
	        	 		//서비스 시작
	        	 		service.setEntireServiceRunning(true);
	        	 		service.showNotificationAndroidV4();
	        	 		//service.startForeground(R.string.app_service_connected, notification);
	        	 		service.PollDataOnEveryMilliseconds();
	        	 		//service.DoSomeWork(msg);
				        // Stop the service using the startId, so that we don't stop
				        // the service in the middle of handling another job
	        	 		//service.stopForeground(removeNotification);
				        service.stopSelf(msg.arg1);//서비스가 보관하는 작업ID를 보관하면.. 아래쪽에서 이 메시지를 이용해서 모두 제어할 수 있으릳.도.. 
				        break;
	        	 	case StrongEggManagerService.SERVICE_TERMINATE:
	        	 		service.setEntireServiceRunning(false);
	        	 		service.stopSelf();
	        	 		removeMessages(StrongEggManagerService.SERVICE_TERMINATE);
	        	 		removeMessages(StrongEggManagerService.SERVICE_RUNNING);
	        	 		break;
	        	 	case StrongEggManagerService.SERVICE_STOP:
	        	 		//서비스 멈춤
	        	 		//이것보단 stopService를 써주세요.
	        	 		//service._isHandlerRunning = false;
	        	 		service.stopSelf(msg.arg1);
	        	 		break;
	        	 	case StrongEggManagerService.SERVICE_INTERVAL_CHANGED:
	        	 		//새 갱신 주기 적용
	        	 		
	        	 		break;
	        	 	default:
	        	 		//아무것도 하지 않음
	        	 		super.handleMessage(msg);//상위 핸들러로 떠넘기기
	        	 		break;
	        	 	}
		         }
			}
		}
	}

	private void setEntireServiceRunning(boolean isRunning)
	{
		if(EggCore == null){
			EggCore = (StrongEggManagerApp) getApplicationContext();
		}
		
		synchronized(serviceSyncObject){
			_isEntireServiceRunning = isRunning;
			EggCore.setIsAutoRefreshRunning(isRunning);
		}
		//broadcastServiceStatus();
	}
	
	public boolean getEntireServiceRunning()
	{
		return _isEntireServiceRunning;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	
		EggCore = (StrongEggManagerApp) getApplicationContext();//EggCore.ApplicationContext;//FC
		//EggCore.setFlag_MonitorServiceRunning(true); 
		
		//자동 새로고침 주기 초기값
		updateInterval = EggCore.App_pollingRate;
		
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread serviceThreadHandler = new HandlerThread(TAG_LOG+"_Handler",
				Process.THREAD_PRIORITY_FOREGROUND);//.THREAD_PRIORITY_BACKGROUND);
		serviceThreadHandler.start();
		
		if(isDebug)
		{
			Log.v(TAG_LOG,"Service created with Priority:"+ serviceThreadHandler.getPriority());
		}
		
		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = serviceThreadHandler.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper,this);//serviceThreadHandler.getLooper(),this);
		//
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null)
		{
			Bundle serviceStartState = intent.getExtras();
			if (serviceStartState != null)
			{
				int startCode = serviceStartState.getInt(StrongEggManagerService.EXTRA_SERVICE_DO);
				// For each start request, send a message to start a job and deliver the
				// start ID so we know which request we're stopping when we finish the
				// job
				Message msg = mServiceHandler.obtainMessage();
				msg.arg1 = startId;
				msg.what = startCode;//SERVICE_CODE.START_SERVICE;
				mServiceHandler.sendMessage(msg);
		
				// Display a notification about us starting. We put an icon in the
				// status bar.
				//EggCore.nonUiThreadHandler.post(new Runnable () {
		
				//	@Override
				//	public void run() {
				//	if(startCode == StrongEggManagerService.SERVICE_START)
				//	{
						//EggCore.showShortToast(R.string.app_service_started);
						//showNotificationAndroidV4();
						
				//	}
				//	}
					
				//});
				
				// If we get killed, after returning from here, restart
				// We want this service to continue running until it is explicitly
				// stopped, so return sticky.
				return START_STICKY;
				// IntentService라면 super.onStartCommand(intent, flags, startId) 를 반환.
			}
			else
			{
				//서비스가 다루지 못하므로 끝내도록 한다.. 가 될까 이거...
				Message msg = mServiceHandler.obtainMessage();
				msg.arg1 = startId;
				msg.what = StrongEggManagerService.SERVICE_TERMINATE;//SERVICE_CODE.START_SERVICE;
				mServiceHandler.sendMessage(msg);
				
				if(isDebug)
				{
					//서비스가 다뤄지지 않은 메시지는..꺼야할텐데..
				}
			}
			//바로 꺼지게 하고싶은데.. 대기상태로 둬야할까.. 다른걸 처리할지도 모르니까..
		}
		else
		{//서비스 시작 명령 인텐트가 null일 때 스스로 멈춰주길 바라.
			stopSelf();
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		setEntireServiceRunning(false);//경고: 이 서비스가 하고 있던 모든 작업이 종료됩니다.
		//EggCore.setFlag_MonitorServiceRunning(false);

		mServiceLooper.quit();//.getLooper().quit();
		// Cancel the persistent notification.
		mNotificationManager.cancel(NOTIFICATION);

		// Tell the user we stopped.
		//EggCore.showShortToast(R.string.app_service_stopped);
		
		//mServiceHandler.removeCallbacks(r);
		
		//null로 바꾸기
		notificationIntent = null;
		contentIntent = null;
		
		//서비스 스레드가 담당하던 모든 메시지 삭제.
		//mServiceHandler.removeCallbacks(checkItOnceRunnable);
		mServiceHandler.removeCallbacksAndMessages(null);
		
		mServiceHandler = null;
		mServiceLooper = null;

		mNotificationManager = null;
		
		EggCore = null;
		
		// 서비스 인텐트 관리자 끄기
		
		super.onDestroy();
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		StrongEggManagerService getService() {
			return StrongEggManagerService.this;
		}
	}
	
	private void showNotificationAndroidV4() {
		//Notification
		if(EggCore == null){
			EggCore = (StrongEggManagerApp) getApplicationContext();
		}
		notificationIntent = new Intent(EggCore.ApplicationContext, MainActivity.class);

		//http://stackoverflow.com/questions/6409358/android-always-return-to-currently-active-activity-from-notification
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);//ACTIVITY_NEW_TASK 참고 http://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_NEW_TASK
		notificationIntent.putExtra(StrongEggManagerApp.EXTRA_SET_PAGE_MAINACTIVITY, (byte)1);//0요약 1사용량 2설정
		
		// The PendingIntent to launch our activity if the user selects this
	    // notification.  Note the use of FLAG_CANCEL_CURRENT so that, if there
	    // is already an active matching pending intent, cancel it and replace
	    // it with the new array of Intents.

	    //PendingIntent contentIntent = PendingIntent.getActivities(this, NOTIFICATION,
	    //        makeMessageIntentStack(this, from, message), PendingIntent.FLAG_CANCEL_CURRENT);
		contentIntent = PendingIntent.getActivity(EggCore.ApplicationContext,
				NOTIFICATION, notificationIntent, //YOUR_PI_REQ_CODE(현재 사용되지 않는 reqCode), notificationIntent,
				//http://stackoverflow.com/questions/1198558/how-to-send-parameters-from-a-notification-click-to-an-activity
				//Extra 를 쓰려면 onNewIntent(Intent intent) 를 사용하며
				//SingleTop을 선언해두면 새로 만들지 않고 처음 실행한 Intent가 항상 쓰인다.
				//방법: Intent의 FLAG_ACTIVITY_CLEAR_TOP로 지정하거나 해당 Activity의 Manifest에 SingleTop을 선언하면 된다.
				//Flag_Update_Current 써주세요. Extra만 새로 만듦. 
				PendingIntent.FLAG_UPDATE_CURRENT);// | PendingIntent.FLAG_CANCEL_CURRENT);
		
		//Resources res = getApplicationContext().getResources();
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

		builder.setContentIntent(contentIntent)
		            .setSmallIcon(EggCore.switchResId_SignalStrength(StrongEggManagerApp.COMMON_STR_NUM_2N,false))//.some_img)
		            //.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
		            //.setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS );//알림 방식.
		            //.setSound(sound, streamType)//소리
		            //.setVibrate(pattern)//진동
		            //.setLights(argb, onMs, offMs)//LED깜빡임
		            //.setUsesChronometer(true)
		            //.setOnlyAlertOnce(false)//알림 막대에 남아있지 않는다
		            //.setTicker(getString(R.string.app_service_name))//알림막대에 순간 표시
		            //.setWhen(System.currentTimeMillis())//굳이 현재시각을 사용한다면 이걸 쓸 필요는 없는걸까..
		            //.setAutoCancel(true)//이 알림을 누르면 알림막대에서 사라진다.
		            .setOngoing(true)//현재진행중
		            //.setProgress()//not implemented yet
		            .setContentTitle(getString(R.string.app_service_started))//제목
		            .setContentText(getString(R.string.app_service_connected))//내용
		            //.setSubText() //Not impelemmented yet
		            ;
		Notification notification = builder.getNotification();//.build(); deprecated라고 쓰지 말라지만, build가 안되에..

				//nm.notify(NOTIFICATION, notification);

		// Send the notification.
		mNotificationManager.notify(NOTIFICATION, notification);
	}
	
	//private void updateSignalStrengthNotificationAndroidV4(String signal, String usedWibro) {
	private void updateSignalStrengthNotificationAndroidV4() {
		
		if (contentIntent != null)
		{
			//Resources res = getApplicationContext().getResources();
			NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
	
			builder.setContentIntent(contentIntent)
			            .setSmallIcon(EggCore.switchResId_SignalStrength(false))//.some_img)
			            //.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
			            //.setUsesChronometer(true)
			            //.setOnlyAlertOnce(false)//알림 막대에 남아있지 않는다
			            //.setTicker(getString(R.string.app_service_name))//알림막대에 순간 표시
			            //.setWhen(System.currentTimeMillis())
			            .setOngoing(true)//현재진행중
			            //.setProgress()//not implemented yet
			            .setContentTitle(EggCore.getNotiTitle())//getString(R.string.app_service_started))//제목
			            .setContentText(EggCore.getNotiText())//getString(R.string.app_service_connected))//내용
			            //.setSubText() //Not impelemmented yet
			            ;
			Notification notification = builder.getNotification();//.build(); deprecated라고 쓰지 말라지만, build가 안되에..
	
			// Send the notification.
			mNotificationManager.notify(NOTIFICATION, notification);
		}
		else
		{
			if(isDebug)
			{
				Log.w(TAG_LOG,"contentIntent is NULL");
			}
		}
	}
	
	private boolean pollWibroCurrentStatus(){
		//서비스가 작동중일 때만 와이브로 상태 정보를 가져옵니다.
		//만약 실패한경우 false를 보내 가능한한 빨리 중단시킬래요.
		boolean wasSuccessful = false;
		
		if(_isEntireServiceRunning)
		{
			if(isDebug)
			{
				// Thread.currentThread().getPriority()
				Log.v(TAG_LOG,"Poller working on priority: "+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
			}
			
			try{
				if(!EggCore.isMiniAutoRefreshPreferred()){
					//http 요청만 하는게 아니라 직접 데이터를 처리하기 때문에 지연이 발생할 수 있다.
					//파싱하는 부분만 다른 스레드로 옮겨볼까ㅏ..
					//일단 최적화부터 시키고 하는게 좋겠다.
					if(EggCore.obtainWibroStatus(true) && EggCore.obtainCurrentSessionWibroUsed(true))
					{
						//updateSignalStrengthNotificationAndroidV4(EggCore.getRaw_WibroSignalStrengthSimple(),EggCore.getLastObtainedUsedWibroBytes());
						updateSignalStrengthNotificationAndroidV4();
	
						//mServiceHandler.postAtTime(this,System.currentTimeMillis()+UPDATE_INTERVAL);
						wasSuccessful = true;
					}
				} else {// 알림막대 간소화 옵션 사용시 시그널 상태만 가져오고 알림막대에만 반영
					if(EggCore.obtainWibroSignalOnly(false))
					{
						updateSignalStrengthNotificationAndroidV4();
						wasSuccessful = true;
					}
					
				}
				
			}catch(NullPointerException npe){
				if(isDebug)
				{
					Log.w(TAG_LOG,"Service Object Destroyed before current job finishes.");
					npe.printStackTrace();
				}
				//서비스 객체가 사라져서 자동중단되겠으나 지정해두는것도 나쁘진 않을거야.
				wasSuccessful = false;
			}
		}
		return wasSuccessful;
	}
	
	private void PollDataOnEveryMilliseconds() {
		//주기적인 정보 따오기를 시작합니다. postAtTime에 따라 UPDATE_INTERVAL 간격으로 메시지가 추가되어요.
		if(checkItOnceRunnable == null)
		{
			checkItOnceRunnable = new Runnable(){
				@Override
				public void run() {
					try
					{
						mServiceHandler.removeCallbacks(this); //다른 메시지 중복실행을 막기 위해 비운다.
						if(_isEntireServiceRunning)
						{
							//mServiceHandler.postDelayed(this,UPDATE_INTERVAL); //처음 위치가 왜 여기일까.
							if(!pollWibroCurrentStatus())
							{
								//0.5초 간격으로 재시도
								if(updateInterval > INITIAL_DELAY)
								{
									updateInterval = INITIAL_DELAY;
								}
								if(isDebug)	{
									Log.w(TAG_LOG,"Failed to poll. Remaining Attempts << "+retryAttemptRemaining);
									Log.w(TAG_LOG,"Failed to poll: next poll interval "+updateInterval+"milisec");
								}
								//재시도 횟수가 MAX_RETRY_ATTEMPTS 회를 넘기면 이 작업 요청을 모두 지우고 자동중단.. 
								if(--retryAttemptRemaining <= 0)
								{
									//mServiceHandler.removeCallbacks(this);
									
									EggCore.TerminateServiceSti();
									//아무 일 없이 중단보다는
									//알림을 올려놓고 재시작을 권유하는것도 좋을거야.
									
									//retryAttemptRemaining = MAX_RETRY_ATTMPTS;////이거 잘못 구현한것 같아.. ㅠ.. 다시 차네..
								}
							} else {
								if(updateInterval != EggCore.App_pollingRate)
								{
									updateInterval = EggCore.App_pollingRate;
									if(isDebug){
										Log.i(TAG_LOG,"Polled Data: next poll interval "+updateInterval+"milisec");
									}
								}
								if (retryAttemptRemaining < MAX_RETRY_ATTMPTS){
									updateInterval = EggCore.App_pollingRate;
									retryAttemptRemaining = MAX_RETRY_ATTMPTS;
									if(isDebug){
										Log.i(TAG_LOG,"Polled Data after Attempts. Reverting back Attempts to >> "+retryAttemptRemaining);
									}
								}
							}
							mServiceHandler.postDelayed(this,updateInterval);
						} else {
							//mServiceHandler.removeCallbacks(this);
							EggCore.TerminateServiceSti();
						}
						
					}catch(Exception e){
						if(isDebug)
						{
							Log.w(TAG_LOG,"GENERIC Exception while polling data.");
							e.printStackTrace();
						}
					}

				}
			};
		}
		
		//	초기 지연이 필요하진 않을거라 생각해서 바로 새로고침☆
		if(mServiceHandler != null){
			if(checkItOnceRunnable != null){
				mServiceHandler.post(checkItOnceRunnable);
			}
		}
	}
}
