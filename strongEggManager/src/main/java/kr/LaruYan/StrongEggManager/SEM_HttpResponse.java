package kr.LaruYan.StrongEggManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.List;
import java.util.Scanner;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public final class SEM_HttpResponse {
	private static final boolean isDebug = false;	
	private static final String TAG_LOG = "SEM_HttpResponse";
	private static final String TAG_TYPE_HTTPCLIENT = "(HttpClient)";
	private static final String TAG_TYPE_HTTPURLCONNECTION = "(HttpURLConnection)";
	
	//HTTP 관련
	private static final String HTTP_AUTH_HEADER = "Authorization";
	private static final String HTTP_AUTH_BASIC_PREFIX = "Basic ";
	private static final int SOCKET_TIMEOUT = 5000;
	private static final int CONNECTION_TIMEOUT = 3000;
	private static final String DEFAULT_PAGE_ENCODING = "UTF-8";
	private static final String HTTP_HEADER_KEY_REALM = "WWW-Authenticate";
	private static final String HTTP_HEADER_KEY_LOCATION = "Location";
	private static final String HTTP_PREFIX_PROTOCOL = "http://";
	private static final int HTTP_STATUS_UNDEFINED = -1;
	//public static final String HTTP_LOOPBACK_ADDRESS = "127.0.0.1";
	//	public final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 1.6; en-us; GenericAndroidDevice) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1";
	
		/**
		 * HTTP 상태 코드. HTTP 규격을 준수합니다.
		 * 
		 * 기본값: -1
		 */
		private int statusCode = HTTP_STATUS_UNDEFINED;
		/**
		 * HTTP Response 헤더의 WWW-Authenticate 내용.
		 * 
		 * NULL일 수 있습니다.
		 */
		private String authRealmString = null;
		/**
		 * HTTP Response의 내용.
		 * 
		 * NULL일 수 있습니다.
		 */
		private String bodyString = null;
		
		/**
		 * SEM_HttpResponse 객체를 올바르게 사용하려면 다음을 따라주세요.
		 * 
		 * @param httpResponse_StatusCode HTTP 규격에 따른 숫자. HTTP OK 등
		 * @param httpResponse_AUTHrealm HTTP UNAUTHORIZED 인 경우 WWW-Authenticate 값.
		 * @param httpResponse_BodyString HTTP response 내용.
		 */
		public SEM_HttpResponse(int httpResponse_StatusCode, String httpResponse_AUTHrealm, String httpResponse_BodyString){
			statusCode = httpResponse_StatusCode;
			authRealmString = httpResponse_AUTHrealm;
			bodyString = httpResponse_BodyString;
		}
		
		/**
		 * 이 객체에 들어있는 HTTP 결과 코드를 반환합니다. 
		 * 
		 * @return HTTP 결과 코드
		 */
		public int getHttpResponseStatusCode(){
			return statusCode;
		}
		
		/**
		 * 이 객체에 들어있는 WWW-Authenticate 값을 반환합니다.
		 * 기본적으로 HTTP UNAUTHORIZED 일 때에만 값을 저장하므로 대개 NULL일 가능성이 높습니다.
		 * 
		 * @return WWW-Authenticate 값. (주의: NULL)
		 */
		public String getHttpResponseAuthRealm(){
			return authRealmString;
		}
		
		/**
		 * 이 객체에 들어있는 WWW-Authenticate 값을 반환합니다.
		 * 매개인자가 true라면, NULL대신 빈 문자열을 반환합니다.
		 * 
		 * @param notNull NULL이 반환되기를 바라지 않는다면 true.
		 * @return WWW-Authenticate 값.
		 */
		public String getHttpResponseAuthRealm(boolean notNull){
			if(notNull){
				return (authRealmString != null) ? authRealmString:"";
			}else{
				return authRealmString;
			}
		}
		
		/**
		 * 이 객체에 들어있는 HTTP Response 내용을 반환합니다.
		 * 대개 브라우저에 보이는 내용입니다. NULL일 수 있습니다.
		 * 
		 * @return HTTP Response 내용 (주의: NULL)
		 */
		public String getHttpResponseBody(){
			return bodyString;
		}
		
		/**
		 * 이 객체에 들어있는 HTTP Response 내용을 반환합니다.
		 * 대개 브라우저에 보이는 내용입니다.
		 * 매개인자가 true라면, NULL대신 빈 문자열을 반환합니다.
		 * 
		 * @param notNull NULL이 반환되기를 바라지 않는다면 true.
		 * @return HTTP Response 내용
		 */
		public String getHttpResponseBody(boolean notNull){
			if(notNull){
				return (bodyString != null) ? bodyString:"";
			}else{
				return bodyString;
			}
		}


	
		/**
		 * 주소가 null인 경우 유효하지 않다고 판단합니다.
		 * null/example.asp 처럼 null로 시작하는 경우에도 유효하지 않습니다.
		 * 반환 코드는 400 BAD REQUEST
		 * 
		 * 주의: http://null/example.asp는 유효하다고 판단합니다.
		 * 
		 * @param URL 유효성 검사를 할 주소.
		 * @return 유효여부.
		 */
		private static final boolean isURLvalid(String URL){
			if(URL == null){
				//URL이 null일 때 잘못된 요청이라고 되돌려보낸다...라고 할 수 없다.. 이미 주소가 가공되서 오니까..
				if(isDebug){
					Log.w(TAG_LOG,"URL is null. returning >> 400 BAD REQUEST.");
				}
				return false;
			}else if(URL.startsWith("null")){
				if(isDebug){
					Log.w(TAG_LOG,"URL MALFORMED. returning >> 400 BAD REQUEST.");
				}
				return false;
			}else{
				return true;
			}
		}
		
		//0. HTTP POST & RECEIVE
		//Runnable 안에서 실행해야 렉이 덜 걸려요
		
		//http://stackoverflow.com/questions/2938502/sending-post-data-in-android
		//http://stackoverflow.com/questions/4627395/http-requests-with-basic-authentication
		/**
		 * 지정된 주소와 Basic 인증 키로 HTTP POST 요청을 수행합니다.
		 * HTTP REDIRECTION는 처리하지 못합니다.
		 * POST 매개변수는 없습니다.
		 * 
		 * @note org.apache.http.client.HttpClient 를 사용합니다. 
		 * 
		 * @param URL 주소
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP POST Response 일부 (상태 코드, WWW-Authenticate, 내용) 
		 */
		public static final SEM_HttpResponse httpRequestPOST_httpClient(String URL,String AuthKey){
			return httpRequestPOST_httpClient(URL,null,AuthKey);
		}
		
		/**
		 * 지정된 주소와 Basic 인증 키, POST 매개변수로 HTTP POST 요청을 수행합니다.
		 * HTTP REDIRECTION는 처리하지 못합니다.
		 * nameValuePair와는 다르게 형식제한은 없습니다.
		 * 
		 * @note org.apache.http.client.HttpClient 를 사용합니다. 
		 * 
		 * @param URL 주소
		 * @param httpPOSTParameters POST 매개변수. (NULL 인 경우 무시)
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP Response 일부(상태 코드, WWW-Authenticate, 내용) 
		 */
		public static final SEM_HttpResponse httpRequestPOST_httpClient(String URL, String httpPOSTParameters, String AuthKey){//List<NameValuePair> httpPOSTParameters)
			if(!isURLvalid(URL)){
				return new SEM_HttpResponse(HttpURLConnection.HTTP_BAD_REQUEST, null, null);
			}
			/*if(URL.equals(HTTP_LOOPBACK_ADDRESS))
			{//Loopback 주소인경우 하지 않고 돌려보낸다.
				if(isDebug)
				{
					Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"URL is LOOPBACK. returning 410 GONE.");
				}
				return new SEM_HttpResponse(HttpURLConnection.HTTP_GONE, null, null);
			}*/
			if(isDebug){
				//HTTP 작업을 어느 스레드에서 하고 있을까..
				Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"http operation on Thread: "+Thread.currentThread().getName());
			}
			
			if(isDebug){
				Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"preparing httpPOST...");
			}
			
		    HttpResponse httpClient_response = null;// = new HttpResponse();
		    // Create a new HttpClient and POST Header & body
	    	HttpClient httpCLIENT = newHttpClientWithManagedParams();
		    HttpPost httpPOST = null;
		    
		    int httpStatusCode = HTTP_STATUS_UNDEFINED;
	        String httpAUTHrealm = null;
	        String httpResponseBodyString = null;
	        SEM_HttpResponse httpRESPONSE = null;	    
	        
	        // http://로 시작하지 않으면 붙입니다.
	        if(! URL.startsWith(HTTP_PREFIX_PROTOCOL)){
	    		URL = HTTP_PREFIX_PROTOCOL + URL ;
	        }
	        
		    try {
		    	httpPOST = new HttpPost( URL );
		    	
		    	//Header
			    //HTTP Basic 인증에 사용할 내용을 추가합니다.
		        //httpPOST.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(Egg_ID, Egg_PW), DEFAULT_PAGE_ENCODING, false));
		    	if(AuthKey != null){
		    		httpPOST.addHeader(HTTP_AUTH_HEADER, HTTP_AUTH_BASIC_PREFIX+AuthKey);
		    		if(isDebug){
						Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"establishing httpGET >> "+URL+" with Authentication Token");
					}
		    	}else{
		    		if(isDebug){
						Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"establishing httpGET >> "+URL+" without Auth");
					}
		    	}

		        //httpPOST.setEntity(new UrlEncodedFormEntity(httpPOSTParameters,DEFAULT_PAGE_ENCODING));
		        if(httpPOSTParameters != null){
		        	//httpPOST 매개변수가 있는경우
		        	httpPOST.setEntity(new StringEntity(httpPOSTParameters,DEFAULT_PAGE_ENCODING));
		        	
		        	if(isDebug){
		        		//무엇이 POST BODY가 될지 확인.
			        	Log.v(TAG_LOG+TAG_TYPE_HTTPCLIENT,"the App will POST >> "+getContent(httpPOST.getEntity().getContent()));
			        }
		        }

		        // Execute HTTP Post Request
		        if(isDebug){
					Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"establishing httpPOST >> "+URL);
				}
		        httpClient_response = httpCLIENT.execute(httpPOST);

		        httpStatusCode = httpClient_response.getStatusLine().getStatusCode();
		        if(isDebug){
					Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"got response from httpPOST << " + httpStatusCode );
				}
		        
		        switch(httpStatusCode){
		        case HttpURLConnection.HTTP_UNAUTHORIZED:
			        Header httpClient_realmHeader = httpClient_response.getFirstHeader(HTTP_HEADER_KEY_REALM);
			        if(httpClient_realmHeader != null){
			        	if(isDebug){
							Log.v(TAG_LOG +TAG_TYPE_HTTPCLIENT,"HTTP AuthRealm << "+httpAUTHrealm);
						}
		        		httpAUTHrealm = httpClient_realmHeader.getValue();
		        		httpClient_realmHeader = null;
		        	}
			        break;
			    default:
			    	break;
		        }

	        	httpResponseBodyString = EntityUtils.toString(httpClient_response.getEntity(),DEFAULT_PAGE_ENCODING); 

		        //httpClient.execute 수행이 끝나면 이와 관련된 리소스를 놓고 연결을 끊는다.
		        httpCLIENT.getConnectionManager().closeExpiredConnections();
		    }catch (UnknownHostException uhe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"UnknownHostException requesting HttpPOST");
		    		uhe.printStackTrace();
		    	}
		    } catch (ClientProtocolException cpe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"ClientProtocolException requesting HttpPOST");
		    		cpe.printStackTrace();
		    	}
		    } catch (IOException ioe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"IOException requesting HttpPOST");
		    		ioe.printStackTrace();
		    	}
		    } catch (IllegalArgumentException iae) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"IllegalArgumentException requesting HttpPOST");
		    		iae.printStackTrace();
		    	}
	        } finally {
	        	httpCLIENT.getConnectionManager().closeExpiredConnections();
		        httpClient_response = null;
		        
	        	//httpClient.execute 수행된것과는 관계없이 이와 관련된 리소스를 놓고 연결을 끊는다.
	        	httpCLIENT.getConnectionManager().shutdown();
	        	//nullify itself
	            httpCLIENT = null;
	            httpPOST = null;
	        	
	        	httpRESPONSE = new SEM_HttpResponse(httpStatusCode,httpAUTHrealm,httpResponseBodyString);
	        	
	        	httpStatusCode = HTTP_STATUS_UNDEFINED;
	            httpAUTHrealm = null;
	            httpResponseBodyString = null;
	            
	            if(isDebug)
				{
					Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"httpPOST processed.");//+httpResponseBodyString);
				}
		    }

		    return httpRESPONSE;
		}

		/**
		 * 지정된 주소와 인증키로 HTTP GET 요청을 수행합니다.
		 * HTTP REDIRECTION도 처리합니다.(stack)
		 * List NameValuePair 인 GET 매개변수들을 URL 뒤에 붙여줍니다.
		 * 
		 * @note java.net.HttpURLConnection 를 사용합니다.
		 * 
		 * @param URL 주소
		 * @param httpGETParameters GET 매개변수
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP GET Response (상태 코드, WWW-Authenticate, 내용)
		 */
		public static final SEM_HttpResponse httpRequestGET_HttpURLConnetcion(String URL, List<NameValuePair> httpGETParameters, String AuthKey){
			if(isDebug){
				//무엇이 GET Param이 될지 확인.
	        	Log.v(TAG_LOG+TAG_TYPE_HTTPURLCONNECTION,"the App will GET with parameters >> "+URLEncodedUtils.format(httpGETParameters, DEFAULT_PAGE_ENCODING));
	        }
			
			if(httpGETParameters != null){
				// ?로 끝나지 않으면 ?로 붙입니다.
				if( !URL.endsWith("?") ){
					URL += "?";
				}
				
				URL += URLEncodedUtils.format(httpGETParameters, DEFAULT_PAGE_ENCODING);//HTTP_PREFIX_PROTOCOL+Egg_IP+"/admin/admin.asp");
				httpGETParameters = null;
			}
			
	        return httpRequestGET_HttpURLConn(URL,AuthKey);
		}
		/**
		 * 지정된 주소와 인증키로 HTTP GET 요청을 수행합니다.
		 * HTTP REDIRECTION도 처리합니다.(stack)
		 * GET 매개변수는 여기서 처리하지 않습니다.
		 * 
		 * @note java.net.HttpURLConnection 를 사용합니다.
		 * 
		 * @param URL 주소
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP GET Response 일부 (상태 코드, WWW-Authenticate, 내용)
		 */
		public static final SEM_HttpResponse httpRequestGET_HttpURLConn(String URL, String AuthKey){
			if(!isURLvalid(URL)){
				return new SEM_HttpResponse(HttpURLConnection.HTTP_BAD_REQUEST, null, null);
			}

			//httpURLConnection을 통한 httpGET 를 실행.
			//StatusCode, Header, Body 를 반환합니다. 
			if(isDebug){
				//HTTP 작업을 어느 스레드에서 하고 있을까..
				Log.v(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"http operation on Thread: "+Thread.currentThread().getName());
			}
			
			if(isDebug){
				Log.v(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"preparing httpGET...");
			}
			HttpURLConnection httpUrlConnection = null;
			String httpResponseBodyString = null;
			InputStream inputStream = null;
			
			int httpStatusCode = HTTP_STATUS_UNDEFINED;
			String httpAUTHrealm = null;
			SEM_HttpResponse httpRESPONSE = null;
			
			// http:// 로 시작하지 않는 경우 붙입니다.
			if(! URL.startsWith(HTTP_PREFIX_PROTOCOL)){
	    		URL = HTTP_PREFIX_PROTOCOL + URL ;
	        }
			
			try {
				//System.setProperty("http.keepAlive", "false");

				httpUrlConnection = (HttpURLConnection) (new URL( URL )).openConnection();
				httpUrlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
				httpUrlConnection.setReadTimeout(SOCKET_TIMEOUT);
				httpUrlConnection.setUseCaches(false);
				httpUrlConnection.setDoOutput(false); //true = POST..
				//httpUrlConnection.setRequestProperty("connection", "close");
				//httpUrlConnection.setDoInput(true);
				//httpUrlConnection.setDoOutput(true); //POST,,,
				httpUrlConnection.setRequestMethod("GET");//setDoOutput을 켜두면 POST..
				//httpUrlConnection.setConnectTimeout(1000);
				//httpUrlConnection.setInstanceFollowRedirects(true);// Redirect 따라가기: 안통했어
				//HttpURLConnection.setFollowRedirects(true); // Redirect 따라가기: 안통했어
				
				if(AuthKey != null){
					httpUrlConnection.setRequestProperty(HTTP_AUTH_HEADER, HTTP_AUTH_BASIC_PREFIX + AuthKey);//인증 값..
					if(isDebug){
						Log.i(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"establishing httpGET >> "+URL+" with Authentication Token");
					}
				}else{
					if(isDebug){
						Log.i(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"establishing httpGET >> "+URL+" without Auth");
					}
				}

				httpUrlConnection.connect();

				if(httpUrlConnection.getHeaderField(HTTP_HEADER_KEY_LOCATION) == null){
					httpStatusCode = httpUrlConnection.getResponseCode();
					if(isDebug){
						Log.i(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"got response from httpGET << " + httpStatusCode );
					}
					//httpUrlConnection.getHeaderFields();
					//httpUrlConnection.getheader
					if(httpStatusCode == HttpURLConnection.HTTP_OK){
						inputStream = httpUrlConnection.getInputStream();
						httpResponseBodyString = getContent(inputStream);
					}else{
						if(httpStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
							httpAUTHrealm = httpUrlConnection.getHeaderField(HTTP_HEADER_KEY_REALM);//httpRESPONSE.getFirstHeader(HEADER_KEY_REALM).getValue();

							if(isDebug){
								Log.v(TAG_LOG,"HTTP AuthRealm: "+httpAUTHrealm);
							}

							//static final, 그리고 클래스 분리를 해서 쓸모없게 되었당.
							//if(AuthKey != null)
							///{//여기서 DoAuthenticate 는 기기 판독을 한 다음, true로 쓰는거라서 토큰 유효 여부를 아는데 좋을거에요.
							//	setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
							//}
						}

						inputStream =  httpUrlConnection.getErrorStream();
						httpResponseBodyString = getContent(inputStream);
					}
				}else{
					if(isDebug){
		    			Log.i(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"redirection found from httpGET");
		    		}
					return httpRequestGET_HttpURLConn(httpUrlConnection.getHeaderField(HTTP_HEADER_KEY_LOCATION),AuthKey);
				}
			} catch(UnknownServiceException use){
				//HTTP 200 OK 이 아니면 이것이 나온다고 해요. 
				if(isDebug){
					Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"UnknownServiceException requesting HttpGET");
					use.printStackTrace();
				}
			} catch (UnknownHostException uhe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"UnknownHostException requesting HttpGET");
		    		uhe.printStackTrace();
		    	}
		    } catch (ClientProtocolException cpe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"ClientProtocolException requesting HttpGET");
		    		cpe.printStackTrace();
		    	}
		    } catch (IOException ioe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"IOException requesting HttpGET");
		    		ioe.printStackTrace();
		    	}
		    } catch (IllegalArgumentException iae) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"IllegalArgumentException requesting HttpGET");
		    		iae.printStackTrace();
		    	}
		    } finally {
				   	try {
				   		if( inputStream != null){
				   			inputStream.close();
				   		}
					} catch (IOException ioe) {
						if(isDebug){
							Log.w(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"IOException closing InputStream after HttpGET");
							ioe.printStackTrace();
						}
					}
			    	httpUrlConnection.disconnect();

			    	inputStream = null;
			    	httpUrlConnection = null;

			    	httpRESPONSE = new SEM_HttpResponse(httpStatusCode, httpAUTHrealm, httpResponseBodyString);
			    	
			    	httpStatusCode = HTTP_STATUS_UNDEFINED;
					httpAUTHrealm = null;
					httpResponseBodyString = null;
			    	if(isDebug){
						Log.v(TAG_LOG + TAG_TYPE_HTTPURLCONNECTION,"httpGET processed.");//+httpResponseBodyString);
					}
			   }
	        //HTTP GET 결과를 돌려줍니다.
			return httpRESPONSE;
		}
		
		/**
		 * 지정된 주소와 인증키로 HTTP GET 요청을 수행합니다.
		 * HTTP REDIRECTION도 처리합니다.(stack)
		 * GET 매개변수는 여기서 처리하지 않습니다.
		 * 
		 * @note org.apache.http.client.HttpClient 를 사용합니다.
		 * 
		 * @param URL 주소
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP GET Response 일부 (상태 코드, WWW-Authenticate, 내용)
		 */
		public static final SEM_HttpResponse httpRequestGET_HttpClient(String URL, String AuthKey){
			if(!isURLvalid(URL)){
				return new SEM_HttpResponse(HttpURLConnection.HTTP_BAD_REQUEST, null, null);
			}

			if(isDebug){
				//HTTP 작업을 어느 스레드에서 하고 있을까..
				Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"http operation on Thread: "+Thread.currentThread().getName());
			}
			if(isDebug){
				Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"preparing httpGET...");
			}
			HttpResponse httpClient_response = null;
			// Create a new HttpClient and GET Header & params
			HttpClient httpCLIENT = newHttpClientWithManagedParams();
	        HttpGet httpGET = null;
	        
	        int httpStatusCode = HTTP_STATUS_UNDEFINED;
	        String httpAUTHrealm = null;
	        String httpResponseBodyString = null;
	        SEM_HttpResponse httpRESPONSE = null;
	        
	        // http://로 시작하지 않는 경우 붙입니다.
	        if(! URL.startsWith(HTTP_PREFIX_PROTOCOL)){
	    		URL = HTTP_PREFIX_PROTOCOL + URL ;
	        }

	        try {
	        	httpGET = new HttpGet( URL );

	            //Header
	        	if(AuthKey != null){
			        //HTTP Basic 인증에 사용할 내용을 추가합니다.
			        //httpGET.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(Egg_ID, Egg_PW), DEFAULT_PAGE_ENCODING, false));
	        		
	        		//안드로이드 기본 브라우저가 이걸 기억하고 있을 땐 이게 영향을 주지 않는듯 하네요.
	        		//..도 사실이 아닙니다.
	        		
			        httpGET.addHeader(HTTP_AUTH_HEADER, HTTP_AUTH_BASIC_PREFIX + AuthKey);
			        if(isDebug){
		    			Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"establishing httpGET >> "+URL+ " with Authentication Token");
		    		}
	        	}else{
	        		if(isDebug){
		    			Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"establishing httpGET >> "+URL+ " without Auth");
		    		}
	        	}
		        // Execute HTTP Post Request
	        	
	        	//와이파이 중단했다가 다시 연결되는 순간 오류나서 꺼진다.. FC가 가리키는 곳은 여기.. 뭘까..
	        	//해결했다. initializeEgg에서 Null된 걸 확인하지 못해서 할 때마다 다시 초기화시켜줬더니된다.
	        	httpClient_response = httpCLIENT.execute(httpGET);
	        	
	        	//Redirect 를 만나면 더 수행해야하므로.. 한 번 더 하게 해야했어. ㅠㅠ
	        	if(httpClient_response.getFirstHeader(HTTP_HEADER_KEY_LOCATION) == null){
		        	httpStatusCode = httpClient_response.getStatusLine().getStatusCode();
		        	if(isDebug){
		    			Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"got response from httpGET << "+ httpStatusCode );
		    		}
		        	
		        	if( httpStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED){
			        	Header httpClient_realmHeader = httpClient_response.getFirstHeader(HTTP_HEADER_KEY_REALM);
			        	/*if(isDebug)
			        	{
			        		Header[] httpHeaders =  httpClient_response.getAllHeaders();
			        		for(Header currentHttpHeader : httpHeaders)//int i = 0; i < httpHeaders.length;i++)
			        		{
			        			Log.v(TAG_LOG+TAG_TYPE_HTTPCLIENT,"Header << name-"+currentHttpHeader.getName()+" field-"+currentHttpHeader.getValue());
			        			//Log.v(TAG_LOG,"Header: name-"+httpHeaders[i].getName()+" field-"+httpHeaders[i].getValue());
			        		}
			        	}*/
			        	if(httpClient_realmHeader != null){
			        		httpAUTHrealm = httpClient_realmHeader.getValue();
			        		if(isDebug){
								Log.v(TAG_LOG+TAG_TYPE_HTTPCLIENT,"HTTP AuthRealm << "+httpAUTHrealm);
			        			//Log.v(TAG_LOG,"HTTP AuthRealm contains: KWD_B2600 "+httpAUTHrealm.contains("KWD-B2600 Web"));
							}
			        		httpClient_realmHeader = null;
			        	}
			        	//if(AuthKey != null)
			        	//{//여기서 DoAuthenticate 는 기기 판독을 한 다음, true로 쓰는거라서 토큰 유효 여부를 아는데 좋을거에요.
			        	//	setConnectionStatus(EGG_CONNSTAT_LOGIN_FAILED);
			        	//}
		        	}
		        	httpResponseBodyString = EntityUtils.toString(httpClient_response.getEntity(),DEFAULT_PAGE_ENCODING); 
	        	}else{
	        		if(isDebug){
		    			Log.i(TAG_LOG + TAG_TYPE_HTTPCLIENT,"redirection found from httpGET");
		    		}
	        		return httpRequestGET_HttpClient(httpClient_response.getFirstHeader(HTTP_HEADER_KEY_LOCATION).getValue(),AuthKey);
	        	}
		        // get and parse HTTP Get Request

		    }catch (UnknownHostException uhe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"UnknownHostException requesting HttpGET");
		    		uhe.printStackTrace();
		    	}
		    }catch (ClientProtocolException cpe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"ClientProtocolException requesting HttpGET");
		    		cpe.printStackTrace();
		    	}
		    } catch (IOException ioe) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"IOException requesting HttpGET");
		    		ioe.printStackTrace();
		    	}
		    } catch (IllegalArgumentException iae) {
		    	if(isDebug){
		    		Log.w(TAG_LOG + TAG_TYPE_HTTPCLIENT,"IllegalArgumentException requesting HttpGET");
		    		iae.printStackTrace();
		    	}
	        } finally{
		        //httpClient.execute 수행이 끝나면 이와 관련된 리소스를 놓고 연결을 끊는다.
		        httpCLIENT.getConnectionManager().closeExpiredConnections();
		        httpClient_response = null;
		        
	        	//httpClient.execute 수행된것과는 관계없이 이와 관련된 리소스를 놓고 연결을 끊는다.
	        	httpCLIENT.getConnectionManager().shutdown();
	        	//nullify itself
	            httpCLIENT = null;
	            httpGET = null;
	        	
	        	httpRESPONSE = new SEM_HttpResponse(httpStatusCode,httpAUTHrealm,httpResponseBodyString);
	        	
	        	httpStatusCode = HTTP_STATUS_UNDEFINED;
	            httpAUTHrealm = null;
	            httpResponseBodyString = null;
	            
	            if(isDebug){
					Log.v(TAG_LOG + TAG_TYPE_HTTPCLIENT,"httpGET processed.");//+httpResponseBodyString);
				}
	        }

	        //HTTP GET 결과를 돌려줍니다.
	        return httpRESPONSE;
		}
		
		/**
		 * 지정된 주소와 인증키로 HTTP GET 요청을 수행합니다.
		 * HTTP REDIRECTION도 처리합니다.(stack)
		 * List NameValuePair 인 GET 매개변수들을 URL 뒤에 붙여줍니다.
		 * 
		 * @note org.apache.http.client.HttpClient 를 사용합니다.
		 * 
		 * @param URL 주소
		 * @param httpGETParameters GET 매개변수
		 * @param AuthKey HTTP Basic Authorization 키. (NULL 인 경우 무시)
		 * 
		 * @return HTTP GET Response (상태 코드, WWW-Authenticate, 내용)
		 */
		public static final SEM_HttpResponse httpRequestGET_HttpClient(String URL, List<NameValuePair> httpGETParameters, String AuthKey){
	    	/* GET에 parameter를 추가하기.
	    	protected String addLocationToUrl(String url){
	    	    if(!url.endsWith("?"))
	    	        url += "?";

	    	    List<NameValuePair> params = new LinkedList<NameValuePair>();

	    	    if (lat != 0.0 && lon != 0.0){
	    	        params.add(new BasicNameValuePair("lat", String.valueOf(lat)));
	    	        params.add(new BasicNameValuePair("lon", String.valueOf(lon)));
	    	    }

	    	    if (address != null && address.getPostalCode() != null)
	    	        params.add(new BasicNameValuePair("postalCode", address.getPostalCode()));
	    	    if (address != null && address.getCountryCode() != null)
	    	        params.add(new BasicNameValuePair("country",address.getCountryCode()));

	    	    params.add(new BasicNameValuePair("user", agent.uniqueId));

	    	    String paramString = URLEncodedUtils.format(params, DEFAULT_PAGE_ENCODING);

	    	    url += paramString;
	    	    return url;
	    	}*/
			
			if(isDebug){
				//무엇이 GET Param이 될지 확인.
	        	Log.v(TAG_LOG+TAG_TYPE_HTTPCLIENT,"the App will GET with parameters >> "+URLEncodedUtils.format(httpGETParameters, DEFAULT_PAGE_ENCODING));
	        }
			
			if(httpGETParameters != null){
				if( !URL.endsWith("?") ){
					URL += "?";
				}
		        URL += URLEncodedUtils.format(httpGETParameters, DEFAULT_PAGE_ENCODING);//HTTP_PREFIX_PROTOCOL+Egg_IP+"/admin/admin.asp");
		        httpGETParameters = null;
			}
			
	        return httpRequestGET_HttpClient(URL,AuthKey);
		}
		/*
		private HttpResponse httpRequestGET(String URL, List<NameValuePair> httpGETParameters)
		{
	    	/* GET에 parameter를 추가하기.
	    	protected String addLocationToUrl(String url){
	    	    if(!url.endsWith("?"))
	    	        url += "?";

	    	    List<NameValuePair> params = new LinkedList<NameValuePair>();

	    	    if (lat != 0.0 && lon != 0.0){
	    	        params.add(new BasicNameValuePair("lat", String.valueOf(lat)));
	    	        params.add(new BasicNameValuePair("lon", String.valueOf(lon)));
	    	    }

	    	    if (address != null && address.getPostalCode() != null)
	    	        params.add(new BasicNameValuePair("postalCode", address.getPostalCode()));
	    	    if (address != null && address.getCountryCode() != null)
	    	        params.add(new BasicNameValuePair("country",address.getCountryCode()));

	    	    params.add(new BasicNameValuePair("user", agent.uniqueId));

	    	    String paramString = URLEncodedUtils.format(params, DEFAULT_PAGE_ENCODING);

	    	    url += paramString;
	    	    return url;
	    	}* /
			
			if( !URL.endsWith("?") )
			{
				URL += "?";
			}
			
	        URLEncodedUtils.format(httpGETParameters, DEFAULT_PAGE_ENCODING);//HTTP_PREFIX_PROTOCOL+Egg_IP+"/admin/admin.asp");
	        
	        return httpRequestGET(URL,true);
		}*/
		/*
		private HttpResponse httpRequestGET(String URL, boolean DoAuthenticate)
		{
			//경고: HttpResponse를 보내주는건, 참조뿐..이어서
			//데이터를 쓰기도 전에 사라지는 증상이 있어 쓰지 않아요.
			HttpResponse httpRESPONSE = null;
			// Create a new HttpClient and GET Header & params
			HttpClient httpCLIENT = newHttpClientWithManagedParams();
	        HttpGet httpGET = new HttpGet(HTTP_PREFIX_PROTOCOL + URL );

	        //HTTP_PREFIX_PROTOCOL
	        if(isDebug)
	        {
	        	Log.v(TAG_LOG,"httpGet given URL: http:// "+URL);
	        }
	        try {
	            //Header
	        	if(DoAuthenticate)
	        	{
			        //HTTP Basic 인증에 사용할 내용을 추가합니다.
			        //httpGET.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(Egg_ID, Egg_PW), DEFAULT_PAGE_ENCODING, false));
			        httpGET.addHeader(HTTP_AUTH_HEADER, HTTP_AUTH_BASIC_PREFIX + Auth);
	        	}
		        // Execute HTTP Post Request
		        httpRESPONSE = httpCLIENT.execute(httpGET);
		        
	       
		        // get and parse HTTP Get Request
	            
		        //httpClient.execute 수행이 끝나면 이와 관련된 리소스를 놓고 연결을 끊는다.
		        httpCLIENT.getConnectionManager().closeExpiredConnections();
		    }catch (UnknownHostException uhe) {
		    	if(isDebug)
		    	{
		    		uhe.printStackTrace();
		    	}
		    }catch (ClientProtocolException cpe) {
		        
		    	if(isDebug)
		    	{
		    		cpe.printStackTrace();
		    	}
		    } catch (IOException ioe) {
		        
		    	if(isDebug)
		    	{
		    		ioe.printStackTrace();
		    	}
		    } catch (IllegalArgumentException iae) {
	        	
		    	if(isDebug)
		    	{
		    		iae.printStackTrace();
		    	}
	        } finally{
	        	//httpClient.execute 수행된것과는 관계없이 이와 관련된 리소스를 놓고 연결을 끊는다.
	        	httpCLIENT.getConnectionManager().shutdown();
	        }
	        
	        
	        //nullify itself
	        httpCLIENT = null;
	        httpGET = null;
	        
	        //HTTP GET 결과를 돌려줍니다.
	        return httpRESPONSE;//httpRESPONSE; //새 객체를 만들어서 주면 낫지 않을까..는 같았다.. 참조 주소만 주는것ㄱ ㅏㅌ아.. ㅠㅠ
		}
		*/
		private static final HttpClient newHttpClientWithManagedParams(){
			//시간초과설정..
			//http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java
			
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			// The default value is zero, that means the timeout is not used. 
			HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIMEOUT);
			// Set the default socket timeout (SO_TIMEOUT) 
			// in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);
			
			//HTTP 302  Moved Temporarily  따라가기. 효과가 없어서 빼자.
			//HttpClientParams.setRedirecting(httpParameters, true);
			//HttpConnectionParams.setRedirecting(params, false);
			
			return new DefaultHttpClient(httpParameters);

		}
		
		//stackoverflow.com/questions/10169705/ioexception-bufferedinputstream-is-closed-in-htc
/*		private static final String getContent(String url) throws IOException, IllegalStateException  {
		      return(new Scanner(new URL(url).openConnection().getInputStream()).useDelimiter("/z").next());
		}
		private static final String getContent(HttpResponse httpRESPONSE) throws IOException, IllegalStateException {
		      return(new Scanner(httpRESPONSE.getEntity().getContent()).useDelimiter("/z").next());
		}*/
		private static final String getContent(InputStream inputStream) throws IOException, IllegalStateException  {
		      return(new Scanner(inputStream).useDelimiter("/z").next());
		}

}
