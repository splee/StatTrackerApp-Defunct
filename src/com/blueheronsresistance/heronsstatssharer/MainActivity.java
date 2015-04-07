package com.blueheronsresistance.heronsstatssharer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	ProgressDialog dialog = null;
    
    String upLoadServerUri;
    TextView messageText;
    Button uploadButton;
    Button closeButton;
    int serverResponseCode = 0;
    Uri imageUri ;
    String fileName;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	    // Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    
	    upLoadServerUri = getString(R.string.upload_uri);

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("image/png".equals(type)) {
	            handleSendImage(intent); // Handle single image being sent
	        }
	    } else {
	        // Handle other intents, such as being started from the home screen
	    }
	    closeButton = (Button) findViewById(R.id.button2);
	    closeButton.setOnClickListener(new OnClickListener() {

	        @Override
	        public void onClick(View v) {
	            // TODO Auto-generated method stub
	            finish();
	            System.exit(0);
	        }
	    });
	    
	}
	
	void handleSendImage(Intent intent) {
	    imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	    ParcelFileDescriptor mInputPFD;
	    messageText = (TextView) findViewById(R.id.textView1);
	    if (imageUri != null) {
	        // Update UI to reflect image being shared
	    	/*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            try {
				/*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                mInputPFD = getContentResolver().openFileDescriptor(imageUri, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MainActivity", "File not found.");
                return;
            }
            // Get a regular file descriptor for the file
            FileDescriptor fd = mInputPFD.getFileDescriptor();
            
            fileName = this.getRealPathFromURI(imageUri);
            ImageView iv = (ImageView) findViewById (R.id.imageView1);
            iv.setImageURI(imageUri);
            
            uploadButton = (Button)findViewById(R.id.button1);
             
            uploadButton.setOnClickListener(new OnClickListener() {           
                @Override
                public void onClick(View v) {
                     
                    dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                     
                    new Thread(new Runnable() {
                            public void run() {
                                 runOnUiThread(new Runnable() {
                                        public void run() {
                                            messageText.setText("uploading started.....");
                                        }
                                    });                     
                               
                                 uploadFile(fileName);
                                                          
                            }
                          }).start();       
                    }
                });

	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    public int uploadFile(String uploadFileName) {
          
    	final String fileName = uploadFileName;
	  
	    HttpURLConnection conn = null;
	    DataOutputStream dos = null; 
	    String lineEnd = "\r\n";
	    String twoHyphens = "--";
	    String boundary = "*****";
	    int bytesRead, bytesAvailable, bufferSize;
	    byte[] buffer;
	    int maxBufferSize = 1 * 1024 * 1024;
	    File sourceFile = new File(fileName);

        if (!sourceFile.isFile()) {
               
               dialog.dismiss();
                
               Log.e("uploadFile", "Source File does not exist: "
                                   +uploadFileName);
                
               runOnUiThread(new Runnable() {
                   public void run() {
                       messageText.setText("Source File does not exist: "
                               +fileName);
                   }
               });
                
               return 0;
            
          }
          else
          {
               try {
                    
                     // open a URL connection to the Servlet
                   FileInputStream fileInputStream = new FileInputStream(sourceFile);
                   URL url = new URL(upLoadServerUri);
                    
                   // Open a HTTP  connection to  the URL
                   conn = (HttpURLConnection) url.openConnection();
                   conn.setDoInput(true); // Allow Inputs
                   conn.setDoOutput(true); // Allow Outputs
                   conn.setUseCaches(false); // Don't use a Cached Copy
                   conn.setRequestMethod("POST");
                   conn.setRequestProperty("Connection", "Keep-Alive");
                   conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                   conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                   conn.setRequestProperty("uploaded_file", fileName);
                    
                   dos = new DataOutputStream(conn.getOutputStream());
          
                   dos.writeBytes(twoHyphens + boundary + lineEnd);
                   dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                             + fileName + "\"" + lineEnd);
                    
                   dos.writeBytes(lineEnd);
          
                   // create a buffer of  maximum size
                   bytesAvailable = fileInputStream.available();
          
                   bufferSize = Math.min(bytesAvailable, maxBufferSize);
                   buffer = new byte[bufferSize];
          
                   // read file and write it into form...
                   bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
                      
                   while (bytesRead > 0) {
                        
                     dos.write(buffer, 0, bufferSize);
                     bytesAvailable = fileInputStream.available();
                     bufferSize = Math.min(bytesAvailable, maxBufferSize);
                     bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
                      
                    }
          
                   // send multipart form data necessary after file data...
                   dos.writeBytes(lineEnd);
                   dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
          
                   // Responses from the server (code and message)
                   serverResponseCode = conn.getResponseCode();
                   String serverResponseMessage = conn.getResponseMessage();
                     
                   Log.i("uploadFile", "HTTP Response is : "
                           + serverResponseMessage + ": " + serverResponseCode);
                    
                   if(serverResponseCode == 200){
                        
                       runOnUiThread(new Runnable() {
                            public void run() {
                                 
                                String msg = "File Upload Complete.";
                                 
                                messageText.setText(msg);
                                Toast.makeText(MainActivity.this, "File Upload Complete.",
                                             Toast.LENGTH_SHORT).show();
                                uploadButton.setText("File Uploaded.");
                                uploadButton.setClickable(false);
                                uploadButton.setEnabled(false);
                                closeButton.setText("Exit");
                            }
                        });               
                   }   
                    
                   //close the streams //
                   fileInputStream.close();
                   dos.flush();
                   dos.close();
                     
              } catch (MalformedURLException ex) {
                   
                  dialog.dismiss(); 
                  ex.printStackTrace();
                   
                  runOnUiThread(new Runnable() {
                      public void run() {
                          messageText.setText("MalformedURLException Exception : check script url.");
                          Toast.makeText(MainActivity.this, "MalformedURLException",
                                                              Toast.LENGTH_SHORT).show();
                      }
                  });
                   
                  Log.e("Upload file to server", "error: " + ex.getMessage(), ex); 
              } catch (Exception e) {
                   
                  dialog.dismiss(); 
                  e.printStackTrace();
                   
                  runOnUiThread(new Runnable() {
                      public void run() {
                          messageText.setText("Got Exception : see logcat ");
                          Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                  Toast.LENGTH_SHORT).show();
                      }
                  });
                  Log.e("Upload file to server Exception", "Exception : "
                                                   + e.getMessage(), e); 
              }
              dialog.dismiss();      
              return serverResponseCode;
               
           } // End else block
        } 
  //Convert the image URI to the direct file system path of the image file
    @SuppressWarnings("deprecation")
	public String getRealPathFromURI(Uri contentUri) {
        String [] proj={MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = managedQuery( contentUri,
        proj,     // Which columns to return
        null,     // WHERE clause; which rows to return (all rows)
        null,     // WHERE clause selection arguments (none)
        null);     // Order-by clause (ascending by name)
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
