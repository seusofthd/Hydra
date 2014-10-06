package com.symlab.hydra.status;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Status implements Parcelable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6492377379482256344L;
	public int numOfProcessors;
	public int cpuFrequency; // kHz
	public float cpuIdleness;
	public int batteryPercentage;
	public float memoryFree;
	public boolean isCharging;

	// private ClassLoader loader;

	public Status() {
		// this.loader = loader;
	}

	public Status(Status s) {
		this.numOfProcessors = s.numOfProcessors;
		this.cpuFrequency = s.cpuFrequency;
		this.cpuIdleness = s.cpuIdleness;
		this.batteryPercentage = s.batteryPercentage;
		this.memoryFree = s.memoryFree;
		this.isCharging = s.isCharging;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(numOfProcessors);
		out.writeInt(cpuFrequency);
		out.writeFloat(cpuIdleness);
		out.writeInt(batteryPercentage);
		out.writeFloat(memoryFree);
		out.writeByte((byte) (isCharging ? 1 : 0));
	}

	public static final Parcelable.Creator<Status> CREATOR = new Parcelable.Creator<Status>() {

		@Override
		public Status createFromParcel(Parcel in) {
			return new Status(in);
		}

		@Override
		public Status[] newArray(int size) {
			return new Status[size];
		}

	};

	private Status(Parcel in) {
		numOfProcessors = in.readInt();
		cpuFrequency = in.readInt();
		cpuIdleness = in.readFloat();
		batteryPercentage = in.readInt();
		memoryFree = in.readFloat();
		isCharging = in.readByte() != 0;
	}

}
