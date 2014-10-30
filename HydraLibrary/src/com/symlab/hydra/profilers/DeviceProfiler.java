package com.symlab.hydra.profilers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.provider.Settings;

import com.symlab.hydra.status.DeviceStatus;

public class DeviceProfiler {
	public static int batteryLevel;
	public static boolean batteryTrackingOn = false;

	/** Not valid value of brightness */
	private static final int NOT_VALID = -1;

	public long batteryVoltageDelta;

	private Context context;
	private long mStartBatteryVoltage;

	/**
	 * Variables for CPU Usage
	 */
	private int PID;
	private ArrayList<Long> pidCpuUsage;
	private ArrayList<Long> systemCpuUsage;
	private long uTime;
	private long sTime;
	private long pidTime;
	private long diffPidTime;
	private long prevPidTime;
	private long userMode;
	private long niceMode;
	private long systemMode;
	private long idleTask;
	private long ioWait;
	private long irq;
	private long softirq;
	private long runningTime;
	private long prevrunningTime;
	private long diffRunningTime;
	private final String pidStatFile;
	private final String statFile;
	private long diffIdleTask;
	private long prevIdleTask;
	private ArrayList<Long> idleSystem;
	private ArrayList<Integer> screenBrightness;
	private HandlerThread updatingThread;
	private Handler pidCpuUsageHandler;
	private Handler screenBrightnessHandler;
	private PowerManager powerManager;

	public ArrayList<Integer> cpuUsage;

	/**
	 * Variables for CPU frequency<br>
	 * Obtained reading the files:<br>
	 * /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq<br>
	 * /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq<br>
	 * /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq
	 */
	private int currentFreq; // The current frequency
	private ArrayList<Integer> frequence;
	private final String curFreqFile = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";

	public DeviceProfiler(Context context) {
		this.context = context;
		updatingThread = new HandlerThread("Updating Device");
		updatingThread.setPriority(Thread.MIN_PRIORITY);
		updatingThread.start();
		pidCpuUsageHandler = new Handler(updatingThread.getLooper());
		screenBrightnessHandler = new Handler(updatingThread.getLooper());
		powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		// initializeVariables();
		PID = android.os.Process.myPid();
		pidStatFile = "/proc/" + PID + "/stat";
		statFile = "/proc/stat";

	}

	private void initializeVariables() {
		pidCpuUsage = new ArrayList<Long>();
		systemCpuUsage = new ArrayList<Long>();
		idleSystem = new ArrayList<Long>();
		frequence = new ArrayList<Integer>();
		screenBrightness = new ArrayList<Integer>();
		cpuUsage = new ArrayList<Integer>();
	}

	/**
	 * Start device information tracking from a certain point in a program
	 * (currently only battery voltage)
	 */
	public void startDeviceProfiling() {
		initializeVariables();
		mStartBatteryVoltage = (long) DeviceStatus.batteryVoltage;
		calculatePidCpuUsage();
		calculateScreenBrightness();
	}

	/**
	 * Stop device information tracking and store the data in the object
	 */
	public void stopAndCollectDeviceProfiling() {
		stopCalculatingPidCpuUsage();
		stopCalculatingScreenBrightness();
		batteryVoltageDelta = (long) DeviceStatus.batteryVoltage - mStartBatteryVoltage;
	}

	boolean firstTime;

	private void calculatePidCpuUsage() {
		firstTime = true;
		pidCpuUsageHandler.post(updatePidCpuUsage);
	}

	private Runnable updatePidCpuUsage = new Runnable() {
		public void run() {
			calculateProcessExecutionTime();
			calculateSystemExecutionTime();
			getCurrentCpuFreq();

			if (!firstTime) {

				pidCpuUsage.add(diffPidTime);
				systemCpuUsage.add(diffRunningTime);

				frequence.add(currentFreq);

				idleSystem.add(diffIdleTask);

			}
			int cpuCur = 0;
			try {
				cpuCur = (int) (100 * (diffRunningTime - diffIdleTask) / diffRunningTime);
			} catch (Exception e) {
			}
			if (cpuCur >= 0 && cpuCur <= 100)
				cpuUsage.add(cpuCur);

			prevPidTime = pidTime; // Log.d("DeviceProfiler", "pidTime: " +
									// pidTime);
			prevrunningTime = runningTime; // Log.d("DeviceProfiler",
											// "runningTime: " + runningTime);
			prevIdleTask = idleTask; // Log.d("DeviceProfiler", "idleTask: " +
										// idleTask);

			firstTime = false;

			pidCpuUsageHandler.postDelayed(updatePidCpuUsage, 1000);

		}
	};

	private void stopCalculatingPidCpuUsage() {
		pidCpuUsageHandler.removeCallbacks(updatePidCpuUsage);
	}

	/**
	 * Open the file "/proc/$PID/stat" and read utime and stime<br>
	 * <b>utime</b>: execution of process in user mode (in jiffies)<br>
	 * <b>stime</b>: execution of process in kernel mode (in jiffies)<br>
	 * These are 14th and 15th variables respectively in the file<br>
	 * The sum <b>pidTime = utime + stime</b> gives the total running time of
	 * process<br>
	 * <b>diffPidTime</b> is the running time of process during the last second<br>
	 * 
	 * @author Sokol
	 */
	private void calculateProcessExecutionTime() {
		try {

			FileReader inPidStat = new FileReader(pidStatFile);
			BufferedReader brPidStat = new BufferedReader(inPidStat);

			String strLine = brPidStat.readLine();

			StringTokenizer st = new StringTokenizer(strLine);

			for (int i = 1; i < 14; i++)
				st.nextToken();

			uTime = Long.parseLong(st.nextToken());
			sTime = Long.parseLong(st.nextToken());
			pidTime = uTime + sTime;
			diffPidTime = pidTime - prevPidTime;

			brPidStat.close();

		} catch (IOException e) {// Catch exception if any
			// Log.d("CpuUsage", "Could not read the file " + pidStatFile);
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (NumberFormatException n) {
			// Log.d("CpuUsage", "Number is not Long");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (Exception e) {
			// Log.d("CpuUsage", "Some error happened");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		}
	}

	/**
	 * Open the file "/proc/stat" and read information about system execution<br>
	 * <b>userMode</b>: normal processes executing in user mode (in jiffies)<br>
	 * <b>niceMode</b>: niced processes executing in user mode (in jiffies)<br>
	 * <b>systemMode</b>: processes executing in kernel mode (in jiffies)<br>
	 * <b>idleTask</b>: twiddling thumbs (in jiffies)<br>
	 * <b>runningTime</b>: total time of execution (in jiffies)<br>
	 * <b>ioWait</b>: waiting for I/O to complete (in jiffies)<br>
	 * <b>irq</b>: servicing interrupts (in jiffies)<br>
	 * <b>softirq</b>: servicing softirq (in jiffies)<br>
	 * <b>diffRunningTime</b>: time of execution during the last second (in
	 * jiffies)<br>
	 * 
	 * @author Sokol
	 */
	private void calculateSystemExecutionTime() {
		try {

			FileReader inStat = new FileReader(statFile);
			BufferedReader brStat = new BufferedReader(inStat);

			String strLine = brStat.readLine();

			StringTokenizer st = new StringTokenizer(strLine);
			st.nextToken();

			userMode = Long.parseLong(st.nextToken());
			niceMode = Long.parseLong(st.nextToken());
			systemMode = Long.parseLong(st.nextToken());
			idleTask = Long.parseLong(st.nextToken());
			ioWait = Long.parseLong(st.nextToken());
			irq = Long.parseLong(st.nextToken());
			softirq = Long.parseLong(st.nextToken());

			runningTime = userMode + niceMode + systemMode + idleTask + ioWait + irq + softirq;
			diffRunningTime = runningTime - prevrunningTime;
			diffIdleTask = idleTask - prevIdleTask;

			brStat.close();

		} catch (IOException e) {// Catch exception if any
			// Log.d("SCpuUsage", "Could not read the file " + statFile);
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (NumberFormatException n) {
			// Log.d("SCpuUsage", "Number is not Long");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (Exception e) {
			// Log.d("SCpuUsage", "Some error happened");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		}
	}

	public static int getMaxCpuFreq() {
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

	public static int getMinCpuFreq() {
		String minFreqFile1 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
		String minFreqFile2 = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq";
		int result = 0;
		FileReader inFreq = null;
		BufferedReader brFreq = null;
		try {
			inFreq = new FileReader(minFreqFile1);
			brFreq = new BufferedReader(inFreq);

			String strLine = brFreq.readLine();

			result = Integer.parseInt(strLine);

			// Log.d("PowerDroid-CpuUsage", "Min Freq is " + result);

		} catch (IOException e) {// Catch exception if any
			// Log.d("PowerDroid-CpuUsage", "Could not read the file " +
			// minFreqFile1);

			String strLine;
			try {
				inFreq = new FileReader(minFreqFile2);
				brFreq = new BufferedReader(inFreq);
				strLine = brFreq.readLine();
				result = Integer.parseInt(strLine);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// Log.d("PowerDroid-CpuUsage", "Min Freq is " + result);
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
		}
		return result;
	}

	private void getCurrentCpuFreq() {
		try {
			FileReader inFreq = new FileReader(curFreqFile);
			BufferedReader brFreq = new BufferedReader(inFreq);

			String strLine = brFreq.readLine();

			currentFreq = Integer.parseInt(strLine);

			brFreq.close();

		} catch (IOException e) {// Catch exception if any
			// Log.d("PowerDroid-CpuUsage", "Could not read the file " +
			// curFreqFile);
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (NumberFormatException n) {
			// Log.d("PowerDroid-CpuUsage", "Number is not Integer");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		} catch (Exception e) {
			// Log.d("PowerDroid-CpuUsage", "Some error happened");
			stopCalculatingPidCpuUsage();
			stopCalculatingScreenBrightness();
		}
	}

	private void calculateScreenBrightness() {
		screenBrightnessHandler.post(updateScreenBrightness);
	}

	private Runnable updateScreenBrightness = new Runnable() {
		public void run() {

			if (!powerManager.isScreenOn()) {
				screenBrightness.add(-1);
			} else {
				int brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, NOT_VALID);
				screenBrightness.add(brightness);
			}
			screenBrightnessHandler.postDelayed(updateScreenBrightness, 1000);

		}
	};

	private void stopCalculatingScreenBrightness() {
		screenBrightnessHandler.removeCallbacks(updateScreenBrightness);
	}

	public int getSeconds() {
		return pidCpuUsage.size();
	}

	public long getSystemCpuUsage(int i) {
		return systemCpuUsage.get(i);
	}

	public long getPidCpuUsage(int i) {
		return pidCpuUsage.get(i);
	}

	public int getFrequence(int i) {
		return frequence.get(i);
	}

	public long getIdleSystem(int i) {
		return idleSystem.get(i);
	}

	public int getScreenBrightness(int i) {
		return screenBrightness.get(i);
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
}
