package com.symlab.hydracloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.network.EC2Instance;

public class Main {

	ArrayList<EC2Instance> availableInstances = new ArrayList<EC2Instance>();

	public Main() {
		try {
			InetAddress ipPub0 = InetAddress.getByName(Constants.VM0_IP_PUB);
			InetAddress ipPrv0 = InetAddress.getByName(Constants.VM0_IP_PRV);
			EC2Instance managerInstance = new EC2Instance(Constants.VM0_NAME, Constants.VM0_ID, "", null, ipPub0, ipPrv0, Constants.CLOUD_PORT, Constants.VM_REGION);
			availableInstances.add(managerInstance);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void RunVMManager() {
		NetworkManagerServer nm = new NetworkManagerServer(availableInstances);
		Thread thread = new Thread(nm);
		thread.start();
		nm.makeconnection();
	}

	public static void main(String[] args) {
		Main main = new Main();
		main.RunVMManager();
	}

}
