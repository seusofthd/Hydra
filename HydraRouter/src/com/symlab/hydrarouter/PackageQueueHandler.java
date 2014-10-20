package com.symlab.hydrarouter;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

import android.util.Log;

import com.symlab.hydra.lib.TaskQueue;
import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;

public class PackageQueueHandler implements Observer {

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
	PacketQueue packetQueue;
	PrintStream printStream;
	WorkerList workerList;

	public PackageQueueHandler(RouterServer routerServer, WorkerList workerList, PacketQueue packetQueue) {
		lock = new Object();
		this.packetQueue = packetQueue;
		this.workerList = workerList;
	}

	@Override
	public void update(Observable observable, Object data) {
		if (observable == packetQueue) {
			if (packetQueue.queueSize() > 0) {
				DataPackage dp = packetQueue.dequeue();
				System.out.println("Processing " + dp.what);
				switch (dp.what) {
				case INIT_OFFLOAD:
					switch (dp.destination) {
					case CLOUD:
						dp.rttRouterToVM = System.currentTimeMillis();
						dp.destination = Msg.CLOUD;
						dp.dest = workerList.toCloud.socket.getInetAddress();
						send(dp, dp.dest);
						break;
					case SMARTPHONE:
						for (Device d : workerList.devices) {
							if (d.state == DeviceState.STATE_AVAILABLE) {
								dp.destination = Msg.SMARTPHONE;
								dp.dest = d.ip;
								System.out.println(dp.dest);
								dp.rttRouterToVM = System.currentTimeMillis();
								send(dp, dp.dest);
								packetQueue.isSmartphoneBusy = true;
								break;
							}
						}
						break;

					case GREEDY:
						if (!packetQueue.isCloudBusy) {
							dp.rttRouterToVM = System.currentTimeMillis();
							dp.destination = Msg.CLOUD;
							dp.dest = workerList.toCloud.socket.getInetAddress();
							send(dp, dp.dest);
							packetQueue.isCloudBusy = true;
						} else if (!packetQueue.isSmartphoneBusy) {
							for (Device d : workerList.devices) {
								if (d.state == DeviceState.STATE_AVAILABLE) {
									dp.destination = Msg.SMARTPHONE;
									dp.dest = d.ip;
									dp.rttRouterToVM = System.currentTimeMillis();
									send(dp, dp.dest);
									packetQueue.isSmartphoneBusy = true;
									break;
								}
							}
						}
						break;

					default:
						if (MainActivity.useCloud) {
							dp.rttRouterToVM = System.currentTimeMillis();
							dp.destination = Msg.CLOUD;
							dp.dest = workerList.toCloud.socket.getInetAddress();
							send(dp, dp.dest);
						} else {
							for (Device d : workerList.devices) {
								if (d.state == DeviceState.STATE_AVAILABLE) {
									dp.destination = Msg.SMARTPHONE;
									dp.dest = d.ip;
									dp.rttRouterToVM = System.currentTimeMillis();
									send(dp, dp.dest);
									packetQueue.isSmartphoneBusy = true;
									break;
								}
							}
						}
						break;
					}
					break;

				case APK_REQUEST:
					send(dp, dp.source);
					break;
				case APK_SEND:
					send(dp, dp.dest);
					break;
				case READY:
					send(dp, dp.source);
					break;
				case EXECUTE:
					send(dp, dp.dest);
					break;
				default:
					break;
				}

			}
		}
	}

	private void send(DataPackage dp, InetAddress ip) {
		if (ip.equals(workerList.toCloud.socket.getInetAddress())) {
			try {
				workerList.toCloud.send(dp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			for (Device d : workerList.devices) {
				if (d.ip.equals(ip)) {

					try {
						d.streams.send(dp);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}
	}

	void setLocalNotOccupied() {
		synchronized (localLock) {
			occupied = false;
			Log.e(TAG, "LOCAL, free");
		}
	}

	public void pause() {
		synchronized (lock) {
			paused = true;
		}
	}

	public void resume() {
		synchronized (lock) {
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
