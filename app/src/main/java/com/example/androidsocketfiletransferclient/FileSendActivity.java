package com.example.androidsocketfiletransferclient;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class FileSendActivity extends ActionBarActivity {

	EditText editTextAddress;
	Button buttonConnect;
	TextView textPort;
	TextView filePath;
	Button browseFile;
	private static final int REQUEST_PICK_FILE = 1;
	private File file;
	// Declare variables
	ProgressDialog mProgressDialog;
	private String TAG = getClass().getSimpleName();

	// PeerToPeer communication variable declear
	WifiP2pManager mManager;
	WifiP2pManager.Channel mChannel;
	BroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	private static String clientIPAddress=null;
	Handler handler;
	private ArrayList<WifiP2pDevice> mDeviceList = new ArrayList<WifiP2pDevice>();
	// Defined here ProgressDialogs
	private ProgressDialog mDiscoverDevicesDialog;
	private TextView mSearchedDeviceName,mConnectDevice;
	static final int SocketServerPORT = 8081;
	private Socket mSendFileSocket = null;
	private int mReceiverServerPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_send);

		editTextAddress = (EditText) findViewById(R.id.address);
		textPort = (TextView) findViewById(R.id.port);
		textPort.setText("port: " + SocketServerPORT);
		buttonConnect = (Button) findViewById(R.id.connect);
		mSearchedDeviceName=(TextView)findViewById(R.id.txt_searched_device);
		mConnectDevice=(TextView)findViewById(R.id.txt_connect_device);

		mConnectDevice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mDeviceList.size()>0) {
					connectDevice();
				}else{
					Toast.makeText(FileSendActivity.this, "No searched device...", Toast.LENGTH_SHORT).show();
				}
			}
		});

		handler=new Handler(){
			@Override
			public void handleMessage(Message msg) {
				String message=(String)msg.obj;
				//refresh textview
				Log.e(TAG, "START THREAD!!!!"+message);
				editTextAddress.setText(message);
				mConnectDevice.setText("Successfully Connected to device " + message);
				mDiscoverDevicesDialog.dismiss();
				mConnectDevice.setEnabled(true);
			}
		};


		filePath = (TextView)findViewById(R.id.file_path);
		browseFile = (Button)findViewById(R.id.select);
		browseFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(FileSendActivity.this, FilePicker.class);
				startActivityForResult(intent, REQUEST_PICK_FILE);
			}
		});
		
		buttonConnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String ip = editTextAddress.getText().toString();
				if (file.length() > 0) {
					new UploadFile(ip).execute(file.getName());
				} else {
					Toast.makeText(FileSendActivity.this, "Select file ", Toast.LENGTH_SHORT).show();
				}

			}
		});


		// Get Peer2Peer WifiManager instance here
		getWifiManagerInstance();

		// Start discovery to nearby devices using peer2peer
		onDiscover();

		ServerSocketThread serverSocketThread=new ServerSocketThread();
		serverSocketThread.start();


	}

	// This method declear all Peer2Peer communication stuff here!!!
	private void getWifiManagerInstance(){

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);
		mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	}

	// This method start to discovery to all nearby devices
	private void onDiscover(){

		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.e(TAG, "onSuccess to peers");
				mDiscoverDevicesDialog=new ProgressDialog(FileSendActivity.this);
				mDiscoverDevicesDialog.setMessage("Searching for device");
				mDiscoverDevicesDialog.show();

			}

			@Override
			public void onFailure(int reasonCode) {
				Log.e(TAG, "onFailure to peers");
			}
		});
	}

	// This method try to connect searched device
	   private void connectDevice() {

		Log.e(TAG, "" + mDeviceList.get(0));
		WifiP2pDevice device = mDeviceList.get(0);


		if (device == null) {
			return;
		}

		final WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;
		config.wps.setup = WpsInfo.PBC;
		config.groupOwnerIntent=15;

		   mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {

				Toast.makeText(FileSendActivity.this, "start to connecting", Toast.LENGTH_SHORT).show();

			}
			@Override
			public void onFailure(int reason) {
				Log.e(TAG,"Failed to connected");
			}
		});
	}


	/**
	 *
	 * Defined here WiFiDirectBroadcastReceiver to call listener for new connection
	 * to get information about group owner and client device
	 *
	 */
	/**
	 * Created by arvind on 21/7/16.
	 */
	public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

		private WifiP2pManager mManager;
		private WifiP2pManager.Channel mChannel;
		private FileSendActivity mActivity;
		WifiP2pManager.PeerListListener myPeerListListener;

		public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
										   FileSendActivity activity) {
			super();
			this.mManager = manager;
			this.mChannel = channel;
			this.mActivity = activity;

		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				// Check to see if Wi-Fi is enabled and notify appropriate activity
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					// Wifi P2P is enabled
					Log.e(TAG, "WiFi P2P is enabled");
				} else {
					// Wi-Fi P2P is not enabled
					Log.e(TAG, "WiFi P2P is not enabled");
				}
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {


				//mDeviceList.clear();
				if (mManager != null) {
					mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
						@Override
						public void onPeersAvailable(WifiP2pDeviceList peers) {
							if (peers != null) {

								try {
									mDeviceList.addAll(peers.getDeviceList());
									mDiscoverDevicesDialog.dismiss();
									Log.e(TAG, "Discovered peers " + mDeviceList.get(0).deviceName);
									mSearchedDeviceName.setText(mDeviceList.get(0).deviceName);
								}catch (Exception e){
									e.printStackTrace();
								}
							}
						}
					});
				}


			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
				// Respond to new connection or disconnections
				if (mManager == null) {
					return;
				}

				NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				if (networkInfo.isConnected()) {
					// We are connected with the other device, request connection
					// info to find group owner IP
					Log.e(TAG, "device is connected successfully now!!!!!!!!!!!!");
					mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
						@Override
						public void onConnectionInfoAvailable(final WifiP2pInfo info) {

							new Thread(new Runnable() {
								@Override
								public void run() {
									// InetAddress from WifiP2pInfo struct.
									InetAddress groupOwnerAddress = info.groupOwnerAddress;

									try {
										// After the group negotiation, we can determine the group owner.
										if (info.isGroupOwner) {
											Log.e(TAG, "this is owner " + groupOwnerAddress.getHostAddress());
										} else {
											Log.e("not a owner ....", "..........");
											//String mPeerIP = groupOwnerAddress.getHostAddress();
											//sendIPAddressSocketThread = new SendIPAddress(mPeerIP);
											//sendIPAddressSocketThread.start();

										}
									}catch (Exception e){
										e.printStackTrace();
									}}
							}).start();
						}});}}}
	}


	/* register the broadcast receiver with the intent values to be matched */
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mReceiver, mIntentFilter);
	}
	/* unregister the broadcast receiver */
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);

		mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.e(TAG, "Removed group successfully ");
			}

			@Override
			public void onFailure(int reason) {
				Log.e(TAG, "Removed group failed ");
			}
		});

	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if(resultCode == RESULT_OK) {

			switch(requestCode) {

				case REQUEST_PICK_FILE:

					if(data.hasExtra(FilePicker.EXTRA_FILE_PATH)) {

						file = new File(data.getStringExtra("file_path"));
						filePath.setText(file.getName().toString());
					}
					break;
			}
		}
	}



	private class UploadFile extends AsyncTask<String,Integer,String>{


		String hostAddress;

		InputStream in;
		OutputStream out;

		public UploadFile(String ipAddress) {
			hostAddress=ipAddress;
			Log.e(TAG,"IP ADDRESS "+hostAddress);

		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Create progress dialog
			mProgressDialog = new ProgressDialog(FileSendActivity.this);
			// Set your progress dialog Title
			// Set your progress dialog Message
			mProgressDialog.setMessage("Sending File, Please Wait!");
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			// Show progress dialog
			mProgressDialog.show();
		}

		@Override
		protected String doInBackground(String... params) {

			try {
				// Get the size of the file
				// Get the size of the file
                mSendFileSocket=null;


				try {

					Log.e(TAG,"Receiver port "+mReceiverServerPort);
					mSendFileSocket = new Socket(hostAddress, mReceiverServerPort);
				}catch (SocketException se){
					Log.e(TAG,se.getMessage());
				}


				byte[] bytes = new byte[16 * 1024];
				in = new FileInputStream(file);
				out = mSendFileSocket.getOutputStream();


				// set file name in bytes
				DataOutputStream dos=new DataOutputStream(out);
				//Sending file name and file size to the server
				dos.writeUTF(file.getName()+"&"+file.length());
				dos.writeLong(bytes.length);
				dos.write(bytes, 0, bytes.length);
				dos.flush();
				int count;
				long total = 0;
				while ((count = in.read(bytes)) > 0) {
					total += count;
                  // Publish the progress
					publishProgress((int) (total * 100 / file.length()));
					out.write(bytes, 0, count);
				}

				out.close();
				in.close();
				mSendFileSocket.close();

			} catch (IOException e) {

				Log.e(TAG,e.getMessage());
				e.printStackTrace();
				final String eMsg = "Something wrong: " + e.getMessage();
				FileSendActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(FileSendActivity.this, eMsg, Toast.LENGTH_LONG).show();
					}});

			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// Update the progress dialog
			mProgressDialog.setProgress(progress[0]);
		}


		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			mProgressDialog.dismiss();

		}
	}



	public class ServerSocketThread extends Thread {

		InputStream in=null;
		Socket socket=null;
		ServerSocket serverSocket=null;

		@Override
		public void run() {
			try {


				try {
					serverSocket = new ServerSocket(SocketServerPORT); // <-- create an unbound socket first


				} catch (Exception ex) {

					Log.e(TAG,ex.getMessage());
				}


				try {
					while (true) {
						socket = serverSocket.accept();
						in = socket.getInputStream();
						DataInputStream clientData = new DataInputStream(in);
						final String fileName = clientData.readUTF();
						Log.e(TAG,"FILE NAME "+fileName);
						String[] addressArray=fileName.split("&");
						clientIPAddress=addressArray[0];
						mReceiverServerPort=Integer.parseInt(addressArray[1]);
						Log.e(TAG,"SERVER PORT "+mReceiverServerPort);


						if(clientIPAddress.contains("192")){
							Log.e(TAG, "Client ip address...... " + clientIPAddress);
							Message message = handler.obtainMessage();
							message.obj = clientIPAddress;
							handler.sendMessage(message);
						}else{
							Log.e(TAG, "received message from ...... " + clientIPAddress);
							mProgressDialog.dismiss();
						}

						Log.e(TAG, "Device connected to ip address " + clientIPAddress);

						in.close();
						socket.close();


					}

				} catch (Exception ex) {
					Log.e(TAG,ex.getMessage());
				}



			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG,e.getMessage());
			}

		}

	}



}
