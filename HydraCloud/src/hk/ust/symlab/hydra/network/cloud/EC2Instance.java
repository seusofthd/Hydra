package hk.ust.symlab.hydra.network.cloud;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

import com.symlab.hydra.network.ServerStreams;

public class EC2Instance {
	public String name;
	public String ID;
	public String type;
	public InstanceState state;
	public InetAddress elasticIP;
	public InetAddress privateIP;
	public int port;
	public String region;
	public int highConditionCount = 0;
	public int lowConditionCount = 0;
	public ServerStreams sstreams = null;

	public EC2Instance() {
	}

	public EC2Instance(String name, String ID, String type, InstanceState state, InetAddress elasticIP, InetAddress privateIP, int port, String region) {
		super();
		this.name = name;
		this.ID = ID;
		this.type = type;
		this.state = state;
		this.elasticIP = elasticIP;
		this.privateIP = privateIP;
		this.port = port;
		this.region = region;
	}

	private void runServer() {
		try {
			executeCommand("./android-x86/rund.sh -cp /android-x86/hydraCloud.apk edu.ut.mobile.network.Main");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getInstanceStatus() {
		// String ret = executeCommand("ec2-describe-instance-status " + ID +
		// " --region " + region);

	}

	public void exportKeys() {
		try {
			// System.out.println(executeCommand("ping -c 3 google.com"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String startInstance() {
		String ret = "";
		if (state == InstanceState.STOPPED) {
			try {
				ret = executeCommand("ec2-start-instances " + ID + " --region " + region);
				state = InstanceState.RUNNING;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public String stopInstance() {
		String ret = "";
		if (state == InstanceState.STOPPED) {
			try {
				ret = executeCommand("ec2-stop-instances " + ID + " --region " + region);
				state = InstanceState.STOPPING;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public String executeCommand(String command) throws Exception {
		StringBuffer output = new StringBuffer();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return output.toString();
	}

}

enum InstanceState {
	STARTING, RUNNING, STOPPING, STOPPED, TERMINATED, UNKNOWN
}
