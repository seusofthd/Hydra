package com.symlab.hydra;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;
import com.symlab.hydra.network.ToRouterConnection;

public class OffloadRequest implements Callable<ArrayList<InetAddress>> {

	private ToRouterConnection toRouter;
	private int requestNum;

	public OffloadRequest(ToRouterConnection toRouter, int num) {
		this.toRouter = toRouter;
		requestNum = num;
	}

	@Override
	public ArrayList<InetAddress> call() {
		ArrayList<InetAddress> ret = null;
		try {
			toRouter.streams.send(DataPackage.obtain(Msg.OFFLOAD, Integer.valueOf(requestNum)));
			DataPackage receive = toRouter.streams.receive();
			System.out.println("Receive message: " + receive.what);
			if (receive.what == Msg.DEVICE_LIST) {
				ret = (ArrayList<InetAddress>) receive.deserialize();
			} else if (receive.what == Msg.CLOUD) {
				ret = (ArrayList<InetAddress>) receive.deserialize();
				// ret = new ArrayList<InetAddress>();
				// ret.add(InetAddress.getByName(Msg.CLOUD_IP));
			}
		} catch (IOException e) {

		}
		return ret;
	}

}
