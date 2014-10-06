package com.symlab.hydra.profilers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Environment;

import com.symlab.hydra.db.DatabaseQuery;

public class Profiler {
	private ProgramProfiler progProfiler;
	private DeviceProfiler devProfiler;
	private Context mContext;

	private static final String storage = Environment.getExternalStorageDirectory().getPath();
	private static final String logFileName = storage + "/hydra-performanceLog.txt";
	private static FileWriter logFileWriter;

	public LogRecord lastLogRecord;

	private ArrayList<String> queryString;
	private DatabaseQuery query;

	// private final int MIN_FREQ = DeviceProfiler.getMinCpuFreq();
	private final int MAX_FREQ = DeviceProfiler.getMaxCpuFreq();
	private long totalEstimatedEnergy;
	private double estimatedCpuEnergy;
	private double estimatedScreenEnergy;

	private long estimatedRemoteEnergy;

	private static ArrayList<Long> remoteEnergy = new ArrayList<Long>();

	public static void addEnergy(Long l) {
		synchronized (remoteEnergy) {
			remoteEnergy.add(l);
		}
	}

	public Profiler(Context context, ProgramProfiler progProfiler, DeviceProfiler devProfiler) {
		this.progProfiler = progProfiler;
		this.devProfiler = devProfiler;
		this.mContext = context;

		if (logFileWriter == null)
			try {
				File logFile = new File(logFileName);
				logFile.createNewFile(); // Try creating new, if doesn't exist
				logFileWriter = new FileWriter(logFile, true);
			} catch (IOException e) {
				e.printStackTrace();
			}

	}

	public void startExecutionInfoTracking(String methodName) {
		progProfiler.methodName = methodName;
		remoteEnergy.clear();
		devProfiler.startDeviceProfiling();
		progProfiler.startExecutionInfoTracking();
	}

	/*
	 * public void startExecutionInfoTracking() { btProfiler.resetProfiler();
	 * devProfiler.startDeviceProfiling();
	 * progProfiler.startExecutionInfoTracking(); }
	 */
	/**
	 * Stop running profilers and log current information
	 * 
	 */
	public LogRecord stopAndLogExecutionInfoTracking(boolean receivedTask) {

		progProfiler.stopAndCollectExecutionInfoTracking();
		devProfiler.stopAndCollectDeviceProfiling();

		LogRecord record = new LogRecord(progProfiler, devProfiler);

		lastLogRecord = record;

		// estimateEnergyConsumption();

		record.energyConsumption = totalEstimatedEnergy;
		record.cpuEnergy = estimatedCpuEnergy;
		record.screenEnergy = estimatedScreenEnergy;
		record.bluetoothEnergy = 0f;

		// Log.i(" Profiler", "Log record - " + record.toString());
		if (!receivedTask) {
			try {
				if (logFileWriter == null) {
					File logFile = new File(logFileName);
					logFile.createNewFile(); // Try creating new, if doesn't
												// exist
					logFileWriter = new FileWriter(logFile, true);
				}

				logFileWriter.append(record.toString() + "\r\n");
				logFileWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			updateDB();
		}
		return record;
	}

	public static void recordTime(long time) {
		try {
			if (logFileWriter == null) {
				File logFile = new File(logFileName);
				logFile.createNewFile(); // Try creating new, if doesn't exist
				logFileWriter = new FileWriter(logFile, true);
			}

			logFileWriter.append((time / 1000000) + "ms\r\n");
			logFileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void recordEnergy(long energy) {
		try {
			if (logFileWriter == null) {
				File logFile = new File(logFileName);
				logFile.createNewFile(); // Try creating new, if doesn't exist
				logFileWriter = new FileWriter(logFile, true);
			}

			logFileWriter.append(energy + "mJ\r\n");
			logFileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void recordRemoteEnergy(long energy) {
		FileWriter fw = null;
		try {
			if (fw == null) {
				File logFile = new File(storage + "/remoteEnergy.txt");
				logFile.createNewFile(); // Try creating new, if doesn't exist
				fw = new FileWriter(logFile, true);
			}

			fw.append(energy + "mJ\r\n");
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void recordCpuUsage(LogRecord log) {
		try {
			if (logFileWriter == null) {
				File logFile = new File(logFileName);
				logFile.createNewFile(); // Try creating new, if doesn't exist
				logFileWriter = new FileWriter(logFile, true);
			}

			logFileWriter.append(log.cpuToString());
			logFileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void recordBatteryLevel(Integer b) {
		try {
			if (logFileWriter == null) {
				File logFile = new File(logFileName);
				logFile.createNewFile(); // Try creating new, if doesn't exist
				logFileWriter = new FileWriter(logFile, true);
			}

			logFileWriter.append(b.toString() + "\n");
			logFileWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateDB() {
		query = new DatabaseQuery(mContext);
		long meanExecDuration;
		long meanEnergyConsumption;
		long recordQuantity;
		String[] selectionArgs;
		String selection = "methodName = ?";

		// Populate the database
		query.appendData("methodName", lastLogRecord.methodName);

		selectionArgs = new String[] { lastLogRecord.methodName };

		/**
		 * Find previous executions of the method and update as needed:
		 * calculate the average of the execDuration including the current one
		 * and update it: for now I think to give more importance to the recent
		 * measure (exponential smoothing)
		 */
		queryString = query.getData(new String[] { "execDuration", "energyConsumption", "recordQuantity" }, selection, selectionArgs, null, null, "execDuration", " ASC");

		if (queryString.isEmpty()) {
			// The method has NOT been executed previosly, so insert info in the
			// db
			query.appendData("execDuration", "" + lastLogRecord.execDuration);
			query.appendData("energyConsumption", "" + lastLogRecord.energyConsumption);
			query.appendData("recordQuantity", "1");
			query.addRow();
		} else {
			// The method has been executed, so calculate the average
			// (exponential smooth)
			// and update the entry in db
			meanExecDuration = (lastLogRecord.execDuration + Long.parseLong(queryString.get(0))) / 2;
			meanEnergyConsumption = (lastLogRecord.energyConsumption + Long.parseLong(queryString.get(1))) / 2;
			recordQuantity = Long.parseLong(queryString.get(2)) + 1;
			query.appendData("execDuration", "" + meanExecDuration);
			query.appendData("energyConsumption", "" + meanEnergyConsumption);
			query.appendData("recordQuantity", "" + recordQuantity);
			query.updateRow();
		}

		queryString.clear();

		// Close the database
		try {
			query.destroy();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void estimateEnergyConsumption() {
		int duration = devProfiler.getSeconds();
		// Log.d("PowerDroid-Energy", "duration: " + duration +
		// "****************");
		estimatedCpuEnergy = estimateCpuEnergy(duration);
		// Log.d("PowerDroid-Energy", "CPU energy: " + estimatedCpuEnergy +
		// " mJ");

		estimatedScreenEnergy = estimateScreenEnergy(duration);
		// Log.d("PowerDroid-Energy", "Screen energy: " + estimatedScreenEnergy
		// + " mJ");

		// Log.d("PowerDroid-Energy", "Bluetooth energy: " +
		// estimatedBluetoothEnergy + " mJ");

		estimatedRemoteEnergy = estimatedRemoteEnergy();

		totalEstimatedEnergy = (long) (estimatedCpuEnergy + estimatedScreenEnergy) + estimatedRemoteEnergy;

		// Log.d("PowerDroid-Energy", "Total energy: " + totalEstimatedEnergy +
		// " mJ");
		// Log.d("PowerDroid-Energy",
		// "-------------------------------------------");
	}

	/**
	 * Estimate the Power for the CPU every send: P0, P1, P2, ..., Pt<br>
	 * where t is the execution time in seconds.<br>
	 * If we calculate the average power Pm = (P0 + P1 + ... + Pt) / t and
	 * multiply<br>
	 * by the execution time we obtain the Energy consumed by the CPU executing
	 * the method.<br>
	 * This is: E_cpu = Pm * t which is equal to: E_cpu = P0 + P1 + ... + Pt<br>
	 * NOTE: This is due to the fact that we measure every second.<br>
	 * 
	 * @param duration
	 *            Duration of method execution
	 * @return The estimated energy consumed by the CPU (mJ)
	 * 
	 * @author Sokol
	 */
	private double estimateCpuEnergy(int duration) {
		double estimatedCpuEnergy = 0;
		double betaUh = 4.34;
		double betaUl = 3.42;
		double betaCpu = 121.46;
		byte freqL = 0, freqH = 0;
		int util;
		byte cpuON;

		for (int i = 0; i < duration; i++) {
			util = calculateCpuUtil(i);

			if (devProfiler.getFrequence(i) == MAX_FREQ)
				freqH = 1;
			else
				freqL = 1;

			/**
			 * If the CPU has been in idle state for more than 90 jiffies<br>
			 * then decide to consider it in idle state for all the second (1
			 * jiffie = 1/100 sec)
			 */
			cpuON = (byte) ((devProfiler.getIdleSystem(i) < 90) ? 1 : 0);

			estimatedCpuEnergy += (betaUh * freqH + betaUl * freqL) * util + betaCpu * cpuON;

			freqH = 0;
			freqL = 0;
		}

		return estimatedCpuEnergy;
	}

	private int calculateCpuUtil(int i) {
		return (int) Math.ceil(100 * devProfiler.getPidCpuUsage(i) / devProfiler.getSystemCpuUsage(i));
	}

	private double estimateScreenEnergy(int duration) {
		double estimatedScreenEnergy = 0;
		double betaBrightness = 2.4;

		for (int i = 0; i < duration; i++) {
			if (devProfiler.getScreenBrightness(i) == -1)
				continue;
			estimatedScreenEnergy += betaBrightness * devProfiler.getScreenBrightness(i);
		}

		return estimatedScreenEnergy;

	}

	private long estimatedRemoteEnergy() {
		long ret = 0;
		for (Long l : remoteEnergy) {
			ret += l;
		}
		return ret;
	}

}
