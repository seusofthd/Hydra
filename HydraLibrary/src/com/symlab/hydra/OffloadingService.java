package com.symlab.hydra;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.Toast;

import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.lib.Utils;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ToRouterConnection;
import com.symlab.hydra.profilers.BluetoothProfiler;
import com.symlab.hydra.profilers.DeviceProfiler;
import com.symlab.hydra.profilers.LogRecord;
import com.symlab.hydra.profilers.Profiler;
import com.symlab.hydra.profilers.ProgramProfiler;
import com.symlab.hydra.status.DeviceStatus;

public class OffloadingService extends Service {
	private static final String TAG = "OffloadingService";
	private String thisApkPath;
	private ExecutorService executor = Executors.newCachedThreadPool();
	private TaskQueue taskQueue;
	private TaskQueueHandler taskQueueHandler;
	public ToRouterConnection toRouter;
	public static boolean serviceStarted = false;
	private ProgramProfiler progProfiler;
	private DeviceProfiler devProfiler;
	private BluetoothProfiler btProfiler;
	private Profiler profiler;
	private PowerManager.WakeLock wakeLock;
	private PowerManager pm;
	public Msg offloadingMethod;
	public File dexOutputDir;
	
	private IOffloadingCallback mServiceCallback;

	private final IOffloadingService.Stub mBinder = new IOffloadingService.Stub() {

		@Override
		public void addTaskToQueue(byte[] offloadableMethodBytes, String apkPath) throws RemoteException {
			File dexFile = new File(apkPath);
			System.out.println("start");
			OffloadableMethod offloadableMethod = (OffloadableMethod) Utils.deserialize(offloadableMethodBytes, dexFile, dexOutputDir);
			taskQueue.enqueue(offloadableMethod);
		}

		@Override
		public String getDeviceId() throws RemoteException {
			return toRouter.macAddress;
		}

		// @Override
		// public void startProfiling(String methodName) throws RemoteException
		// {
		// profiler.startExecutionInfoTracking(methodName);
		// }
		//
		// @Override
		// public LogRecord stopProfiling(boolean receivedTask) throws
		// RemoteException {
		// return profiler.stopAndLogExecutionInfoTracking(receivedTask);
		// }

		@Override
		public void startHelping() throws RemoteException {
			registerAndStartServer();

		}

		@Override
		public void stopHelping() throws RemoteException {
			unregisterAndStopServer();

		}

		@Override
		public void registerCallback(IOffloadingCallback arg0) throws RemoteException {
			mServiceCallback = arg0;

		}

		@Override
		public void unregisterCallback() throws RemoteException {
			mServiceCallback = null;
		}

	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show();
		dexOutputDir = getDir("dex", Context.MODE_PRIVATE);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServiceWakelook");
		wakeLock.acquire();
		taskQueue = new TaskQueue(this);
		toRouter = new ToRouterConnection(this, taskQueue);
		taskQueueHandler = new TaskQueueHandler(this, toRouter, taskQueue);
		taskQueue.addObserver(taskQueueHandler);
		executor.execute(toRouter);
		progProfiler = new ProgramProfiler();
		devProfiler = new DeviceProfiler(this);
		btProfiler = new BluetoothProfiler();
		profiler = new Profiler(this, progProfiler, devProfiler, btProfiler);
		return START_STICKY;
	}


	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	private void registerAndStartServer() {
		try {
			toRouter.streams.send(DataPackage.obtain(Msg.REGISTER, toRouter.macAddress));
		} catch (IOException e) {
		}
	}

	private void unregisterAndStopServer() {
		try {
			toRouter.streams.send(DataPackage.obtain(Msg.UNREGISTER, toRouter.macAddress));
		} catch (Exception e) {
		}
	}

	public static synchronized void setServiceOn() {
		serviceStarted = true;
	}

	public static synchronized void setServiceOff() {
		serviceStarted = false;
	}
	
	public void setResults(OffloadableMethod arg0) {
		try {
			mServiceCallback.setResult(Utils.serialize2(arg0));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		toRouter.disconnectToRouter();
		wakeLock.release();
		setServiceOff();
		DeviceStatus.newInstance(this).tearDown();
		super.onDestroy();
	}

}
