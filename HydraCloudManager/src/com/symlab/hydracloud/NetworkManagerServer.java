package com.symlab.hydracloud;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.cloud.Pack;
import com.symlab.hydra.profilers.Profiler;

public class NetworkManagerServer implements Runnable {

	private ExecutorService listener;
	ArrayList<EC2Instance> availableInstances;
	ArrayList<EC2Instance> reservedInstances;

	public NetworkManagerServer(ArrayList<EC2Instance> availableInstances) {
		this.availableInstances = availableInstances;
		reservedInstances = new ArrayList<EC2Instance>();
		initializeReseervedInstance();
		listener = Executors.newCachedThreadPool();
	}

	private void initializeReseervedInstance() {
		try {
			InetAddress ipPub1 = InetAddress.getByName(Constants.VM1_IP_PUB);
			InetAddress ipPrv1 = InetAddress.getByName(Constants.VM1_IP_PRV);
			EC2Instance reserveInstance1 = new EC2Instance(Constants.VM1_NAME, Constants.VM1_ID, "", null, ipPub1, ipPrv1, Constants.VM_PORT, Constants.VM_REGION);
			reservedInstances.add(reserveInstance1);

			// InetAddress ipPub2 = InetAddress.getByName(Constants.VM2_IP_PUB);
			// InetAddress ipPrv2 = InetAddress.getByName(Constants.VM2_IP_PRV);
			// EC2Instance reserveInstance2 = new
			// EC2Instance(Constants.VM2_NAME, Constants.VM2_ID, "", null,
			// ipPub2, ipPrv2, Constants.CLOUD_PORT, Constants.VM_REGION);
			// reservedInstances.add(reserveInstance2);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}

	public void startNewVM() {
		EC2Instance newInstance = reservedInstances.remove(0);
		newInstance.startInstance();
		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
		}
		availableInstances.add(newInstance);
	}

	public void stopVM(EC2Instance instance) {
		availableInstances.remove(instance);
		instance.stopInstance();
		try {
			Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
		}
		reservedInstances.add(instance);
	}

	private void registerVM(EC2Instance instance, ServerStreamsJava sstreams) {
		instance.sstreams = sstreams;
		reservedInstances.remove(instance);
		availableInstances.add(instance);
		System.out.println("VM " + instance.name + " is registred.");
	}

	private void unregisterVM(EC2Instance instance) {
		availableInstances.remove(instance);
		reservedInstances.add(instance);
		System.out.println("VM " + instance.name + " is UNregistred.");
	}

	public boolean makeconnection() {
		ServerSocket serversoc = null;
		if (serversoc == null || serversoc.isClosed()) {
			try {
				serversoc = new ServerSocket(Constants.CLOUD_PORT);
				serversoc.setSoTimeout(0);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		while (true) {
			try {
				System.out.println("server listening to " + serversoc.getLocalPort() + " for the router");
				Socket mysocket = serversoc.accept();
				InputStream in = mysocket.getInputStream();
				OutputStream out = mysocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(out);
				ObjectInputStream ois = new ObjectInputStream(in);
				// DynamicObjectInputStream ois = new
				// DynamicObjectInputStream(in,
				// this.getClass().getClassLoader());
				toRouter = new ServerStreamsJava(ois, oos);
				System.out.println("Connection to router established");
				listener.execute(new Receiving(mysocket, toRouter));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	ServerStreamsJava toRouter;

	@Override
	public void run() {
		ServerSocket serversoc = null;
		if (serversoc == null || serversoc.isClosed()) {
			try {
				serversoc = new ServerSocket(Constants.VM_PORT);
				serversoc.setSoTimeout(0);
			} catch (IOException ex) {
			}
		}

		while (true) {
			try {
				System.out.println("server listening to " + serversoc.getLocalPort() + " for other VMs");
				Socket mysocket = serversoc.accept();
				InputStream in = mysocket.getInputStream();
				OutputStream out = mysocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(out);
				ObjectInputStream ois = new ObjectInputStream(in);
				ServerStreamsJava vmStreams = new ServerStreamsJava(ois, oos);
				System.out.println("Connection to VM " + mysocket.getInetAddress().toString() + " established");
				listener.execute(new Receiving(mysocket, vmStreams));
			} catch (Exception ex) {
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
		ServerStreamsJava sstreams = null;
		Socket socket = null;
		EC2Instance instance = null;

		public Receiving(Socket socket, ServerStreamsJava sstreams) {
			super();
			this.sstreams = sstreams;
			this.socket = socket;
		}

		Runnable sender = new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					DataPackage sentMessage = DataPackage.obtain(Msg.PING);
					try {
						sstreams.send(sentMessage);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		};

		int counter = 0;

		@Override
		public void run() {
			// new Thread(sender).start();
			DataPackage receive = DataPackage.obtain(Msg.NONE);
			DataPackage sentMessage = null;
			Long totalExecDuration = null;
			String apkFilePath = "";
			File dexFile = null;
			boolean connectionloss = false;
			while (!connectionloss && receive != null) {
				try {
					receive = sstreams.receive();
					System.out.println("Received Msg: " + (receive != null ? receive.what : "null") + " from " + socket.getInetAddress().toString().replaceAll("/", "") + ":" + socket.getLocalPort());
				} catch (IOException e) {
					connectionloss = true;
					e.printStackTrace();
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
					// System.out.println("HashName: " + hashName);
					// dexOutputDir = context.getDir("dex",
					// Context.MODE_PRIVATE);
					// apkFilePath = dexOutputDir.getAbsolutePath() + "/" +
					// hashName + ".apk";
					// System.out.println("NodeServer: " + "apkFilePath: " +
					// apkFilePath);
					sentMessage = DataPackage.obtain(Msg.READY);
					try {
						sstreams.send(sentMessage);
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					System.out.println("Message " + Msg.READY + " is sent to the Router");
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
					dexFile = new File(apkFilePath);
					try {
						FileOutputStream fout = new FileOutputStream(dexFile);
						BufferedOutputStream bout = new BufferedOutputStream(fout, Constants.BUFFER);
						bout.write(bf.toByteArray());
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
					ServerStreamsJava VMsteams = getAvailableVMstream();
					// System.out.println("VM is selected. Sending to VM to execute...");
					receive.rttManagerToVM = System.currentTimeMillis();
//					for (EC2Instance ec2Instance : availableInstances) {
//						if (ec2Instance.socket != null && !ec2Instance.socket.isClosed()) {
							// availableInstances.remove(ec2Instance);
							// continue;
//							try {
//								ec2Instance.sstreams.send(receive);
//								System.out.println("send to " + ec2Instance.socket.getInetAddress() + " Waiting for results...");
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//					}
					 try {
						VMsteams.send(receive);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

					// run loclly or remotelly
					// MethodPackage methodPack = null;
					// methodPack = (MethodPackage) receive.data;
					// totalExecDuration = System.nanoTime();
					// Future<ResultContainer> future = workerPool.submit(new
					// Worker(methodPack));
					// pool.execute(new SendResult(future, null,
					// totalExecDuration));

					// progProfiler = new
					// ProgramProfiler(methodPack.receiver.getClass().getName()
					// + "#" + methodPack.methodName);
					// profiler = new Profiler(context, progProfiler,
					// devProfiler, btProfiler);
					// profiler.startExecutionInfoTracking();
					break;
				case REQUEST_STATUS:
					// try {
					// sstreams.send(DataPackage.obtain(Msg.RESPONSE_STATUS,
					// DeviceStatus.newInstance(context).readStatus()));
					// } catch (IOException e) {
					// connectionloss = true;
					// }
					break;

				case CTRL_VM:
					// update instance fields
					break;
				case REG_VM:
					// for (EC2Instance ec2Instance : availableInstances) {
					// if (ec2Instance.socket==null ||
					// ec2Instance.socket.isClosed()) {
					// availableInstances.remove(ec2Instance);
					// }
					// }
					instance = new EC2Instance();
					instance.socket = socket;
					instance.sstreams = sstreams;
					availableInstances.add(instance);

					// String instanceID = (String) receive.deserialize();
					// for (EC2Instance ec2Instance : reservedInstances) {
					// if (ec2Instance.ID.equalsIgnoreCase(instanceID)) {
					// instance = ec2Instance;
					// registerVM(instance, sstreams);
					// break;
					// }
					// }
					// long avr = 0;
					// for (int i = 0; i < 10; i++) {
					// byte[] bytes = new byte[1048576];
					// DataPackage aa = DataPackage.obtain(Msg.FREE, bytes);
					// long now = System.currentTimeMillis();
					// try {
					// sstreams.send(aa);
					// } catch (IOException e1) {
					// e1.printStackTrace();
					// }
					// now = System.currentTimeMillis() - now;
					// avr += now;
					// System.out.println(now / 1000f);
					// }
					// System.out.println("AVR : " + avr / 10000f);

					break;
				case RESULT:
					System.out.println("Result is received from " + receive.dest + " Sending to Router...");
					receive.rttManagerToVM = System.currentTimeMillis() - receive.rttManagerToVM;
					System.out.println("RTT (Manager->VM->Manager) = " + receive.rttManagerToVM / 1000f);
					System.out.println("Pure Exc Time = " + receive.pureExecTime / 1000f);
					if (counter++ == availableInstances.size()) {
						receive.finish = true;
						counter = 0;
					}
					try {
						toRouter.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}

					break;
				case FREE:
					try {
						toRouter.send(receive);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					break;
				}
			}
			sstreams.tearDownStream();
			return;
		};

		private ServerStreamsJava getAvailableVMstream() {
			for (int i = 1; i < availableInstances.size(); i++) {
				if (availableInstances.get(i).sstreams != null) {
					return availableInstances.get(i).sstreams;
				}
			}
			return null;
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
				// try {

				// try {
				// Long startExecTime = System.nanoTime();
				//
				// final DexClassLoader classloader = new
				// DexClassLoader("/home/uhuntu/android-x86/app.jar",
				// "/home/uhuntu/android-x86/app.jar", null,
				// this.getClass().getClassLoader());
				// System.out.println(classloader.toString());
				// System.out.println("1");
				// Class<Object> classToLoad = (Class<Object>)
				// classloader.loadClass("com.symlab.testoffloading.Sudoku");
				// classToLoad.toString();
				// System.out.println("2");
				// final Object myInstance = classToLoad.newInstance();
				// System.out.println("3");
				// final Method doSomething =
				// classToLoad.getMethod("hasSolution");
				// System.out.println("4");
				// doSomething.invoke(myInstance);
				// System.out.println("5");
				//
				// execDuration = System.nanoTime() - startExecTime;
				// } catch (Exception e) {
				// e.printStackTrace();
				// }

				// Method method =
				// methodPack.receiver.getClass().getDeclaredMethod(methodPack.methodName,
				// methodPack.paraTypes);
				// method.setAccessible(true);
				// Long startExecTime = System.currentTimeMillis();
				// result = method.invoke(methodPack.receiver,
				// methodPack.paraValues);
				// execDuration = System.currentTimeMillis() - startExecTime;
				// ret = new ResultContainer(false, methodPack.receiver, result,
				// execDuration, 0L);
				// } catch (NoSuchMethodException e) {
				// ret = new ResultContainer(true, methodPack.receiver, new
				// RemoteNodeException(e.getMessage(), e), 0L, 0L);
				// } catch (InvocationTargetException e) {
				// ret = new ResultContainer(true, methodPack.receiver, new
				// RemoteNodeException(e.getMessage(), e), 0L, 0L);
				// } catch (IllegalAccessException e) {
				// ret = new ResultContainer(true, methodPack.receiver, new
				// RemoteNodeException(e.getMessage(), e), 0L, 0L);
				// }
				// Log.e("Worker", "Remote Result: " + ret.result);
				return ret;

			}

		}

		private class SendResult implements Runnable {
			private Future<ResultContainer> future;
			// private Profiler profiler;
			private Long totalExecDuration;

			public SendResult(Future<ResultContainer> f, Profiler profiler, Long time) {
				future = f;
				// this.profiler = profiler;
				this.totalExecDuration = time;
			}

			@Override
			public void run() {
				try {
					ResultContainer result = future.get();
					// LogRecord log =
					// profiler.stopAndLogExecutionInfoTracking(true);
					// result.energyConsumption = log.energyConsumption;
					totalExecDuration = System.nanoTime() - totalExecDuration;
					DataPackage sentMessage = DataPackage.obtain(Msg.RESULT, result);
					result.pureExecutionDuration = totalExecDuration;
					try {
						sstreams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
					// sentMessage = DataPackage.obtain(Msg.FREE,
					// toRouter.myId);
					// toRouter.send(sentMessage);
					// profiler.stopAndLogExecutionInfoTracking(totalExecDuration,
					// "Me");
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
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

	void returnnull(ObjectOutputStream oos) {
		if (oos != null)
			try {
				oos.writeObject(null);
				oos.flush();
			} catch (IOException ex1) {

			}
	}

}
