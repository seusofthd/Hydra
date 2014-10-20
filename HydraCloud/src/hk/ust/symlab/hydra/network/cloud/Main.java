package hk.ust.symlab.hydra.network.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.content.Intent;

import com.symlab.hydra.lib.Constants;
import com.symlab.hydra.network.cloud.EC2Instance;
import com.symlab.hydracloud.CloudService;

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

	public void testExecutionSpeed() {
		int[] array = new int[100000];
		// System.out.println("start sorting...");
		boolean finished = false;
		int n = array.length;
		while (n > 0) {
			int new_n = 0;
			for (int i = 1; i < n; i++) {
				if (array[i - 1] > array[i]) {
					int temp = array[i - 1];
					array[i - 1] = array[i];
					array[i] = temp;
					new_n = i;
				}
			}
			n = new_n;
		}
		// float a = 1,b = 1;
		// for (int i = 0; i < 10000; i++) {
		// for (int j = 0; j < 100000; j++) {
		// a = (a + b + i);
		// b = 1/a;
		// }
		// }
		//
		// System.out.println("sorting is finished.");
	}

	public void RunVMClient() {
//		CloudService cloudService = new CloudService();
//		cloudService.onCreate();
//		cloudService.onStart(null, 0);
		
	}

	public static void main(String[] args) {
		System.out.println("Running VM Client...");
		Main main = new Main();
		main.RunVMClient();
	}

}
