package com.example.androidsocketfiletransferclient;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;

public class FileReceivedActivity extends ActionBarActivity {

	TextView infoIp, infoPort,fileNameTextView,progressbarStatusValueTextView;
	Button btnCancel;
	ProgressBar mProgressBar;
	RelativeLayout layoutProgressStatus;

	static final int SocketServerPORT = 8081;
	//ServerSocket serverSocket;

	ServerSocketThread serverSocketThread;
	private ProgressDialog mProgressDialog;
	long totalSize=0;
	boolean isStopDownloading;


	// PeerToPeer communication variable declear
	WifiP2pManager mManager;
	WifiP2pManager.Channel mChannel;
	BroadcastReceiver mReceiver;
	IntentFilter mIntentFilter;
	private String TAG = getClass().getSimpleName();
	private TextView mSearchedDevice;
	private ArrayList<WifiP2pDevice> mDeviceList = new ArrayList<WifiP2pDevice>();
	private SendIPAddress sendIPAddressSocketThread;
	private String mIPAddress;
	static Handler handler = new Handler(Looper.getMainLooper());
	private ServerSocket serverSocket = null;
	private Socket mSocket=null;
	private int mLocalPortNumber;
	private boolean isListening;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_received);

		infoIp = (TextView) findViewById(R.id.infoip);
		infoPort = (TextView) findViewById(R.id.infoport);
		mSearchedDevice=(TextView)findViewById(R.id.txt_device);

		layoutProgressStatus=(RelativeLayout)findViewById(R.id.layout_progress_status);
		fileNameTextView=(TextView)findViewById(R.id.text_file_name);
		progressbarStatusValueTextView=(TextView)findViewById(R.id.text_progress_value);
		mProgressBar=(ProgressBar)findViewById(R.id.dialogProgressBar);
		btnCancel=(Button)findViewById(R.id.btn_cancel);
		/*btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (socket != null) {
					isStopDownloading = true;
					new CancelDownloading(String.valueOf(socket.getInetAddress()), SocketServerPORT).execute("run");
				}
			}
		});*/

		infoIp.setText(getIpAddress());


		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);
		mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



		handler=new Handler(){
			@Override
			public void handleMessage(Message msg) {
				String message=(String)msg.obj;
				//refresh textview
				Log.e(TAG, "START THREAD!!!!"+message);
				// Create progress dialog
				//mProgressDialog = new ProgressDialog(FileReceivedActivity.this);
				// Set your progress dialog Title
				layoutProgressStatus.setVisibility(View.VISIBLE);
				// Set your progress dialog Message
				fileNameTextView.setText(message);
				mProgressBar.setIndeterminate(false);
				mProgressBar.setMax(100);
				// Show progress dialog
				mProgressBar.setVisibility(View.VISIBLE);
			}
		};


		// This method start to discover near by devices
		onDiscover();

		serverSocketThread = new ServerSocketThread();
		serverSocketThread.start();
	}


	private void onDiscover() {
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Toast.makeText(FileReceivedActivity.this,"Start to discover near by devices ",Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Start to discover ");

			}
			@Override
			public void onFailure(int reasonCode) {
				Log.e(TAG, "onFailur to peers");
			}
		});


	}


	/**

	 * Created by arvind on 21/7/16.
	 */
	public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

		private WifiP2pManager mManager;
		private WifiP2pManager.Channel mChannel;
		private FileReceivedActivity mActivity;
		WifiP2pManager.PeerListListener myPeerListListener;

		public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
										   FileReceivedActivity activity) {
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

				mDeviceList.clear();
				if (mManager != null) {
					mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
						@Override
						public void onPeersAvailable(WifiP2pDeviceList peers) {
							if (peers != null) {

								if(peers.getDeviceList().size()>0) {
									mDeviceList.addAll(peers.getDeviceList());
									Log.e(TAG, "Discovered peers " + mDeviceList.size());
									mSearchedDevice.setText("Searched Device " + mDeviceList.get(0).deviceName);
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
									Log.e(TAG,"Group owner address..."+groupOwnerAddress);
									try {
										// After the group negotiation, we can determine the group owner.
										if (info.isGroupOwner) {
											Log.e(TAG, "this is owner " + groupOwnerAddress.getHostAddress());

										} else {
											Log.e("not a owner ....", "..........");
											String mPeerIP = groupOwnerAddress.getHostAddress();
											sendIPAddressSocketThread = new SendIPAddress(mPeerIP);
											sendIPAddressSocketThread.start();

										}
									}catch (Exception e){
										e.printStackTrace();
									}

								}
							}).start();

						}
					});
				}

			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				// Respond to this device's wifi state changing

			}
		}
	}

	private class SendIPAddress extends Thread {
		String dstAddress;
		int dstPort;
		InputStream in=null;
		OutputStream out=null;
		Socket mSendIPAddressSocket = null;


		public SendIPAddress(String ipAddress) {
			Log.e(TAG,"Group owner address..."+ipAddress);
			this.dstAddress=ipAddress;

		}

		@Override
		public void run() {
			try {
				// Get the size of the file
				// Get the size of the file
				String status="Cancel";

				Log.e(TAG,"Group owner address##########..."+dstAddress);

				mSendIPAddressSocket = new Socket(dstAddress, SocketServerPORT);
				byte[] bytes = new byte[16 * 1024];
				 in = new ByteArrayInputStream(status.getBytes(StandardCharsets.UTF_8));
				 out = mSendIPAddressSocket.getOutputStream();

				// set file name in bytes
				DataOutputStream dos=new DataOutputStream(out);
				//Sending file name and file size to the server
				Log.e(TAG,"Send IP Address to"+mDeviceList.get(0).deviceName);
				Log.e(TAG,"ip address!!!!!!11 which sending "+mIPAddress);
				Log.e(TAG,"LOCAL PORT NUMBER "+mLocalPortNumber);
				dos.writeUTF(mIPAddress+"&"+mLocalPortNumber);
				dos.writeLong(bytes.length);
				dos.write(bytes, 0, bytes.length);
				dos.flush();
				int count;
				long total = 0;
				while ((count = in.read(bytes)) > 0) {
					out.write(bytes, 0, count);
				}
				//out.close();
				in.close();
				mSendIPAddressSocket.close();

			} catch (IOException e) {
				e.printStackTrace();
				final String eMsg = "Something wrong: " + e.getMessage();


			} finally {
				if(mSendIPAddressSocket != null){
					try {
						//Get the return message from the server
						mSendIPAddressSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}


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
		Log.e(TAG,"onPause()....");

		if(sendIPAddressSocketThread!=null && sendIPAddressSocketThread.isAlive()){
			sendIPAddressSocketThread.interrupt();
		}


		if(serverSocket!=null){
			try {
				Log.e(TAG,"socket is null");
				if(serverSocket.isBound()){


					Log.e(TAG, "Server is bound " + serverSocket.isBound());
				    serverSocket.close();
				}


			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			Log.e(TAG,"Socket is null");
		}



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
	protected void onDestroy() {
		super.onDestroy();



	}

	private String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
					.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces
						.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface
						.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip=inetAddress.getHostAddress();

						mIPAddress=inetAddress.getHostAddress();
						Log.e(TAG,"CLIENT IP ADDRESS "+mIPAddress);
					}

				}

			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//ip += "Something Wrong! " + e.toString() + "\n";
		}
		return ip;
	}




	public ServerSocket create(int[] ports) throws IOException {
		for (int port : ports) {
			try {
				return new ServerSocket(port);
			} catch (IOException ex) {
				Log.e(TAG,ex.getMessage());
				continue; // try next port
			}
		}

		// if the program gets here, no port in the range was found
		throw new IOException("no free port found");
	}
	
	public class ServerSocketThread extends Thread {

		InputStream in=null;
		OutputStream out = null;
		@Override
		public void run() {
			try {

				try {
					Log.e(TAG,"START TO RUN.....");
					//serverSocket = new ServerSocket(SocketServerPORT);


					serverSocket = new ServerSocket(); // <-- create an unbound socket first
					serverSocket.setReuseAddress(true);
					serverSocket.setSoTimeout(0);
					if(serverSocket.isBound()){
						Log.e(TAG,"Server is bound ..."+serverSocket.isBound());
					}else{
						Log.e(TAG,"Server is not bound ..."+serverSocket.isBound());
					}

					serverSocket.bind(new InetSocketAddress(SocketServerPORT)); // <-- now bind it*/
					//serverSocket = create(new int[] { 3843, 4584, 4843 });

					//Log.e(TAG,"mLocalPortNumber..."+mLocalPortNumber);



					} catch (IOException ex) {

					Log.e(TAG,ex.getMessage());
				}

				try {
					while (true) {

						mSocket = serverSocket.accept();
						in = mSocket.getInputStream();

						DataInputStream clientData = new DataInputStream(in);
						final String fileName = clientData.readUTF();
						final String[] separated = fileName.split("&");

						//int size=Integer.parseInt(separated[1]);
                        //totalSize=size;

						File file = new File(Environment.getExternalStorageDirectory(), separated[0]);
						if(!file.exists())
							try {
								file.createNewFile();
							} catch (IOException e) {
								e.printStackTrace();
							}

						try {
							out = new FileOutputStream(file.getAbsolutePath());

						} catch (final FileNotFoundException ex) {

							FileReceivedActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									String eMsg=ex.getMessage();
									Toast.makeText(FileReceivedActivity.this, "Exception 1"+eMsg, Toast.LENGTH_LONG).show();
								}
							});
						}

						byte[] bytes = new byte[1024];
						int count;
						int total=0;

						Message message = handler.obtainMessage();
						message.obj = separated[0];
						handler.sendMessage(message);

						while ((count = in.read(bytes)) > 0) {
							if(!isStopDownloading) {
								//Log.e(TAG,"File writting");
								total += count;
								// Publish the progress
								//publishProgress((int) (total * 100 / fileSize));
								out.write(bytes, 0, count);
							}else{

								boolean deleted = file.delete();
								isStopDownloading = false;
								break;
							}

						}

						 out.close();
						 in.close();
					     mSocket.close();
						 //serverSocket.close();

						}

				} catch (final Exception ex) {
					Log.e(TAG,ex.getMessage());

					FileReceivedActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {

							String eMsg = ex.getMessage();
							Toast.makeText(FileReceivedActivity.this, "Exception 2"+eMsg, Toast.LENGTH_LONG).show();
						}
					});
				}




			} catch (final Exception e) {
				Log.e(TAG,e.getMessage());
				// TODO Auto-generated catch block
				FileReceivedActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {

						String eMsg = e.getMessage();
						Toast.makeText(FileReceivedActivity.this, "Exception 3" + eMsg, Toast.LENGTH_LONG).show();
					}
				});
			} finally {

				if (mSocket != null) {
					/*try {
						//mSocket.close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
				}
			}
		}

	}
	


	private void publishProgress(final int progress){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressBar.setProgress(progress);
				progressbarStatusValueTextView.setText(String.valueOf(progress) + "%" + "               " + totalSize);
			}
		});

	}


	/*private class CancelDownloading extends AsyncTask<String,Integer,String> {

		String dstAddress;
		int dstPort;
		Socket socket = null;
		InputStream in=null;
		OutputStream out=null;
		public CancelDownloading(String ipAddress, int dstPort) {
			this.dstAddress=ipAddress;
			System.out.println("ip address "+ipAddress);

		}
		@TargetApi(Build.VERSION_CODES.KITKAT)
		@Override
		protected String doInBackground(String... params) {

			try {

				// Get the size of the file
				// Get the size of the file
				String status="Cancel";
				socket = new Socket("192.168.1.110", 8081);
				byte[] bytes = new byte[16 * 1024];
				in = new ByteArrayInputStream(status.getBytes(StandardCharsets.UTF_8));
				out = socket.getOutputStream();

				// set file name in bytes
				DataOutputStream dos=new DataOutputStream(out);
				//Sending file name and file size to the server
				//dos.writeUTF(file.getName()+"&"+file.length());
				dos.writeLong(bytes.length);
				dos.write(bytes, 0, bytes.length);
				dos.flush();
				int count;
				long total = 0;
				while ((count = in.read(bytes)) > 0) {

					// Publish the progress
					//publishProgress((int) (total * 100 / file.length()));
					out.write(bytes, 0, count);
				}
				out.close();
				in.close();
				socket.close();

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mProgressBar.setVisibility(View.INVISIBLE);

					}
				});



			} catch (IOException e) {

				e.printStackTrace();

				final String eMsg = "Something wrong: " + e.getMessage();


			} finally {
				if(socket != null){
					try {
						//Get the return message from the server
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}


			return null;
		}

	}*/


}
