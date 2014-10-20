package com.symlab.hydrarouter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.widget.Toast;

public class RouterService extends Service {
	private static final String TAG = "RouterService";
	private WorkerList workerList = new WorkerList();
	private RouterServer routerServer;
	PacketQueue packetQueue = new PacketQueue();
	private PowerManager.WakeLock wakeLock;
	private PowerManager pm;

	private final IBinder mBinder = new MyBinder();

	public class MyBinder extends Binder {
		RouterService getService() {
			return RouterService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServiceWakelook");
		wakeLock.acquire();
		routerServer = new RouterServer(workerList, packetQueue);
		routerServer.startServer();
		PackageQueueHandler taskQueueHandler = new PackageQueueHandler(routerServer, workerList, packetQueue);
		packetQueue.addObserver(taskQueueHandler);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		routerServer.stopServer();
		wakeLock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
