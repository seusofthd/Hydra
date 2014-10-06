package com.symlab.hydrarouter;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import com.symlab.hydra.network.ServerStreams;
import com.symlab.hydra.status.Status;

public class WorkerList {
	private static final String TAG = "WorkerList";

	public ServerStreams toCloud;
	ArrayList<Device> devices;

	public WorkerList() {
		devices = new ArrayList<Device>();
	}

	public synchronized void addDevice(Device device) {
		devices.add(device);
	}

	public void removeDevice(Device device) {
		devices.remove(device);
	}

	public Device getDevices(String id) {
		for (Device d : devices) {
			if (d.id.equals(id)) {
				return d;
			}
		}
		return null;
	}

	public Device getDevices(InetAddress ip) {
		for (Device d : devices) {
			System.out.println(d.ip + "is found.");
			if (d.ip.equals(ip)) {
				return d;
			}
		}
		return null;
	}

}

class Device {
	String id;
	InetAddress ip;
	Status status;
	DeviceState state;
	Socket socket;
	ServerStreams streams;

	public Device(String id, InetAddress ip, Status status, DeviceState state, ServerStreams streams, Socket socket) {
		super();
		this.id = id;
		this.ip = ip;
		this.status = status;
		this.state = state;
		this.streams = streams;
		this.socket = socket;
	}

}

enum DeviceState {
	STATE_AVAILABLE, STATE_OCCUPIED, STATE_LOWPOWER, STATE_NOT_AVAILABLE
}