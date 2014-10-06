package com.symlab.hydra;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.util.Log;

import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.network.ToRouterConnection;
import com.symlab.hydra.profilers.DeviceProfiler;
import com.symlab.hydra.profilers.Profiler;
import com.symlab.hydra.profilers.ProgramProfiler;
import com.symlab.hydra.status.DeviceStatus;

public class NodeServer extends Thread {

	private ServerStreams sstreams;
	private Context context;
	private ExecutorService workerPool;
	private ExecutorService pool;

	private ToRouterConnection toRouter;
	private String thisApkPath;

	public NodeServer(ServerStreams ss, Context c, ToRouterConnection toRouter, String thisApkPath) {
		sstreams = ss;
		context = c;
		workerPool = Executors.newFixedThreadPool(1);
		pool = Executors.newCachedThreadPool();
		this.toRouter = toRouter;
		this.thisApkPath = thisApkPath;
		// sstreams.addDex(new File(thisApkPath));
	}

	@Override
	public void run() {
		DataPackage receive = DataPackage.obtain(Msg.NONE);
		DataPackage sentMessage = null;
		ProgramProfiler progProfiler = new ProgramProfiler();
		DeviceProfiler devProfiler = new DeviceProfiler(context);
		Profiler profiler = null;
//		Profiler profiler = new Profiler(context, progProfiler, devProfiler);
		Long totalExecDuration = null;
		String apkFilePath = "";
		File dexOutputDir = null;
		File dexFile = null;
		boolean connectionloss = false;
		while (!connectionloss && receive != null) {
			try {
				receive = sstreams.receive();
				Log.d("NodeServer", "Received message: " + (receive != null ? receive.what : "null"));
			} catch (IOException e) {
				connectionloss = true;
				Log.e("NodeServer", "Connection Loss");
			}
			if (connectionloss || receive == null) {
				sentMessage = DataPackage.obtain(Msg.FREE, toRouter.myId);
				try {
					toRouter.streams.send(sentMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
			switch (receive.what) {
			case PING:
				sentMessage = DataPackage.obtain(Msg.PONG);
				try {
					sstreams.send(sentMessage);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				break;
			case INIT_OFFLOAD:
				String hashName = (String) receive.deserialize();
				// String[] temp = appName_hashCode.split("#");
				// String appName = temp[0].trim();
				// long lastModified = Long.parseLong(temp[1].trim());
				// Log.e("NodeServer", "Received lastModified: " +
				// lastModified);
				Log.d("NodeServer", "HashName***" + hashName);
				dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
				apkFilePath = dexOutputDir.getAbsolutePath() + "/" + hashName + ".apk";
				Log.d("NodeServer", "apkFilePath: " + apkFilePath);
				if (apkPresent(apkFilePath)) {
					sentMessage = DataPackage.obtain(Msg.READY);
					sstreams.addDex(apkFilePath, dexOutputDir.getAbsolutePath());
					try {
						sstreams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
					connectionloss = true;
				} else {
					sentMessage = DataPackage.obtain(Msg.APK_REQUEST);
					try {
						sstreams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;
			case APK_SEND:
				ByteFile bf = (ByteFile) receive.deserialize();
				// Log.e("NodeServer", "1");
				dexFile = new File(apkFilePath);
				// Log.e("NodeServer", dexFile.getAbsolutePath());
				try {
					FileOutputStream fout = new FileOutputStream(dexFile);
					// Log.e("NodeServer", "3");
					BufferedOutputStream bout = new BufferedOutputStream(fout, Constants.BUFFER);
					// Log.e("NodeServer", "4");
					bout.write(bf.toByteArray());
					// Log.e("NodeServer", "5");
					bout.close();
					// Log.e("NodeServer", "6");
					sstreams.addDex(apkFilePath, dexOutputDir.getAbsolutePath());
					// Log.e("NodeServer", "7");
					sentMessage = DataPackage.obtain(Msg.READY);
					sstreams.send(sentMessage);
					// Log.e("NodeServer", "8");
				} catch (IOException e) {

				}
				break;
			case EXECUTE:
				MethodPackage methodPack = null;
				methodPack = (MethodPackage) receive.deserialize();

				// progProfiler = new
				// ProgramProfiler(methodPack.receiver.getClass().getName() +
				// "#" + methodPack.methodName);
				// profiler = new Profiler(context, progProfiler, devProfiler,
				// btProfiler);
				// totalExecDuration = System.nanoTime();
				// profiler.startExecutionInfoTracking();
				Future<ResultContainer> future = workerPool.submit(new Worker(methodPack));
				pool.execute(new SendResult(future, profiler, receive));
				break;
			case REQUEST_STATUS:
				try {
					sstreams.send(DataPackage.obtain(Msg.RESPONSE_STATUS, DeviceStatus.newInstance(context).readStatus()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		sstreams.tearDownStream();
		return;
	}

	private class Worker implements Callable<ResultContainer> {
		private MethodPackage methodPack;

		public Worker(MethodPackage mp) {
			methodPack = mp;
		}

		@Override
		public ResultContainer call() {
			ResultContainer ret = null;
			Object result = null;
			Long execDuration = null;
			try {
				Method method = methodPack.receiver.getClass().getDeclaredMethod(methodPack.methodName, methodPack.paraTypes);
				method.setAccessible(true);
				Long startExecTime = System.currentTimeMillis();
				result = method.invoke(methodPack.receiver, methodPack.paraValues);
				execDuration = System.currentTimeMillis() - startExecTime;
				System.out.println("Pure Execution time (including invokation) = " + execDuration / 1000f);
				ret = new ResultContainer(false, methodPack.receiver, result, execDuration, 0L, methodPack.id);
			} catch (NoSuchMethodException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0L, methodPack.id);
			} catch (InvocationTargetException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0L, methodPack.id);
			} catch (IllegalAccessException e) {
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0L, methodPack.id);
			}
			Log.e("Worker", "Remote Result: " + ret.result);
			return ret;

		}

	}

	private class SendResult implements Runnable {
		private Future<ResultContainer> future;
		// private Profiler profiler;
		// private Long totalExecDuration;
		DataPackage sentMessage;
		InetAddress source;

		public SendResult(Future<ResultContainer> f, Profiler profiler, DataPackage dataPackage) {
			future = f;
			// this.profiler = profiler;
			// this.totalExecDuration = time;
			this.source = dataPackage.source;
			this.sentMessage = dataPackage;
		}

		@Override
		public void run() {
			try {
				ResultContainer result = future.get();
				// LogRecord log =
				// profiler.stopAndLogExecutionInfoTracking(true);
				// result.energyConsumption = log.energyConsumption;
				// result.pureExecutionDuration = totalExecDuration;
				// totalExecDuration = System.nanoTime() - totalExecDuration;
				// DataPackage sentMessage = DataPackage.obtain(Msg.RESULT,
				// result);

				sentMessage.serialize(result);
				sentMessage.what = Msg.RESULT;
				sentMessage.pureExecTime = result.pureExecutionDuration;
				sstreams.send(sentMessage);
				// sentMessage = DsataPackage.obtain(Msg.FREE,
				// toRouter.myId);
				// toRouter.send(sentMessage);
				// profiler.stopAndLogExecutionInfoTracking(totalExecDuration,
				// "Me");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private boolean apkPresent(String filename) {
		// return false;
		File apkFile = new File(filename);
		if (apkFile.exists()) {
			return true;
		} else {
			return false;
		}
	}
}
