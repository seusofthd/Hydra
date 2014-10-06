package com.symlab.hydrarouter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StrictMode;
import android.widget.Toast;

public class RouterService extends Service {
	private static final String TAG = "RouterService";
	private WorkerList statusTable = new WorkerList();
	private RouterServer server = new RouterServer(statusTable);

	private PowerManager.WakeLock wakeLock;
	private PowerManager pm;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				client = msg.replyTo;
				try {
					client.send(Message.obtain(null, 0));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 1:
				// try {
				// client.send(Message.obtain(null, 1, getTable()));
				// } catch (RemoteException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	// private String getTable() {
	// String ret = "Devices number: ";
	// ArrayList<String> list = statusTable.getList();
	// ret += list.size() + "\n";
	// for (String s : list) {
	// ret += s.substring(s.length() - 2) + " ";
	// StringTokenizer st = new
	// StringTokenizer(statusTable.getIp(s).getHostAddress(), ".");
	// String tmp = "n/a";
	// while (st.hasMoreTokens()) {
	// tmp = st.nextToken();
	// }
	// ret += tmp + " ";
	// switch (statusTable.getState(s)) {
	// case WorkerList.STATE_AVAILABLE:
	// ret += "available";
	// break;
	// case WorkerList.STATE_OCCUPIED:
	// ret += "occupied";
	// break;
	// case WorkerList.STATE_LOWPOWER:
	// ret += "low power";
	// break;
	// case WorkerList.STATE_NOT_AVAILABLE:
	// ret += "not available";
	// break;
	// }
	//
	// }
	// return ret;
	// }

	final Messenger mMessenger = new Messenger(handler);

	Messenger client = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServiceWakelook");
		wakeLock.acquire();
		server.startServer();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		server.stopServer();
		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

}
