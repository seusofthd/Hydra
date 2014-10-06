package hk.ust.symlab.hydra.network.cloud;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.RemoteNodeException;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.network.cloud.Pack;
import com.symlab.hydra.profilers.Profiler;

public class NetworkManagerClient extends EC2Instance implements Runnable {
	ServerSocket serversoc = null;
	private ExecutorService workerPool;
	private ExecutorService pool;
	private ExecutorService listener;

	public NetworkManagerClient() {
		workerPool = Executors.newCachedThreadPool();
		pool = Executors.newCachedThreadPool();
		listener = Executors.newCachedThreadPool();
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
//				String instanceID = executeCommand("ec2metadata --instance-id").trim();
				String instanceID = Constants.VM1_ID.trim();
				DataPackage dataPackage = DataPackage.obtain(Msg.REG_VM, instanceID);
				sstreams.send(dataPackage);
				System.out.println("connection to " + Constants.VM0_IP_PRV + ":" + Constants.VM_PORT + " with instance id " + instanceID + " established");
				listener.execute(this);
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
			// ProgramProfiler progProfiler = new ProgramProfiler();
			// DeviceProfiler devProfiler = new DeviceProfiler(context);
			// Profiler profiler = new Profiler(context, progProfiler,
			// devProfiler);
			Long totalExecDuration = null;
			String apkFilePath = "";
			File dexOutputDir = null;
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
				// if (connectionloss || receive == null) {
				// sentMessage = DataPackage.obtain(Msg.FREE,
				// toRouter.myId);
				// toRouter.send(sentMessage);
				// break;
				// }
				switch (receive.what) {
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
					// String[] temp = appName_hashCode.split("#");
					// String appName = temp[0].trim();
					// long lastModified = Long.parseLong(temp[1].trim());
					// System.out.println("NodeServer: " + "HashName: " +
					// hashName);
					// dexOutputDir = context.getDir("dex",
					// Context.MODE_PRIVATE);
					// apkFilePath = dexOutputDir.getAbsolutePath() + "/" +
					// hashName + ".apk";
					// System.out.println("NodeServer: " + "apkFilePath: " +
					// apkFilePath);
					sentMessage = DataPackage.obtain(Msg.READY);
					try {
						sstreams.send(sentMessage);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					// if (apkPresent(apkFilePath)) {
					// sentMessage = DataPackage.obtain(Msg.READY);
					// // sstreams.addDex(apkFilePath,
					// dexOutputDir.getAbsolutePath());
					// try {
					// sstreams.send(sentMessage);
					// } catch (IOException e) {
					// connectionloss = true;
					// }
					// }
					// else {
					// sentMessage = DataPackage.obtain(Msg.APK_REQUEST);
					// try {
					// sstreams.send(sentMessage);
					// } catch (IOException e) {
					// connectionloss = true;
					// }
					// }
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
						// sstreams.addDex(apkFilePath,
						// dexOutputDir.getAbsolutePath());
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
					// ProgramProfiler(methodPack.receiver.getClass().getName()
					// + "#" + methodPack.methodName);
					// profiler = new Profiler(context, progProfiler,
					// devProfiler, btProfiler);
					// totalExecDuration = System.;
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
				case REG_VM:
					System.out.println("receive REG_VM");
					break;
				}

			}
			try {
				sstreams.tearDownStream();
//				socket.close();
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
					System.out.println("before invoke");
					result = method.invoke(methodPack.receiver, methodPack.paraValues);
					System.out.println("after invoke");
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

//	}

	void returnnull(ObjectOutputStream oos) {
		if (oos != null)
			try {
				oos.writeObject(null);
				oos.flush();
			} catch (IOException ex1) {

			}
	}

}
