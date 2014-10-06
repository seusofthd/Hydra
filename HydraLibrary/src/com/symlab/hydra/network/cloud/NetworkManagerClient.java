package com.symlab.hydra.network.cloud;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.util.Log;

public class NetworkManagerClient {
	int portnum;
	Socket mysocket = null;
	InputStream in = null;
	OutputStream out = null;
	ObjectInputStream ois = null;
	ObjectOutputStream oos = null;
	byte[] serverAddress = new byte[4];
	CloudController callingparent = null;
	long startTime = 0;

	public NetworkManagerClient(byte[] serverAddress, int port) {

		this.serverAddress = serverAddress;
		portnum = port;
	}

	public void setNmf(CloudController callingparent) {
		this.callingparent = callingparent;
	}

	public boolean connect() {
		mysocket = new Socket();
		try {
			mysocket.connect(new InetSocketAddress(Inet4Address.getByAddress(serverAddress), portnum), NetInfo.waitTime);

			startTime = System.currentTimeMillis();
			in = mysocket.getInputStream();
			out = mysocket.getOutputStream();
			oos = new ObjectOutputStream(out);
			ois = new ObjectInputStream(in);
			Log.i("NetworkManager", "Connection to amazon ec2 established");

			return true;
		} catch (IOException ex) {
			Log.e("NetworkManager", ex.getMessage());
			callingparent.setResult(null, null);
			return false;
		}
	}

	public void send(String functionName, Class[] paramTypes, Object[] funcArgValues, Object state, Class stateDType) {
		try {
			new Sending(new Pack(functionName, stateDType, state, funcArgValues, paramTypes)).send();
		} catch (Exception ex) {
			Log.e("NetworkManager", ex.getMessage());
			callingparent.setResult(null, null);
		}
	}

	class Sending implements Runnable {
		Pack MyPack = null;
		ResultPack result = null;

		public Sending(Pack MyPack) {
			this.MyPack = MyPack;
		}

		public void send() {
			Thread t = new Thread(this);
			t.start();
		}

		@Override
		public void run() {
			try {

				oos.writeObject(MyPack);
				oos.flush();

				result = (ResultPack) ois.readObject();

				Log.e("NetworkManager", "time out : " + ((System.currentTimeMillis() - startTime) < NetInfo.waitTime));
				if ((System.currentTimeMillis() - startTime) < NetInfo.waitTime) {
					Log.e("NetworkManager", "result : " + result);
					if (result == null) {

						callingparent.setResult(null, null);
					} else {
						Log.e("NetworkManager", "return result");
						callingparent.setResult(result.getresult(), result.getstate());
					}
				}

				oos.close();
				ois.close();

				in.close();
				out.close();

				mysocket.close();

				oos = null;
				ois = null;

				in = null;
				out = null;
				mysocket = null;

			} catch (Exception ex) {
				ex.printStackTrace();
				Log.e("NetworkManager", ex.getLocalizedMessage());
				callingparent.setResult(null, null);
			}
		}

	}

}
