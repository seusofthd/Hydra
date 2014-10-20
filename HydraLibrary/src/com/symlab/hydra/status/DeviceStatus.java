package com.symlab.hydra.status;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class DeviceStatus {

	private Context context;
	private Status status = new Status();;
	public int battery;
	private boolean isCharging;

	private final IntentFilter intentFilter = new IntentFilter();
	private BroadcastReceiver receiver = null;
	private ActivityManager activityManager;
	private MemoryInfo mi;
	private Runtime runtime;

	private boolean receiverRegistered = false;

	private static DeviceStatus ds = null;

	public static int batteryVoltage;
	public static float batteryLevel;

	private DeviceStatus(Context context) {
		this.context = context;
		runtime = Runtime.getRuntime();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		// intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
		// intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
		receiver = new BatteryBroadcastReceiver();
		context.registerReceiver(receiver, intentFilter);
		receiverRegistered = true;
		activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		mi = new MemoryInfo();

	}

	public static DeviceStatus newInstance(Context context) {
		if (ds == null) {
			ds = new DeviceStatus(context);
		}
		return ds;
	}

	public Status readStatus() {
		Status status = new Status();
		status.numOfProcessors = runtime.availableProcessors();
		status.cpuFrequency = getMaxCpuFreq();
		status.cpuIdleness = 100f - readCpuUsage() * 100f;
		// try {
		activityManager.getMemoryInfo(mi);
		status.memoryFree = mi.availMem / 1048576f;
		/*
		 * } catch (Exception ex) { if (activityManager == null)
		 * Log.d("DeviceStatus", "activityManager is null");
		 * ex.printStackTrace(); }
		 */
		status.isCharging = isCharging;
		status.batteryPercentage = battery;
		// Log.d("DeviceStatus", String.format("Show Battery = %f",
		// status.batteryPercentage));
		return status;
	}

	public float readCpuUsage() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);
			} catch (Exception e) {
			}

			reader.seek(0);
			load = reader.readLine();
			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}

	private int getMaxCpuFreq() {
		String maxFreqFile1 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
		String maxFreqFile2 = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
		int result = 0;
		FileReader inFreq = null;
		BufferedReader brFreq = null;
		try {
			inFreq = new FileReader(maxFreqFile1);
			brFreq = new BufferedReader(inFreq);

			String strLine = brFreq.readLine();

			result = Integer.parseInt(strLine);

			// Log.d("PowerDroid-CpuUsage", "Max Freq is " + result);

		} catch (IOException e) {// Catch exception if any
			// Log.d("PowerDroid-CpuUsage", e.toString());
			// Log.d("PowerDroid-CpuUsage", "Could not read the file " +
			// maxFreqFile1);

			String strLine;
			try {
				inFreq = new FileReader(maxFreqFile2);
				brFreq = new BufferedReader(inFreq);
				strLine = brFreq.readLine();
				result = Integer.parseInt(strLine);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// Log.d("PowerDroid-CpuUsage", "Max Freq is " + result);

			return result;

		} catch (NumberFormatException n) {
			// Log.d("PowerDroid-CpuUsage", "Number is not Integer");
		} catch (Exception e) {
			// Log.d("PowerDroid-CpuUsage", "Some error happened");
		} finally {
			try {
				brFreq.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// return result;
		}
		return result;
	}

	private class BatteryBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				// Log.d("DeviceStatus", "receive ACTION_BATTERY_CHANGED");
				// int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
				// -1);
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				// int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
				// -1);
				int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				// int temperature =
				// intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
				int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
				// Log.d("DeviceStatus", String.format("voltage = %d",
				// voltage));
				battery = 100 * level / scale;
				isCharging = plugged != 0;
				batteryVoltage = voltage;
				batteryLevel = battery;
				// readStatus();
				// Log.d("DeviceStatus", String.format("Battery = %f",
				// batteryPercentage));

			}

		}

	}

	public void tearDown() {
		if (receiverRegistered) {
			context.unregisterReceiver(receiver);
			receiverRegistered = false;
		}
	}

}
