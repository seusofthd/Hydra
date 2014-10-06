package com.symlab.hydra.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.util.Log;

import com.symlab.hydra.NodeServer;
import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.lib.DynamicObjectInputStream;

public class ServiceRegister {

	private static final String TAG = "ServiceRegister";

	private Context context;
	private OffloadeeServer serverThread;

	private ToRouterConnection toRouter;
	private String deviceId;
	private String thisApkPath;

	private ServerSocket serverSocket;
	private Boolean isServerShutdown = true;

	private Socket mSocket = null;
	private DynamicObjectInputStream ois = null;
	private ObjectOutputStream oos = null;

	public ServiceRegister(Context context, ToRouterConnection toRouter, String thisApkPath) {
		this.context = context;
		this.toRouter = toRouter;
		this.deviceId = toRouter.myId;
		this.thisApkPath = thisApkPath;
	}

	public void registerService() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					registerToRouter();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// if (registered) {
				// Thread t = new NodeServer(toRouter.streams, context,
				// toRouter, thisApkPath);
				// t.start();
				// System.out.println("NodeServer is Created");
				// }
				// startServer();
			}
		}).start();
	}

	public void unregisterService() {
		new Thread(new Runnable() {
			@Override
			public void run() {
//				boolean unregistered = false;
				unregisterFromRouter();
//				if (unregistered){
					stopServer();
//				}
			}
		}).start();
	}

	private class OffloadeeServer extends Thread {

		@Override
		public void run() {

			try {
				serverSocket = new ServerSocket(Constants.DEVICE_PORT);
			} catch (IOException e) {
				Log.e(TAG, "Cannot create Server Socket: " + e.getMessage());
				e.printStackTrace();

			}
			while (!isServerShutdown) {
				try {
					Log.d(TAG, "Waiting for connection...");
					mSocket = serverSocket.accept();
					if (mSocket != null) {
						Log.d(TAG, "Socket connected...");
						// ois = new DynamicObjectInputStream(new
						// BufferedInputStream(mSocket.getInputStream()));
						ois = new DynamicObjectInputStream(mSocket.getInputStream(), this.getClass().getClassLoader());
						// oos = new ObjectOutputStream(new
						// BufferedOutputStream(mSocket.getOutputStream()));
						oos = new ObjectOutputStream(mSocket.getOutputStream());
						Thread t = new NodeServer(new ServerStreams(ois, oos), context, toRouter, thisApkPath);
						t.start();
						/*
						 * try { t.join();
						 * 
						 * } catch (InterruptedException e) {
						 * e.printStackTrace(); }
						 */} else {
						Log.d(TAG, "NULL Socket connected");
					}
				} catch (IOException e) {

				}
			}
			try {
				if (mSocket != null)
					mSocket.close();
				if (serverSocket != null)
					serverSocket.close();
			} catch (IOException e) {

			}
		}
	}

	private void startServer() {
		synchronized (isServerShutdown) {
			if (isServerShutdown) {
				serverThread = new OffloadeeServer();
				isServerShutdown = false;
				serverThread.start();
			}
		}
	}

	private void stopServer() {
		synchronized (isServerShutdown) {
			if (!isServerShutdown) {
				isServerShutdown = true;
				try {
					serverSocket.close();
					serverSocket = null;
				} catch (IOException e) {

				}
			}
		}
	}

	private void registerToRouter() throws IOException {
		toRouter.streams.send(DataPackage.obtain(Msg.REGISTER, deviceId));
	}

	private void unregisterFromRouter() {
		try {
			toRouter.streams.send(DataPackage.obtain(Msg.UNREGISTER, deviceId));
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}
}
