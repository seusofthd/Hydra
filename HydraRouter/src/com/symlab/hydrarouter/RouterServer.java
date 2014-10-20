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
import java.util.LinkedList;
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
	PacketQueue packetQueue;
	private Boolean isServerShutdown = true;

	public RouterServer(WorkerList workerList, PacketQueue packetQueue) {
		this.workerList = workerList;
		this.packetQueue = packetQueue;
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
			workerList.toCloud.socket = socketCloud;
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
					packetQueue.enqueue(receive);
					break;
				case APK_REQUEST:
					packetQueue.enqueue(receive);
					break;
				case APK_SEND:
					packetQueue.enqueue(receive);
					break;
				case READY:
					packetQueue.enqueue(receive);
					break;
				case EXECUTE:
					packetQueue.enqueue(receive);
					break;
				case REQUEST_STATUS:
					break;
				case RESULT:
					receive.rttRouterToVM = System.currentTimeMillis() - receive.rttRouterToVM;
					System.out.println("RTT(Router->Manager->VM->Manager->Router) = " + receive.rttRouterToVM / 1000f);
					InetAddress deviceID = (InetAddress) receive.source;
					Device device = workerList.getDevices(deviceID);
					receive.finish = false;
					packetQueue.isCloudBusy=false;
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
				case INIT_OFFLOAD:
					packetQueue.enqueue(receive);
					break;
				case APK_REQUEST:
					packetQueue.enqueue(receive);
					break;
				case APK_SEND:
					packetQueue.enqueue(receive);
					break;
				case READY:
					packetQueue.enqueue(receive);
					break;
				case EXECUTE:
					packetQueue.enqueue(receive);
					break;
				case FREE:
					String name = (String) receive.deserialize();
					// statusTable.setState(name, WorkerList.STATE_AVAILABLE);
					Log.d(TAG, "Free device: " + name);
					break;
				case RESPONSE_STATUS:
					Status s = (Status) receive.deserialize();
					break;
				case RESULT:
					receive.rttRouterToVM = System.currentTimeMillis() - receive.rttRouterToVM;
					System.out.println("RTT(Router->Offloadee->Router) = " + receive.rttRouterToVM / 1000f);
					InetAddress deviceID = (InetAddress) receive.source;
					Device device = workerList.getDevices(deviceID);
					packetQueue.isSmartphoneBusy=false;
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
