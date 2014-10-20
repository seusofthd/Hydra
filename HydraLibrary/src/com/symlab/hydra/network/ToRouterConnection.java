package com.symlab.hydra.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.symlab.hydra.lib.ApkHash;
import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.network.cloud.Pack;
import com.symlab.hydra.profilers.Profiler;

public class ToRouterConnection implements Runnable {

	private static final String TAG = "ToRouterConnection";

	private boolean connectedToRouter = false;
	private InetAddress routerAddress = null;

	private Context context;
	private IntentFilter intentFilter;

	public Socket socket;
	private DynamicObjectInputStream ois = null;
	private ObjectOutputStream oos = null;
	public ServerStreams streams = null;
	TaskQueue taskQueue;
	public String macAddress;

	PrintStream printStream;

	public ToRouterConnection(Context context, TaskQueue taskQueue) {
		this.context = context;
		this.taskQueue = taskQueue;
		intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		socket = new Socket();
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInf = wifiManager.getConnectionInfo();
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		int ipTemp = dhcpInfo.gateway;
		macAddress = wifiInf.getMacAddress();
		try {
			routerAddress = InetAddress.getByAddress(new byte[] { (byte) ipTemp, (byte) (ipTemp >> 8), (byte) (ipTemp >> 16), (byte) (ipTemp >> 24) });
			if ("192.168.3.1".equals(routerAddress.getHostAddress()))
				routerAddress = InetAddress.getByName("192.168.3.253");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void disconnectToRouter() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		if (connectedToRouter) {
			if (oos == null || ois == null)
				connectedToRouter = false;
			else {
				Log.e(TAG, "Already connected");
			}
			return;
		}
		if (routerAddress == null) {
			connectedToRouter = false;
			Log.e(TAG, "No router Address");
		} else {
			try {
				System.out.println("trying to connect to the Router " + routerAddress.toString().replaceAll("/", "") + ":" + Constants.DEVICE_PORT);
				socket.connect(new InetSocketAddress(routerAddress, Constants.DEVICE_PORT), Constants.TIMEOUT);
				System.out.println("connected!");
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new DynamicObjectInputStream(socket.getInputStream());
				streams = new ServerStreams(ois, oos, socket);
				System.out.println(streams);
				streams.send(DataPackage.obtain(Msg.SUPPORT_OFFLOAD));
				System.out.println("Checking Router Support...");
				DataPackage ret = (DataPackage) streams.receive();
				System.out.println("Receive support response: " + ret.what);
				if (ret.what == Msg.SUPPORT_OFFLOAD) {
					connectedToRouter = true;
					new Thread(new Receiving(streams)).start();
				}

				try {
					printStream = new PrintStream(new File("/sdcard/res.csv"));
				} catch (FileNotFoundException e1) {
				}

			} catch (Exception e) {
				e.printStackTrace();
				connectedToRouter = false;
			}
		}
	}

	class Receiving implements Runnable {
		String functionName = null;
		Class[] paramTypes = null;
		Object[] paramValues = null;
		Object state = null;
		Class stateDType = null;
		Pack myPack = null;
		ServerStreams sstreams = null;
		private ExecutorService workerPool;
		private ExecutorService pool;

		public Receiving(ServerStreams sstreams) {
			super();
			this.sstreams = sstreams;
			workerPool = Executors.newCachedThreadPool();
			pool = Executors.newCachedThreadPool();
		}

		int counter = 0;

		@Override
		public void run() {
			DataPackage receive = DataPackage.obtain(Msg.NONE);
			Long totalExecDuration = null;
			String apkFilePath = "";
			File dexFile = null;
			boolean connectionloss = false;
			while (!connectionloss && receive != null) {
				try {
					receive = sstreams.receive();
					System.out.println("Received message: " + (receive != null ? receive.what + " id=" + receive.id : "null"));
				} catch (IOException e) {
					connectionloss = true;
					e.printStackTrace();
					break;
				}
				switch (receive.what) {
				case PING:
					receive.what = Msg.PONG;
					try {
						sstreams.send(receive);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					break;
				case SUPPORT_OFFLOAD:
					receive.what = Msg.SUPPORT_OFFLOAD;
					try {
						sstreams.send(receive);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				case INIT_OFFLOAD:
					String hashName = (String) receive.deserialize();
					Log.d("NodeServer", "HashName***" + hashName);
					File dexOutputDir = context.getDir("dex", Context.MODE_PRIVATE);
					apkFilePath = dexOutputDir.getAbsolutePath() + "/" + hashName + ".apk";
					Log.d("NodeServer", "apkFilePath: " + apkFilePath);
					if (new File(apkFilePath).exists()) {
						receive.what = Msg.READY;
//						btProfiler.addStartTime(System.nanoTime());
						try {
							sstreams.send(receive);
						} catch (IOException e) {
							e.printStackTrace();
						}
//						btProfiler.addEndTime(System.nanoTime());
					}
					else {
						receive.what = Msg.APK_REQUEST;
//						btProfiler.addStartTime(System.nanoTime());
						try {
							sstreams.send(receive);
						} catch (IOException e) {
							e.printStackTrace();
						}
//						btProfiler.addEndTime(System.nanoTime());
					}
					break;
				case APK_REQUEST:
					try {
						OffloadableMethod offloadableMethod = taskQueue.getOffloadableMethod(receive.id);
						File apkFile = new File(offloadableMethod.apkName);
						FileInputStream fin = new FileInputStream(apkFile);
						BufferedInputStream bis = new BufferedInputStream(fin);
						byte[] tempArray = new byte[(int) apkFile.length()];
						bis.read(tempArray, 0, tempArray.length);
						bis.close();
						String hashValue = ApkHash.hash(tempArray);
						receive.what = Msg.APK_SEND;
						receive.serialize(new ByteFile(tempArray));
						streams.send(receive);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					break;
				case APK_SEND:
					ByteFile bf = (ByteFile) receive.deserialize();
					dexFile = new File(apkFilePath);
					try {
						FileOutputStream fout = new FileOutputStream(dexFile);
						BufferedOutputStream bout = new BufferedOutputStream(fout, Constants.BUFFER);
						bout.write(bf.toByteArray());
						bout.close();
						sstreams.addDex(dexFile);
						receive.what = Msg.READY;
						sstreams.send(receive);
					} catch (IOException e) {

					}
					break;
				case READY:
					try {
						System.out.println("ID = "  + receive.id);
						OffloadableMethod offloadableMethod = taskQueue.dequeue(receive.id);
						receive.what = Msg.EXECUTE;
						receive.serialize(offloadableMethod.methodPackage);
						offloadableMethod.dataPackage = receive;
						receive.rttDeviceToVM = System.currentTimeMillis();

						ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
						ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
						objectOutputStream.writeObject(receive);
						objectOutputStream.flush();
						objectOutputStream.close();

						streams.send(receive);
						System.out.println("sent method with size of " + byteOutputStream.toByteArray().length + ". waiting for result");
					} catch (IOException e) {
						e.printStackTrace();
					}

					break;
				case EXECUTE:
					MethodPackage methodPack = null;
					methodPack = (MethodPackage) receive.deserialize();

					// progProfiler = new
					// ProgramProfiler(methodPack.receiver.getClass().getName()
					// +
					// "#" + methodPack.methodName);
					// profiler = new Profiler(context, progProfiler,
					// devProfiler,
					// btProfiler);
					// totalExecDuration = System.nanoTime();
					// profiler.startExecutionInfoTracking();
					Future<ResultContainer> future = workerPool.submit(new Worker(methodPack));
					pool.execute(new SendResult(future, null, receive));
					break;
				case REQUEST_STATUS:
					// try {
					// sstreams.send(DataPackage.obtain(Msg.RESPONSE_STATUS,
					// DeviceStatus.newInstance(context).readStatus()));
					// } catch (IOException e) {
					// connectionloss = true;
					// }
					break;

				case REGISTERED:
					break;

				case UNREGISTERED:
					break;

				case RESULT:
					receive.rttDeviceToVM = System.currentTimeMillis() - receive.rttDeviceToVM;
					ResultContainer resultContainer = (ResultContainer) receive.deserialize();
					printStream.println(receive.dest + "," + (receive.pureExecTime) / 1000f + "," + (receive.rttRouterToVM) / 1000f);
					System.out.println("Total RTT = " + (receive.rttDeviceToVM) / 1000f + " DST=" + receive.destination);
					System.out.println("RTT (Router to VM/offloadee) = " + (receive.rttRouterToVM) / 1000f);
					System.out.println("RTT (Manager to VM) = " + (receive.rttManagerToVM) / 1000f);
					System.out.println("Pure Exec Time = " + (resultContainer.pureExecutionDuration) / 1000f);
					Object result = resultContainer.result;
					Profiler.addEnergy(resultContainer.energyConsumption);
					if (resultContainer.isExceptionOrError) {
						// throw (RemoteNodeException) container.result;
					} else {
						result = resultContainer.result;
						Profiler.addEnergy(resultContainer.energyConsumption);
					}
					counter = (counter + 1) % 1;
					if (counter == 0) {
						receive.finish = true;
					}
					taskQueue.setResult(receive);

					break;
				case FREE:
					try {
						streams.send(receive);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				}
			}
			sstreams.tearDownStream();
			return;
		};
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
				// totalExecDuration = System.nanoTime() -
				// totalExecDuration;
				// DataPackage sentMessage = DataPackage.obtain(Msg.RESULT,
				// result);

				sentMessage.serialize(result);
				sentMessage.what = Msg.RESULT;
				sentMessage.pureExecTime = result.pureExecutionDuration;
				streams.send(sentMessage);
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
}