package com.symlab.hydra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;

import com.symlab.hydra.lib.OffloadableMethod;

public class HydraHelper implements Serializable {

	private static final long serialVersionUID = 5281756857587061347L;

	private static final String TAG = "HydraHelper";
	public IBinder service;
	private IOffloadingService mService;
	private Context context;

	private boolean mIsBound;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			HydraHelper.this.service = service;
			mService = IOffloadingService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
			} catch (RemoteException e) {
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			if (mService != null) {
				try {
					mService.unregisterCallback();
				} catch (RemoteException e) {
				}
			}
			mService = null;
		}
	};

	

	public HydraHelper(Context context) {
		this.context = context;
	}

	public void startService(Intent intent) {
		context.startService(intent);
	}

	public void stopService(Intent intent) {
		context.stopService(intent);
	}

	public void bindService() {
		Intent intent = new Intent(IOffloadingService.class.getName());
		context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	public void unbindService() {
		if (mIsBound) {
			context.unbindService(mConnection);
			mIsBound = false;
		}
	}

	public void startHelping() {
		// if (mService != null) {
		// try {
		// mService.startHelping();
		// } catch (RemoteException e) {
		// e.printStackTrace();
		// }
		// }
	}

	public void stopHelping() {
		// if (mService != null) {
		// try {
		// mService.stopHelping();
		// } catch (RemoteException e) {
		// e.printStackTrace();
		// }
		// }
	}

	
	ArrayList<OffloadableMethod> waitingTasks = new ArrayList<OffloadableMethod>();
	public void postTask(OffloadableMethod offloadableMethod, String apkPath) {
		System.out.println(offloadableMethod);
		byte[] dataByte;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(offloadableMethod);
			dataByte = b.toByteArray();
		} catch (IOException e) {
			dataByte = new byte[1];
			e.printStackTrace();
		}
		
		try {
			mService.addTaskToQueue(dataByte, apkPath);
			waitingTasks.add(offloadableMethod);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}
	
	private IOffloadingCallback mCallback = new IOffloadingCallback.Stub() {
		@Override
		public void setResult(byte[] offloadableMethodBytes) throws RemoteException {
			OffloadableMethod offloadableMethod = null;
			try {
				ByteArrayInputStream b = new ByteArrayInputStream(offloadableMethodBytes);
				ObjectInputStream o = new ObjectInputStream(b);
				offloadableMethod = (OffloadableMethod) o.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for (OffloadableMethod om : waitingTasks) {
				System.out.println(om.methodPackage.id);
				if (om.methodPackage.id.equals(offloadableMethod.methodPackage.id)) {
					om.result = offloadableMethod.result;
					om.execDuration = offloadableMethod.execDuration;
					synchronized (om) {
						om.notifyAll();
					}
					waitingTasks.remove(om);
					break;
				}
			}
			System.out.println("In HydraHelper = " + offloadableMethod.execDuration/1000f);
		}

	};

}
