package com.symlab.hydra.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;

public class ServerStreams {

	private ObjectInputStream objIn;
	private ObjectOutputStream objOut;
	public Socket socket;

	public ServerStreams(ObjectInputStream ois, ObjectOutputStream oos) {
		this.objIn = ois;
		this.objOut = oos;
	}

	public ServerStreams(ObjectInputStream ois, ObjectOutputStream oos, Socket socket) {
		this.objIn = ois;
		this.objOut = oos;
		this.socket = socket;
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

	public void tearDownStream() {
		try {
			objIn.close();
			objOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
