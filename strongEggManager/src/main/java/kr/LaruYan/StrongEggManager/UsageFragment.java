package kr.LaruYan.StrongEggManager;

import kr.LaruYan.StrongEggManager.StrongEggManagerApp.OnUsageKtRefreshEggInfoListener;
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

public class UsageFragment extends Fragment {
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_UsageFragment";
	
	//private WeakReference<StrongEggManagerApp> wkrfEggCore;
	private StrongEggManagerApp EggCore;
	
	private OnUsageKtRefreshEggInfoListener usageFragment_OnUsageKtRefreshEggInfoListener;
	private boolean isUiUpdateListenerAttached = false;
	
	private View Vw_usage;
	private PullToRefreshScrollView PtRSv_Usage;
	private View Vw_DummyBottom_PullDownToRefresh;
	

	//사용량 정보
	private TextView Tv_Usage_header;
	private TextView Tv_UsagePlan;
	private TextView Tv_UsagePacketsSimple;
	private TextView Tv_UsagePacketsDetailed;
	private TextView Tv_UsageBillsSimple;
	private TextView Tv_UsageBillsDetailed;
    
    //새로고침 버튼.
	private TextView Tv_UsageCheckWeb;

	
	public static UsageFragment newInstance() {
		UsageFragment frag = new UsageFragment();
        return frag;
    }

	/** 사용량 화면 */
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
		super.onCreateView(inflater,container,savedInstanceState);
	    
	    if(isDebug)
		{
			Log.v(TAG_LOG,"onCreateView()");
		}
	    
	    //first need a reference to activity, then to application
	    EggCore = (StrongEggManagerApp) getActivity().getApplicationContext();
	    
	    Vw_usage = inflater.inflate(R.layout.activity_usage, null);//(LinearLayout)inflater.inflate(R.layout.activity_usage, container, false);
	    EggCore.uiCompositionThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				initializeUi(inflater, Vw_usage, savedInstanceState);
			}
		});

	    if(isDebug)
		{
			Log.v(TAG_LOG,"onCreateView() finished.");
		}
	    
	    return Vw_usage;
	}
	
	private void initializeUi(LayoutInflater inflater, final View viewToBeAttached, Bundle savedInstanceState)
	{

		final View inflatedView = inflater.inflate(R.layout.fragment_usage, null);//(LinearLayout)inflater.inflate(R.layout.activity_settings, container, false);
	    Vw_DummyBottom_PullDownToRefresh =  inflatedView.findViewById(R.id.viewRect_PullDownToRefreshDummy);
		
		PtRSv_Usage = (PullToRefreshScrollView)inflatedView.findViewById(R.id.scrollView_Usage);
		PtRSv_Usage.setDisableScrollingWhileRefreshing(false);
		PtRSv_Usage.setShowViewWhileRefreshing(true);
		PtRSv_Usage.setBackgroundResId(R.color.StripColor);
		PtRSv_Usage.setLastUpdatedLabel(getString(R.string.item_header_recentobatined)+getString(R.string.wb_status_unknown));
		Vw_DummyBottom_PullDownToRefresh.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,PtRSv_Usage.getHeaderHeight()));
		PtRSv_Usage.setOnRefreshListener(new OnRefreshListener<ScrollView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
				EggCore.taskThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						//TODO 사용량 새로고침
						EggCore.checkWibroUsageFromKT();
					}});
				Vw_DummyBottom_PullDownToRefresh.setVisibility(View.INVISIBLE);
			}});

		//섹션 머릿말
		Tv_Usage_header = (TextView) inflatedView.findViewById(R.id.textView_Usage_label);
		
	    //사용량 정보
	    Tv_UsagePlan = (TextView) inflatedView.findViewById(R.id.textView_Usage_Plan);
	    Tv_UsagePlan.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				EggCore.showShortToast(R.string.guide_plan_info);
			}
		
		});
		Tv_UsagePacketsSimple = (TextView) inflatedView.findViewById(R.id.textView_Usage_packetsSimple);
		Tv_UsagePacketsDetailed = (TextView) inflatedView.findViewById(R.id.textView_Usage_packetsDetailed);
		Tv_UsagePacketsSimple.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				toggleVisibility(Tv_UsagePacketsDetailed);
			}
		
		});
		Tv_UsageBillsSimple = (TextView) inflatedView.findViewById(R.id.textView_Usage_billsSimple);
		Tv_UsageBillsDetailed = (TextView) inflatedView.findViewById(R.id.textView_Usage_billsDetailed);
		Tv_UsageBillsSimple.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				toggleVisibility(Tv_UsageBillsDetailed);
			}
		
		});
		
		//고객센터 버튼
	    Tv_UsageCheckWeb = (TextView) inflatedView.findViewById(R.id.textView_Usage_CheckWeb);
	    Tv_UsageCheckWeb.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View v) {
				//TODO 프로그램 을 검색해서 KT고객센터(없으면 숨김)/미니고객센터(없으면 설치 표시)/웹 을 보여주기
				EggCore.broadcastThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						((MainActivity) getActivity()).popupChooseKTappDialog(v.getContext());
					}});
			}
		
		});

	    
	    if(savedInstanceState != null)
		{
	    	if(isDebug){
	        	Log.v(TAG_LOG,"savedInstanceState is available");
	        }
	    	try{
	    		//TODO 가져오기 시작, 미리 저장된 상태정보를 꺼내 화면을 다시 구성해요.
		    	
				//가져오기 끝
			}catch(NullPointerException npe){
	        	if(isDebug)
	        	{
	        		Log.v(TAG_LOG,"NullPointerException get Extras on savedInstanceState");
	        		npe.printStackTrace();
	        	}
	        } 
		}
	    else
	    {
	    	if(isDebug){
	        	Log.v(TAG_LOG,"There is NO savedInstanceState");
	        }
	    	
	    	String[] previousUsageStrings = EggCore.loadLastObtainedUsage();
	    	updateUi_WibroUsageKT(previousUsageStrings[0],previousUsageStrings[1], previousUsageStrings[2], previousUsageStrings[3], previousUsageStrings[4], previousUsageStrings[5], previousUsageStrings[6],previousUsageStrings[7]);
	    }
			    
	    //다 만들고 나서 view 를 달아달라고 요청.
	    final RelativeLayout relativeLayout_view = (RelativeLayout) viewToBeAttached.findViewById(R.id.RelativeLayout_usageFragment);
//        final ViewGroup viewParent = ((ViewGroup)relativeLayout_view.getParent());
        
	    Runnable Run_attachMainUi = (new Runnable(){
			@Override
			public void run() {
				
				ViewGroup viewParent = ((ViewGroup)relativeLayout_view.getParent());
				if(viewParent != null){
				//기존의 불러오는중 화면을 빼고 지금껏 만든 화면을 놓아요.
					viewParent.addView(inflatedView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					viewParent.removeView(relativeLayout_view);
					viewParent = null;
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
				if(usageFragment_OnUsageKtRefreshEggInfoListener == null)
			    {
					usageFragment_OnUsageKtRefreshEggInfoListener = new OnUsageKtRefreshEggInfoListener() {

						@Override
						public void onRefreshFailedWibroUsageKT() {
							// TODO Auto-generated method stub
							if(isUiUpdateListenerAttached){
								EggCore.mainThreadHandler.post(new Runnable(){
									@Override
									public void run() {
										// 새로고침 실패.
										PtRSv_Usage.onRefreshComplete();
										Vw_DummyBottom_PullDownToRefresh.setVisibility(View.GONE);
									}});
							}
						}

						@Override
						public void onRefreshedWibroUsageKT(long lastObtained,
								String wibroPlan,
								String providedPackets, String usedPackets,
								String basicFee, String usedFee,
								String additionalCharges) {
							updateUi_WibroUsageKT(lastObtained,
									wibroPlan,
									providedPackets, usedPackets,
									basicFee, usedFee,
									additionalCharges);
							//Vw_DummyBottom_PullDownToRefresh.setVisibility(View.GONE);
							//UI스레드가 아닌 곳에서 UI를 건드렸으니 Crash 원인이었어~!!!! 9/9/2013
						}
			        };
			    }
				attachUiUpdateListeners();
			}});
	}

	private void toggleVisibility(View requestedView){
		if(requestedView.getVisibility() == View.VISIBLE){
			requestedView.setVisibility(View.GONE);
		}
		else {
			requestedView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(isDebug){
        	Log.v(TAG_LOG,"onSaveInstanceState(Bundle outState)");
        }
        try{
	        //내보내기 시작, 화면에 있던 것을 상태정보에 집어넣어요.
	
	        
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
		//updateUI();
		
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

	private final boolean attachUiUpdateListeners()
	{
		if(!isUiUpdateListenerAttached)
		{
			if(usageFragment_OnUsageKtRefreshEggInfoListener != null)
			{
				EggCore.setOnUsageKtRefreshEggInfoListener(usageFragment_OnUsageKtRefreshEggInfoListener);
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
			if(usageFragment_OnUsageKtRefreshEggInfoListener != null)
			{
				isUiUpdateListenerAttached = false;
				EggCore.removeOnUsageKtRefreshEggInfoListener(usageFragment_OnUsageKtRefreshEggInfoListener);
				if(isDebug)
				{
					Log.i(TAG_LOG,"UiUpdate Listener is removed.");
				}
				return true;
			}
			else
			{
				isUiUpdateListenerAttached = false;
				if(isDebug)
				{
					Log.w(TAG_LOG,"UiUpdate Listener is NULL");
				}
			}
		}
		else
		{
			if(isDebug)
			{
				Log.v(TAG_LOG,"UiUpdate Listener is already removed. (as of FLAG)");
			}
		}
		
		return false;
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
					usageFragment_OnUsageKtRefreshEggInfoListener = null;
		
					Tv_UsageCheckWeb = null;
					
					Tv_UsagePlan = null;
					Tv_UsagePacketsSimple = null;
					Tv_UsagePacketsDetailed = null;
					Tv_UsageBillsSimple = null;
					Tv_UsageBillsDetailed = null;
					
					Tv_Usage_header = null;
					
					PtRSv_Usage = null;
					Vw_DummyBottom_PullDownToRefresh = null;
					Vw_usage = null;
					
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
	/* 새로고침 하지도 않는 코드를 두어서 무엇하리오 아무것도 안하는거. 9/9/2013
	private void updateUI(){
		EggCore.uiCompositionThreadHandler.post(new Runnable(){

			@Override
			public void run() {
				// TODO 임시방편..
				ui();
			}});
	}
	
	private void ui(){
		EggCore.mainThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				// 서비스 상태변화
				//Tv_SignalStrength.setText("신호세기"+intent.getByteExtra(StrongEggManagerApp.EXTRA_SIGNAL_STRENGTH, StrongEggManagerService.SIGNAL_LOST));
				//tgBtn_MonitorService.setChecked(EggCore.isServiceRunning());
			}
		});
	}
	*/
	private void updateUi_WibroUsageKT(final long lastObtained,
			final String wibroPlan,
			final String providedPackets, final String usedPackets,
			final String basicFee, final String usedFee,
			final String additionalCharges){
		updateUi_WibroUsageKT(StrongEggManagerApp.getDateTime_inCurrentTimeZone(lastObtained),
				StrongEggManagerApp.getDate_inCurrentTimeZone(lastObtained),
				wibroPlan,
				providedPackets, usedPackets,
				basicFee, usedFee,
				additionalCharges);
	}/*
	private void updateUi_WibroUsageKT(final String lastObtained,
			final String lastObtainedDate,
			final String wibroPlan, 
			final String providedPackets, final String usedPackets,
			final String basicFee, final String usedFee,
			final String additionalCharges) {
		EggCore.uiCompositionThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				refreshUi_WibroUsageKT(getString(R.string.item_header_plan)+wibroPlan,
						getString(R.string.item_header_packets_remaining)+StrongEggManagerApp.putCommaNumeric(Integer.parseInt(providedPackets,10)-Integer.parseInt(usedPackets,10))+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB,
						getString(R.string.item_header_packets_used)+StrongEggManagerApp.putCommaNumeric(usedPackets)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB+StrongEggManagerApp.COMMON_STR_CRLF+
						getString(R.string.item_header_packets_provided)+StrongEggManagerApp.putCommaNumeric(providedPackets)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB,
						getString(R.string.item_header_bills_today)+StrongEggManagerApp.putCommaNumeric(usedFee)+getString(R.string.unit_krw),
						getString(R.string.item_header_bills_basic)+StrongEggManagerApp.putCommaNumeric(basicFee)+getString(R.string.unit_krw)+StrongEggManagerApp.COMMON_STR_CRLF+
						getString(R.string.item_header_bills_additionalcharge)+StrongEggManagerApp.putCommaNumeric(additionalCharges)+getString(R.string.unit_krw),
						(lastObtained != null ? getString(R.string.item_header_recentobatined)+lastObtained:getString(R.string.not_yet_refreshed)),
						(lastObtainedDate != null ? getString(R.string.section_header_usage_with_date,lastObtainedDate):getString(R.string.section_header_usage)));
			}});
	}*/
	
	
	private void updateUi_WibroUsageKT(final String lastObtained,
			final String lastObtainedDate,
			final String wibroPlan, 
			final String providedPackets, final String usedPackets,
			final String basicFee, final String usedFee,
			final String additionalCharges) {
		EggCore.uiCompositionThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				refreshUi_WibroUsageKT(getString(R.string.item_header_plan)+wibroPlan,
						EggCore.showString_WibroRemainingPacket(providedPackets, usedPackets),
						getString(R.string.item_header_packets_used)+EggCore.showString_WibroUsedPacket(usedPackets)+StrongEggManagerApp.COMMON_STR_CRLF+
						getString(R.string.item_header_packets_provided)+StrongEggManagerApp.putCommaNumeric(providedPackets)+StrongEggManagerApp.COMMON_UNIT_PACKETS_MB,
						getString(R.string.item_header_bills_today)+StrongEggManagerApp.putCommaNumeric(usedFee)+getString(R.string.unit_krw),
						getString(R.string.item_header_bills_basic)+StrongEggManagerApp.putCommaNumeric(basicFee)+getString(R.string.unit_krw)+StrongEggManagerApp.COMMON_STR_CRLF+
						getString(R.string.item_header_bills_additionalcharge)+StrongEggManagerApp.putCommaNumeric(additionalCharges)+getString(R.string.unit_krw),
						(lastObtained != null ? getString(R.string.item_header_recentobatined)+lastObtained:getString(R.string.not_yet_refreshed)),
						(lastObtainedDate != null ? getString(R.string.section_header_usage_with_date,lastObtainedDate):getString(R.string.section_header_usage)));
			}});
	}
	

	private void refreshUi_WibroUsageKT(final String strUsagePlan,
			final String strUsagePacketsSimple, final String strUsagePacketsDetailed,
			final String strUsageBillsSimple, final String strUsageBillsDetailed,
			final String strRefreshedTime,final String strRefreshedDateOnly){
		EggCore.mainThreadHandler.post(new Runnable(){
					
			@Override
			public void run() {
				Tv_Usage_header.setText(strRefreshedDateOnly);
				Tv_UsagePlan.setText(strUsagePlan);
				Tv_UsagePacketsSimple.setText(strUsagePacketsSimple);
				Tv_UsagePacketsDetailed.setText(strUsagePacketsDetailed);
				Tv_UsageBillsSimple.setText(strUsageBillsSimple);
				Tv_UsageBillsDetailed.setText(strUsageBillsDetailed);
				PtRSv_Usage.setLastUpdatedLabel(strRefreshedTime);
				PtRSv_Usage.onRefreshComplete();
				Vw_DummyBottom_PullDownToRefresh.setVisibility(View.GONE);
			}});
	}

}
