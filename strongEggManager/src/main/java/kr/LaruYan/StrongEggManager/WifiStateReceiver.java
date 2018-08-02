package kr.LaruYan.StrongEggManager;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;


public class WifiStateReceiver extends BroadcastReceiver {
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_WIFISTATRCVR";
	
	private static final byte WIFI_UNAVAILABLE = -12;
	private static final byte WIFI_DISCONNECTED = -11;
	private static final byte WIFI_SEARCHING = 13;
	private static final byte WIFI_ASSOCIATING = 12;
	private static final byte WIFI_AUTHENTICATION = 11;
	private static final byte WIFI_CONNECTED = 10;
		
	@Override // TODO final 붙은걸 없앨 방법을 찾아보자.
	public void onReceive(final Context context, final Intent intent) {
		StrongEggManagerApp broadcastHandleEggCore = (StrongEggManagerApp) context.getApplicationContext();
		
		broadcastHandleEggCore.broadcastThreadHandler.post(new Runnable (){
			@Override
			public void run() {
				StrongEggManagerApp EggCore = (StrongEggManagerApp) context.getApplicationContext();
				
				if((WifiManager.WIFI_STATE_CHANGED_ACTION).equals(intent.getAction())){
					switch( intent.getIntExtra(WifiManager. EXTRA_WIFI_STATE , WifiManager.WIFI_STATE_UNKNOWN))
					{
					case WifiManager.WIFI_STATE_ENABLING:
					case WifiManager.WIFI_STATE_ENABLED:
						//별 일 안해도 될것 같당.. 활성화/활성중이 아닌경우엔.
						if(isDebug)
						{
							Log.v(TAG_LOG,"new Wifi state << ENABLED | ENABLING");
						}
						break;
					default:
						if(isDebug)
						{
							Log.v(TAG_LOG,"new Wifi state << DISABLING | DISABLED | UNKNOWN");
						}
						EggCore.setConnectionStatus(StrongEggManagerApp.EGG_CONNSTAT_NO_WIFI);
						break;
					}
				}else{ // SUPPLICANT_STATE_CHANGED 
					//NOT COMPLETED 인식중..
					if(isDebug)
					{
						Log.v(TAG_LOG,"new Wifi supplicant state << "+(intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
					}
					switch(parseWifiState((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)))
					{
					case WIFI_SEARCHING:
					case WIFI_DISCONNECTED:
						EggCore.cancelCheckWifiEvent();
						EggCore.setConnectionStatus(StrongEggManagerApp.EGG_CONNSTAT_NO_WIFI);
						break;
					case WIFI_CONNECTED:
						EggCore.cancelCheckWifiEvent();
						EggCore.sendCheckWifiEvent(0);
						break;
//					case WIFI_SEARCHING:
//						if(EggCore.getConnectionStatus()>StrongEggManagerApp.EGG_CONNSTAT_NOW_RECOGNIZING)//
					case WIFI_ASSOCIATING:
					case WIFI_AUTHENTICATION:
					case WIFI_UNAVAILABLE:
					default:
						break;
					}
					EggCore = null;
				}
			}
		});

		if(isDebug)
		{
				Log.v(TAG_LOG,"Wifi broadcast processed.");
		}
		
		broadcastHandleEggCore = null;
	}
	
	private static final byte parseWifiState(SupplicantState wifiSupplicantState){
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			return parseWifiStateAPI14(wifiSupplicantState);
		}else{
			return parseWifiStateFallback(wifiSupplicantState);
		}
	}

	@TargetApi(14)
	private static final byte parseWifiStateAPI14(SupplicantState wifiSupplicantState){
		if(wifiSupplicantState != null){
			switch(wifiSupplicantState){
				case ASSOCIATING:
				case ASSOCIATED:
					return WIFI_ASSOCIATING;
				case AUTHENTICATING:
				case GROUP_HANDSHAKE:
				case FOUR_WAY_HANDSHAKE:
					return WIFI_AUTHENTICATION;
				case COMPLETED:
					return WIFI_CONNECTED;
				case DORMANT:
				case DISCONNECTED:
					return WIFI_DISCONNECTED;
				case SCANNING:
					return WIFI_SEARCHING;
				case INACTIVE:
				case INTERFACE_DISABLED:
				case INVALID:
				case UNINITIALIZED:
				default:
					return WIFI_UNAVAILABLE;
			}
		}else{
			return WIFI_UNAVAILABLE;
		}
	}
	
	private static final byte parseWifiStateFallback(SupplicantState wifiSupplicantState){
		if(wifiSupplicantState != null){
			switch(wifiSupplicantState){
				case ASSOCIATING:
				case ASSOCIATED:
					return WIFI_ASSOCIATING;
				case GROUP_HANDSHAKE:
				case FOUR_WAY_HANDSHAKE:
					return WIFI_AUTHENTICATION;
				case COMPLETED:
					return WIFI_CONNECTED;
				case DORMANT:
				case DISCONNECTED:
					return WIFI_DISCONNECTED;
				case SCANNING:
					return WIFI_SEARCHING;
				case INACTIVE:
				case INVALID:
				case UNINITIALIZED:
				default:
					return WIFI_UNAVAILABLE;
			}
		}else{
			return WIFI_UNAVAILABLE;
		}
	}

}
