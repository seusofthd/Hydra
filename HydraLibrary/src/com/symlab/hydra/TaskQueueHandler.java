package com.symlab.hydra;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.pm.PackageManager;
import android.util.Log;

import com.symlab.hydra.lib.ApkHash;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.OffloadingNetworkException;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ToRouterConnection;
import com.symlab.hydra.profilers.Profiler;

public class TaskQueueHandler implements Runnable {

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
	private TaskQueue queue;
	private ToRouterConnection toRouter;
	private PackageManager packageManager;
	private ExecutorService executor;
	private ExecutorService offloadRequestExecutor;

	private Profiler profiler;

	PrintStream printStream ;
	public TaskQueueHandler(OffloadingService context, final ExecutorService executor, final ToRouterConnection toRouter, TaskQueue queue, PackageManager packageManager, Profiler profiler) {
		lock = new Object();
		this.context = context;
		this.executor = executor;
		this.toRouter = toRouter;
		this.queue = queue;
		this.packageManager = packageManager;
		this.profiler = profiler;
		offloadRequestExecutor = Executors.newCachedThreadPool();
		try {
			printStream = new PrintStream(new File("/sdcard/res.csv"));
		} catch (FileNotFoundException e1) {
		}
	}

//	private void submitTask(final OffloadableMethod offloadableMethod, ServerStreams streams) {
		// TaskWrapper taskWrapper = new TaskWrapper(context, packageManager,
		// offloadableMethod, streams, queue, this);
//		class TaskWrapper implements Callable<Object> {
//			@Override
//			public Object call() throws Exception {
//				Object ret = null;
//				if (ret != null) {
//					offloadableMethod.resultTicket.setResultReady();
//				} else
//					throw new Exception("*****null result returned!*****");
//				return ret;
//			}
//		}
		// Future<Object> future = executor.submit(new TaskWrapper());
		// offloadableMethod.resultTicket.setHolder(future);
		// execute(offloadableMethod);

//	}

	@Override
	public void run() {
		// switchingRunning = false;
		// Thread t = new Thread(new Runnable(){
		//
		// @Override
		// public void run() {
		// //int tick = 0;
		// Log.i(TAG, "switchingRunning:" + switchingRunning +
		// "  Thread.currentThread().isInterrupted(): " +
		// Thread.currentThread().isInterrupted());
		// while(switchingRunning && !Thread.currentThread().isInterrupted()) {
		// float cpu = 100 * DeviceStatus.newInstance(context).readCpuUsage();
		// //if (tick == 0)
		// Log.i(TAG, "CPU: " + cpu);
		// if (cpu > 80f) {
		// synchronized(onlyOffloadLock) {
		// onlyOffload = true;
		// doingBoth = false;
		// }
		// } else if (cpu < 30f) {
		// synchronized(onlyOffloadLock) {
		// onlyOffload = false;
		// doingBoth = false;
		// }
		// } else {
		// synchronized(onlyOffloadLock) {
		// doingBoth = true;
		// }
		// }
		// //tick++;
		// //if (tick == 5) tick = 0;
		// SystemClock.sleep(500);
		// }
		// }
		//
		// });
		// t.start();
		while (!stopped && !Thread.currentThread().isInterrupted()) {
			if (queue.queueSize() > 0) {
				synchronized (onlyOffloadLock) {
					// Log.d(TAG, "doingBoth:" + doingBoth + " onlyOffload:" +
					// onlyOffload);
					// Future<ArrayList<InetAddress>> f =
					// offloadRequestExecutor.submit(new
					// OffloadRequest(toRouter, queue.queueSize()));
					// ArrayList<InetAddress> list = new
					// ArrayList<InetAddress>();
					// if (f==null) {
					// offloadToCloud=true;
					// }else{
					// try {
					// list = f.get();
					// } catch (InterruptedException e1) {
					// e1.printStackTrace();
					// } catch (ExecutionException e1) {
					// e1.printStackTrace();
					// }
					// Log.d(TAG, "num of device:" + list.size());
					// if (list.size()==0) {
					// onlyOffload=false;
					// }else{
					// onlyOffload=true;
					// }
					// }
					// if (doingBoth || !onlyOffload) {
					// synchronized(localLock) {
					// Log.e(TAG, "Acquire local lock, occpied");
					// if (!occupied && queue.queueSize() != 0) {
					// submitTask(queue.dequeue(), null);
					// occupied = true;
					// }
					// //Log.e(TAG, "Release local lock, occpied");
					// }
					// }
					if (doingBoth || onlyOffload) {
						// Log.d(TAG, "num of tasks :  " + queue.queueSize());
						// if (queue.queueSize() != 0) {
						// Future<ArrayList<InetAddress>> f =
						// offloadRequestExecutor.submit(new
						// OffloadRequest(toRouter, queue.queueSize()));
						try {
							// ArrayList<InetAddress> list = f.get();
							// System.out.println("Receive " + list.size() +
							// " devices");
							// for (InetAddress address : list) {
							// submitTask(queue.dequeue(), toRouter.streams);
							execute(queue.dequeue());
							// pause();
							// }

							// synchronized (lock) {
							// if (paused) {
							// lock.wait(10000);
							// }
							// }
						} catch (Exception e) {
							e.printStackTrace();
						}
						// }
					}
				}
			}

		}
		// switchingRunning = false;
	}

	boolean remotely = true;

	private void execute(OffloadableMethod offloadableMethod) {
		Object result = null;
		try {
			if (remotely && toRouter.streams != null) {
				System.out.println("Executing Remotely ...");
				try {
					sendAndExecute(offloadableMethod);
				} catch (RemoteNodeException e) {
					remotely = false;
					executeLocally(offloadableMethod);
				} catch (OffloadingNetworkException e) {
					remotely = false;
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

	private void executeLocally(OffloadableMethod offloadableMethod) {
		Object result = null;
		Method m;
		try {
			DataPackage sentMessage = DataPackage.obtain(Msg.EXECUTE, offloadableMethod.methodPackage);
			sentMessage.pureExecTime = System.currentTimeMillis();
			offloadableMethod.dataPackage = sentMessage;

			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
		    objectOutputStream.writeObject(sentMessage);
		    objectOutputStream.flush();
		    objectOutputStream.close();
		    System.out.println("### SIZE ###   " + byteOutputStream.toByteArray().length);
			
		    
			Class<?> temp = offloadableMethod.methodPackage.receiver.getClass();
			m = temp.getDeclaredMethod(offloadableMethod.methodPackage.methodName, offloadableMethod.methodPackage.paraTypes);
			m.setAccessible(true);
			result = m.invoke(offloadableMethod.methodPackage.receiver, offloadableMethod.methodPackage.paraValues);
			offloadableMethod.result = result;
			sentMessage.pureExecTime = System.currentTimeMillis() - sentMessage.pureExecTime;
			printStream.println(sentMessage.pureExecTime / 1000f);
			synchronized (offloadableMethod) {
				offloadableMethod.notifyAll();
			}
			System.out.println("Total Exec Time (including method invocation) = " + sentMessage.pureExecTime / 1000f);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendAndExecute(OffloadableMethod offloadableMethod) throws RemoteNodeException, OffloadingNetworkException {
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
			// sentMessage = DataPackage.obtain(Msg.INIT_OFFLOAD, hashValue);
			// toRouter.streams.send(sentMessage);
			// response = toRouter.streams.receive();
			// System.out.println("Response Receive: " + response.what);
			// if (response.what == Msg.APK_REQUEST) {
			// sentMessage = DataPackage.obtain(Msg.APK_SEND, new
			// ByteFile(tempArray));
			// toRouter.streams.send(sentMessage);
			// response = toRouter.streams.receive();
			// } else if (response.what == Msg.READY) {
			tempArray = null;
			sentMessage = DataPackage.obtain(Msg.EXECUTE, offloadableMethod.methodPackage, context.toRouter.socket.getLocalAddress());
			offloadableMethod.dataPackage = sentMessage;
			sentMessage.rttDeviceToVM = System.currentTimeMillis();
			
			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
		    objectOutputStream.writeObject(sentMessage);
		    objectOutputStream.flush();
		    objectOutputStream.close();
		    System.out.println("### SIZE ###   " + byteOutputStream.toByteArray().length);
			
			toRouter.streams.send(sentMessage);
			System.out.println("sent method. waiting for result");

			// response = toRouter.streams.receive();
			// System.out.println("RECIEVED " + response.what);
			// if (response.what == Msg.RESULT) {
			// }

			// }
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
