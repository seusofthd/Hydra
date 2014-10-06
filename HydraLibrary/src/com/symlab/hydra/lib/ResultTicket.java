package com.symlab.hydra.lib;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.os.Parcel;
import android.os.Parcelable;

public class ResultTicket implements Parcelable {

	private Class<?> reutrnType;
	private Future<?> resultHolder = null;
	boolean resultReady = false;
	private Object lock;
	private Long energy;

	public ResultTicket(Class<?> reutrnType) {
		this.reutrnType = reutrnType;
		lock = new Object();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeSerializable(reutrnType);
		out.writeValue(resultHolder);
		out.writeByte((byte) (resultReady ? 1 : 0));
		out.writeValue(lock);
		out.writeLong(energy);
	}

	public static final Parcelable.Creator<ResultTicket> CREATOR = new Parcelable.Creator<ResultTicket>() {

		@Override
		public ResultTicket createFromParcel(Parcel in) {
			return new ResultTicket(in);
		}

		@Override
		public ResultTicket[] newArray(int size) {
			return new ResultTicket[size];
		}

	};

	private ResultTicket(Parcel in) {
		reutrnType = (Class<?>) in.readSerializable();
		resultHolder = (Future<?>) in.readValue(null);
		resultReady = in.readByte() != 0;
		lock = in.readValue(null);
		energy = in.readLong();
	}

	public void setHolder(Future<?> future) {
		resultHolder = future;
	}

	public void setResultReady() {
		synchronized (lock) {
			resultReady = true;
			lock.notifyAll();
		}
	}

	public void setEnergy(Long e) {
		energy = e;
	}

	public long getEnergy() {
		return energy;
	}

	public Object getResult() {
		Object result = null;
		// Log.d("Future","Waiting resultHolder");
		synchronized (lock) {
			while (!resultReady) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		// Log.e("Future","Can get result");
		try {
			result = resultHolder.get();
			// Log.e("Future","Result got");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return result;
	}

}
