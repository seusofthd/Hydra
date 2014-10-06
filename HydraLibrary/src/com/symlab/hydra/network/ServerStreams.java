package com.symlab.hydra.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.InetAddress;
import java.net.Socket;

import com.symlab.hydra.lib.DynamicObjectInputStream;

public class ServerStreams {

	private DynamicObjectInputStream objIn;
	private ObjectOutputStream objOut;
	private Socket socket;

	public ServerStreams(DynamicObjectInputStream ois, ObjectOutputStream oos) {
		this.objIn = ois;
		this.objOut = oos;
	}

	public ServerStreams(DynamicObjectInputStream ois, ObjectOutputStream oos, Socket socket) {
		this.objIn = ois;
		this.objOut = oos;
		this.socket = socket;
	}

	public InetAddress getLocalIpAddress() {
		return socket.getLocalAddress();
	}

	public void send(DataPackage data) throws IOException {
		synchronized (objOut) {
			objOut.writeObject(data);
			objOut.flush();
		}
	}

	public DataPackage receive() throws IOException {
		DataPackage ret = null;
		try {
			ret = (DataPackage) objIn.readObject();
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void addDex(String dexFile, String optDir) {
		objIn.addDex(dexFile, optDir);
	}

	public void tearDownStream() {
		try {
			objIn.close();
			objOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
