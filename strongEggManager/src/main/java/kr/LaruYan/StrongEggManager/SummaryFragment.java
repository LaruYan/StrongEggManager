package kr.LaruYan.StrongEggManager;

import kr.LaruYan.StrongEggManager.StrongEggManagerApp.OnLocalRefreshEggInfoListener;
import kr.LaruYan.StrongEggManager.StrongEggManagerApp.OnWiFiAPChangedListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;

public class SummaryFragment extends Fragment {
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_SummaryFragment";

	private StrongEggManagerApp EggCore;
	
	//UI 내용 변경 요청
	//private BroadcastReceiver summaryFragment_broadcastReceiver;
	//private boolean isUiUpdateReceiverRegistered = false; 
	private OnLocalRefreshEggInfoListener summaryFragment_OnLocalRefreshEggInfoListener;
	private OnWiFiAPChangedListener summaryFragment_OnWiFiAPChangedListener;
	private boolean isUiUpdateListenerAttached = false;
	
	
	public static final String EXTRA_SCROLL_Y = "ScrollY";
	
	public static final String EXTRA_BIN_WIBROSIGNAL_EXPANDED = "WbSigEx";
	public static final String EXTRA_INT_WIBROSIGNAL_DRAWABLE = "WbSigDw";
	public static final String EXTRA_TEXT_WIBROSIGNAL_STATUS = "WbSigSt";
	public static final String EXTRA_TEXT_WIBROSIGNAL_DETAILED = "WbUsgDt";
	
	public static final String EXTRA_BIN_WIBROUSAGE_EXPANDED = "WbUsgEx";
	public static final String EXTRA_TEXT_WIBROUSAGE_STATUS = "WbUsgSt";
	public static final String EXTRA_TEXT_WIBROUSAGE_DETAILED = "WbUsgDt";
	
	public static final String EXTRA_BIN_WIBROALLOCATION_EXPANDED = "WbAlcEx";
	public static final String EXTRA_TEXT_WIBROALLOCATION_STATUS = "WbAlcSt";
	public static final String EXTRA_TEXT_WIBROALLOCATION_DETAILED = "WbAlcDt";
	
	public static final String EXTRA_TEXT_POWER_STATUS = "EgPwrSt";
	
	public static final String EXTRA_BIN_WIFI_EXPANDED = "EgWifEx";
	public static final String EXTRA_TEXT_WIFI_STATUS = "EgWifSt";
	public static final String EXTRA_TEXT_WIFI_DETAILED = "EgWifDt";
	
	//UI구성요소
	private View Vw_summary;
	private PullToRefreshScrollView PtRSv_Summary;
	private View Vw_DummyBottom_PullDownToRefresh;
	
	//항목
	private TextView Tv_WbSignal_Status;
	private TextView Tv_WbSignal_Detailed;
	

	private TextView Tv_WbUsage_Status;
	private TextView Tv_WbUsage_Detailed;
	
	private TextView Tv_WbAlloc_Status;
	private TextView Tv_WbAlloc_Detailed;
	
	private TextView Tv_EggPower_Status;
	
	private TextView Tv_EggWifi_Status;
	private TextView Tv_EggWifi_Detailed;
	
	public static SummaryFragment newInstance() {
		SummaryFragment frag = new SummaryFragment();
        return frag;
    }
	
	/** 요약 화면 */
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
		super.onCreateView(inflater,container,savedInstanceState);
	    
	    if(isDebug)
		{
			Log.v(TAG_LOG,"onCreateView()");
		}
	    
	    //first need a reference to activity, then to application
		EggCore = (StrongEggManagerApp) getActivity().getApplicationContext();
		
		//저 null 자리에 container를 넣었더니 child View가 있으니 먼저 지우고 하라며 싫증낸다.. ㅠㅠ 
		//아무 말없이.. 로그에서 찾기도 어렵게.. 미워.
				Vw_summary = inflater.inflate(R.layout.activity_summary, null);//(LinearLayout)inflater.inflate(R.layout.activity_summary, container, false);

				EggCore.uiCompositionThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						initializeUi(inflater, Vw_summary, savedInstanceState);
					}
			    });
			    

			    if(isDebug)
				{
					Log.v(TAG_LOG,"onCreateView() finished.");
				}
				
		        return Vw_summary;
			}
			
			//영구적 보존을 원하면 onSaveInstanceState보단 onPause를 사용.
			@Override
		    public void onSaveInstanceState(Bundle outState) {
		        super.onSaveInstanceState(outState);
		        if(isDebug){
		        	Log.v(TAG_LOG,"onSaveInstanceState(Bundle outState)");
		        }
		        try{
			        //내보내기 시작, 화면에 있던 것을 상태정보에 집어넣어요.
			        //outState.putString("DEBUGTEST", (String) Tv_Debug.getText());
			        outState.putInt(EXTRA_SCROLL_Y, PtRSv_Summary.getScrollY());
			        
			    	outState.putCharSequence(EXTRA_TEXT_WIBROSIGNAL_STATUS, Tv_WbSignal_Status.getText());
			    	outState.putCharSequence(EXTRA_TEXT_WIBROSIGNAL_DETAILED, Tv_WbSignal_Detailed.getText());
			    	//outState.putBoolean(EXTRA_INT_WIBROSIGNAL_DRAWABLE, Tv_WbSignal_Status.getCom)
			    	outState.putCharSequence(EXTRA_TEXT_WIBROUSAGE_STATUS, Tv_WbUsage_Status.getText());
			    	outState.putBoolean(EXTRA_BIN_WIBROUSAGE_EXPANDED,((Tv_WbUsage_Detailed.getVisibility() == View.VISIBLE)? true : false));
			    	outState.putCharSequence(EXTRA_TEXT_WIBROUSAGE_DETAILED, Tv_WbUsage_Detailed.getText());

			    	outState.putCharSequence(EXTRA_TEXT_WIBROALLOCATION_STATUS, Tv_WbAlloc_Status.getText());
			    	outState.putBoolean(EXTRA_BIN_WIBROALLOCATION_EXPANDED,((Tv_WbAlloc_Detailed.getVisibility() == View.VISIBLE)? true : false));
			    	outState.putCharSequence(EXTRA_TEXT_WIBROALLOCATION_DETAILED, Tv_WbAlloc_Detailed.getText());
			    	
			    	outState.putCharSequence(EXTRA_TEXT_POWER_STATUS, Tv_EggPower_Status.getText());
			    	
			    	outState.putCharSequence(EXTRA_TEXT_WIFI_STATUS, Tv_EggWifi_Status.getText());
			    	outState.putBoolean(EXTRA_BIN_WIFI_EXPANDED,((Tv_EggWifi_Detailed.getVisibility() == View.VISIBLE)? true : false));
			    	outState.putCharSequence(EXTRA_TEXT_WIFI_DETAILED, Tv_EggWifi_Detailed.getText());

			        //내보내기 끝,   
			        //경고: saveInstanceState는 영구적이지 않아요.
		        }catch(NullPointerException npe){
		        	if(isDebug)
		        	{
		        		Log.v(TAG_LOG,"NullPointerException During onSaveInstanceState(Bundle outState)");
		        		npe.printStackTrace();
		        	}
		        }
		    }

			//BroadCastReceiver로 UI 갱신을 한다. Activity로 해야한다고 했던데,
				//.. 여기서 구현해버리면 곤란할지도..
					//http://www.websmithing.com/2011/02/01/how-to-update-the-ui-in-an-android-activity-using-data-from-a-background-service/
				@Override
				public void onResume()
				{
					//
					super.onResume();
					if(isDebug)
					{
						Log.v(TAG_LOG,"onResume()");
					}
					//화면 전환 될때 화면 갱신 요청..부하는 크겠지만 이방법외엔 모르겠어요.
					do_updateUI_Local(true,true,true);
					
					//화면에 나타나게 된 이 Fragment를 정리 우선 대상에서 빼요.
					setUserVisibleHint(true);
					
					EggCore.broadcastThreadHandler.post(new Runnable(){
						@Override
						public void run(){
							attachUiUpdateListeners();
						}
					});
					
					if(isDebug)
					{
						Log.v(TAG_LOG,"onResume() finished");
					}
				}

				@Override
				public void onPause()
				{
					//
					if(isDebug)
					{
						Log.v(TAG_LOG,"onPause()");
					}

					EggCore.broadcastThreadHandler.post(new Runnable(){
						@Override
						public void run(){
							detachUiUpdateListeners();
						}
					});
					EggCore.uiCompositionThreadHandler.post(new Runnable(){

						@Override
						public void run() {
							//TODO
							//onSaveInstanceState()는 onStop() 전에 실행되기 때문에
							// 아래쪽의 null들을 했다간 onSaveInstanceState에선 거둘게 없을것이다.
							
							if(isRemoving())//?? .isFinishing())이거랑은 다를거야.
							{
								if(isDebug)
								{
									Log.v(TAG_LOG,"this fragment is being removed");
								}
								//unregisterUiUpdateBroadCastReceiver();
								
								// onDestroy 보다 먼저 불려지고 동작을 보증하는 메소드이므로 onPause+isFinishing()조합으로 하자.
								summaryFragment_OnLocalRefreshEggInfoListener = null;
								
								//요약 UI의 항목들
								Tv_WbSignal_Status = null;
								Tv_WbSignal_Detailed = null;
								
								Tv_WbUsage_Status = null;
								Tv_WbUsage_Detailed = null;
								
								Tv_WbAlloc_Status = null;
								Tv_WbAlloc_Detailed = null;
								
								Tv_EggPower_Status = null;
								
								Tv_EggWifi_Status = null;
								Tv_EggWifi_Detailed = null;
								
								PtRSv_Summary = null;
								Vw_DummyBottom_PullDownToRefresh = null;
								
								EggCore = null;
							}
						}
					});
					
					//화면에 나타나지 않는 이 Fragment를 정리 우선 대상으로 두어요.  
			        setUserVisibleHint(false);
			        
					super.onPause();
					
					if(isDebug)
					{
						Log.v(TAG_LOG,"onPause() finished.");
					}
				}	
				
				
				private void initializeUi(LayoutInflater inflater, final View viewToBeAttached, Bundle savedInstanceState)
				{
					final View inflatedView = inflater.inflate(R.layout.fragment_summary, null);//(LinearLayout)inflater.inflate(R.layout.activity_settings, container, false);
					Vw_DummyBottom_PullDownToRefresh =  inflatedView.findViewById(R.id.viewRect_PullDownToRefreshDummy);
					//LinL_Summary = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_Summary);
					
					//당겨서 새로고침이 라이브러리 프로젝트로 만들어주신 chrisbanes께 감사드려요.ㅠㅠ 광복절 하루동안 이게 뭐야아아..ㅠㅠㅠ
					//https://github.com/chrisbanes/Android-PullToRefresh/wiki/Quick-Start-Guide
					PtRSv_Summary = (PullToRefreshScrollView) inflatedView.findViewById(R.id.scrollView_Summary); 
					PtRSv_Summary.setBackgroundResId(R.color.StripColor);
					//PtRSv_Summary.setLastUpdatedLabel(getString(R.string.item_header_recentobatined)+"HCSTR_ 알 수 없음");
					Vw_DummyBottom_PullDownToRefresh.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,PtRSv_Summary.getHeaderHeight()));
					//PtRSv_Summary.set
					///if(getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ){
					//	if(isDebug){
					//		Log.v(TAG_LOG,"This Android Device has TouchScreen.enabling gesture");
					//	}
						PtRSv_Summary.setOnRefreshListener(new OnRefreshListener<ScrollView>() {
			
							@Override
							public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
								EggCore.taskThreadHandler.post(new Runnable(){
									@Override
									public void run() {
										// TODO Auto-generated method stub
										//EggCore.showShortToast("HCSTR_ 요약 po새로고침wer");
										EggCore.LocalRefresh();
										
										EggCore.mainThreadHandler.post(new Runnable(){
											@Override
											public void run() {
												PtRSv_Summary.onRefreshComplete();
												//PtRSv_Summary.setLastUpdatedLabel(getString(R.string.item_header_recentobatined)+System.currentTimeMillis());
												
												Vw_DummyBottom_PullDownToRefresh.setVisibility(View.GONE);
											}});
									}});
								//TIP ClassCastException을 막기 위해 소속 View의 레이아웃 지시사항을 따르게 해야한다.
								//RelativeLayout 소속 linLayout의 경우 LinearLayout. 이나 ViewGroup. 의 것이 아닌, 직속의 RelativeLayout을 써야한다.  
								//linLayout.setLayoutParams(new android.widget.RelativeLayout.LayoutParams(50,50));
								Vw_DummyBottom_PullDownToRefresh.setVisibility(View.INVISIBLE);
							}});
						PtRSv_Summary.setDisableScrollingWhileRefreshing(false);
						PtRSv_Summary.setShowViewWhileRefreshing(true);

				    
					//와이브로 수신률 표시
					Tv_WbSignal_Status = (TextView) inflatedView.findViewById(R.id.textView_Wibro_Signal);
					Tv_WbSignal_Detailed = (TextView) inflatedView.findViewById(R.id.textView_Wibro_SignalDetailed);
					Tv_WbSignal_Status.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							toggleVisibility(Tv_WbSignal_Detailed);
						}
					
					});
					
					//와이브로 할당 정보 표시
					Tv_WbAlloc_Status = (TextView) inflatedView.findViewById(R.id.textView_Wibro_currentStatus);
					Tv_WbAlloc_Detailed = (TextView) inflatedView.findViewById(R.id.textView_Wibro_StatusDetailed);
					Tv_WbAlloc_Status.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							toggleVisibility(Tv_WbAlloc_Detailed);
						}
					
					});
					
					//와이브로 사용량 표시
					Tv_WbUsage_Status = (TextView) inflatedView.findViewById(R.id.textView_Wibro_Usage);
					Tv_WbUsage_Detailed = (TextView) inflatedView.findViewById(R.id.textView_Wibro_UsageDetailed);
					Tv_WbUsage_Status.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							toggleVisibility(Tv_WbUsage_Detailed);
						}
					
					});

					//에그 방전 상황 표시
					Tv_EggPower_Status = (TextView) inflatedView.findViewById(R.id.textView_Power_Status);
					Tv_EggPower_Status.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							EggCore.showShortToast(R.string.guide_power_option);
						}
					
					});
					
					//에그 와이파이 상태 표시
					Tv_EggWifi_Status = (TextView) inflatedView.findViewById(R.id.textView_Wifi_Status);
					Tv_EggWifi_Detailed = (TextView) inflatedView.findViewById(R.id.textView_Wifi_StatusDetailed);
					Tv_EggWifi_Status.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							toggleVisibility(Tv_EggWifi_Detailed);
						}
					
					});
		/*
					if(savedInstanceState != null)
					{
						if(isDebug){
				        	Log.v(TAG_LOG,"savedInstanceState is available");
				        }
						
						if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1){
							restoreUi_savedStateAPI12(savedInstanceState);
						}else{
							restoreUi_savedState(savedInstanceState);
						}
					}
					else
					{
						if(isDebug){
				        	Log.v(TAG_LOG,"There is NO savedInstanceState");
				        }
//						Tv_Debug.setText(EggCore.getEggIP());
					}
						  */  
				    //다 만들고 나서 view 를 달아달라고 요청준비
					final RelativeLayout relativeLayout_view = (RelativeLayout) viewToBeAttached.findViewById(R.id.RelativeLayout_summaryFragment);
					Runnable Run_attachMainUi = (new Runnable(){
						@Override
						public void run() {
							
							ViewGroup viewParent = ((ViewGroup)relativeLayout_view.getParent());
							if(viewParent != null){
								//기존의 불러오는중 화면을 빼고 지금껏 만든 화면을 놓아요.
								viewParent.addView(inflatedView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
								viewParent.removeView(relativeLayout_view);
							}else{
								//무..무엇인가 잘못되었다.
								if(isDebug){
									Log.e(TAG_LOG,"viewParent is NULL");
								}
								//다시 시도 확인
								Button Btn_Retry = (Button)relativeLayout_view.findViewById(R.id.button_Retry);
								if(Btn_Retry != null){
									final Runnable recursion = this;//메모리 장악당할수도 있겠다. ㅠㅠ
									Btn_Retry.setOnClickListener(new OnClickListener(){
										@Override
										public void onClick(View view) {
											EggCore.mainThreadHandler.postDelayed(recursion,StrongEggManagerApp.DEFAULT_UI_DELAY);
										}});
									
									Btn_Retry.setVisibility(View.VISIBLE);
								}
							}
						}
				    });
					EggCore.mainThreadHandler.post(Run_attachMainUi);
					
				    EggCore.broadcastThreadHandler.post(new Runnable(){
						@Override
						public void run() {
							if(summaryFragment_OnLocalRefreshEggInfoListener == null)
						    {
								summaryFragment_OnLocalRefreshEggInfoListener = new OnLocalRefreshEggInfoListener() {

									@Override
									public void onRefreshedWibroStatus() {
										if(isUiUpdateListenerAttached){
											//TODO
											do_updateUI_Local(true,false,false);///
										}
									}

									@Override
									public void onRefreshedWibroUsageSession() {
										if(isUiUpdateListenerAttached){
											//TODO
											do_updateUI_Local(false,true,false);
										}
									}

									@Override
									public void onLocalRefreshed() {
										//고봐 여기 없잖아 ㅠㅠ 그래서 달아야지
								do_updateUI_Local(true,true,false);
							}

				        };
				    }
					if(summaryFragment_OnWiFiAPChangedListener == null){
						summaryFragment_OnWiFiAPChangedListener = new OnWiFiAPChangedListener(){

							@Override
							public void onWiFiAPChanged(String ssid, boolean hidden, String bssid, String clientIPA) {
								//와이파이 상태변화..
								if(isUiUpdateListenerAttached){
									updateUi_WiFi(ssid, hidden, bssid, clientIPA);
								}
							}
						};
					}
					attachUiUpdateListeners();
				}});
		}
		/*
		private void restoreUi_savedState(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
				try{
					//TODO 가져오기 시작, 미리 저장된 상태정보를 꺼내 화면을 다시 구성해요.
//					Tv_Debug.setText(savedInstanceState.getString("DEBUGTEST"));
					Tv_WbSignal_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROSIGNAL_STATUS));
					//Tv_WbSignal_Status.setCompoundDrawablesWithIntrinsicBounds( savedInstanceState.getInt(EXTRA_INT_WIBROSIGNAL_DRAWABLE), 0, 0, 0);
					Tv_WbSignal_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROSIGNAL_EXPANDED) ? View.VISIBLE : View.GONE);
					Tv_WbSignal_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROSIGNAL_DETAILED));
					
					Tv_WbUsage_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROUSAGE_STATUS));
					Tv_WbUsage_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROUSAGE_EXPANDED) ? View.VISIBLE : View.GONE);
					Tv_WbUsage_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROUSAGE_DETAILED));

					Tv_WbAlloc_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROALLOCATION_STATUS));
					Tv_WbAlloc_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROALLOCATION_EXPANDED) ? View.VISIBLE : View.GONE);
					Tv_WbAlloc_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROALLOCATION_DETAILED));
			    	
					Tv_EggPower_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_POWER_STATUS));
			    	
					Tv_EggWifi_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIFI_STATUS));
					Tv_EggWifi_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIFI_EXPANDED) ? View.VISIBLE : View.GONE);
					Tv_EggWifi_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIFI_DETAILED));
					
					setScroll(PtRSv_Summary,0, savedInstanceState.getInt(EXTRA_SCROLL_Y,0));
					//가져오기 끝
				}catch(NullPointerException npe){
		        	if(isDebug)
		        	{
		        		Log.v(TAG_LOG,"NullPointerException get Extras on savedInstanceState");
		        		npe.printStackTrace();
		        	}
		        }
		}
		
		@TargetApi(12)
		private void restoreUi_savedStateAPI12(Bundle savedInstanceState) {
			//TODO 가져오기 시작, 미리 저장된 상태정보를 꺼내 화면을 다시 구성해요.
//					Tv_Debug.setText(savedInstanceState.getString("DEBUGTEST"));
			Tv_WbSignal_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROSIGNAL_STATUS,"?"));
			//Tv_WbSignal_Status.setCompoundDrawablesWithIntrinsicBounds( savedInstanceState.getInt(EXTRA_INT_WIBROSIGNAL_DRAWABLE, R.drawable.ic_stat_notify_waiting_dark), 0, 0, 0);
			Tv_WbSignal_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROSIGNAL_EXPANDED, false) ? View.VISIBLE : View.GONE);
			Tv_WbSignal_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROSIGNAL_DETAILED,"?"));
			
			Tv_WbUsage_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROUSAGE_STATUS,"?"));
			Tv_WbUsage_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROUSAGE_EXPANDED, false) ? View.VISIBLE : View.GONE);
			Tv_WbUsage_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROUSAGE_DETAILED,"?"));

			Tv_WbAlloc_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROALLOCATION_STATUS,"?"));
			Tv_WbAlloc_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIBROALLOCATION_EXPANDED, false) ? View.VISIBLE : View.GONE);
			Tv_WbAlloc_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIBROALLOCATION_DETAILED,"?"));
	    	
			Tv_EggPower_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_POWER_STATUS,"?"));
	    	
			Tv_EggWifi_Status.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIFI_STATUS,"?"));
			Tv_EggWifi_Detailed.setVisibility(savedInstanceState.getBoolean(EXTRA_BIN_WIFI_EXPANDED,false) ? View.VISIBLE : View.GONE);
			Tv_EggWifi_Detailed.setText(savedInstanceState.getCharSequence(EXTRA_TEXT_WIFI_DETAILED,"?"));
			
			setScroll(PtRSv_Summary,0, savedInstanceState.getInt(EXTRA_SCROLL_Y,0));
		}
		
		private void setScroll(final PullToRefreshScrollView PtRSv,final int x, final int y)
		{
			if (y > 0)
				//가로에 대응하진 않으니 씁쓸하다. y가 0일경우 가로만이라도 스크롤할 수는 없당!
			{
				//스레드를 만들어 다음 Queue에 넣어, 그 Queue가 작동할때 스크롤을 시켜주도록 합니다.
				//시간차..(..)
				EggCore.mainThreadHandler.post(new Runnable(){
					@Override
					public void run()
					{
						PtRSv.post(new Runnable() {
				    	    @Override
				    	    public void run() {
				    	    	PtRSv.scrollTo(0, y);//바로 스크롤
				    	//    	Sv.scrollBy(0, y);//바로 스크롤
				    	    } 
				    	});
					}
				
				});
			}
			if (x > 0)
				//세로에 대응하진 않으니 씁쓸하다. x가 0일경우 세로만이라도 스크롤할 수는 없당!
			{
				//스레드를 만들어 다음 Queue에 넣어, 그 Queue가 작동할때 스크롤을 시켜주도록 합니다.
				//시간차..(..)
				EggCore.mainThreadHandler.post(new Runnable(){
					@Override
					public void run()
					{
						PtRSv.post(new Runnable() {
				    	    @Override
				    	    public void run() {
				    	    	PtRSv.scrollTo(x, 0);//바로 스크롤
				    	//    	Sv.scrollBy(x, 0);//바로 스크롤
				    	    } 
				    	});
					}
				
				});
			}
		}
		*/
		private void toggleVisibility(View requestedView){
			if(requestedView.getVisibility() == View.VISIBLE){
				requestedView.setVisibility(View.GONE);
			}
			else {
				requestedView.setVisibility(View.VISIBLE);
			}
		}

		private final boolean attachUiUpdateListeners()
		{
			if(!isUiUpdateListenerAttached)
			{
				if(summaryFragment_OnLocalRefreshEggInfoListener != null)
				{
					EggCore.setOnLocalRefreshEggInfoListener(summaryFragment_OnLocalRefreshEggInfoListener);
					EggCore.setOnWiFiAPChangedListener(summaryFragment_OnWiFiAPChangedListener);
					
					isUiUpdateListenerAttached = true;
					if(isDebug)
					{
						Log.i(TAG_LOG,"UiUpdate Listeners set.");
					}
					return true;

				}
				else
				{
					isUiUpdateListenerAttached = false;
			        if(isDebug)
					{
						Log.w(TAG_LOG,"UiUpdate Listeners are NULL.");
					}
				}
				
			}
			else
			{
				if(isDebug)
				{
					Log.v(TAG_LOG,"UiUpdate Listeners are already set. (as of FLAG)");
				}
			}
			
			return false;
		}
		
		private final boolean detachUiUpdateListeners()
		{
			if(isUiUpdateListenerAttached)
			{
				if(summaryFragment_OnLocalRefreshEggInfoListener != null && summaryFragment_OnWiFiAPChangedListener != null)
				{
					isUiUpdateListenerAttached = false;
					EggCore.removeOnLocalRefreshEggInfoListener(summaryFragment_OnLocalRefreshEggInfoListener);
					EggCore.removeOnWiFiAPChangedListener(summaryFragment_OnWiFiAPChangedListener);
					if(isDebug)
					{
						Log.i(TAG_LOG,"UiUpdate Listeners are removed.");
					}
					return true;
				}
				else
				{
					isUiUpdateListenerAttached = false;
					if(isDebug)
					{
						Log.w(TAG_LOG,"UiUpdate Listeners are NULL.");
					}
				}
			}
			else
			{
				if(isDebug)
				{
					Log.v(TAG_LOG,"UiUpdate Listeners are already removed. (as of FLAG)");
				}
			}
			
			return false;
		}
		
		
		private void do_updateUI_Local(final boolean isStatus, final boolean isSession,final boolean isWiFi){
			EggCore.uiCompositionThreadHandler.post(new Runnable(){

				@Override
				public void run() {
					// TODO 임시방편..
					if(isStatus){
						updateUi_WibroStatus();
					}
					if(isSession){
						updateUi_WibroSession();
					}
					if(isWiFi){
						updateUi_WiFi();
					}
				}
			
			});
		}
		
		private void updateUi_WibroStatus(){
			updateUi_WibroStatus(//EggCore.
					EggCore.switchResId_SignalStrength(true),
					EggCore.switchString_SignalStrength(),
					EggCore.switchWibroStatus(),EggCore.getRaw_WibroSignalDetailed(),
					EggCore.getChecked_WibroIP(),
					EggCore.getRaw_WibroGateway(),EggCore.getRaw_WibroDnsPrimary(),EggCore.getRaw_WibroDnsSecondary(),
					EggCore.switchResId_BatteryLevel(), EggCore.switchString_BatteryLevel());
		}
		
		private void updateUi_WibroStatus(
				final int wibro_signalStrengthIcon,
				final String wibro_signalStrength,
				final String wibro_status, final String wibro_CINRRSSI,
				final String wibro_IP,
				final String wibro_GW,final String wibro_1DNS,final String wibro_2DNS,
				final int egg_batteryLevel, final String BatteryLevel){
			if(isDebug)
			{
				Log.v(TAG_LOG,"updateUI()-WibroStatus");
			}

			EggCore.uiCompositionThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					if(wibro_CINRRSSI != null){
						int delimiter = wibro_CINRRSSI.indexOf(StrongEggManagerApp.COMMON_STR_SLASHWITHSPACES, 1);
						if(delimiter != -1){
							refreshUi_WibroStatus(
									wibro_signalStrengthIcon,									
									getString(R.string.item_title_signalsimple)+wibro_signalStrength,
									getString(R.string.item_header_wibrostatus)+wibro_status+StrongEggManagerApp.COMMON_STR_CRLF+
									getString(R.string.item_header_cinr_and_rssi)+wibro_CINRRSSI.substring(0, delimiter)+" dB / "+wibro_CINRRSSI.substring(delimiter+3)+" dBm",
									wibro_IP != null ? getString(R.string.item_title_ipv4) + wibro_IP : getString(R.string.wb_status_notconnected),
									getString(R.string.item_header_gateway)+ wibro_GW +StrongEggManagerApp.COMMON_STR_CRLF+getString(R.string.item_header_dns)+wibro_1DNS+StrongEggManagerApp.COMMON_STR_SLASHWITHSPACES+wibro_2DNS,
									egg_batteryLevel,getString(R.string.item_title_battery)+BatteryLevel);
						}
					}else{
						refreshUi_WibroStatus(
								wibro_signalStrengthIcon,
								getString(R.string.item_title_signalsimple)+wibro_signalStrength,
								getString(R.string.item_header_cinr_and_rssi)+wibro_CINRRSSI,
								wibro_IP != null ? getString(R.string.item_title_ipv4) + wibro_IP : getString(R.string.wb_status_notconnected),
								getString(R.string.item_header_gateway)+ wibro_GW +StrongEggManagerApp.COMMON_STR_CRLF+getString(R.string.item_header_dns)+wibro_1DNS+StrongEggManagerApp.COMMON_STR_SLASHWITHSPACES+wibro_2DNS,
								egg_batteryLevel,getString(R.string.item_title_battery)+BatteryLevel);
					}
				}});
		}
		private void refreshUi_WibroStatus(
				final int intWbSignal_Icon,
				final String strWbSignal_Simple,
				final String strWbSignal_Detailed,
				final String strWbAlloc_Simple,
				final String strWbAlloc_Detailed,
				final int intEgBatt_Icon,
				final String strEggPower_Simple){
			EggCore.mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					//와이브로 수신률 표시
					Tv_WbSignal_Status.setCompoundDrawablesWithIntrinsicBounds(intWbSignal_Icon, 0, 0, 0);
					Tv_WbSignal_Status.setText(strWbSignal_Simple);
					Tv_WbSignal_Detailed.setText(strWbSignal_Detailed);
					
					//와이브로 할당 정보 표시
					Tv_WbAlloc_Status.setText(strWbAlloc_Simple);
					Tv_WbAlloc_Detailed.setText(strWbAlloc_Detailed);
					
					//에그 방전 상황 표시
					Tv_EggPower_Status.setCompoundDrawablesWithIntrinsicBounds(intEgBatt_Icon, 0, 0, 0);
					Tv_EggPower_Status.setText(strEggPower_Simple);
				}});
		}
		private void updateUi_WibroSession(){
			updateUi_WibroSession(
					//"HCSTR_ 요금제",
					EggCore.getProcessed_WibroElapsedTime(),
					EggCore.getRaw_WibroCurrentSessionUsedMB_DN(),
					EggCore.getRaw_WibroCurrentSessionUsedMB_UP() );
		}
		
		private void updateUi_WibroSession(
				//final String wibro_Plan,
				final String wibro_CurrentElapsed,
				final String wibro_CurrentDN,
				final String wibro_CurrentUP ){
			if(isDebug)
			{
				Log.v(TAG_LOG,"updateUI()-WibroUsageSession");
			}
			EggCore.uiCompositionThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					refreshUi_WibroSession(wibro_CurrentElapsed,
							getString(R.string.item_header_download)+(wibro_CurrentDN == null ? getString(R.string.wb_status_unknown) : StrongEggManagerApp.putCommaNumeric(wibro_CurrentDN)+StrongEggManagerApp.COMMON_UNIT_PACKETS_BYTE)+StrongEggManagerApp.COMMON_STR_CRLF+
							getString(R.string.item_header_upload)+(wibro_CurrentUP == null ? getString(R.string.wb_status_unknown) : StrongEggManagerApp.putCommaNumeric(wibro_CurrentUP)+StrongEggManagerApp.COMMON_UNIT_PACKETS_BYTE));
				}});
		}
		private void refreshUi_WibroSession(final String strWbUsage_Simple,final String strWbUsage_Detailed){
			
			
			EggCore.mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					//와이브로 사용량 표시
					Tv_WbUsage_Status.setText(strWbUsage_Simple);
					Tv_WbUsage_Detailed.setText(strWbUsage_Detailed);
				}});
		}
		
		private void updateUi_WiFi(){
			updateUi_WiFi(EggCore.getSSID(),
					false,
					EggCore.getBSSID(),
					EggCore.getClientIP() );
		}
		private void updateUi_WiFi(final String aP_ssid,final boolean aP_isHidden, final String aP_bssid, final String client_Ipa){
			if(isDebug)
			{
				Log.v(TAG_LOG,"updateUI()-WiFi");
			}
			EggCore.uiCompositionThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					/*refreshUi_WiFi(aP_ssid,getString(R.string.item_header_apname)+
							getString(aP_isHidden ? R.string.wifi_ssid_hidden:R.string.wifi_ssid_visible)+StrongEggManagerApp.COMMON_STR_SLASHWITHSPACES+
							getString(R.string.item_header_phone_ip)+client_Ipa+StrongEggManagerApp.COMMON_STR_CRLF+getString(R.string.item_header_channel)+StrongEggManagerApp.COMMON_STR_SLASHWITHSPACES+getString(R.string.item_header_bssid)+aP_bssid
							);*/
					refreshUi_WiFi(aP_ssid,getString(R.string.item_header_phone_ip)+client_Ipa+StrongEggManagerApp.COMMON_STR_CRLF+
							getString(R.string.item_header_bssid)+aP_bssid
							);
				}});
		}
		private void refreshUi_WiFi(final String strEggWifi_Simple, final String strEggWifi_Detailed){
			EggCore.mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					//에그 와이파이 상황 표시
					Tv_EggWifi_Status.setText(strEggWifi_Simple);
					Tv_EggWifi_Detailed.setText(strEggWifi_Detailed);
				}});
		}

}
