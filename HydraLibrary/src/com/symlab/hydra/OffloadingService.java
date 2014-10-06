package com.symlab.hydra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.Toast;

import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.network.ServiceRegister;
import com.symlab.hydra.network.ToRouterConnection;
import com.symlab.hydra.profilers.DeviceProfiler;
import com.symlab.hydra.profilers.LogRecord;
import com.symlab.hydra.profilers.Profiler;
import com.symlab.hydra.profilers.ProgramProfiler;
import com.symlab.hydra.status.DeviceStatus;

public class OffloadingService extends Service {

	private static final String TAG = "OffloadingService";

	private PackageManager packageManager;// = getPackageManager();
	private String thisApkPath;
	private ExecutorService executor = Executors.newCachedThreadPool();
	private String myId;

	private TaskQueue taskQueue;
	private TaskQueueHandler taskQueueHandler;

	public ToRouterConnection toRouter;
	private ServiceRegister serviceRegister;

	// private OffloadingService me;// = this;

	public static boolean serviceStarted = false;
	private ProgramProfiler progProfiler;
	private DeviceProfiler devProfiler;
	private Profiler profiler;
	private PowerManager.WakeLock wakeLock;
	private PowerManager pm;
	private final IOffloadingService.Stub mBinder = new IOffloadingService.Stub() {

		@Override
		public void addTaskToQueue(OffloadableMethod offloadableMethod) throws RemoteException {
			taskQueue.enqueue(offloadableMethod);
		}

		@Override
		public String getDeviceId() throws RemoteException {
			return myId;
		}

		@Override
		public void startProfiling(String methodName) throws RemoteException {
			profiler.startExecutionInfoTracking(methodName);
		}

		@Override
		public LogRecord stopProfiling(boolean receivedTask) throws RemoteException {
			return profiler.stopAndLogExecutionInfoTracking(receivedTask);
		}

		@Override
		public void startHelping() throws RemoteException {
			registerAndStartServer();

		}

		@Override
		public void stopHelping() throws RemoteException {
			unregisterAndStopServer();

		}

	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "Starting Service", Toast.LENGTH_SHORT).show();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServiceWakelook");
		wakeLock.acquire();
		WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInf = wifiMan.getConnectionInfo();
		myId = wifiInf.getMacAddress();
		taskQueue = new TaskQueue();
		packageManager = getPackageManager();
		/*
		 * try { String dexInPath =
		 * packageManager.getApplicationInfo(getPackageName(), 0).sourceDir;
		 * BufferedInputStream bis = null; OutputStream dexWriter = null; int
		 * BUF_SIZE = 8 * 1024; final File dexOut = new File(getDir("dex",
		 * Context.MODE_PRIVATE), getPackageName() + ".apk"); try { bis = new
		 * BufferedInputStream(new FileInputStream(dexInPath)); dexWriter = new
		 * BufferedOutputStream(new FileOutputStream(dexOut)); byte[] buf = new
		 * byte[BUF_SIZE]; int len; while((len = bis.read(buf, 0, BUF_SIZE)) >
		 * 0) { dexWriter.write(buf, 0, len); } dexWriter.close(); bis.close();
		 * } catch (IOException e) { if (dexWriter != null) { try {
		 * dexWriter.close(); } catch (IOException ioe) { ioe.printStackTrace();
		 * } } if (bis != null) { try { bis.close(); } catch (IOException ioe) {
		 * ioe.printStackTrace(); } } } thisApkPath = dexOut.getAbsolutePath();
		 * Log.d(TAG, "DexPath: " + thisApkPath); } catch (NameNotFoundException
		 * e) { e.printStackTrace(); }
		 */
		toRouter = new ToRouterConnection(this, myId,taskQueue);
		taskQueueHandler = new TaskQueueHandler(this, executor, toRouter, taskQueue, packageManager, profiler);
		serviceRegister = new ServiceRegister(this, toRouter, thisApkPath);
		progProfiler = new ProgramProfiler();
		devProfiler = new DeviceProfiler(this);
//		profiler = new Profiler(this, progProfiler, devProfiler);
		executor.execute(taskQueueHandler);
		// statusUpdater.startUpdate();
		// me = this;
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	private void registerAndStartServer() {
		serviceRegister.registerService();
	}

	private void unregisterAndStopServer() {
		serviceRegister.unregisterService();
	}

	public static synchronized void setServiceOn() {
		serviceStarted = true;
	}

	public static synchronized void setServiceOff() {
		serviceStarted = false;
	}

	/*
	 * private void stopService() { networkInterface.shutdownServer();
	 * stopSelf(); }
	 */
	@Override
	public void onDestroy() {
		toRouter.unregisterReceiver();
		toRouter.disconnectToRouter();
		wakeLock.release();
		setServiceOff();
		DeviceStatus.newInstance(this).tearDown();
		super.onDestroy();
	}

}
