package kr.LaruYan.StrongEggManager;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class IPv4Input{
	private static final boolean isDebug = StrongEggManagerApp.isDebug;
	//private static final String TAG_LOG = "IPv4Input";
	private static final String SINGLESTRING_PERIOD=".";
	
	private View IPv4InputView = null;
	
	private EditText Et_IPv4_A = null;
	private EditText Et_IPv4_B = null;
	private EditText Et_IPv4_C = null;
	private EditText Et_IPv4_D = null;
	
	public IPv4Input(View IncludeView)
	{
		IPv4InputView = IncludeView;
		
		Et_IPv4_A = (EditText) IPv4InputView.findViewById(R.id.editText_IPv4_A);
		Et_IPv4_A.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent KeyEv) {
				//. 을 누른경우 다음 칸으로 이동.
				//Log.v("IPv4Input",KeyEv.getKeyCode()+"/"+keyCode);

				if(KeyEv.getAction() == KeyEvent.ACTION_UP)
				{
						//XXX.168.127.255
						//첫 번째 칸, 두 번째 칸으로 활성상태 변경.
						if(Et_IPv4_A.length() > 2)
						{
							Et_IPv4_B.requestFocus();
							return true;
						}
				}
				
				return false;
			}
			
		});
		Et_IPv4_A.setOnFocusChangeListener(new OnFocusChangeListener () {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
				{//비활성 상태가 되면 해당 칸 값을 검사.
						//XXX.168.127.255
						//첫 번째 칸 입력값 검사.
						VerifyInputRange(Et_IPv4_A);
					
				}
				else
				{//활성 상태가 되면 선택된 칸 모든 글자를 전체선택.
						//XXX.168.127.255
						//첫 번째 칸
						Et_IPv4_A.selectAll();
				}
			}
		
		});

		Et_IPv4_B = (EditText) IPv4InputView.findViewById(R.id.editText_IPv4_B);
		Et_IPv4_B.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEv) {
				// . 을 누른경우 다음 칸으로 이동.
				//Log.v("IPv4Input",KeyEv.getKeyCode()+"/"+keyCode);

				if(keyEv.getAction() == KeyEvent.ACTION_UP)
				{
						//192.XXX.127.255
						//두 번째 칸, 세 번째 칸으로 활성상태 변경.
						if(Et_IPv4_B.length() > 2)
						{
							Et_IPv4_C.requestFocus();
							return true;
						}
				}
				return false;
			}
			
		});
		Et_IPv4_B.setOnFocusChangeListener(new OnFocusChangeListener(){

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
				{//비활성 상태가 되면 해당 칸 값을 검사.
						//192.XXX.127.255
						//두 번째 칸 입력값 검사.
						VerifyInputRange(Et_IPv4_B);
				}
				else
				{//활성 상태가 되면 선택된 칸 모든 글자를 전체선택.
						//192.XXX.127.255
						//두 번째 칸 
						Et_IPv4_B.selectAll();
				}
			}
			
		});
		
		Et_IPv4_C = (EditText) IPv4InputView.findViewById(R.id.editText_IPv4_C);
		Et_IPv4_C.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEv) {
				// . 을 누른경우 다음 칸으로 이동.
				//Log.v("IPv4Input",KeyEv.getKeyCode()+"/"+keyCode);

				if(keyEv.getAction() == KeyEvent.ACTION_UP)
				{
						//192.168.XXX.255
						//세 번째 칸, 네 번째 칸으로 활성상태 변경.
						if(Et_IPv4_C.length() > 2)
						{
							Et_IPv4_D.requestFocus();
							return true;
						}
				}
				return false;
			}
			
		});
		Et_IPv4_C.setOnFocusChangeListener(new OnFocusChangeListener(){

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
				{//비활성 상태가 되면 해당 칸 값을 검사.
						//192.168.XXX.255
						//세 번째 칸 입력값 검사.
						VerifyInputRange(Et_IPv4_C);
				}
				else
				{//활성 상태가 되면 선택된 칸 모든 글자를 전체선택.
						//192.168.XXX.255
						//세 번째 칸 
						Et_IPv4_C.selectAll();
				}
			}
			
		});
		
		Et_IPv4_D = (EditText) IPv4InputView.findViewById(R.id.editText_IPv4_D);
		Et_IPv4_D.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent keyEv) {
				// . 을 누른경우 다음 칸으로 이동.
				//Log.v("IPv4Input",KeyEv.getKeyCode()+"/"+keyCode);

				if(keyEv.getAction() == KeyEvent.ACTION_UP)
				{
					//192.168.127.XXX
					//네 번째 칸, 입력 완료. 입력 풀기.
						if(Et_IPv4_D.length() > 2)
						{
							Et_IPv4_D.clearFocus();
							return true;
						}
				}
				return false;
			}
		});
		Et_IPv4_D.setOnFocusChangeListener(new OnFocusChangeListener(){

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
				{//비활성 상태가 되면 해당 칸 값을 검사.
						//192.168.127.XXX
						//네 번째 칸 입력값 검사.
						VerifyInputRange(Et_IPv4_D);
				}
				else
				{//활성 상태가 되면 선택된 칸 모든 글자를 전체선택.
						//192.168.127.XXX
						//네 번째 칸
						Et_IPv4_D.selectAll();
				}
			}
			
		});

	}
	/*
	public static byte[] intToIPv4Bytes(int IPv4Integer) {
		//UNCERTAIN
		//1181772947 -> {70, 112, 108, 147}
		byte[] IPv4Bytes = new byte[4];

		IPv4Bytes[3] =  intToPseudoUnsignedByte(( IPv4Integer >> 24 ) & 0xFF);
		IPv4Bytes[2] =  intToPseudoUnsignedByte(( IPv4Integer >> 16 ) & 0xFF);
		IPv4Bytes[1] =  intToPseudoUnsignedByte(( IPv4Integer >>  8 ) & 0xFF);
		IPv4Bytes[0] =  intToPseudoUnsignedByte(  IPv4Integer         & 0xFF);
		return IPv4Bytes;
	}*/
	public final static String intToIPv4String(int IPv4Integer) {
		//Little_endian 대응이 되지 않아있어서 곤란하다.. ㅠㅠ
		//1181772947 -> "70.112.108.147" 
		return (( IPv4Integer >> 24 ) & 0xFF ) + SINGLESTRING_PERIOD +
		       (( IPv4Integer >> 16 ) & 0xFF ) + SINGLESTRING_PERIOD +
		       (( IPv4Integer >>  8 ) & 0xFF ) + SINGLESTRING_PERIOD +
		       (  IPv4Integer & 0xFF         );
	}
	public  final static String intLsbToIPv4String(int IPv4Integer) {
	
		//2473357382 -> "70.112.108.147" 
		return  (  IPv4Integer & 0xFF         ) + SINGLESTRING_PERIOD +
				(( IPv4Integer >>  8 ) & 0xFF ) + SINGLESTRING_PERIOD +
				(( IPv4Integer >> 16 ) & 0xFF ) + SINGLESTRING_PERIOD +
				(( IPv4Integer >> 24 ) & 0xFF );
	}
	public  final static int bytesToIPv4Integer(byte[] IPv4Bytes)
	{
		//{70, 112, 108, 147} -> 1181772947
	    return (( IPv4Bytes[3] & 0xFF ) << 24) |
	           (( IPv4Bytes[2] & 0xFF ) << 16) |
	           (( IPv4Bytes[1] & 0xFF ) << 8 ) |
	            ( IPv4Bytes[0] & 0xFF        );
	}
	public  final static String bytesToIPv4String(byte[] IPv4Bytes)
	{
		//{70, 112, 108, 147} -> "70.112.108.147"
		return  IPv4Bytes[3] + SINGLESTRING_PERIOD +
				IPv4Bytes[2] + SINGLESTRING_PERIOD +
				IPv4Bytes[1] + SINGLESTRING_PERIOD +
				IPv4Bytes[0] ;
	}/*
	public static byte[] stringToIPv4Bytes(String IPv4String)
	{
		//UNCERTAIN
		//"70.112.108.147" -> {70, 112, 108, 147};
		String[] IPv4Strings = IPv4String.split(SINGLESTRING_PERIOD);
		byte[] IPv4Bytes = new byte[4];
		IPv4Bytes[3] = intToPseudoUnsignedByte(Short.parseShort(IPv4Strings[3]));
		IPv4Bytes[2] = intToPseudoUnsignedByte(Short.parseShort(IPv4Strings[2]));
		IPv4Bytes[1] = intToPseudoUnsignedByte(Short.parseShort(IPv4Strings[1]));
		IPv4Bytes[0] = intToPseudoUnsignedByte(Short.parseShort(IPv4Strings[0]));
		
		return null;
	}
	*/
	public final static byte intToPseudoUnsignedByte(int n)
	{
		//0 ~ 255 의 수를 -128 ~ +127 로 변환.
		//Java엔 unsigned byte가 없어서 그랬어요.
		if (n < 128)
		{
			return (byte) n;
		}
		return (byte)(n - 256);
	}
	public final static int pseudoUnsignedByteToInt(byte n)
	{
		return n & 0xFF;
	}
	 /*
	public void setIPv4Input(String IPv4String)
	{
		setIPv4Input(stringToIPv4Bytes(IPv4String));
	}
	
	public void setIPv4Input(int IPv4Integer)
	{
		setIPv4Input(intToIPv4Bytes(IPv4Integer));
	}
	
	public void setIPv4Input(byte[] IPv4Bytes)
	{
		Et_IPv4_A.setText(String.valueOf(IPv4Bytes[3] & 0xFF));
		Et_IPv4_B.setText(String.valueOf(IPv4Bytes[2] & 0xFF));
		Et_IPv4_C.setText(String.valueOf(IPv4Bytes[1] & 0xFF));
		Et_IPv4_D.setText(String.valueOf(IPv4Bytes[0] & 0xFF));
	}*/

	public void setIPv4String(String IPv4String)
	{
		String[] IPv4Strings = IPv4String.split(SINGLESTRING_PERIOD);
		Et_IPv4_A.setText(IPv4Strings[0] );
		Et_IPv4_B.setText(IPv4Strings[1] );
		Et_IPv4_C.setText(IPv4Strings[2] );
		Et_IPv4_D.setText(IPv4Strings[3] );
	}
	
	public String getIPv4String()
	{
		return Et_IPv4_A.getText()+SINGLESTRING_PERIOD+Et_IPv4_B.getText()+SINGLESTRING_PERIOD+Et_IPv4_C.getText()+SINGLESTRING_PERIOD+Et_IPv4_D.getText();
	}
	
	private final static boolean VerifyInputRange(EditText editText)
	{
		boolean isDot = false;
		try
		{
			//0~255 까지 표현할 수 있는 자료형이 short부터라서 아쉬워요.
			//byte는 signed, uint나 char는 parse가 없어요.
			short verifyIP = Short.parseShort(editText.getText().toString());
			if(verifyIP > 255)
			{
				verifyIP = 255;//editText.setText("255");
			}
			editText.setText(String.valueOf(verifyIP));
		}
		catch (NumberFormatException nfe)
		{
			if(isDebug)
			{
				nfe.printStackTrace();
			}
			editText.setText(String.valueOf(0));
		}
		return isDot;
	}
/*
	@Override
	public boolean onKey(View view, int keyCode, KeyEvent KeyEv) {
		// . 을 누른경우 다음 칸으로 이동.
		//Log.v("IPv4Input",KeyEv.getKeyCode()+"/"+keyCode);

		if(KeyEv.getAction() == KeyEvent.ACTION_UP)
		{
			switch(view.getId())
			{
			case R.id.editText_IPv4_A:
				//XXX.168.127.255
				//첫 번째 칸, 두 번째 칸으로 활성상태 변경.
				if(Et_IPv4_A.length() > 2)
				{
					Et_IPv4_B.requestFocus();
					return true;
				}
				break;
			case R.id.editText_IPv4_B:
				//192.XXX.127.255
				//두 번째 칸, 세 번째 칸으로 활성상태 변경.
				if(Et_IPv4_B.length() > 2)
				{
					Et_IPv4_C.requestFocus();
					return true;
				}
				break;
			case R.id.editText_IPv4_C:
				//192.168.XXX.255
				//세 번째 칸, 네 번째 칸으로 활성상태 변경.
				if(Et_IPv4_C.length() > 2)
				{
					Et_IPv4_D.requestFocus();
					return true;
				}
				break;
			case R.id.editText_IPv4_D:
				//192.168.127.XXX
				//네 번째 칸, 입력 완료. 입력 풀기.
				if(Et_IPv4_D.length() > 2)
				{
					Et_IPv4_D.clearFocus();
					return true;
				}
				break;
			default:
				break;
			}
		}
		
		return false;
	}

	@Override
	public void onFocusChange(View wasFocusedView, boolean hasFocus) {
		//Log.v("HASFOCUS",hasFocus+"");
		if(!hasFocus)
		{//비활성 상태가 되면 해당 칸 값을 검사.
			switch(wasFocusedView.getId())
			{
			case R.id.editText_IPv4_A:
				//XXX.168.127.255
				//첫 번째 칸 입력값 검사.
				VerifyInputRange(Et_IPv4_A);
				break;
			case R.id.editText_IPv4_B:
				//192.XXX.127.255
				//두 번째 칸 입력값 검사.
				VerifyInputRange(Et_IPv4_B);
				break;
			case R.id.editText_IPv4_C:
				//192.168.XXX.255
				//세 번째 칸 입력값 검사.
				VerifyInputRange(Et_IPv4_C);
				break;
			case R.id.editText_IPv4_D:
				//192.168.127.XXX
				//네 번째 칸 입력값 검사.
				VerifyInputRange(Et_IPv4_D);
				break;
			default:
				break;
			}
		}
		else
		{//활성 상태가 되면 선택된 칸 모든 글자를 전체선택.
			switch(wasFocusedView.getId())
			{
			case R.id.editText_IPv4_A:
				//XXX.168.127.255
				//첫 번째 칸
				Et_IPv4_A.selectAll();
				break;
			case R.id.editText_IPv4_B:
				//192.XXX.127.255
				//두 번째 칸 
				Et_IPv4_B.selectAll();
				break;
			case R.id.editText_IPv4_C:
				//192.168.XXX.255
				//세 번째 칸 
				Et_IPv4_C.selectAll();
				break;
			case R.id.editText_IPv4_D:
				//192.168.127.XXX
				//네 번째 칸
				Et_IPv4_D.selectAll();
				break;
			default:
				break;
			}
		}
	}*/

}
