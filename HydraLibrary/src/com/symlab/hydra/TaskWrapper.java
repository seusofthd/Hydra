package com.symlab.hydra;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.Callable;

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
import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.profilers.Profiler;

public class TaskWrapper implements Callable<Object> {
	private static final String TAG = "TaskWrapper";

	private OffloadingService context;
	private ServerStreams streams;
	private PackageManager packageManager;
	private OffloadableMethod offloadableMethod;
	private InetAddress target;
	private TaskQueue queue;
	private TaskQueueHandler tqHandler;

	private boolean remotely;

	public TaskWrapper(OffloadingService context, PackageManager packageManager, OffloadableMethod method, ServerStreams streams, final TaskQueue queue, final TaskQueueHandler tqHandler) {
		this.context = context;
		this.packageManager = packageManager;
		this.offloadableMethod = method;
		this.streams = streams;
		this.queue = queue;
		this.tqHandler = tqHandler;
		remotely = true;
		Log.d(TAG, "Set Task " + method.methodPackage.methodName + " to " + target + " ***");
	}

	@Override
	public Object call() throws Exception {
		Object ret = execute();
		if (ret != null) {
//			offloadableMethod.resultTicket.setResultReady();
		} else
			throw new Exception("*****null result returned!*****");
		return ret;
	}

	private Object execute() {
		Object result = null;
		try {
			if (remotely && streams != null) {
				System.out.println("Executing Remotely ...");
				try {
					result = sendAndExecute();
				} catch (RemoteNodeException e) {
					remotely = false;
					result = executeLocally();
				} catch (OffloadingNetworkException e) {
					remotely = false;
					Log.e(TAG, "Network Error, Executing Locally...");
					result = executeLocally();
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("Executing Locally...");
				result = executeLocally();
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		tqHandler.resume();
		return result;
	}

	private Object executeLocally() {
		Object result = null;
		Method m;
		try {
			Long startExecTime = System.currentTimeMillis();
			Class<?> temp = offloadableMethod.methodPackage.receiver.getClass();
			m = temp.getDeclaredMethod(offloadableMethod.methodPackage.methodName, offloadableMethod.methodPackage.paraTypes);
			m.setAccessible(true);
			result = m.invoke(offloadableMethod.methodPackage.receiver, offloadableMethod.methodPackage.paraValues);
			Long execDuration = System.currentTimeMillis() - startExecTime;
			System.out.println("Total Exec Time (including method invocation) = " + execDuration / 1000f);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private Object sendAndExecute() throws RemoteNodeException, OffloadingNetworkException {
		DataPackage sentMessage = null;
		DataPackage response = null;
		Object result = null;
		byte[] tempArray;
		try {
			String apkName = packageManager.getApplicationInfo(offloadableMethod.appName, 0).sourceDir;
			File apkFile = new File(apkName);
			FileInputStream fin = new FileInputStream(apkFile);
			BufferedInputStream bis = new BufferedInputStream(fin);
			tempArray = new byte[(int) apkFile.length()];
			bis.read(tempArray, 0, tempArray.length);
			bis.close();
			String hashValue = ApkHash.hash(tempArray);
			sentMessage = DataPackage.obtain(Msg.INIT_OFFLOAD, hashValue);
			streams.send(sentMessage);
			response = streams.receive();
			System.out.println("Response Receive: " + response.what);
			if (response.what == Msg.APK_REQUEST) {
				sentMessage = DataPackage.obtain(Msg.APK_SEND, new ByteFile(tempArray));
				streams.send(sentMessage);
				response = streams.receive();
			} else if (response.what == Msg.READY) {
				tempArray = null;
				sentMessage = DataPackage.obtain(Msg.EXECUTE, offloadableMethod.methodPackage, context.toRouter.socket.getLocalAddress());
				sentMessage.rttDeviceToVM = System.currentTimeMillis();
				streams.send(sentMessage);
				System.out.println("sent method. waiting for result");
				response = streams.receive();
				System.out.println("RECIEVED " + response.what);
				if (response.what == Msg.RESULT) {
					response.rttDeviceToVM = System.currentTimeMillis() - response.rttDeviceToVM;
					ResultContainer container = (ResultContainer) response.deserialize();
					System.out.println("Total RTT = " + (response.rttDeviceToVM) / 1000f);
					System.out.println("RTT (Router to VM/offloadee) = " + (response.rttRouterToVM) / 1000f);
					System.out.println("RTT (Manager to VM) = " + (response.rttManagerToVM) / 1000f);
					System.out.println("Pure Exec Time = " + (container.pureExecutionDuration) / 1000f);
					Class<?>[] pTypes = { Serializable.class };
					result = container.result;
					Profiler.addEnergy(container.energyConsumption);
					if (container.isExceptionOrError) {
						throw (RemoteNodeException) container.result;
					} else {
						result = container.result;
						Profiler.addEnergy(container.energyConsumption);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
