package fm.last.android.scrobbler;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import fm.last.api.MD5;
import fm.last.api.RadioTrack;
import fm.last.api.Session;
import fm.last.util.UrlUtil;


/** This is super basic, we know, we'll improve as necessary
  * Also it is syncronous, which can be painful for sure, but generally, will not hang the GUI
  * So it'll do until 1.0.1...
  * @author <max@last.fm> 
  */
public class AudioscrobblerService extends Object 
{
	/** would be more useful at fm.last package level */
	private static class Log 
	{
		final private static String TAG = "Last.fm";

		public static void d( String s )
		{
			android.util.Log.d( TAG, s );
		}
		
		public static void i( String s )
		{
			android.util.Log.i( TAG, s );
		}

		public static void e( String s )
		{
			android.util.Log.e( TAG, s );
		}
		
		public static void e( Throwable e )
		{
			android.util.Log.e( TAG, e.toString() );
		}
	}	

	// used by handshake
	private String mUsername;
	private String mSessionKey;
	private String mSharedSecret;
	private String mApiKey;
	
	// responses from handshake
	private String mSessionId;
	private URL mNpUrl;
	private URL mSubsUrl;
	
	public void init( Session session, String api_key, String shared_secret )
	{
		mUsername = session.getName();
		mSessionKey = session.getKey();
		mSharedSecret = shared_secret;
		mApiKey = api_key;
	}
	
	private static String timestamp()
	{
		return new Long( System.currentTimeMillis() / 1000 ).toString();
	}
	
    private void handshake() throws IOException
    {
    	mSessionId = "";
    	
    	String timestamp = timestamp();
    	
    	Map<String, String> params = new HashMap<String, String>();
    	params.put( "hs", "true" );
    	params.put( "p", "1.2.1" );
    	params.put( "c", "ass" ); //FIXME
    	params.put( "v", "0.1" ); // FIXME
    	params.put( "u", mUsername );
    	params.put( "t", timestamp );
    	params.put( "a", MD5.getInstance().hash( mSharedSecret + timestamp ) );
    	params.put( "api_key", mApiKey );
    	params.put( "sk", mSessionKey );

		URL url = new URL( "http://post.audioscrobbler.com/?" + UrlUtil.buildQuery( params ) );
		
		String lines[] = UrlUtil.doGet( url ).split( "\n" );
		
		if (lines.length < 4) throw new IOException();
		
		mSessionId = lines[1];
		mNpUrl = new URL( lines[2] );
		mSubsUrl = new URL( lines[3] );
    }
    
    public void nowPlaying( RadioTrack t ) throws IOException
    {
    	if (mSessionId.length() == 0) handshake();
    	
		Map<String, String> params = new HashMap<String, String>();
		params.put( "s", mSessionKey );
		params.put( "a", t.getCreator() );
		params.put( "t", t.getTitle() );
		params.put( "b", t.getAlbum() );
		params.put( "l", t.getDuration() );
 							  
    	String response = UrlUtil.doPost( mNpUrl, UrlUtil.buildQuery( params ) );
    	
    	Log.i( response );
    	
    	if (!response.trim().equals( "OK" ))
    		handshake();
    }

    public void submit( RadioTrack t, long timestamp ) throws IOException
    {
    	submit( t, timestamp, "" );
    }
    
    /** valid ratings are, L for love, B for banned and S for skip, 
      * you can only specify one! 
      */
    public void submit( RadioTrack t, long timestamp, String ratingCharacter ) throws IOException
    {
    	if (mSessionId.length() == 0) handshake();
    	
		Map<String, String> params = new HashMap<String, String>();
		params.put( "s", mSessionKey );
		params.put( "a[0]", t.getCreator() );
		params.put( "t[0]", t.getTitle() );
		params.put( "b[0]", t.getAlbum() );
		params.put( "l[0]", t.getDuration() );
		params.put( "i[0]", timestamp() );
		params.put( "o[0]", "L" + t.getTrackAuth() );
		params.put( "r[0]", ratingCharacter );
		
		String response = UrlUtil.doPost( mNpUrl, UrlUtil.buildQuery( params ) );
		
		Log.i( response );

    	if (!response.trim().equals( "OK" ))
    		handshake();    
    }
}