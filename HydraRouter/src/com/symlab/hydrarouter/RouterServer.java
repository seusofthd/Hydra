package com.symlab.hydrarouter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.provider.ContactsContract.RawContacts.Data;
import android.util.Log;

import com.symlab.hydra.lib.ByteFile;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.status.Status;

public class RouterServer implements Runnable {
	private static final String TAG = "RouterServer";

	private ServerSocket serverSocket = null;
	private WorkerList workerList;
	private ExecutorService pool;

	private Boolean isServerShutdown = true;

	public RouterServer(WorkerList workerList) {
		this.workerList = workerList;
		pool = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		Socket toDeviceSocket = null;
		try {
			serverSocket = new ServerSocket(Constants.DEVICE_PORT);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while (!isServerShutdown) {
			try {
				System.out.println("Listening to " + Constants.DEVICE_PORT + " for smartphones");
				MainActivity.append("Listening to " + Constants.DEVICE_PORT + " for smartphones");
				toDeviceSocket = serverSocket.accept();
				
				System.out.println("Device " + toDeviceSocket.getInetAddress().getHostAddress() + " connected.");
				InputStream in = toDeviceSocket.getInputStream();
				OutputStream out = toDeviceSocket.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(out);
				DynamicObjectInputStream ois = new DynamicObjectInputStream(in, this.getClass().getClassLoader());
				ServerStreams sstreams = new ServerStreams(ois, oos);
				Device device = new Device(null, toDeviceSocket.getInetAddress(), null, DeviceState.STATE_NOT_AVAILABLE, sstreams, toDeviceSocket);
				pool.execute(new DeviceReceiving(device));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return;
	}

	public void startServer() {
		synchronized (isServerShutdown) {
			if (isServerShutdown) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (MainActivity.useCloud) {
							connectToCloud();
						}
					}
				}).start();
				isServerShutdown = false;
				pool.execute(this);
			}
		}
	}

	ServerStreams toCloud = null;
	boolean connectedToCloud = false;
	Socket socketCloud = new Socket();
	private boolean connectToCloud() {
		try {
			System.out.println("trying to connect to the VM Manager...");
			MainActivity.append("trying to connect to the VM Manager...");
			socketCloud.connect(new InetSocketAddress(Constants.VM0_IP_PUB, Constants.CLOUD_PORT), Constants.TIMEOUT);
			ObjectOutputStream oos = new ObjectOutputStream(socketCloud.getOutputStream());
			DynamicObjectInputStream ois = new DynamicObjectInputStream(socketCloud.getInputStream());
			toCloud = new ServerStreams(ois, oos);
			workerList.toCloud = toCloud;
			DataPackage initalMessage = DataPackage.obtain(Msg.SUPPORT_OFFLOAD);
			toCloud.send(initalMessage);
			System.out.println("Checking Cloud Support...");
			pool.execute(new CloudReceiving(toCloud));
		} catch (IOException e) {
			Log.e(TAG, "Cannot Check Router Support " + e.getMessage());
			e.printStackTrace();
			connectedToCloud = false;
		}
		return connectedToCloud;
	}

	public void stopServer() {
		synchronized (isServerShutdown) {
			if (!isServerShutdown) {
				isServerShutdown = true;
				try {
					serverSocket.close();
//					socketCloud.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				serverSocket = null;
			}
		}
		System.out.println("Server Stopped");
	}

	class CloudReceiving implements Runnable {
		ServerStreams toCloud = null;

		public CloudReceiving(ServerStreams toCloudStreams) {
			super();
			this.toCloud = toCloudStreams;

		}

		@Override
		public void run() {
			DataPackage receive = DataPackage.obtain(Msg.NONE);
			DataPackage sentMessage = null;
			String apkFilePath = "";
			File dexFile = null;
			boolean connectionloss = false;
			while (!connectionloss && receive != null) {
				try {
					receive = toCloud.receive();
					MainActivity.append("Received message: " + (receive != null ? receive.what : "null") + " From Cloud");
					System.out.println("Received message: " + (receive != null ? receive.what : "null") + " From Cloud");
				} catch (IOException e) {
					connectionloss = true;
					System.out.println("Connection Loss");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					connectToCloud();
					break;
				}
				switch (receive.what) {
				case PING:
					sentMessage = DataPackage.obtain(Msg.PONG);

					try {
						toCloud.send(sentMessage);
					} catch (IOException e1) {
						e1.printStackTrace();
					}

					break;
				case SUPPORT_OFFLOAD:
					connectedToCloud = true;
					System.out.println("Check Cloud Support : " + connectedToCloud);
					break;
				case INIT_OFFLOAD:
					String hashName = (String) receive.deserialize();
					// System.out.println("HashName: " + hashName);
					sentMessage = DataPackage.obtain(Msg.READY);
					try {
						toCloud.send(sentMessage);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.out.println("Router send messeage: " + Msg.READY);
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
						toCloud.send(sentMessage);
						// try {
						// Thread.sleep(10000);
						// } catch (InterruptedException e) {
						// e.printStackTrace();
						// }
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case EXECUTE:
					break;
				case REQUEST_STATUS:
					break;
				case RESULT:
					receive.rttRouterToVM = System.currentTimeMillis() - receive.rttRouterToVM;
					System.out.println("RTT(Router->Manager->VM->Manager->Router) = " + receive.rttRouterToVM / 1000f);
					InetAddress deviceID = (InetAddress) receive.source;
					Device device = workerList.getDevices(deviceID);
					receive.finish = false;
					try {
						device.streams.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Results was recieved and sent to its origin");
					break;
				case FINISH:
					deviceID = (InetAddress) receive.source;
					device = workerList.getDevices(deviceID);
					try {
						device.streams.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case PONG:
					long rtt = System.currentTimeMillis() - rttStart;
					System.out.println("RTT = " + rtt / 1000f);
					break;

				case FREE:
					break;

				}
			}
			toCloud.tearDownStream();
			return;
		};
	}

	long rttStart = 0;
	Runnable sendingThread = new Runnable() {
		public void run() {
			while (true) {
				try {
					System.out.println("sending PING");
					rttStart = System.currentTimeMillis();
					toCloud.send(DataPackage.obtain(Msg.PING));
					Thread.sleep(10000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	class DeviceReceiving implements Runnable {
		private Device device;

		// private String clientId = "";

		public DeviceReceiving(Device device) {
			super();
			this.device = device;
		}

		@Override
		public void run() {
			DataPackage receive = DataPackage.obtain(Msg.NONE);
			DataPackage sentMessage = null;
			boolean connectionBroken = false;
			while (!connectionBroken && receive != null) {
				try {
					receive = device.streams.receive();
					MainActivity.append("Received message: " + (receive != null ? receive.what : "null") + " From Smartphone");
					System.out.println("Received message: " + (receive != null ? receive.what : "null") + " From Smartphone");
				} catch (IOException e) {
					System.out.println("Connection Loss");
					connectionBroken = true;
					break;
				}
				if (connectionBroken || receive == null)
					break;
				switch (receive.what) {
				case SUPPORT_OFFLOAD:
					// clientId = (String) receive.data;
					// device.id = clientId;
					workerList.addDevice(device);
					System.out.println(workerList.devices.size() + " devices are connected.");
					sentMessage = DataPackage.obtain(Msg.SUPPORT_OFFLOAD);
					try {
						device.streams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}

					break;
				case REGISTER:
					System.out.println("Device " + device.ip.toString() + " is registered");
					device.state = DeviceState.STATE_AVAILABLE;
					int num = 0;
					for (Device d : workerList.devices) {
						if (d.state == DeviceState.STATE_AVAILABLE) {
							num++;
						}
					}
					sentMessage = DataPackage.obtain(Msg.REGISTERED);
					try {
						device.streams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println(num + " devices is available for helping.");
					break;
				case UNREGISTER:
					device.state = DeviceState.STATE_NOT_AVAILABLE;
					System.out.println("Device " + device.ip.toString() + " is unregistered");
					num = 0;
					for (Device d : workerList.devices) {
						if (d.state == DeviceState.STATE_AVAILABLE) {
							num++;
						}
					}
					System.out.println(num + " devices is available for helping.");
					sentMessage = DataPackage.obtain(Msg.UNREGISTERED);
					try {
						device.streams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case OFFLOAD:
					// ArrayList<InetAddress> list = new
					// ArrayList<InetAddress>();
					// if (connectedToCloud) {
					// list.add(device.socket.getLocalAddress());
					// sentMessage = DataPackage.obtain(Msg.CLOUD, list);
					// } else {
					// System.out.println("Number of total connected devices : "
					// + workerList.devices.size());
					// for (Device d : workerList.devices) {
					// if (d.state == DeviceState.STATE_AVAILABLE) {
					// list.add(d.ip);
					// break;
					// }
					// }
					// sentMessage = DataPackage.obtain(Msg.DEVICE_LIST, list);
					// }
					// list.add(toCloud.getLocalIpAddress());
					// try {
					// device.streams.send(sentMessage);
					// } catch (IOException e) {
					// e.printStackTrace();
					// }

					break;
				case INIT_OFFLOAD:
					sentMessage = DataPackage.obtain(Msg.READY);
					try {
						device.streams.send(sentMessage);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case FREE:
					String name = (String) receive.deserialize();
					// statusTable.setState(name, WorkerList.STATE_AVAILABLE);
					Log.d(TAG, "Free device: " + name);
					break;
				case RESPONSE_STATUS:
					Status s = (Status) receive.deserialize();
					// if (clientId != "")
					// statusTable.setStatus(clientId, s);
					// else
					// Log.e(TAG, "This device is not registered!");
					break;
				case EXECUTE:
					receive.rttRouterToVM = System.currentTimeMillis();
					if (connectedToCloud && MainActivity.useCloud) {
						System.out.println("*Sending task to the Cloud*");
						try {
							workerList.toCloud.send(receive);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						for (Device d : workerList.devices) {
							if (d.state == DeviceState.STATE_AVAILABLE) {
								try {
									System.out.println("*Sending method to offloadee " + d.ip + "*");
									d.streams.send(receive);
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
							}
						}
					}
					device.streams = device.streams;
					break;
				case RESULT:
					receive.rttRouterToVM = System.currentTimeMillis() - receive.rttRouterToVM;
					System.out.println("RTT(Router->Offloadee->Router) = " + receive.rttRouterToVM / 1000f);
					InetAddress deviceID = (InetAddress) receive.source;
					Device device = workerList.getDevices(deviceID);
					try {
						device.streams.send(receive);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Results was recieved and sent to its origin");
					break;
				}

			}

		}
	}

}