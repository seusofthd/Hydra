package com.symlab.hydra;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

import android.content.pm.PackageManager;
import android.util.Log;

import com.symlab.hydra.lib.ApkHash;
import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.OffloadingNetworkException;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ToRouterConnection;

public class TaskQueueHandler implements Observer {

	private static final String TAG = "TaskQueueHandler";
	private boolean paused = true;
	private boolean stopped = false;
	private boolean globalWait = true;
	private Object lock;

	Object localLock = new Object();
	Object globalLock = new Object();
	boolean occupied = false;

	Object onlyOffloadLock = new Object();
	boolean onlyOffload = true;
	boolean offloadToCloud = false;
	boolean doingBoth = false;
	boolean switchingRunning = true;
	private OffloadingService context;
	private TaskQueue taskQueue;
	private ToRouterConnection toRouter;


	public TaskQueueHandler(OffloadingService context, final ToRouterConnection toRouter, TaskQueue taskQueue) {
		lock = new Object();
		this.context = context;
		this.toRouter = toRouter;
		this.taskQueue = taskQueue;
	}

	@Override
	public void update(Observable observable, Object data) {
		if (observable == taskQueue) {
			if (taskQueue.queueSize() > 0) {
				execute(taskQueue.dequeue());
			}
		}
	}


	public void execute(OffloadableMethod offloadableMethod) {
		Object result = null;
		try {
			if (offloadableMethod.offloadingMethod!=Msg.LOCAL && toRouter.streams != null) {
				System.out.println("Executing Remotely ...");
				try {
					sendAndExecute(offloadableMethod);
				} catch (RemoteNodeException e) {
					executeLocally(offloadableMethod);
				} catch (OffloadingNetworkException e) {
					executeLocally(offloadableMethod);
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("Executing Locally...");
				executeLocally(offloadableMethod);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		resume();
	}

	private void executeLocally(final OffloadableMethod offloadableMethod) {
		new Thread() {
			public void run() {
				Object result = null;
				Method m;
				Long execDuration = System.currentTimeMillis();
				try {
					System.out.println(taskQueue.dequeue(offloadableMethod.methodPackage.id).methodPackage.id);
					Class<?> temp = offloadableMethod.methodPackage.object.getClass();
					m = temp.getDeclaredMethod(offloadableMethod.methodPackage.methodName, offloadableMethod.methodPackage.paraTypes);
					m.setAccessible(true);
					result = m.invoke(offloadableMethod.methodPackage.object, offloadableMethod.methodPackage.paraValues);
					offloadableMethod.result = result;
					execDuration = System.currentTimeMillis() - execDuration;
					ResultContainer resultContainer = new ResultContainer(false, offloadableMethod.methodPackage.object, result, execDuration, 0L, offloadableMethod.methodPackage.id);
					taskQueue.setResult(resultContainer);
					System.out.println("Total Exec Time (including method invocation) = " + execDuration / 1000f);
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	private void sendAndExecute(OffloadableMethod offloadableMethod) throws RemoteNodeException, OffloadingNetworkException {
		DataPackage sentMessage = null;
		DataPackage response = null;
		Object result = null;
		byte[] tempArray;
		try {
			File apkFile = new File(offloadableMethod.apkPath);
			System.out.println("APK file = " + offloadableMethod.apkPath);
			FileInputStream fin = new FileInputStream(apkFile);
			BufferedInputStream bis = new BufferedInputStream(fin);
			tempArray = new byte[(int) apkFile.length()];
			bis.read(tempArray, 0, tempArray.length);
			bis.close();
			String hashValue = ApkHash.hash(tempArray);
			sentMessage = DataPackage.obtain(Msg.INIT_OFFLOAD, hashValue);
			sentMessage.id = offloadableMethod.methodPackage.id;
			sentMessage.destination = offloadableMethod.offloadingMethod;
			sentMessage.source = toRouter.socket.getLocalAddress();
			toRouter.streams.send(sentMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void setLocalNotOccupied() {
		synchronized (localLock) {
			// Log.e(TAG, "Acquire local lock, free");
			occupied = false;
			Log.e(TAG, "LOCAL, free");
		}
	}

	public void pause() {
		synchronized (lock) {
			// Log.e(TAG, "Acquire lock, pause");
			paused = true;
			// Log.e(TAG, "Release lock, pause");
		}
	}

	public void resume() {
		synchronized (lock) {
			// Log.e(TAG, "Acquire lock, free");
			paused = false;
			lock.notifyAll();
		}
	}

	public void globalPause() {
		synchronized (globalLock) {
			globalWait = true;
		}
	}

	public void globalResume() {
		synchronized (globalLock) {
			globalWait = false;
			globalLock.notifyAll();
			Log.e(TAG, "Release globalLock, free");
		}
	}
}
