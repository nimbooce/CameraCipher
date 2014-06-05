package info.guardianproject.iocipherexample;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.VirtualFileSystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

public class GalleryActivity extends Activity {
	private final static String TAG = "Gallery";

	private List<String> item = null;
	private List<String> path = null;
	private String[] items;
	private String dbFile;
	private String root = "/";
	private VirtualFileSystem vfs;
	
    private GestureDetector mGestureDetector;
    
    private List<Card> mCards;
    private CardScrollView mCardScrollView;



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/myfiles.db";
		
        mGestureDetector = createGestureDetector(this);
        
        
	}
	
	
	 @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.main, menu);
	        return true;
	    }
	
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        // Handle item selection. Menu items typically start another
	        // activity, start a service, or broadcast another intent.
	        switch (item.getItemId()) {
	            case R.id.menu_camera:
	            	startPhotoCamera();
	                return true;
	            case R.id.menu_video:
	            	startPhotoCamera();
	                return true;
	            default:
	                return super.onOptionsItemSelected(item);
	        }
	    }
	    
	 private class ExampleCardScrollAdapter extends CardScrollAdapter {

	        @Override
	        public int getPosition(Object item) {
	            return mCards.indexOf(item);
	        }

	        @Override
	        public int getCount() {
	            return mCards.size();
	        }

	        @Override
	        public Object getItem(int position) {
	            return mCards.get(position);
	        }

	        @Override
	        public int getViewTypeCount() {
	            return Card.getViewTypeCount();
	        }

	        @Override
	        public int getItemViewType(int position){
	            return mCards.get(position).getItemViewType();
	        }

	        @Override
	        public View getView(int position, View convertView,
	                ViewGroup parent) {
	            return  mCards.get(position).getView(convertView, parent);
	        }
	    }

	protected void onResume() {
		super.onResume();
		vfs = new VirtualFileSystem(dbFile);
		// TODO don't use a hard-coded password! prompt for the password
		vfs.mount("my fake password");
		getFileList(root);
		
		if (mCardScrollView == null)
		{
			mCardScrollView = new CardScrollView(this);
			
			mCardScrollView.setOnItemClickListener(new OnItemClickListener()
			{

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					final File file = new File(path.get(arg2));
        			final Uri uri = Uri.parse(IOCipherContentProvider.FILES_URI + file.getName());
        			showItem(uri, file);					
				}
				
			});
								
					
						
	        ExampleCardScrollAdapter adapter = new ExampleCardScrollAdapter();
	        mCardScrollView.setAdapter(adapter);
	        mCardScrollView.activate();
	        setContentView(mCardScrollView);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		try
		{
			vfs.mount("XXXXXXXXXXXXXXX"); //this ensures the old password is cleared
		}catch(IllegalArgumentException iae){}
		//vfs.unmount();
	}
	

	
	
	// To make listview for the list of file
	public void getFileList(String dirPath) {


		item = new ArrayList<String>();
		path = new ArrayList<String>();
        mCards = new ArrayList<Card>();

		File file = new File(dirPath);
		File[] files = file.listFiles();

		if (!dirPath.equals(root)) {
			item.add(root);
			path.add(root);// to get back to main list

			item.add("..");
			path.add(file.getParent()); // back one level
		}

		for (int i = 0; i < files.length; i++) {

			File fileItem = files[i];
			path.add(fileItem.getPath());
			if (fileItem.isDirectory()) {
				// input name directory to array list
				item.add("[" + fileItem.getName() + "]");
			} else {
				// input name file to array list
				item.add(fileItem.getName());
				
				mCards.add(makeCard(fileItem));
				
			}
		}
		// declare array with specific number of items
		items = new String[item.size()];
		// send data arraylist(item) to array(items)
		item.toArray(items);
	}

	
	
	private void showItem (Uri uri, File file)
	{
		try {
			String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			if (fileExtension.equals("ts"))
				mimeType = "application/mpeg*";
			
			if (mimeType == null)
				mimeType = "application/octet-stream";

			if (mimeType.startsWith("image"))
			{
				 Intent intent = new Intent(GalleryActivity.this,ImageViewerActivity.class);
				  intent.setType(mimeType);
				  intent.putExtra("vfs", file.getAbsolutePath());
				  startActivity(intent);	
			}
			else if (fileExtension.equals("ts") || mimeType.startsWith("video"))
			{
				//shareVideoUsingStream(file, mimeType);
			}
			else {
	          Intent intent = new Intent(Intent.ACTION_VIEW);													
			  intent.setDataAndType(uri, mimeType);
			  startActivity(intent);
			}
			 
			
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "No relevant Activity found", e);
		}
	}
	
	private ServerSocket ss = null;
	private boolean keepServerRunning = false;
	
	private void shareVideoUsingStream(final File f, final String mimeType)
	{
		
		final int port = 8080;
		keepServerRunning = false;
		
		final String shareMimeType = "application/mpegts";
		
		try
		{
			if (ss != null)
				ss.close();
		}
		catch (Exception e){}
		
		new Thread ()
		{
			public void run ()
			{
				try {
					
					ss = new ServerSocket(port);
					Socket socket = ss.accept();
					
					StringBuilder sb = new StringBuilder();
					sb.append( "HTTP/1.1 200\r\n");
					sb.append( "Content-Type: " + shareMimeType + "\r\n");
					sb.append( "Content-Length: " + f.length() + "\r\n\r\n" );
					
					BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
					
					bos.write(sb.toString().getBytes());
					
					int len = -1;
					FileInputStream fis = new FileInputStream(f);
					
					int idx = 0;
					
					byte[] b = new byte[8096];
					while ((len = fis.read(b)) != -1)
					{
						bos.write(b,0,len);
						idx+=len;
						Log.d(TAG,"sharing via stream: " + idx);
					}

					fis.close();
					bos.flush();
					bos.close();
					
					socket.close();
					ss.close();
					ss = null;
					
				} catch (IOException e) {
					Log.d("ServerShare","web share error",e);
				}
			}
		}.start();
		
		Uri uri = Uri.parse("http://localhost:" + port + f.getAbsolutePath());
		
		Intent intent = new Intent(GalleryActivity.this,VideoViewerActivity.class);
												
		intent.setDataAndType(uri, mimeType);
		startActivity(intent);
		  
		
	}
	
	
	public java.io.File exportToDisk (File fileIn)
	{
		java.io.File fileOut = null;
		
		try {
			
			fileOut = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileIn.getName());
			FileInputStream fis = new FileInputStream(fileIn);		
			java.io.FileOutputStream fos = new java.io.FileOutputStream(fileOut);
			
			byte[] b = new byte[4096];
			int len;
			while ((len = fis.read(b))!=-1)
			{
				fos.write(b, 0, len);
			}
			
			fis.close();
			fos.flush();
			fos.close();
			
		} catch (IOException e) {
			Log.d(TAG,"error exporting",e);
		}
		
		return fileOut;
		
	}
	
	public Card makeCard (File f)
	{
		String mimeType = null;

		String[] tokens = f.getName().split("\\.(?=[^\\.]+$)");
		
		if (tokens.length > 1)
			mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.getName().split("\\.")[1]);
		
		if (mimeType == null)
			mimeType = "application/octet-stream";

		Card card = new Card(this);
	        card.setText(f.getName());
	        card.setFootnote("Size: " + f.length());
	        card.setImageLayout(Card.ImageLayout.LEFT);
	        if (mimeType.startsWith("image")){
				
				try
				{
					card.addImage(getPreview(f));
				}
				catch (Exception e)
				{
					Log.d(TAG,"error showing thumbnail",e);
					card.addImage(R.drawable.text);	
				}
			}
			else
			{
				card.addImage(R.drawable.text);
			}
	        
	    return card;
	}

	
	private final static int THUMB_DIV = 8;
	
	private Bitmap getPreview(File fileImage) throws FileNotFoundException {

		
	    BitmapFactory.Options bounds = new BitmapFactory.Options();
	    
	    bounds.inJustDecodeBounds = true;
	    BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(fileImage)), null, bounds);
	    
	    if ((bounds.outWidth == -1) || (bounds.outHeight == -1))
	        return null;

//	    opts.inSampleSize = 4;//originalSize / THUMBNAIL_SIZE;	 
	    
	    Bitmap b = BitmapFactory.decodeStream(new BufferedInputStream(new FileInputStream(fileImage)), null, null);
	    
	    return Bitmap.createScaledBitmap(b, bounds.outWidth/THUMB_DIV, bounds.outWidth/THUMB_DIV, false);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		if (requestCode == 1)
			getFileList(root);
	}
	
	private void startVideoCamera ()
	{

    	Intent intent = new Intent(GalleryActivity.this,VideoRecorderActivity.class);
    	intent.putExtra("basepath", "/");
    	startActivityForResult(intent, 1);
    	
	}
	
	private void startPhotoCamera ()
	{
		Intent intent = new Intent(GalleryActivity.this,SecureSelfieActivity.class);
    	intent.putExtra("basepath", "/");
    	startActivityForResult(intent, 1);
	}
	
	 private GestureDetector createGestureDetector(Context context) {
		    GestureDetector gestureDetector = new GestureDetector(context);
		        //Create a base listener for generic gestures
		        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
		            @Override
		            public boolean onGesture(Gesture gesture) {
		                if (gesture == Gesture.TAP) {

		                    return true;
		                } else if (gesture == Gesture.TWO_TAP) {
		                	
		                	openOptionsMenu();
		                    return true;
		                } else if (gesture == Gesture.SWIPE_RIGHT) {
		                    

		                	
		                    return true;
		                } else if (gesture == Gesture.SWIPE_LEFT) {
		                	
		                	
		                    return true;
		                }
		                return false;
		            }
		        });
		        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
		            @Override
		            public void onFingerCountChanged(int previousCount, int currentCount) {
		              // do something on finger count changes
		            }
		        });
		        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
		            @Override
		            public boolean onScroll(float displacement, float delta, float velocity) {
		                // do something on scrolling
		            	return false;
		            }
		        });
		        return gestureDetector;
		    }
	 
	 /*
	     * Send generic motion events to the gesture detector
	     */
	    @Override
	    public boolean onGenericMotionEvent(MotionEvent event) {
	        if (mGestureDetector != null) {
	            return mGestureDetector.onMotionEvent(event);
	        }
	        return false;
	    }
	
}
