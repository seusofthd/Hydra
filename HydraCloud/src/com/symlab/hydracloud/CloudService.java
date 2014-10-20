package com.symlab.hydracloud;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.network.cloud.EC2Instance;
import com.symlab.hydra.network.cloud.Pack;
import com.symlab.hydra.profilers.Profiler;

public class CloudService extends Service implements Runnable {
	private final IBinder mBinder = new MyBinder();
	ServerSocket serversoc = null;
	private ExecutorService workerPool;
	private ExecutorService pool;

	public class MyBinder extends Binder {
		CloudService getService() {
			return CloudService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	Context context;
	public CloudService(Context context) {
		this.context = context;
		System.out.println("constructor");
		workerPool = Executors.newCachedThreadPool();
		pool = Executors.newCachedThreadPool();
	}
	
	@Override
	public void onCreate() {
		System.out.println("in service on create");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("in service on start command");
		makeconnection();
		return Service.START_STICKY;
	}

	private EC2Instance getEC2Information() {
		EC2Instance ec2Instance = new EC2Instance();

		String a = "http://169.254.169.254/latest/meta-data/";
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new URL(a + "instance-id").openConnection().getInputStream()));
			ec2Instance.ID = br.readLine();
			br = new BufferedReader(new InputStreamReader(new URL(a + "instance-type").openConnection().getInputStream()));
			ec2Instance.type = br.readLine();
			br = new BufferedReader(new InputStreamReader(new URL(a + "local-ipv4").openConnection().getInputStream()));
			ec2Instance.privateIP = InetAddress.getByName(br.readLine());
			br = new BufferedReader(new InputStreamReader(new URL(a + "public-ipv4").openConnection().getInputStream()));
			ec2Instance.publicIP = InetAddress.getByName(br.readLine());
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ec2Instance;
	}

	Socket socket = new Socket();

	public boolean makeconnection() {
		while (true) {
			try {
				System.out.println("trying to connect to " + Constants.VM0_IP_PRV + ":" + Constants.VM_PORT);
				socket = new Socket();
				socket.connect(new InetSocketAddress(Constants.VM0_IP_PRV, Constants.VM_PORT), 10000);
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(out);
				DynamicObjectInputStream ois = new DynamicObjectInputStream(in, this.getClass().getClassLoader());
				sstreams = new ServerStreams(ois, oos);

				EC2Instance ec2Instance = getEC2Information();
				DataPackage dataPackage = DataPackage.obtain(Msg.REG_VM, ec2Instance);
				sstreams.send(dataPackage);
				System.out.println("connection to " + Constants.VM0_IP_PRV + ":" + Constants.VM_PORT + " established");
				pool.execute(this);
				return true;
			} catch (Exception ex) {
				try {
					Thread.sleep(3 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	String functionName = null;
	Class[] paramTypes = null;
	Object[] paramValues = null;
	Object state = null;
	Class stateDType = null;
	Pack myPack = null;
	ServerStreams sstreams = null;

	@Override
	public void run() {
		DataPackage receive = DataPackage.obtain(Msg.NONE);
		DataPackage sentMessage = null;
		Long totalExecDuration = null;
		String apkFilePath = "";
		File dexFile = null;
		boolean connectionloss = false;
		while (!connectionloss && receive != null) {
			try {
				receive = sstreams.receive();
				System.out.println("Received message: " + (receive != null ? receive.what : "null"));
			} catch (IOException e) {
				connectionloss = true;
				break;
			}
			switch (receive.what) {
			case REG_VM:
				System.out.println("receive REG_VM");
				break;
			case PING:
				sentMessage = DataPackage.obtain(Msg.PONG);
				try {
					sstreams.send(sentMessage);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				break;
			case SUPPORT_OFFLOAD:
				sentMessage = DataPackage.obtain(Msg.SUPPORT_OFFLOAD);
				try {
					sstreams.send(sentMessage);
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
					try {
						sstreams.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					receive.what = Msg.APK_REQUEST;
					try {
						sstreams.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}
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
			case EXECUTE:
				MethodPackage methodPack = null;
				methodPack = (MethodPackage) receive.deserialize();
				Future<ResultContainer> future = workerPool.submit(new Worker(methodPack));
				pool.execute(new SendResult(future, null, receive));
				break;
			}

		}
		try {
			sstreams.tearDownStream();
			// socket.close();
			Thread.sleep(1000);
			makeconnection();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return;
	};

	private class Worker implements Callable<ResultContainer> {
		private MethodPackage methodPack;

		public Worker(MethodPackage mp) {
			methodPack = mp;
		}

		@Override
		public ResultContainer call() {
			System.out.println("in worker");
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
			} catch (Exception e) {
				e.printStackTrace();
				ret = new ResultContainer(true, methodPack.receiver, new RemoteNodeException(e.getMessage(), e), 0L, 0L, methodPack.id);
			}
			// Log.e("Worker", "Remote Result: " + ret.result);
			return ret;

		}

	}

	private class SendResult implements Runnable {
		private Future<ResultContainer> future;
		// private Profiler profiler;
		// private Long totalExecDuration;
		DataPackage sentMessage;

		public SendResult(Future<ResultContainer> f, Profiler profiler, DataPackage dataPackage) {
			future = f;
			// this.profiler = profiler;
			// this.totalExecDuration = time;
			this.sentMessage = dataPackage;
		}

		@Override
		public void run() {
			try {
				System.out.println("in sender");
				ResultContainer result = future.get();
				// LogRecord log =
				// profiler.stopAndLogExecutionInfoTracking(true);
				// result.energyConsumption = log.energyConsumption;
				// result.pureExecutionDuration = totalExecDuration;
				// totalExecDuration = System.nanoTime() -
				// totalExecDuration;
				// sentMessage = DataPackage.obtain(Msg.RESULT, result,
				// source);
				sentMessage.serialize(result);
				sentMessage.what = Msg.RESULT;
				sentMessage.pureExecTime = result.pureExecutionDuration;
				sentMessage.dest = socket.getLocalAddress();
				sstreams.send(sentMessage);
				// sentMessage = DataPackage.obtain(Msg.FREE,
				// toRouter.myId);
				// toRouter.send(sentMessage);
				// profiler.stopAndLogExecutionInfoTracking(totalExecDuration,
				// "Me");
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (IOException e) {
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

	// }

	void returnnull(ObjectOutputStream oos) {
		if (oos != null)
			try {
				oos.writeObject(null);
				oos.flush();
			} catch (IOException ex1) {

			}
	}

}
