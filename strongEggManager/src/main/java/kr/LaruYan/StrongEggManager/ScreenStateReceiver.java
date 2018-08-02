package kr.LaruYan.StrongEggManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

//http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
public class ScreenStateReceiver extends BroadcastReceiver {
 
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	private static final String TAG_LOG = "SEM_SCREENSTATRCVR";

    @Override // TODO final 붙은걸 없앨 방법을 찾아보자.
    public void onReceive(final Context context, final Intent intent) {
    	StrongEggManagerApp broadcastHandleEggCore = (StrongEggManagerApp) context.getApplicationContext();
		
		broadcastHandleEggCore.broadcastThreadHandler.post(new Runnable (){
			@Override
			public void run() {
				StrongEggManagerApp EggCore = (StrongEggManagerApp) context.getApplicationContext();
				if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
					if(isDebug)	{
							Log.v(TAG_LOG,"Screen is Off");
					}
		            EggCore.pause();
		        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
		        	if(isDebug)	{
							Log.v(TAG_LOG,"Screen is On");
					}
		        	EggCore.resume();
		        }
		        //Intent i = new Intent(context, StrongEggManagerService.class);
		        //i.putExtra("screen_state", screenOff);
		        //context.startService(i);
				EggCore = null;
			}
		});

		if(isDebug)
		{
				Log.v(TAG_LOG,"Screen On Off broadcast processed.");
		}
		
		broadcastHandleEggCore = null;
    }
 
}