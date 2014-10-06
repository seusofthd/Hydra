package com.symlab.hydra;

import java.io.Serializable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.profilers.LogRecord;

public class DandelionHelper implements Serializable {

	private static final String TAG = "DandelionHelper";

	private IOffloadingService mService;
	private Context mContext;
	private Intent offloadingServiceIntent;
	private String appName;

	private boolean serviceBound;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Service Binding...");
			mService = IOffloadingService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Log.d(TAG, "Service Broken");
		}

	};

	public DandelionHelper(Context context) {
		this.mContext = context;
		offloadingServiceIntent = new Intent(context, OffloadingService.class);
		appName = context.getPackageName();
		serviceBound = false;
	}

	public void initializeOHelper() {
		Log.d(TAG, "Call initialize");
		startOffloadingService();
		if (!serviceBound) {
			Log.d(TAG, "Bind Service");
			mContext.bindService(offloadingServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			serviceBound = true;
		}

	}

	public boolean startOffloadingService() {
		if (!OffloadingService.serviceStarted) {
			Log.d(TAG, "Start Service");
			mContext.startService(offloadingServiceIntent);
			OffloadingService.setServiceOn();
		}
		if (OffloadingService.serviceStarted)
			return true;
		else
			return false;
	}

	public void tearDownOHelper() {
		if (serviceBound) {
			mContext.unbindService(serviceConnection);
			serviceBound = false;
			mService = null;
			Log.d(TAG, "Service Unbinding...");
		}
	}

	public boolean stopOffloadingService() {
		if (OffloadingService.serviceStarted) {
			Log.d(TAG, "Stop Service");

			mContext.stopService(offloadingServiceIntent);
			OffloadingService.setServiceOff();
		}
		if (!OffloadingService.serviceStarted)
			return true;
		else
			return false;
	}

	public void postTask(MethodPackage methodPack, Class<?> reutrnType) {
		OffloadableMethod offloadableMethod = new OffloadableMethod(mContext, appName, methodPack, reutrnType);
		try {
			mService.addTaskToQueue(offloadableMethod);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void postTask(OffloadableMethod offloadableMethod) {
		try {
			mService.addTaskToQueue(offloadableMethod);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public String myId() {
		String ret = "";
		try {
			if (mService != null)
				ret += mService.getDeviceId();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void startProfiling(String methodName) {
		if (mService != null) {
			try {
				mService.startProfiling(methodName);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public LogRecord stopProfiling() {
		LogRecord log = null;
		if (mService != null) {
			try {
				log = mService.stopProfiling(false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return log;
	}

	public void startHelping() {
		if (mService != null) {
			try {
				mService.startHelping();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			Log.e(TAG, "no service bound");
	}

	public void stopHelping() {
		if (mService != null) {
			try {
				mService.stopHelping();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			Log.e(TAG, "no service bound");
	}
}
