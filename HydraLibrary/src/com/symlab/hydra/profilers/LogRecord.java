package com.symlab.hydra.profilers;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class LogRecord implements Parcelable {
	public String methodName;
	public Long execDuration;
	public Long energyConsumption;
	public double cpuEnergy;
	public double screenEnergy;
	public double bluetoothEnergy;

	public ArrayList<Integer> cpuUsage;
	// public ArrayList<Float> batteryLevels;

	// public int instructionCount;
	// public int methodCount;

	public Long batteryVoltageChange;

	public LogRecord(ProgramProfiler progProfiler, DeviceProfiler devProfiler) {
		methodName = progProfiler.methodName;
		execDuration = progProfiler.execTime;

		// instructionCount = progProfiler.instructionCount;
		// methodCount = progProfiler.methodInvocationCount;

		batteryVoltageChange = devProfiler.batteryVoltageDelta;
		// cpuUsage = devProfiler.cpuUsage;
		// batteryLevels = devProfiler.batteryLevels;
	}

	/**
	 * Convert the log record to string for storing
	 */
	public String toString() {
		String progProfilerRecord = methodName.substring(methodName.indexOf('#') + 1) + ", " + execDuration / 1000000 + "ms, " + energyConsumption + "mJ, " + cpuEnergy + "mJ, " + screenEnergy + "mJ, " + bluetoothEnergy + "mJ";// +
																																																									// instructionCount
																																																									// +
																																																									// ", "
																																																									// +
																																																									// methodCount;

		String devProfilerRecord = "" + batteryVoltageChange + "mV";

		return progProfilerRecord + ", " + devProfilerRecord;
	}

	public String cpuToString() {
		String ret = "";
		int size = cpuUsage.size();
		for (int i = 0; i < size; i++) {
			ret += cpuUsage.get(i) + "\n";
		}

		return ret;
	}

	/*
	 * public String batteryToString() { String ret = ""; int size =
	 * batteryLevels.size(); for (int i = 0; i < size; i++) { ret +=
	 * batteryLevels.get(i) + "\n"; }
	 * 
	 * return ret; }
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(methodName);
		out.writeLong(execDuration);
		out.writeLong(energyConsumption);
		out.writeDouble(cpuEnergy);
		out.writeDouble(screenEnergy);
		out.writeDouble(bluetoothEnergy);
		// out.writeInt(instructionCount);
		// out.writeInt(methodCount);
		out.writeLong(batteryVoltageChange);
		Bundle b = new Bundle();
		b.putIntegerArrayList("cpuUsage", cpuUsage);
		out.writeBundle(b);
		// out.writeArrayList(batteryLevels);
	}

	public static final Parcelable.Creator<LogRecord> CREATOR = new Parcelable.Creator<LogRecord>() {

		@Override
		public LogRecord createFromParcel(Parcel in) {
			return new LogRecord(in);
		}

		@Override
		public LogRecord[] newArray(int size) {
			return new LogRecord[size];
		}

	};

	private LogRecord(Parcel in) {
		methodName = in.readString();
		execDuration = in.readLong();
		energyConsumption = in.readLong();
		cpuEnergy = in.readDouble();
		screenEnergy = in.readDouble();
		bluetoothEnergy = in.readDouble();
		// instructionCount = in.readInt();
		// methodCount = in.readInt();
		batteryVoltageChange = in.readLong();
		Bundle b = in.readBundle();
		cpuUsage = b.getIntegerArrayList("cpuUsage");
		// batteryLevels = in.readArrayList(null);
	}
}
