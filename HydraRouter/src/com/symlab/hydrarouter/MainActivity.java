package com.symlab.hydrarouter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static String TAG = "MainActivity";
	private Intent service;
	private static TextView tv;
	// private String name;
	public static boolean useCloud = false;

	// private ServiceConnection mConnection = new ServiceConnection() {
	// public void onServiceConnected(ComponentName className, IBinder service)
	// {
	// // This is called when the connection with the service has been
	// // established, giving us the object we can use to
	// // interact with the service. We are communicating with the
	// // service using a Messenger, so here we get a client-side
	// // representation of that from the raw IBinder object.
	// messenger = new Messenger(service);
	// Message m = Message.obtain(null, 0);
	// m.replyTo = myMessenger;
	// try {
	// messenger.send(m);
	// } catch (RemoteException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// public void onServiceDisconnected(ComponentName className) {
	// // This is called when the connection with the service has been
	// // unexpectedly disconnected -- that is, its process crashed.
	// messenger = null;
	// }
	// };

	// public String getLocalIpAddress() {
	// try {
	// String ip = "";
	// for (Enumeration<NetworkInterface> en =
	// NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	// NetworkInterface intf = en.nextElement();
	// ip += intf.getName() + ": ";
	// for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
	// enumIpAddr.hasMoreElements();) {
	// InetAddress inetAddress = enumIpAddr.nextElement();
	// if (!inetAddress.isLoopbackAddress()) {
	// ip += inetAddress.getHostAddress() + " ";
	// Log.i("IP", "***** IP=" + ip);
	//
	// }
	// }
	// ip += "\n";
	// }
	// return ip;
	// } catch (SocketException ex) {
	// Log.e("IP", ex.toString());
	// }
	// return "";
	// }

	// private Handler h = new Handler();
	//
	// private Runnable showList = new Runnable() {
	//
	// @Override
	// public void run() {
	// try {
	// messenger.send(Message.obtain(null, 1));
	// } catch (RemoteException e) {
	// e.printStackTrace();
	// }
	// h.postDelayed(showList, 1000);
	// }
	//
	// };

	// private Messenger messenger = null;
	// private Messenger myMessenger = new Messenger(new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// switch (msg.what) {
	// case 0:
	// h.post(showList);
	// break;
	// case 1:
	// // tv.setText(name + (String) msg.obj);
	// break;
	// default:
	// super.handleMessage(msg);
	// }
	// }
	// });

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.tv);
		tv.setMovementMethod(new ScrollingMovementMethod());
		service = new Intent(this, RouterService.class);
		CheckBox cb;
		cb = (CheckBox) findViewById(R.id.checkBox1);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				useCloud = isChecked;
			}
		});
		cb.setChecked(true);

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {

					runOnUiThread(new Runnable() {
						public void run() {
							tv.setText(out);
						}
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}).start();

	}

	public void startservice(View v) {
		try {
			startService(service);
			// bindService(service, mConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "Server Running...");
			append("Service is Started");
		} catch (Exception e) {

		}
	}

	public void stopservice(View v) {
		try {
			// h.removeCallbacks(showList);
			// unbindService(mConnection);
			stopService(service);
			Log.d(TAG, "Server Stopped");
			append("Service is Stopped");
		} catch (Exception e) {
		}
	}

	public static String out = "HydraRouter";

	public static void append(String str) {
		out = str + "\n" + out;
	}

	public void println(final String str) {
		runOnUiThread(new Runnable() {
			public void run() {
				tv.setText(str + "\n" + tv.getText());
			}
		});
	}

	public void clearScrean() {
		runOnUiThread(new Runnable() {
			public void run() {
				tv.setText("");
			}
		});
	}

}
