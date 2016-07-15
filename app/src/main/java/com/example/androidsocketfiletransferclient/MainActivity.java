package com.example.androidsocketfiletransferclient;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends ActionBarActivity {

	EditText editTextAddress;
	Button buttonConnect;
	TextView textPort;

	static final int SocketServerPORT = 8080;

	TextView filePath;
	Button browseFile;
	private static final int REQUEST_PICK_FILE = 1;

	private File file;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		editTextAddress = (EditText) findViewById(R.id.address);
		textPort = (TextView) findViewById(R.id.port);
		textPort.setText("port: " + SocketServerPORT);
		buttonConnect = (Button) findViewById(R.id.connect);


		filePath = (TextView)findViewById(R.id.file_path);

		browseFile = (Button)findViewById(R.id.select);
		browseFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(MainActivity.this, FilePicker.class);
				startActivityForResult(intent, REQUEST_PICK_FILE);
			}
		});
		
		buttonConnect.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				ClientRxThread clientRxThread = 
					new ClientRxThread(
						editTextAddress.getText().toString(), 
						SocketServerPORT);
				
				clientRxThread.start();
			}});
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if(resultCode == RESULT_OK) {

			switch(requestCode) {

				case REQUEST_PICK_FILE:

					if(data.hasExtra(FilePicker.EXTRA_FILE_PATH)) {

						file = new File(data.getStringExtra("file_path"));
						System.out.println("file size "+file.length());
						filePath.setText(file.getPath());
					}
					break;
			}
		}
	}

	private class ClientRxThread extends Thread {
		String dstAddress;
		int dstPort;

		ClientRxThread(String address, int port) {
			dstAddress = address;
			dstPort = port;
		}

		@Override
		public void run() {
			Socket socket = null;
			
			try {
//				socket = new Socket(dstAddress, dstPort);


				/*String yourFilePath = Environment.getExternalStorageDirectory() + "/Android/" + "recording.mp3";
				System.out.println("file path " + yourFilePath);
*/
				//File file = new File( yourFilePath );
				System.out.println("file size " + file.length());

				try {
					MediaPlayer mp = new MediaPlayer();
					mp.setDataSource(MainActivity.this, Uri.fromFile(file));
					mp.prepare();
					mp.start();

				}catch (Exception e){
					e.printStackTrace();
				}




//				byte[] bytes = new byte[(int) file.length()];
//				InputStream is = socket.getInputStream();
//			    FileOutputStream fos = new FileOutputStream(file);
//			    BufferedOutputStream bos = new BufferedOutputStream(fos);
//			    int bytesRead = is.read(bytes, 0, bytes.length);
//			    bos.write(bytes, 0, bytesRead);
//			    bos.close();
//			    socket.close();
//
//			    MainActivity.this.runOnUiThread(new Runnable() {
//
//					@Override
//					public void run() {
//						Toast.makeText(MainActivity.this,
//								"Finished",
//								Toast.LENGTH_LONG).show();
//					}});


//				socket = new Socket(host, 4444);

//				File file = new File("M:\\test.xml");
				// Get the size of the file
				socket = new Socket(dstAddress, 8081);
				long length = file.length();
				byte[] bytes = new byte[16 * 1024];
				InputStream in = new FileInputStream(file);
				OutputStream out = socket.getOutputStream();

				int count;
				while ((count = in.read(bytes)) > 0) {
					out.write(bytes, 0, count);
				}

				out.close();
				in.close();
				socket.close();
				
			} catch (IOException e) {

				e.printStackTrace();
				
				final String eMsg = "Something wrong: " + e.getMessage();
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(MainActivity.this, 
								eMsg, 
								Toast.LENGTH_LONG).show();
					}});
				
			} finally {
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
