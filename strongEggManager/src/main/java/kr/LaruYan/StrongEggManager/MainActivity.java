package kr.LaruYan.StrongEggManager;

import kr.LaruYan.StrongEggManager.StrongEggManagerApp.OnAppStateChangedListener;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

//Put PagerTabStrip run for pre-honeycomb. 
//http://miquniqu.blogspot.com/2012/07/pagertabstrip.html
public class MainActivity extends FragmentActivity implements OnKeyListener{
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_MainActivity";
	
	//private static final String PACKAGE_NAME_KT_CARE_MINI = "com.show.mini";
	private static final String PACKAGE_NAME_KT_CARE_FULL = "com.ktshow.cs";
	private static final String SITE_ADDRESS_KT_CARE = "https://m.mycs.olleh.com:451/kt12/molleh/comm/go_page.jsp?page_code=M035";
	
	private StrongEggManagerApp EggCore;
	private int displayedEggConnStat = StrongEggManagerApp.EGG_CONNSTAT_NOW_RECOGNIZING;
	
	private OnAppStateChangedListener mainActivity_OnAppStateChangedListener;
	private boolean isUiUpdateListenerAttached = false;	
	
	
	//using developer
//	private Handler fooHandler;
//	private Handler barHandler;
//	private String testString1;
//	private String testString2;
//	private String modelString1;
//	private String modelString2;
	
	/////////
	private View Vw_main;
	
	//액션바
	private ImageButton ImgBtn_Home;
	
	private ProgressBar PrgBar_Refresh;
	private ImageButton ImgBtn_Refresh;
	private ImageButton ImgBtn_Power;
	private ImageButton ImgBtn_More;
	
	private ImageButton ImgBtn_Login;
	private ImageButton ImgBtn_WiFi;
	private ImageButton ImgBtn_Settings;
	private ImageButton ImgBtn_LatestFirmwarePage;
	
	//메뉴
	private View viewRect_menuUnFocusArea;
	private LinearLayout LinL_powerMenu;
	private LinearLayout LinL_moreMenu;
	private boolean isPowerMenuOn = false;
	private boolean isMoreMenuOn = false;
	private Animation Anim_PowerMenuPopup;
	private Animation Anim_PowerMenuClose;
	private Animation Anim_MoreMenuPopup;
	private Animation Anim_MoreMenuClose;
	
	//메뉴 소속 항목
	//들의 LinearLayout을 CompoundDrawable로 바꿔서 TextView로 합쳤다.
	private TextView Tv_powerMenu_Sleep;
	private TextView Tv_powerMenu_Reboot;
	private TextView Tv_powerMenu_PowerOff;
	
	//private LinearLayout LinL_moreMenu_AutoRefresh;
	private TextView Tv_moreMenu_AutoRefresh;
	//private TextView Tv_moreMenu_LogIn;//왠지 긁어 부스럼 같은 기능이라 완료 화면에서 보이지 않게 할래요.
	private TextView Tv_moreMenu_OllehCare;
	private TextView Tv_moreMenu_WebCM;
	private TextView Tv_moreMenu_WiFi;
	private TextView Tv_moreMenu_Settings;

	//사용할 수 없음을 알리기. 
	private ScrollView Sv_NotifyUnavailable;
	private TextView Tv_NNA_Title;
	private TextView Tv_NNA_ssid;
	private ImageView ImgV_NNA_Pic;
	private TextView Tv_NNA_Description;
	
	////
	private Fragment summaryFragment;
	private Fragment usageFragment;
	//private Fragment settingsFragment;
	
	//Inflate 과정을 특정 조건에만 하게 하려고 여기에 viewpager를 넣어두었어요.
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    
    /*
     * MainActivity
     * PagerTabStrip을 이용하여 UI를 구성하고, StrongEggManagerApp에서 실제 로직을 처리합니다.
     */

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //super.setContentView(R.layout.activity_main);
        
        if(isDebug)
		{
			Log.v(TAG_LOG,"onCreate()");
		}
       
        //final LayoutInflater inflater = getLayoutInflater();
        Vw_main = getLayoutInflater().inflate(R.layout.activity_initial, null);
        super.setTheme(R.style.AppTheme);
	    super.setContentView(Vw_main);
	    
	    //http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/1.5_r4/android/app/Activity.java#Activity.onCreate%28android.os.Bundle%29
	    //에 따르면 테마 설정.. 의 기능을 먼저 한 것으로 보인다. 그러면 여기다 둬도 괜찮겠지?
	    //별 차이는 없넹 다시 위에 놓을래
	    //super.onCreate(savedInstanceState);
	    
	    EggCore = (StrongEggManagerApp)getApplicationContext();
	    EggCore.uiCompositionThreadHandler.post(new Runnable(){
	    	@Override
			public void run() {
				initializeUi(getLayoutInflater(), Vw_main, savedInstanceState);
			}
	    });
	    
	    if(isDebug)
		{
			Log.v(TAG_LOG,"onCreate() finished.");
		}
     }
    
    
    private void initializeUi(LayoutInflater inflater, final View viewToBeAttached, final Bundle savedInstanceState)
    {
    	final View inflatedView = inflater.inflate(R.layout.fragment_main, null);//(LinearLayout)inflater.inflate(R.layout.activity_main, container, false);
    	
    	EggCore.initialCheckAutoRefreshIsRunning();

    	//이용 대기, 사용할 수 없음을 알림 대개 여기선 선언만..(..)
    	Sv_NotifyUnavailable = (ScrollView)inflatedView.findViewById(R.id.scrollView_NotifyNotAvailable);
    	Tv_NNA_Title = (TextView) inflatedView.findViewById(R.id.textView_NNA_Title);
    	Tv_NNA_ssid = (TextView) inflatedView.findViewById(R.id.textView_NNA_NameOfNetwork);
    	ImgV_NNA_Pic = (ImageView) inflatedView.findViewById(R.id.imageView_NNA_Picture);
    	Tv_NNA_Description = (TextView) inflatedView.findViewById(R.id.textView_NNA_Description);
    	
    	//펼침메뉴
    	LinL_powerMenu = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_Menu_Power);
    	LinL_moreMenu = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_Menu_More);
    	
    	//클릭하면 메뉴가 사라지도록 할 빈 영역
    	viewRect_menuUnFocusArea = (View) inflatedView.findViewById(R.id.viewRect_menuUnFocusArea);
    	viewRect_menuUnFocusArea.setOnClickListener(new OnClickListener (){

			@Override
			public void onClick(View v) {
				//펼침 메뉴 접어넣기
				if(isPowerMenuOn)
				{
					try{
						LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
					}catch(NullPointerException npe){
						if(isDebug)
						{
							Log.v(TAG_LOG,"NullPointerException closing Power Menu");
							npe.printStackTrace();
						}
					}
				}
				if(isMoreMenuOn)
				{
					try{
						LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
					}catch(NullPointerException npe){
						if(isDebug)
						{
							Log.v(TAG_LOG,"NullPointerException closing Overflow Menu");
							npe.printStackTrace();
						}
					}
				}
			}
    		
    	});
    	Anim_PowerMenuPopup = AnimationUtils.loadAnimation(this, R.anim.grow_fade_in);
    	Anim_PowerMenuPopup.setAnimationListener(new AnimationListener (){

			@Override
			public void onAnimationEnd(Animation anim) {
				// 전원 메뉴
				if(LinL_powerMenu != null)
				{
					LinL_powerMenu.clearAnimation();
					//isPowerMenuOn = true;
				}
			}

			@Override
			public void onAnimationRepeat(Animation anim) {

				
			}

			@Override
			public void onAnimationStart(Animation anim) {
				//클릭하면 메뉴가 사라지도록 할 빈 영역
				if(viewRect_menuUnFocusArea != null)
				{
					viewRect_menuUnFocusArea.setVisibility(View.VISIBLE);
				}
				
				// 전원 메뉴
				if(!isPowerMenuOn)
				{
					//LinL_powerMenu.setEnabled(true);
					if(LinL_powerMenu != null)
					{
						
						//if(LinL_powerMenu.getVisibility() != View.VISIBLE)
						//{
							LinL_powerMenu.setVisibility(View.VISIBLE);
						//}
					}
					isPowerMenuOn = true;
					
					if(ImgBtn_Power != null){
						ImgBtn_Power.setSelected(true);
					}
				}
			}
    		
    	});
    	Anim_PowerMenuClose = AnimationUtils.loadAnimation(this, R.anim.grow_reverse_fade_out);
    	Anim_PowerMenuClose.setAnimationListener(new AnimationListener (){

			@Override
			public void onAnimationEnd(Animation anim) {
				// 전원 메뉴
				if(isPowerMenuOn)
				{
					if(LinL_powerMenu != null)
					{
						//if(LinL_powerMenu.getVisibility() != View.GONE)
						//{
							LinL_powerMenu.setVisibility(View.GONE);
						//}
							LinL_powerMenu.clearAnimation();
					}
					isPowerMenuOn = false;
					
				}
				
			}

			@Override
			public void onAnimationRepeat(Animation anim) {
				
			}

			@Override
			public void onAnimationStart(Animation anim) {
				//클릭하면 메뉴가 사라지도록 할 빈 영역
				if(viewRect_menuUnFocusArea != null){
					viewRect_menuUnFocusArea.setVisibility(View.INVISIBLE);
				}
				if(ImgBtn_Power != null){
					ImgBtn_Power.setSelected(false);
				}
				// 전원 메뉴
				//if(LinL_powerMenu != null)
				//{
				//	LinL_powerMenu.setEnabled(false);
				//}
			}
    		
    	});
    	Anim_MoreMenuPopup = AnimationUtils.loadAnimation(this, R.anim.grow_fade_in);
    	Anim_MoreMenuPopup.setAnimationListener(new AnimationListener (){

			@Override
			public void onAnimationEnd(Animation anim) {
				// 더 보기 메뉴
				if(LinL_moreMenu != null){
					LinL_moreMenu.clearAnimation();
				}
			}

			@Override
			public void onAnimationRepeat(Animation anim) {

				
			}

			@Override
			public void onAnimationStart(Animation anim) {
				//클릭하면 메뉴가 사라지도록 할 빈 영역
				if(viewRect_menuUnFocusArea != null){
					viewRect_menuUnFocusArea.setVisibility(View.VISIBLE);
				}
				
				//더 보기 메뉴
				if(!isMoreMenuOn)
				{
					if(LinL_moreMenu != null){
						//LinL_moreMenu.setEnabled(true);
						//if(LinL_powerMenu.getVisibility() != View.VISIBLE)
						//{
						LinL_moreMenu.setVisibility(View.VISIBLE);
						//}
					}
					isMoreMenuOn = true;
					
					if(ImgBtn_More != null){
						ImgBtn_More.setSelected(true);
					}
				}

			}
    		
    	});
    	Anim_MoreMenuClose = AnimationUtils.loadAnimation(this, R.anim.grow_reverse_fade_out);
    	Anim_MoreMenuClose.setAnimationListener(new AnimationListener (){

			@Override
			public void onAnimationEnd(Animation anim) {
				//더 보기 메뉴
				if(isMoreMenuOn)
				{
					if(LinL_moreMenu != null)
					{
						LinL_moreMenu.setVisibility(View.GONE);
						LinL_moreMenu.clearAnimation();
					}
					isMoreMenuOn = false;
					
				}
			}

			@Override
			public void onAnimationRepeat(Animation anim) {
				
			}

			@Override
			public void onAnimationStart(Animation anim) {
				//클릭하면 메뉴가 사라지도록 할 빈 영역
				if(viewRect_menuUnFocusArea != null){
					viewRect_menuUnFocusArea.setVisibility(View.INVISIBLE);
				}
				if(ImgBtn_More != null){
					ImgBtn_More.setSelected(false);
				}
				//더 보기 메뉴
				//if(LinL_moreMenu != null)
				//{
				//	LinL_moreMenu.setEnabled(false);
				//}
			}
    		
    	});
   	
    	
    	//펼침 메뉴 접어넣기
   			
		try{
			//애니메이션 초기화는 가능할까. 일단 효과는 있어보여요.
	    	LinL_powerMenu.startAnimation(Anim_PowerMenuPopup);
	    	//LinL_powerMenu.clearAnimation();
	    	LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
	    	//LinL_powerMenu.clearAnimation();
	    	LinL_moreMenu.startAnimation(Anim_MoreMenuPopup);
	    	LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
		}catch(NullPointerException npe){
			if(isDebug)
			{
				Log.v(TAG_LOG,"NullPointerException initializing menu anim");
				npe.printStackTrace();
			}
		}
   	

		
    	//액션바 따라하기
    	//액션바 오른쪽 버튼 배치를 위한 틀.
    	//홈 제목      ○ ② ①
		if(isDebug){
			final TextView Tv_ActionBarLabel = (TextView)inflatedView.findViewById(R.id.textView_ActionBarLabel);
			//HandlerThread fooThread = new HandlerThread("foo",Process.THREAD_PRIORITY_URGENT_AUDIO);
			//fooThread.start();
			//HandlerThread barThread = new HandlerThread("bar",Process.THREAD_PRIORITY_URGENT_AUDIO);
			//barThread.start();
			//fooHandler = new Handler(fooThread.getLooper());
			//barHandler = new Handler(barThread.getLooper());
			Tv_ActionBarLabel.setOnClickListener(new OnClickListener (){

				@Override
				public void onClick(View v) {

					popupPreferenceDialog();
					// NOCHECK이 빠르다.. 단순히 값을 넣는 작업 뿐이라면
					/*
					Log.v(TAG_LOG,"valuecheckbench_started");
					
					testString1 = "";
					testString2 = "";
					
					modelString1 = "";
					modelString2 = "";
					final int trials = Short.MAX_VALUE*10;

					for(byte jk = 0; jk < 10; jk++){
					fooHandler.post(new Runnable(){

						@Override
						public void run() {
							long startTime = System.currentTimeMillis();
							for (int i = 0; i<trials;i++){
								modelString1 = (i%12)+" times many";
								testString1 = modelString1;
								modelString1 = null;
							}
							Log.v(TAG_LOG,"valuecheckbench_nocheck: "+(System.currentTimeMillis()-startTime));
						}});
					barHandler.post(new Runnable(){

						@Override
						public void run() {
							long startTime = System.currentTimeMillis();
							for (int i = 0; i<trials;i++){
								modelString2 = (i%12)+" times many";
								if( ! testString2.equals(modelString2)){
									testString2 = modelString2;
								}
								modelString2 = null;
							}
							Log.v(TAG_LOG,"valuecheckbench_check: "+(System.currentTimeMillis()-startTime));
						}});
					}
					08-29 11:54:36.608: V/SEM_MainActivity(1646): valuecheckbench_started
					08-29 11:54:36.868: V/SEM_MainActivity(1646): valuecheckbench_started
					08-29 11:54:37.038: V/SEM_MainActivity(1646): valuecheckbench_started
					08-29 11:54:43.658: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 7014
					08-29 11:54:43.818: V/SEM_MainActivity(1646): valuecheckbench_check: 7192
					08-29 11:54:50.098: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6434
					08-29 11:54:50.318: V/SEM_MainActivity(1646): valuecheckbench_check: 6495
					08-29 11:54:56.769: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6669
					08-29 11:54:57.229: V/SEM_MainActivity(1646): valuecheckbench_check: 6912
					08-29 11:55:03.289: V/SEM_MainActivity(1646): valuecheckbench_check: 6057
					08-29 11:55:03.489: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6716
					08-29 11:55:09.348: V/SEM_MainActivity(1646): valuecheckbench_check: 6062
					08-29 11:55:10.378: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6886
					08-29 11:55:15.689: V/SEM_MainActivity(1646): valuecheckbench_check: 6316
					08-29 11:55:16.569: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6191
					08-29 11:55:21.979: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 5405
					08-29 11:55:22.350: V/SEM_MainActivity(1646): valuecheckbench_check: 6636
					08-29 11:55:28.399: V/SEM_MainActivity(1646): valuecheckbench_check: 6045
					08-29 11:55:28.519: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6534
					08-29 11:55:35.118: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6564
					08-29 11:55:35.320: V/SEM_MainActivity(1646): valuecheckbench_check: 6919
					08-29 11:55:41.249: V/SEM_MainActivity(1646): valuecheckbench_check: 5926
					08-29 11:55:42.079: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6956
					08-29 11:55:47.200: V/SEM_MainActivity(1646): valuecheckbench_check: 5951
					08-29 11:55:48.649: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6557
					08-29 11:55:53.499: V/SEM_MainActivity(1646): valuecheckbench_check: 6294
					08-29 11:55:55.389: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6736
					08-29 11:55:59.150: V/SEM_MainActivity(1646): valuecheckbench_check: 5646
					08-29 11:56:02.291: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6873
					08-29 11:56:05.659: V/SEM_MainActivity(1646): valuecheckbench_check: 6512
					08-29 11:56:08.469: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6152
					08-29 11:56:11.719: V/SEM_MainActivity(1646): valuecheckbench_check: 6055
					08-29 11:56:15.060: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6592
					08-29 11:56:18.490: V/SEM_MainActivity(1646): valuecheckbench_check: 6761
					08-29 11:56:21.570: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6505
					08-29 11:56:23.980: V/SEM_MainActivity(1646): valuecheckbench_check: 5459
					08-29 11:56:29.080: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 7497
					08-29 11:56:30.100: V/SEM_MainActivity(1646): valuecheckbench_check: 6120
					08-29 11:56:35.889: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6801
					08-29 11:56:36.799: V/SEM_MainActivity(1646): valuecheckbench_check: 6686
					08-29 11:56:42.890: V/SEM_MainActivity(1646): valuecheckbench_check: 6077
					08-29 11:56:42.910: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 7012
					08-29 11:56:49.170: V/SEM_MainActivity(1646): valuecheckbench_check: 6270
					08-29 11:56:49.430: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6517
					08-29 11:56:55.291: V/SEM_MainActivity(1646): valuecheckbench_check: 6083
					08-29 11:56:55.860: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6432
					08-29 11:57:01.691: V/SEM_MainActivity(1646): valuecheckbench_check: 6400
					08-29 11:57:02.181: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6300
					08-29 11:57:07.780: V/SEM_MainActivity(1646): valuecheckbench_check: 6080
					08-29 11:57:08.740: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6556
					08-29 11:57:13.670: V/SEM_MainActivity(1646): valuecheckbench_check: 5880
					08-29 11:57:15.220: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6477
					08-29 11:57:19.470: V/SEM_MainActivity(1646): valuecheckbench_check: 5791
					08-29 11:57:22.061: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6820
					08-29 11:57:25.441: V/SEM_MainActivity(1646): valuecheckbench_check: 5971
					08-29 11:57:28.491: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6433
					08-29 11:57:31.301: V/SEM_MainActivity(1646): valuecheckbench_check: 5860
					08-29 11:57:34.981: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6481
					08-29 11:57:37.410: V/SEM_MainActivity(1646): valuecheckbench_check: 6099
					08-29 11:57:41.261: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 6280
					08-29 11:57:43.301: V/SEM_MainActivity(1646): valuecheckbench_check: 5895
					08-29 11:57:44.992: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 3731
					08-29 11:57:47.492: V/SEM_MainActivity(1646): valuecheckbench_nocheck: 2501

					*/
					
					//android:textStyle="bold"가 안되는 글꼴을 강제로 굵게 표시. 가능할까?.. 글꼴 자체의 문제라 되지 않는다.
			    	//http://stackoverflow.com/questions/11357632/how-to-make-bold-textview-android-japanese-character
			    	//	<string name="some_japanese">
			    	//	<![CDATA[<b>日本</b>
			    	//	]]>
			    	//	</string>
			    	//textView.setText(Html.fromHtml(getResources().getString(R.string.some_japanese)));
					//Tv_ActionBarLabel.setText(Html.fromHtml(getResources().getString(R.string.some_japanese)));
				}});
			
		}
		
		
    	//첫 상태가 인식중 상태인데, 그건 1번 자리에 와이파이 버튼이 있어야하므로 아직 가로가 살아있는 2번을 쓰기로 했다.
		
		
    	//홈 버튼
        ImgBtn_Home = (ImageButton)inflatedView.findViewById(R.id.imageButton_home);
        //Btn_Home.getBackground().setAlpha(0);
        ImgBtn_Home.setVisibility(View.VISIBLE);
        ImgBtn_Home.setVisibility(View.INVISIBLE);
        ImgBtn_Home.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
//				Toast.makeText(getApplicationContext(), "홈!", Toast.LENGTH_SHORT).show();
				switchPageFromState((byte)0);//,true);//요약 화면. 움직이는 스크롤
			}
        	
        });
        //새로고침 버튼
        PrgBar_Refresh = (ProgressBar) inflatedView.findViewById(R.id.progressBar_Refresh);
        ImgBtn_Refresh = (ImageButton)inflatedView.findViewById(R.id.imageButton_Refresh);
        ImgBtn_Refresh.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				ImgBtn_Refresh.setVisibility(View.GONE);
				PrgBar_Refresh.setVisibility(View.VISIBLE);
				EggCore.taskThreadHandler.post(new Runnable(){

					@Override
					public void run() {
						//TODO 페이지 번호에 따라  다른걸 refresh.
						switch(mViewPager.getCurrentItem())
						{
						case 1: // 사용량 페이지
							EggCore.checkWibroUsageFromKT();
							break;
						default: // 요약 페이지
							EggCore.LocalRefresh();
							break;
						}
						//broadcastUpdateUiFragment(StrongEggManagerApp.BROADCAST_UI_SUMMARY_UPDATE);
						EggCore.mainThreadHandler.post(new Runnable(){

							@Override
							public void run() {
								ImgBtn_Refresh.setVisibility(View.VISIBLE);
								PrgBar_Refresh.setVisibility(View.GONE);
							}
							
						});
					}
					
				});
				
			}
			
		});
        ImgBtn_Refresh.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_refresh);
				//ImgBtn_Refresh.setSelected(false);
				return false;
			}
        	
        }); 
        
        //로그인 버튼은 로그인 실패 때만 나오니까.
        ImgBtn_Login = (ImageButton) inflatedView.findViewById(R.id.imageButton_Login);
        ImgBtn_Login.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// 로그인 작업이 중요한 순간에 작은 곳에 배치해선 안되겠징..
				//이지만 디자인을 해칠 수 있으니 액션바에도 놓아둘래요 ㅇ_ㅇ
				popupRetryLogin();
			}
			
		});
        ImgBtn_Login.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_login);
				//ImgBtn_Refresh.setSelected(false);
				return false;
			}
        });
       
        
    	//와이파이 설정 버튼. 
        ImgBtn_WiFi = (ImageButton) inflatedView.findViewById(R.id.imageButton_Wifi);
    	ImgBtn_WiFi.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// 로그인 작업이 중요한 순간에 작은 곳에 배치해선 안되겠징
				//EggCore.showRetryLoginDialog(v.getContext());
				ImgBtn_WiFi.setEnabled(false);
				
				//popupPreferenceDialog();
				//popupRetryLogin();
				//popupChooseKTappDialog(v.getContext());
				
				popupWifiSetting();
				EggCore.mainThreadHandler.postDelayed(new Runnable(){
					@Override
					public void run(){
						ImgBtn_WiFi.setEnabled(true);
					}
				},StrongEggManagerApp.DEFAULT_UI_DELAY);
			}
			
		});

    	ImgBtn_WiFi.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_wifi);
				//ImgBtn_Refresh.setSelected(false);
				return false;
			}
        	
        });

        //설정 버튼은.. 로그인 실패때만..
    	ImgBtn_Settings = (ImageButton) inflatedView.findViewById(R.id.imageButton_Settings);
    	ImgBtn_Settings.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				//환경설정 변경 창 표시 
				popupPreferenceDialog();
			}
			
		});
    	ImgBtn_Settings.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_settings);
				//ImgBtn_Refresh.setSelected(false);
				return false;
			}
        	
        });;

    	//장치 알 수 없을 땐 와이파이 바뀔때까지 비워둬야죠. ㅠㅠ
        //다른 와이파이 고를 수 있게 해둘거야 낙담 하지말어.
        
        
        //펌웨어 페이지 버튼은.. 펌웨어 인식 실패때만..
        ImgBtn_LatestFirmwarePage = (ImageButton) inflatedView.findViewById(R.id.imageButton_LatestFirmwarePage);
        ImgBtn_LatestFirmwarePage.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				//최신 펌웨어 페이지 표시 
				popupFirmwarePage();
			}
			
		});
        ImgBtn_LatestFirmwarePage.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_update);
				//ImgBtn_Refresh.setSelected(false);
				return false;
			}
        	
        });;
        
        
        //전원 메뉴
        ImgBtn_Power = (ImageButton)inflatedView.findViewById(R.id.imageButton_Power);
        ImgBtn_Power.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_powerMenu.startAnimation(Anim_PowerMenuPopup);
				
			}});
        ImgBtn_Power.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_power_option);
				//ImgBtn_Power.setSelected(false);
				return false;
			}
        	
        });
        //LinL_powerMenu_Sleep = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_Sleep);
        Tv_powerMenu_Sleep = (TextView) inflatedView.findViewById(R.id.textView_menu_Sleep);
        Tv_powerMenu_Sleep.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
				
				EggCore.taskThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						EggCore.Hibernate();
					}});
			}});
    	//LinL_powerMenu_Reboot = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_Reboot);
        Tv_powerMenu_Reboot = (TextView) inflatedView.findViewById(R.id.textView_menu_Reboot);
        Tv_powerMenu_Reboot.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
				
				EggCore.taskThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						EggCore.Reboot();
					}});
			}});
    	//LinL_powerMenu_PowerOff = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_PowerOff);
    	Tv_powerMenu_PowerOff = (TextView) inflatedView.findViewById(R.id.textView_menu_PowerOff);
    	Tv_powerMenu_PowerOff.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
				
				EggCore.taskThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						EggCore.PowerOff();
					}});
			}});
    	
    	//더 보기 메뉴
        ImgBtn_More = (ImageButton)inflatedView.findViewById(R.id.imageButton_More);
        ImgBtn_More.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {

				LinL_moreMenu.startAnimation(Anim_MoreMenuPopup);
			}});
        ImgBtn_More.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View v) {
				EggCore.showShortToast(R.string.action_overflow);
				//ImgBtn_More.setSelected(false);
				return false;
			}
        	
        });
        
    	//LinL_moreMenu_AutoRefresh = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_ServiceToggle);
        Tv_moreMenu_AutoRefresh = (TextView) inflatedView.findViewById(R.id.textView_menu_ServiceToggle);
        Tv_moreMenu_AutoRefresh.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				
				//자동 새로고침 메뉴항목
				if(!EggCore.isAutoRefreshRunning())
    			{
    				//꺼져있을 때 서비스 실행
    				EggCore.LaunchServiceSti();
    				//Tv_moreMenu_AutoRefresh.setText(R.string.app_service_turnoff);
    			}
    			else
    			{
    				//꺼져있을 때 서비스 중단
    				EggCore.TerminateServiceSti();
    				//Tv_moreMenu_AutoRefresh.setText(R.string.app_service_turnon);
    			}
				
			}});
    	Tv_moreMenu_AutoRefresh = (TextView) inflatedView.findViewById(R.id.textView_menu_ServiceToggle);
    	//자동 새로고침 메뉴항목
    	//자동 새로고침 메뉴항목
		
		
    	//아무래도 이 자리엔 로그인 버튼보단 와이파이 선택이 있는게 좋을것 같다.
    	//어차피 401 UNAUTHORIZED 뜨면 로그인 실패 화면으로 넘겨버리ㅡㄴ걸.
    	//LinL_moreMenu_LogIn = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_LogIn);
    	/*Tv_moreMenu_LogIn = (TextView) inflatedView.findViewById(R.id.textView_menu_LogIn); adfkjzlcvj;
    	Tv_moreMenu_LogIn.setOnClickListener(new OnClickListener(){akjlkdjfla
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				//http://stackoverflow.com/questions/1561803/android-progressdialog-show-crashes-with-getapplicationcontext
				//Context를 소속 View에서 가져오게 만들면 해결! 
				//EggCore.showRetryLoginDialog(v.getContext());//getActivityContext());
				popupRetryLogin();
			}});*/
    	
    	//KT고객센터 실행 버튼.
    	Tv_moreMenu_OllehCare = (TextView) inflatedView.findViewById(R.id.textView_menu_KTCare);
    	Tv_moreMenu_OllehCare.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				
				popupChooseKTappDialog(v.getContext());
			}});
    	//웹CM 실행 버튼.
    	Tv_moreMenu_WebCM = (TextView) inflatedView.findViewById(R.id.textView_menu_WebCM);
    	Tv_moreMenu_WebCM.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				
				popupWebCM();
			}});
    	//LinL_moreMenu_WiFi = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_WiFi);
    	Tv_moreMenu_WiFi = (TextView) inflatedView.findViewById(R.id.textView_menu_WiFi);
    	Tv_moreMenu_WiFi.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				
				popupWifiSetting();
			}});
    	
    	//LinL_moreMenu_Settings = (LinearLayout) inflatedView.findViewById(R.id.linearLayout_menu_Settings);
    	Tv_moreMenu_Settings = (TextView) inflatedView.findViewById(R.id.textView_menu_Settings);
    	Tv_moreMenu_Settings.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				
				//환경설정 변경 창 표시 
				popupPreferenceDialog();
			}});
        
    	/*
    	 * 이 자리 대신 View Stub을 두고 거기서 inflate할 때 쓸래요.
    	 * inflateViewPager()
    	 * 
    	 * 가 아니라. 가끔 onResume때 View가 사라지는 문제가 있다. ㅠ 
    	 * 최적화 노력이 과했나..
    	 * 그래서.. ViewPagerAdapter를 추후 다는 방식으로 해결할래.. 그건 activity에서 주기가 관리되니까.
    	 */
//    	initializeViewPager(inflatedView);
    	//mViewPager = (ViewPager) inflatedView.findViewById(R.id.pager);
    	//mViewPager.setVisibility(View.VISIBLE);
    	//mViewPager.setVisibility(View.INVISIBLE);
    	// Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());//,initPages());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) inflatedView.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new OnPageChangeListener(){

        	@Override
        	public void onPageScrollStateChanged(int state) {
        		//
        	}

        	@Override
        	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        		//
        	}

        	@Override
        	public void onPageSelected(int position) {
        		// 페이지가 선택되어 제목 아래에 표시가 나타난 경우.
        		// 위 두 가지랑은 다르게 스크롤 중이거나 시작했거나 끝났거나 신경쓰지 않아요.
        		//switchHomeActionBarButton(position);
        		if(position == 0)//0요약 1사용량 2설정
        		{//첫 페이지인 경우 홈 버튼을 비활성화
        			//ImgBtn_Home.setBackgroundResource(R.color.transparentWhite);
        			//ImgBtn_Home.setClickable(false);
        			ImgBtn_Home.setVisibility(View.INVISIBLE);
        		}else{//첫 페이지가 아닌 경우 홈 버튼을 활성화
        			//ImgBtn_Home.setBackgroundResource(R.drawable.button_home);
        			//ImgBtn_Home.setClickable(true);
        			ImgBtn_Home.setVisibility(View.VISIBLE);
        		}
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
	    	
	    }
        
	    
	    //'뒤로'버튼을 누른다던지 OnDestroy를 거쳐버린 경우 발주한 액티비티를 찾아얗나다.
	    //현재 페이지 위치 정보만 담겨있어요. 2012 7월 29일 현재
        Bundle requesteState = getIntent().getExtras();//.getString("ComingFrom");
        switchPageFromState(requesteState);
        //switchHomeActionBarButton(mViewPager.getCurrentItem());
        
        
        
        
      //다 만들고 나서 view 를 달아달라고 요청. 준비
	    //final View viewToBeAttached = beInflatedOn_NonMain;
        //final ViewGroup container = (ViewGroup)viewToBeAttached.getParent();
        final LinearLayout linearLayout_view = ((LinearLayout) viewToBeAttached.findViewById(R.id.LinearLayout_mainActivity));
        //final ViewGroup viewParent = ((ViewGroup)relativeLayout_view.getParent());

        EggCore.mainThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				try{
					ViewGroup viewParent = ((ViewGroup)linearLayout_view.getParent());
					//기존의 불러오는중 화면을 빼고 지금껏 만든 화면을 놓아요.
					viewParent.addView(inflatedView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					viewParent.removeView(linearLayout_view);
				}catch(NullPointerException npe){
					//무..무엇인가 잘못되었다.
					if(isDebug){
						Log.e(TAG_LOG,"NullPointerException add&removing view to be attached.");
					}
					//다시 시도 확인은 main 화면에선 필요가 없을것 같은데.. parent가 null일것 같지도 않아서 한 번 더 넣는걸로 만족..
					EggCore.mainThreadHandler.postDelayed(this,StrongEggManagerApp.DEFAULT_UI_DELAY);
					/*
					Button Btn_Retry = (Button)relativeLayout_view.findViewById(R.id.button_Retry);
					if(Btn_Retry != null){
						final Runnable recursion = this;//메모리 장악당할수도 있겠다. ㅠㅠ
						Btn_Retry.setOnClickListener(new OnClickListener(){
							@Override
							public void onClick(View view) {
								EggCore.mainThreadHandler.postDelayed(recursion,StrongEggManagerApp.DEFAULT_UI_DELAY);
							}});
						
						Btn_Retry.setVisibility(View.VISIBLE);
					}*/
				}
			}
	    });
       
	    //만약 UI가 만들어지는동안 연결이 되었다면 처리할 필요가 있어요.
        updateUI_OpenClose();
        updateUI_Service(EggCore.isAutoRefreshRunning());

        EggCore.broadcastThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				if(mainActivity_OnAppStateChangedListener == null){
					mainActivity_OnAppStateChangedListener = new OnAppStateChangedListener() {

						@Override
						public void onConnectionChanged(byte connectionStat) {
							updateUI_OpenClose(connectionStat);
						}

						@Override
						public void onServiceStateChanged(boolean isRunning) {
							updateUI_Service(isRunning);
						}
			        };
				}
				attachUiUpdateListeners();
			}});
    }
    
    private void updateUI_Service(final boolean isRunning){
    	if(EggCore != null){
	    	if(EggCore.mainThreadHandler != null){
		    	EggCore.mainThreadHandler.post(new Runnable(){
					@Override
					public void run() {
						Tv_moreMenu_AutoRefresh.setText(isRunning?R.string.app_service_turnon:R.string.app_service_turnoff);
					}});
	    	}
    	}
    }
    
    private void switchPageFromState(Bundle requestedState)
    {
    	if( requestedState != null)
		{
			switchPageFromState(requestedState.getByte(StrongEggManagerApp.EXTRA_SET_PAGE_MAINACTIVITY,(byte)-1));
		}
    }
    public void switchPageFromState(final byte requestedPage)
    {
    	if( requestedPage >= 0 )
		{
    		EggCore.mainThreadHandler.post(new Runnable(){
				@Override
				public void run() {
					//Toast.makeText(this, requestedPage+"page", Toast.LENGTH_SHORT).show();
		        	//-1, 즉 받아오지 못한경우가 아니라면
			       	//몇 번 페이지를 화면에 보여주기.,부드러운 스크롤을 함
			        mViewPager.setCurrentItem(requestedPage,true);
				}});
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
        	outState.putByte(StrongEggManagerApp.EXTRA_SET_PAGE_MAINACTIVITY, (byte)mViewPager.getCurrentItem());
        	//내보내기 끝, 경고: saveInstanceState는 영구적이지 않아요.
        }catch(NullPointerException npe){
        	if(isDebug)
        	{
        		Log.w(TAG_LOG,"NullPointerException During onSaveInstanceState(Bundle outState)");
        		npe.printStackTrace();
        	}
        }
    }
	
	@Override
	public void onNewIntent(Intent intent) {
		if(isDebug)
		{
			Log.v(TAG_LOG,"onNewIntent(Intent intent)");
		}
		super.onNewIntent(intent);
		//알림막대에서 클릭하여 실행된 경우 여기서 잡자.
		Bundle requestedState = intent.getExtras();

		if( requestedState != null)
		{
			switchPageFromState(requestedState);
		}
		if(isDebug)
		{
			Log.v(TAG_LOG,"onNewIntent(Intent intent) Finished");
		}
	}
	
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter{
  	
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        //해당 위치의 Fragment가 없는경우 다시 만들기
        @Override
        public Fragment getItem(int position) {
        	/*
        	Fragment fragment = null;
        	switch(position)
        	{
        		case 1:
        			fragment = UsageFragment.newInstance();//2: 사용량 화면
        			break;
        		case 2:
        			fragment = SettingsFragment.newInstance();//3: 설정 화면
        			break;
        		default:
        			fragment = SummaryFragment.newInstance();//1,기본값: 요약 화면
        			break;
        	}*/
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return 2;//ListFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.tab_summary);
                case 1: return getString(R.string.tab_usage);
                case 2: return getString(R.string.tab_settings);
            }
            return null;
        }
       
    }

	
	public Fragment getFragment(int position)
	{
    	switch(position)
    	{
    		case 1:
    			if(usageFragment == null)
    			{
    				if(isDebug)
    				{
    					Log.v(TAG_LOG,"usageFragment is null, creating new one.");
    				}
    				usageFragment = UsageFragment.newInstance();//2: 사용량 화면
    			}
    			return usageFragment;
    			//break;
    		/*case 2:
    			if(settingsFragment == null)
    			{
    				if(isDebug)
    				{
    					Log.v(TAG_LOG,"settingsFragment is null, creating new one.");
    				}
    				settingsFragment = SettingsFragment.newInstance();//3: 설정 화면
    			}
    			return settingsFragment;
    			//break;*/
    		default:
    			if(summaryFragment == null)
    			{
    				if(isDebug)
    				{
    					Log.v(TAG_LOG,"summaryFragment is null, creating new one.");
    				}
    				summaryFragment = SummaryFragment.newInstance();//1,기본값: 요약 화면
    			}
    			return summaryFragment;
    			//break;
    	}
    	//return fragment;
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
		do_updateUI();

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
	
	private void do_updateUI() {

		EggCore.uiCompositionThreadHandler.post(new Runnable(){
			@Override
			public void run() {
				updateUI_OpenClose();
			}});
//		processUpdateUIbroadcast_service();
	}
	private final boolean attachUiUpdateListeners()
	{
		if(!isUiUpdateListenerAttached)
		{
			if(mainActivity_OnAppStateChangedListener != null)
			{
				EggCore.setOnAppStateChangedListener(mainActivity_OnAppStateChangedListener);

				isUiUpdateListenerAttached = true;
				if(isDebug)
				{
					Log.i(TAG_LOG,"UiUpdateListeners set.");
				}
				return true;

			}
			else
			{
				isUiUpdateListenerAttached = false;
		        if(isDebug)
				{
					Log.w(TAG_LOG,"UiUpdateListeners are NULL.");
				}
			}
			
		}
		else
		{
			if(isDebug)
			{
				Log.v(TAG_LOG,"UiUpdateListeners is already set. (as of FLAG)");
			}
		}
		
		return false;
	}
	
	private final boolean detachUiUpdateListeners()
	{
		if(isUiUpdateListenerAttached)
		{
			if(mainActivity_OnAppStateChangedListener != null)
			{
				isUiUpdateListenerAttached = false;
				EggCore.removeOnAppStateChangedListener(mainActivity_OnAppStateChangedListener);
				
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
		
	
		//UI가 화면에 있을 때만 UPDATE에 대응하도록 하는게 좋다고 생각했는데 이러면 창 전환에 대응하기가 어려워져.
		//OnResume에서 다시 확인해보라고 시키는 수밖에 없겠어.
		EggCore.uiCompositionThreadHandler.post(new Runnable(){

			@Override
			public void run() {
				//TODO
				//onSaveInstanceState()는 onStop() 전에 실행되기 때문에
				// 아래쪽의 null들을 했다간 onSaveInstanceState에선 거둘게 없을것이다.

				
				if(isFinishing())
				{
					if(isDebug)
					{
						Log.v(TAG_LOG,"this Activity is being removed");
					}
					//unregisterUiUpdateBroadCastReceiver(); //그래서 여기다 놓았지.만 그렇게  효과적이진 않네.
					//제대로 등록해제되지도 않구.
					// onDestroy 보다 먼저 불려지고 동작을 보증하는 메소드이므로 onPause+isFinishing()조합으로 하자.
					//settingsFragment = null;
					summaryFragment = null;
					usageFragment = null;

					Sv_NotifyUnavailable = null;
					//사용할 수 없음을 알리기. 
					Tv_NNA_Title = null;
					Tv_NNA_ssid = null;
					ImgV_NNA_Pic = null;
					Tv_NNA_Description = null;
					
					//메뉴 소속 항목
					//LinL_powerMenu_Sleep = null;
					//LinL_powerMenu_Reboot = null;
					//LinL_powerMenu_PowerOff = null;
					Tv_powerMenu_Sleep = null;
					Tv_powerMenu_Reboot = null;
					Tv_powerMenu_PowerOff = null;
					
					//LinL_moreMenu_AutoRefresh = null;
					Tv_moreMenu_AutoRefresh = null;
					//LinL_moreMenu_Settings = null;
					//LinL_moreMenu_LogIn = null;
					//LinL_moreMenu_WiFi = null;
					Tv_moreMenu_OllehCare = null;
					Tv_moreMenu_WebCM = null;
					//Tv_moreMenu_LogIn = null;
					Tv_moreMenu_WiFi = null;
					Tv_moreMenu_Settings = null;
					
					//메뉴
					viewRect_menuUnFocusArea = null;
					LinL_powerMenu = null;
					LinL_moreMenu = null;
					isPowerMenuOn = false;
					isMoreMenuOn = false;
					Anim_PowerMenuPopup = null;
					Anim_PowerMenuClose = null;
					Anim_MoreMenuPopup = null;
					Anim_MoreMenuClose = null;

					//액션바
					ImgBtn_Login = null;
					ImgBtn_WiFi = null;
					ImgBtn_LatestFirmwarePage = null;
					ImgBtn_Settings = null;
					PrgBar_Refresh = null;
					ImgBtn_Refresh =null;
					ImgBtn_Power= null;
					ImgBtn_More = null;
								
					
					ImgBtn_Home = null;
					
					mainActivity_OnAppStateChangedListener = null;
					
					mViewPager = null;
				    mSectionsPagerAdapter = null;
				    
				    ImgBtn_Home = null;
				    Vw_main = null;

					EggCore = null;
				}
			}
			
		});
		
		//TODO실험코드 onPause 때 밀린 UI 업데이트, 토스트 모두 없애서 MainActivity Null오류에 대응. 대단히 fail
		//EggCore.uiCompositionThreadHandler.removeCallbacksAndMessages(null);
		//EggCore.mainThreadHandler.removeCallbacksAndMessages(null);
		//\
		
		super.onPause();
		
		if(isDebug)
		{
			Log.v(TAG_LOG,"onPause() finished.");
		}
	}
	
	//this is called when the screen rotates.
	// (onCreate is no longer called when screen rotates due to manifest, see: android:configChanges)
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
	    super.onConfigurationChanged(newConfig);
	    //setContentView(R.layout.main);

	    //InitializeUI();
	}
/*
	private void updateUi_ServiceState()
	{
		updateUi_ServiceState(EggCore.isAutoRefreshRunning());
	}
	private void updateUi_ServiceState(final boolean isRunning)
	{
		refreshUi_ServiceState(isRunning? R.string.app_service_turnon : R.string.app_service_turnoff);
	}
	private void refreshUi_ServiceState(final int strId){
		EggCore.mainThreadHandler.post(new Runnable(){

			@Override
			public void run() {
				Tv_moreMenu_AutoRefresh.setText(strId);
			}});
	}*/
	private void updateUI_OpenClose()
	{
		updateUI_OpenClose(EggCore.getConnectionStatus());
	}
	private void updateUI_OpenClose(byte EggConnStat)
	{
		if(EggConnStat != displayedEggConnStat)
		{
			if(isDebug)
			{
				Log.v(TAG_LOG,"Theres a need for refreshing its openclose state Current#New State << "+EggConnStat+" # "+displayedEggConnStat);
			}
			displayedEggConnStat = EggConnStat;
			
			//View Size조절을 위해 준비.
			try{
			switch(EggConnStat)
			{
				case StrongEggManagerApp.EGG_CONNSTAT_COMPLETED:
					// 앱 이용가능 상태
					refreshUi_OpenClose(false,false,false,false,
							false,true,true,true,
							false,StrongEggManagerApp.COMMON_STR_EMPTY,StrongEggManagerApp.COMMON_STR_EMPTY,
							getString(R.string.error_nna_disposed),null,R.drawable.ic_plan,StrongEggManagerApp.COMMON_STR_EMPTY);
					
					break;
				case StrongEggManagerApp.EGG_CONNSTAT_LOGIN_FAILED:
					// 로그인 실패, 로그인 정보 재설정
					refreshUi_OpenClose(false,true,true,true,
							false,false,false,false,
							true,getString(R.string.error_nna_loginfailed_title),EggCore.getSSID(),
							getString(R.string.action_login),(new OnClickListener(){
								@Override
								public void onClick(View v) {
									popupRetryLogin();
								}
					    	}),R.drawable.nna_loginfail,getString(R.string.error_nna_loginfailed_desc));
					break;
				case StrongEggManagerApp.EGG_CONNSTAT_UNSUPPORTED:
					// 장비 업데이트 필요
					refreshUi_OpenClose(false,false,true,true,
							true,false,false,false,
							true,getString(R.string.error_nna_unsupported_title),EggCore.getSSID(),
							getString(R.string.action_update),(new OnClickListener(){
								@Override
								public void onClick(View v) {
									popupWebCM();
								}
					    	}),R.drawable.nna_unknown,getString(R.string.error_nna_unsupported_desc));
					break;
				case StrongEggManagerApp.EGG_CONNSTAT_UNRECOGNIZED:
					// 장비 인식불가
					refreshUi_OpenClose(false,false,true,false,
							false,false,false,false,
							true,getString(R.string.error_nna_unrecognized_title),EggCore.getSSID(),
							getString(R.string.action_wifi),(new OnClickListener(){
								@Override
								public void onClick(View v) {
									popupWifiSetting();
								}
					    	}),R.drawable.nna_another_wifi,getString(R.string.error_nna_unrecognized_desc));
					break;
				case StrongEggManagerApp.EGG_CONNSTAT_NO_WIFI:
					// 와이파이 인식불가
					refreshUi_OpenClose(false,false,true,false,
							false,false,false,false,
							true,getString(R.string.error_nna_nowifiavailable_title),getString(R.string.error_nna_nowifiavailable_ssid),
							getString(R.string.action_wifi),(new OnClickListener(){
								@Override
								public void onClick(View v) {
									popupWifiSetting();
								}
					    	}),R.drawable.nna_no_wifi,getString(R.string.error_nna_nowifiavailable_desc));
					break;
				default:
					//인식중 또는 알수없는 오류
					//잠시 기다려주세요.
					refreshUi_OpenClose(true,false,true,false,
							false,false,false,false,
							true,getString(R.string.error_nna_pleasewait_title),getString(R.string.error_nna_pleasewait_ssid),
							getString(R.string.action_wifi),(new OnClickListener(){
								@Override
								public void onClick(View v) {
									popupWifiSetting();
								}
					    	}),R.drawable.nna_recognizing,getString(R.string.error_nna_pleasewait_desc));
					break;
			}
			} catch (NullPointerException npe){
				//TODO 왜 이걸 이렇게 잡아야하는가
			}
		}else{
			if(isDebug)
			{
				Log.v(TAG_LOG,"no need to refresh its openclose state");
			}
		}
		
		//EggConnStat = -1;
	}
	private void refreshUi_OpenClose(
			final boolean vw_prgbarVisible,
			final boolean vw_loginVisible,
			final boolean vw_wifiVisible,
			final boolean vw_settingsVisible,
			final boolean vw_firmwarepageVisible,
			final boolean vw_refreshVisible,
			final boolean vw_powerVisible,
			final boolean vw_moreVisible,

			final boolean nna_Visible,
			final String nna_Title,
			final String nna_ssid,
			final String nna_PicActionName,
			final OnClickListener nna_PicClick,
			final int nna_drawable,
			final String nna_desc
			){
//		final boolean vw_autorefreshToggled =EggCore.isAutoRefreshRunning();
		//TODO NullPointerException이 보인다 조심하자. 왜 뜨는지는.. EggCore때문일까 내부 요소가 null되서 그런걸까.
		if(EggCore.mainThreadHandler != null){
		EggCore.mainThreadHandler.post(new Runnable(){
			
			@Override
			public void run() {
				//Tv_moreMenu_AutoRefresh.setText(EggCore.isAutoRefreshRunning()? R.string.app_service_turnon : R.string.app_service_turnoff);

				PrgBar_Refresh.setVisibility(vw_prgbarVisible?View.VISIBLE:View.GONE);
				ImgBtn_Login.setVisibility(vw_loginVisible?View.VISIBLE:View.GONE);
				ImgBtn_WiFi.setVisibility(vw_wifiVisible?View.VISIBLE:View.GONE);
				ImgBtn_Settings.setVisibility(vw_settingsVisible?View.VISIBLE:View.GONE);
				ImgBtn_LatestFirmwarePage.setVisibility(vw_firmwarepageVisible?View.VISIBLE:View.GONE);

				ImgBtn_Refresh.setVisibility(vw_refreshVisible?View.VISIBLE:View.GONE);
				ImgBtn_Power.setVisibility(vw_powerVisible?View.VISIBLE:View.GONE);
				ImgBtn_More.setVisibility(vw_moreVisible?View.VISIBLE:View.GONE);

				mViewPager.setVisibility(nna_Visible?View.GONE:View.VISIBLE);
				//완료된 상태였다가 와이파이가 끊어지는 등 다른 상태로 되었을 때는 가리는게 좋을까 두고 비치게 하는게 좋을까..
				//해서 가리도록 만들었다.. ㅠㅠ 예쁘지 않은뎁..
				
				//연결 완료. 기념으로 에그 현재상태를 가져와보죠. ㅇ_ㅇ..
				//서비스 작동중이라면 곧 갱신될테니 생략☆
				if(!nna_Visible)
				{
					EggCore.taskThreadHandler.post(new Runnable(){
						@Override
						public void run() {
							EggCore.LocalRefresh();
						}});
				}
				
				Sv_NotifyUnavailable.setVisibility(nna_Visible?View.VISIBLE:View.GONE);
				Tv_NNA_Title.setText(nna_Title);
		    	Tv_NNA_ssid.setText(nna_ssid);
		    	//ImgV_NNA_Pic.setImageDrawable(drawable);
		    	ImgV_NNA_Pic.setOnClickListener(nna_PicClick);
		    	ImgV_NNA_Pic.setContentDescription(nna_PicActionName);
		    	ImgV_NNA_Pic.setImageResource(nna_drawable);
		    	Tv_NNA_Description.setText(nna_desc);
		    	}});
		}
	}
	
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(isDebug)
		{
			Log.v(TAG_LOG,"Activity: onKeyUp << "+keyCode);
		}
		
		if(keyCode == KeyEvent.KEYCODE_MENU) {
			// ........
			if(ImgBtn_More.getVisibility() == View.VISIBLE)
			{
				if(isMoreMenuOn)
				{
					LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				}
				else
				{
					LinL_moreMenu.startAnimation(Anim_MoreMenuPopup);
				}
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	//메뉴 키 길게 누를때 키보드가 튀어나오는것을 방지. 
	//http://stackoverflow.com/questions/7143978/how-do-i-disable-long-press-option-menu-key-to-bring-up-keyboard
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(isDebug)
		{
			Log.v(TAG_LOG,"Activity: onKeyDown << "+keyCode);
		}
		
		if(event.isLongPress())
		{
			// Eat the long press event so the keyboard doesn't come up.
		    switch (keyCode)
		    {
		    case KeyEvent.KEYCODE_MENU:
	
		    	//일부러라도 키보드를 보이게 하려면  아래 주소 참고.. 
		    	//http://stackoverflow.com/questions/11816292/android-show-keyboard-on-long-menu-click
		    	//
		    	//InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		    	//	inputMethodManager.showSoftInput(lv, InputMethodManager.SHOW_IMPLICIT);
			    	
		    	//break;
			    return true;
		    case KeyEvent.KEYCODE_BACK:
				//뒤로 버튼.. 길게 눌리면 앱 완전종료☆는 아직 아닌것 같당.
				//break;
				//EggCore.showShortToast("HCSTR_뒤로뒤로길게");
				return true;
			default:
				return super.onKeyDown(keyCode, event);
		    }
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
	
	@Override
	public void onBackPressed(){
		if(isDebug){
			Log.v(TAG_LOG,"onBackPressed()");
		}
		
		//Called when the activity has detected the user's press of the back key.
		//The default implementation simply finishes the current activity,
		//but you can override this to do whatever you want. 
		//펼침 메뉴 접어넣기
		if(isPowerMenuOn)
		{
			if(LinL_powerMenu != null){
				try{
					LinL_powerMenu.startAnimation(Anim_PowerMenuClose);
				}catch(NullPointerException npe){
					if(isDebug)
					{
						Log.v(TAG_LOG,"NullPointerException closing Power Menu");
						npe.printStackTrace();
					}
				}
			}
		}
		else if(isMoreMenuOn)
		{
			if(LinL_moreMenu != null){
				try{
					LinL_moreMenu.startAnimation(Anim_MoreMenuClose);
				}catch(NullPointerException npe){
					if(isDebug)
					{
						Log.v(TAG_LOG,"NullPointerException closing Overflow Menu");
						npe.printStackTrace();
					}
				}
			}
		}
		else{
			//메뉴가 펼쳐져 있지 않은경우 일반적인 뒤로 버튼으로 동작.
			super.onBackPressed();
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if(isDebug)
		{
			Log.v(TAG_LOG,"Activyt: onKey << "+keyCode);
		}
		return false;
	}

	private void popupWifiSetting(){
		popupAnotherIntentSpecified(new Intent(Settings.ACTION_WIFI_SETTINGS));
		/*EggCore.uiCompositionThreadHandler.post(new Runnable (){
			@Override
			public void run() {
				try{
					//와이파이 설정을 새 액티비티(본 앱과는 별개로)로 열기.
					startActivity((new Intent(Settings.ACTION_WIFI_SETTINGS)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
				}catch (ActivityNotFoundException anfe){
					EggCore.showShortToast(R.string.error_wifi_setting_not_exists);
					if(isDebug){
						Log.w(TAG_LOG,"WiFi Settings Cannot be opened because it's not found");
						anfe.printStackTrace();
					}
				}
				
			}});*/
		/** http://stackoverflow.com/questions/11158955/intent-filter-for-settings-activity
		 * 
		 *  I am trying to receive the broadcast whenever activitymanager launches system settings.
		 *  What is the intent filter for system settings activity?
		 * 
		 *  No intent is broadcasted when System Settings are launched.
		 *  But you can create a service and check the top activity in activity stack like that:
		 */
		/*
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> processes = activityManager.getRunningTasks(1);
		ComponentName componentInfo = processes.get(0).topActivity;
		String className = componentInfo.getClassName();    
		String packageName = componentInfo.getPackageName();
		if(className.equalsIgnoreCase("com.android.settings.Settings"))
		{
		    // DO YOUR JOB HERE
		}*/

	
	}
	/*
	@Override
	public void onFinishEditDialog(String inputText) {
		EggCore.showShortToast(inputText);
	}*/
/*
    private void showEditDialog() {
        FragmentManager fm = getSupportFragmentManager();
        LogInDialogFragment editNameDialog = new LogInDialogFragment();
        editNameDialog.show(fm, "fragment_edit_name");
    }
*/
	
	private void popupRetryLogin(){
		EggCore.uiCompositionThreadHandler.post(new Runnable (){
			@Override
			public void run() {
				showRetryLoginDialog();
				//showEditDialog();
			}
		});
	}
	private void popupWebCM(){
		popupAnotherIntentSpecified(new Intent(Intent.ACTION_VIEW, Uri.parse("http://"+EggCore.getEggIP())));
	}
	
	private void popupFirmwarePage(){
		if(EggCore.getEggType() == StrongEggManagerApp.EGG_KWDB2600_GENERIC){
			popupAnotherIntentSpecified(new Intent(Intent.ACTION_VIEW, Uri.parse("http://web.modacom.co.kr/ko/support/download_view.php?seq_id=10479")));
		}else if(EggCore.getEggType() == StrongEggManagerApp.EGG_KWFB2700_GENERIC){
			popupAnotherIntentSpecified(new Intent(Intent.ACTION_VIEW, Uri.parse("http://infomark.co.kr/kboard/kboard.php?board=down&act=view&no=10")));
		}
	}
	
	//*
	//1.[앱] 에그 접근 인증키(사용자이름+비밀번호)
	private void showRetryLoginDialog()// final Context activityContext)
	{
		// 재로그인 대화상자
		// ApplicationContext
		AlertDialog.Builder retryLoginDialogBuilder = new AlertDialog.Builder(
				this);// activityContext);//getApplicationContext());

		View retryLoginView = LayoutInflater.from(this).inflate(
				R.layout.dialog_login, null);
		final EditText et_newID = (EditText) retryLoginView
				.findViewById(R.id.editText_username);
		final EditText et_newPW = (EditText) retryLoginView
				.findViewById(R.id.editText_password);

		// getApplicationContext().
		// retryLoginDialogBuilder.setIconAttribute(android.R.drawable.ic_dialog_map);//.attr.alertDialogIcon)
		retryLoginDialogBuilder.setTitle(R.string.action_login);
		// retryLoginDialogBuilder.setCancelable(false);//false: '뒤로' 버튼으로 닫을 수
		// 없다.
		retryLoginDialogBuilder.setView(retryLoginView);
		retryLoginDialogBuilder.setIcon(R.drawable.ic_action_login);
		retryLoginDialogBuilder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						//dialog.dismiss();
						// OK
						// 새 ID/PW 판독 뒤 적용.하게 만들면 좋겠는데
						// isCanceled = true;//외부메소드라,값이 바뀌지 않는다.
						// showShortToast(et_newID.getText().toString()+":"+et_newPW.getText().toString());
						EggCore.taskThreadHandler.post(new Runnable() {

							@Override
							public void run() {
								EggCore.showShortToast(R.string.noti_login_renewed);

								EggCore.renewAuthKey(et_newID.getText().toString(),
										et_newPW.getText().toString());
							}
						});
					}
				});
		
		retryLoginDialogBuilder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		
				
		final AlertDialog retryLoginDialog = retryLoginDialogBuilder.create();
		/*
		retryLoginDialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				//retryLoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				retryLoginDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			}
		});*/



		et_newID.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// 다음/엔터 키를 누르면 비밀번호 입력칸으로
				switch (actionId) {
				case EditorInfo.IME_ACTION_GO:
				case EditorInfo.IME_ACTION_SEND:
				case EditorInfo.IME_ACTION_DONE:
				case EditorInfo.IME_ACTION_NEXT:
					et_newPW.requestFocus();
					break;
				default:
					break;
				}
				return false;
			}
		});/*
		et_newPW.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				// 다음/엔터 키를 누르면 완료
				switch (actionId) {
				case EditorInfo.IME_ACTION_GO:
				case EditorInfo.IME_ACTION_SEND:
				case EditorInfo.IME_ACTION_DONE:
				case EditorInfo.IME_ACTION_NEXT:
					/*if (et_newPW.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
						if (et_newID.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
							retryLoginDialog.dismiss();
							return false;
						}
					}/ * /
					break;
				default:
					break;
				}
				
				return false;
			}
		});*/

		/*/ TODO onKey가 먹히지 않는다면 버튼을 만져선 안되겠지
		et_newID.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEv) {

				if (isDebug) {
					Log.v(TAG_LOG, "Dialog: OnKey ID << " +keyCode);
				}
				if (keyEv.getAction() == KeyEvent.ACTION_UP) {
					// ID입력이 4자 이상인 경우.
					
					if (et_newID.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
						if (et_newPW.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
							retryLoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
							return false;
						}
					}
					retryLoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
				return false;
			}

		});
		et_newPW.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEv) {

				if (isDebug) {
					Log.v(TAG_LOG, "Dialog: OnKey PW << " +keyCode);
				}
				if (keyEv.getAction() == KeyEvent.ACTION_UP) {
					// 패스워드 입력이 4자 이상인경우
					if (et_newPW.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
						if (et_newID.length() > StrongEggManagerApp.MINIMUM_CREDS_CHARS) {
							retryLoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
							return false;
						}
					}
					retryLoginDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
				return false;
			}

		});*/
		retryLoginDialog.show();

		et_newID.requestFocus();
	}

		private void popupPreferenceDialog()
		{
			EggCore.uiCompositionThreadHandler.post(new Runnable (){
				@Override
				public void run() {
					showPreferenceDialog();
				}
			});
		}
		private void showPreferenceDialog(){
			//dialog도 UI의 일부니까.. taskThreadHandler 대신 이걸 써볼까..
			
			//uiCompositionThreadHandler.post(new Runnable (){

			//	@Override
			//	public void run() {
					//final boolean isCanceled = false;
					
					//설정 대화상자
					AlertDialog.Builder preferenceDialogBuilder = new AlertDialog.Builder(this);//getApplicationContext());
					
					//Thread-safe 작업이 아닌건 아쉬워. 
					View preferenceView = LayoutInflater.from(this).inflate(R.layout.dialog_preference, null);
					final CheckBox cb_httpClient = (CheckBox) preferenceView.findViewById(R.id.checkBox_pref_useHttpClient);
					final CheckBox cb_darkIcon = (CheckBox) preferenceView.findViewById(R.id.checkBox_pref_useDarkIcon);
					final CheckBox cb_persistMntrng = (CheckBox) preferenceView.findViewById(R.id.checkBox_pref_persistMonitoring);
					final CheckBox cb_miniAutoRefresh = (CheckBox) preferenceView.findViewById(R.id.checkBox_pref_useMiniAutorefresh);
					
					final Spinner spn_pollingRate = (Spinner) preferenceView.findViewById(R.id.spinner_pollingRate);
					// Create an ArrayAdapter using the string array and a default spinner layout
					ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
					        R.array.updateInterval, android.R.layout.simple_spinner_item);
					// Specify the layout to use when the list of choices appears
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					// Apply the adapter to the spinner
					spn_pollingRate.setAdapter(adapter);
					
					final ArrayAdapter<CharSequence> pollingRateAdapter = ArrayAdapter.createFromResource(this,
					        R.array.updateIntervalValues, android.R.layout.simple_spinner_item);
					
					final LinearLayout ll_pollingRate = (LinearLayout) preferenceView.findViewById(R.id.linearLayout_pollingRate); 
					final EditText et_pollingRate = (EditText) preferenceView.findViewById(R.id.editText_pollingRate);
					
					//처음 대화상자의 상태. (체크..)
					cb_httpClient.setChecked(EggCore.isHttpClientPreferred());
					cb_darkIcon.setChecked(EggCore.isDarkIconSetPreferred());
					cb_persistMntrng.setChecked(EggCore.isPersistantMonitoringAlwaysPreferred());
					cb_miniAutoRefresh.setChecked(EggCore.isMiniAutoRefreshPreferred());
					spn_pollingRate.setSelection(EggCore.getPositionPollingRate());
					et_pollingRate.setText(EggCore.App_pollingRate+"");
					
					//spinner 값 변경시 et_pollingRate 값도 바뀌도록 함
					spn_pollingRate.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){

						@Override
						public void onItemSelected(AdapterView<?> parent,
								View view, int pos, long id) {
							// An item was selected. You can retrieve the selected item using
					        // parent.getItemAtPosition(pos)
							if(pos == 0){
								et_pollingRate.setText(EggCore.App_pollingRate+"");
								ll_pollingRate.setVisibility(View.VISIBLE);
							}else{
								ll_pollingRate.setVisibility(View.GONE);
								et_pollingRate.setText(pollingRateAdapter.getItem(pos));
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> parent) {
							// Another interface callback
							
						}
						
					});
					
					
					
					//FrameLayout fl = (FrameLayout) findViewById(android.R.id.custom);
					//fl.addView(preferenceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					
					//getApplicationContext().
					preferenceDialogBuilder.setTitle(R.string.action_settings);
					//preferenceDialog.setCancelable(false);//false: '뒤로' 버튼으로 닫을 수 없다.
					preferenceDialogBuilder.setView(preferenceView);
					preferenceDialogBuilder.setIcon(R.drawable.ic_action_settings);
					preferenceDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           @Override
			    	   public void onClick(DialogInterface dialog, int id) {
			               //OK
			        	   //변경설정 적용하기
			        	   //바꿀때 바로 적용하는것도 나쁘진 않네.
			        	   //하지만 분리해두는게 좋겠당. 다이얼로그를 앱 클래스에서 정의하는것도 좀 그렇구..
			        	   //다시 쓰려면 이것도 클래스로 만들면 좋겠지
			        	   EggCore.setPreferredNow(cb_httpClient.isChecked(), cb_darkIcon.isChecked(), cb_persistMntrng.isChecked(), cb_miniAutoRefresh.isChecked(),
			        			  Long.parseLong(et_pollingRate.getText()+""));
	        			   
			        	   EggCore.commitAppSetting();
			           }
			       });
			       preferenceDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			           @Override
			    	   public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			       /*preferenceDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// Cancel
						//변경사항이 반영되지 않습니다.
					}
			       });*/
					
					//mainThreadHandler.post(new Runnable(){

					//	@Override
					//	public void run() {
							// 	메모리에 생성한 다음 실행합니다
							//메인 스레드에서 할 필요가 있어요.
							preferenceDialogBuilder.create().show();
					//	}
						
					//});
					
					//return isCanceled;
				//}
				
			//});
			
		}
		public void popupChooseKTappDialog(final Context ActivityContext)
		{
			EggCore.uiCompositionThreadHandler.post(new Runnable (){
				@Override
				public void run() {
					showChooseKTappDialog(ActivityContext);
				}
			});
		}
		private void showChooseKTappDialog(Context ActivityContext){
		
			try{
					//설정 대화상자
					AlertDialog.Builder chooseKTappDialogBuilder = new AlertDialog.Builder(ActivityContext);//getApplicationContext());
					
					//Intent 모음..
					final Intent ktCareFullAppIntent = getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME_KT_CARE_FULL);
					//final Intent ktCareMiniAppIntent = getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME_KT_CARE_MINI);
					final Intent ktCareWebIntent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(SITE_ADDRESS_KT_CARE));
					
					//Thread-safe 작업이 아닌건 아쉬워. 
					View chooseKTappView = LayoutInflater.from(ActivityContext).inflate(R.layout.dialog_select_kt_cs, null);
					TextView Tv_KTcareFullApp = (TextView) chooseKTappView.findViewById(R.id.textView_SelectApp_KtFull);
					//TextView Tv_KTcareMiniApp = (TextView) chooseKTappView.findViewById(R.id.textView_SelectApp_KtMini);
					TextView Tv_KTcareWeb = (TextView) chooseKTappView.findViewById(R.id.textView_SelectApp_KtWeb);
					
					if(ktCareFullAppIntent != null)
					{//KT 모바일 고객센터 앱을 찾았다
						//Tv_KTcareFullApp.setVisibility(View.VISIBLE);

						Tv_KTcareFullApp.setOnClickListener(new OnClickListener (){
							@Override
							public void onClick(View v) {
								popupAnotherIntentSpecified(ktCareFullAppIntent);
							}
						});
					} else {//못찾았다.. 마켓으로..
						//Tv_KTcareFullApp.setCompoundDrawables(left, top, right, bottom);
						Tv_KTcareFullApp.setText(getString(R.string.action_app_install_neccessary)+getString(R.string.action_app_ktcare));
						Tv_KTcareFullApp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_androidmarket, 0,0,0);
						Tv_KTcareFullApp.setOnClickListener(new OnClickListener (){
							@Override
							public void onClick(View v) {
								popupAnotherIntentSpecified(new Intent(Intent.ACTION_DEFAULT,Uri.parse("market://details?id="+PACKAGE_NAME_KT_CARE_FULL)));
							}
						});
					} 
						/*{//못찾았다. 마켓엔 없으니 숨기자.
						Tv_KTcareFullApp.setVisibility(View.GONE);
					}
					/*
					if(ktCareMiniAppIntent != null)
					{//KT 미니 고객센터 앱을 찾았다.
						Tv_KTcareMiniApp.setOnClickListener(new OnClickListener (){
							@Override
							public void onClick(View v) {
								popupAnotherIntentSpecified(ktCareMiniAppIntent);
							}
						});
					}else{//못찾았다.. 마켓으로..
						//Tv_KTcareFullApp.setCompoundDrawables(left, top, right, bottom);
						Tv_KTcareMiniApp.setText(getString(R.string.action_app_install_neccessary)+getString(R.string.action_app_ktcaremini));
						Tv_KTcareMiniApp.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_androidmarket, 0,0,0);
						Tv_KTcareMiniApp.setOnClickListener(new OnClickListener (){
							@Override
							public void onClick(View v) {
								popupAnotherIntentSpecified(new Intent(Intent.ACTION_DEFAULT,Uri.parse("market://details?id="+PACKAGE_NAME_KT_CARE_MINI)));
							}
						});
					}*/
					Tv_KTcareWeb.setOnClickListener(new OnClickListener (){
						@Override
						public void onClick(View v) {
							popupAnotherIntentSpecified(ktCareWebIntent);
						}
					});
					
					chooseKTappDialogBuilder.setTitle(R.string.action_launch_ktcare_app);
					//preferenceDialog.setCancelable(false);//false: '뒤로' 버튼으로 닫을 수 없다.
					chooseKTappDialogBuilder.setView(chooseKTappView);
					chooseKTappDialogBuilder.setIcon(R.drawable.ic_action_web);
					chooseKTappDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			           @Override
			    	   public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
					chooseKTappDialogBuilder.create().show();
			}catch(NullPointerException npe){
				if(isDebug){
					Log.e(TAG_LOG,"NullPointerException preparing Choose KT Care Method.");
					npe.printStackTrace();
				}
					
			}
		}
		
		private void popupAnotherIntentSpecified(final Intent requestedIntent){
		EggCore.uiCompositionThreadHandler.post(new Runnable (){
			@Override
			public void run() {
				startAnotherIntentSpecified(requestedIntent);
			}});
		}
		private void startAnotherIntentSpecified(Intent requestedIntent){
			try{
				//새 액티비티(본 앱과는 별개로)로 열기.
				requestedIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(requestedIntent);
				
				//http://stackoverflow.com/questions/3872063/android-launch-an-application-from-another-application
				//다른 앱(메인 액티비티 한정) 실행.
				//getActivity().getPackageManager().getLaunchIntentForPackage("com.package.address");

				////Intent intent = new Intent(Intent.ACTION_MAIN);
				////intent.setComponent(new ComponentName("com.package.address","com.package.address.MainActivity"));
				////startActivity(intent);
			}catch( NullPointerException npe){// NameNotFoundException pmnnfe){
				//는 컴파일이 앙대..서 Nullpointerexception으로 대체.
				EggCore.showShortToast(R.string.error_action_launch_app_failed);
				if(isDebug){
					Log.w(TAG_LOG,"Specified app not found >> "+requestedIntent.getPackage()+" # "+requestedIntent.getAction());
					npe.printStackTrace();
				}
			}catch (ActivityNotFoundException anfe){
				EggCore.showShortToast(R.string.error_wifi_setting_not_exists);
				if(isDebug){
					Log.w(TAG_LOG,"WiFi Settings Cannot be opened because it's not found");
					anfe.printStackTrace();
				}
			}
		}
		
		public void popupAnotherAppSpecified(final String packageName){
			
			EggCore.uiCompositionThreadHandler.post(new Runnable (){
				@Override
				public void run() {
					startAnotherIntentSpecified(getPackageManager().getLaunchIntentForPackage(packageName));
				}});
		}
}
