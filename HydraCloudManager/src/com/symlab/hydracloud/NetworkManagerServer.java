package com.symlab.hydracloud;

import java.io.File;
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

import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.ResultContainer;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.EC2Instance;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ServerStreams;
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

	private void registerVM(EC2Instance instance, ServerStreams sstreams) {
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
				toRouter = new ServerStreams(ois, oos);
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

	ServerStreams toRouter;

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
				ServerStreams vmStreams = new ServerStreams(ois, oos);
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
		ServerStreams sstreams = null;
		Socket socket = null;
		EC2Instance instance = new EC2Instance();

		public Receiving(Socket socket, ServerStreams sstreams) {
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
					ServerStreams VMsteams = getAvailableVMstream();
					receive.rttManagerToVM = System.currentTimeMillis();
					try {
						VMsteams.send(receive);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					break;
				case APK_REQUEST:
					try {
						toRouter.send(receive);
					} catch (IOException e4) {
						e4.printStackTrace();
					}
					break;
				case APK_SEND:
					VMsteams = getAvailableVMstream();
					receive.rttManagerToVM = System.currentTimeMillis();
					try {
						VMsteams.send(receive);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					break;
				case READY:
					try {
						toRouter.send(receive);
					} catch (IOException e3) {
						e3.printStackTrace();
					}
					break;
				case EXECUTE:
					VMsteams = getAvailableVMstream();
					receive.rttManagerToVM = System.currentTimeMillis();
					try {
						VMsteams.send(receive);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					break;
				case REQUEST_STATUS:
					// try {
					// sstreams.send(DataPackageAbstract.obtain(Msg.RESPONSE_STATUS,
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
					instance.socket = socket;
					instance.sstreams = sstreams;
					EC2Instance ec2Instance = (EC2Instance) receive.deserialize();
					instance.publicIP = ec2Instance.publicIP;
					instance.privateIP = ec2Instance.privateIP;
					instance.ID = ec2Instance.ID;
					instance.type = ec2Instance.type;
					availableInstances.add(instance);
					System.out.println("VM " + instance.ID + " type=" + instance.type + " local-ip=" + instance.privateIP + " pub-ip=" + instance.publicIP + " is registered.");

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

		private ServerStreams getAvailableVMstream() {
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
					// sentMessage = DataPackageAbstract.obtain(Msg.FREE,
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
