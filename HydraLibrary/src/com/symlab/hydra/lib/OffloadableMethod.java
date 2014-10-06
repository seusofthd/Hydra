package com.symlab.hydra.lib;

import java.util.ArrayList;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.symlab.hydra.db.DatabaseQuery;
import com.symlab.hydra.network.DataPackage;

public class OffloadableMethod implements Parcelable {

	public int id;
	public String appName;
	public MethodPackage methodPackage;
//	public ResultTicket resultTicket;
	public Class<?> reutrnType;
	public Object result;
	public DataPackage dataPackage;

	public long execDuration;
	public long energyConsumption;
	public long recordQuantity;

	public OffloadableMethod(Context context, String appName, MethodPackage methodPackage, Class<?> reutrnType) {
		this.appName = appName;
		this.methodPackage = methodPackage;
//		resultTicket = new ResultTicket(reutrnType);
		this.reutrnType = reutrnType;
		// this.resultListener = resultListener;
		/*
		 * DatabaseQuery query = new DatabaseQuery(context); String
		 * classMethodName = methodPackage.receiver.getClass().toString() + "#"
		 * + methodPackage.methodName; ArrayList<String> queryString =
		 * query.getData(new String[] {"execDuration", "energyConsumption",
		 * "recordQuantity"}, "methodName = ?", new String[] {classMethodName} ,
		 * null, null, "execDuration", " ASC"); boolean noResult =
		 * (queryString.size() == 0); execDuration = noResult ? 0 :
		 * Long.parseLong(queryString.get(0)); energyConsumption = noResult ? 0
		 * : Long.parseLong(queryString.get(1)); recordQuantity = noResult ? 0 :
		 * Long.parseLong(queryString.get(2));
		 */
	}

	public void queryMethodInfo(Context context) {
		DatabaseQuery query = new DatabaseQuery(context);
		String classMethodName = methodPackage.receiver.getClass().toString() + "#" + methodPackage.methodName;
		ArrayList<String> queryString = query.getData(new String[] { "execDuration", "energyConsumption", "recordQuantity" }, "methodName = ?", new String[] { classMethodName }, null, null, "execDuration", " ASC");
		boolean noResult = (queryString.size() == 0);
		execDuration = noResult ? 0 : Long.parseLong(queryString.get(0));
		energyConsumption = noResult ? 0 : Long.parseLong(queryString.get(1));
		recordQuantity = noResult ? 0 : Long.parseLong(queryString.get(2));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(appName);
		out.writeSerializable(methodPackage);
//		out.writeParcelable(resultTicket, flags);
		out.writeLong(execDuration);
		out.writeLong(energyConsumption);
		out.writeLong(recordQuantity);
	}

	public static final Parcelable.Creator<OffloadableMethod> CREATOR = new Parcelable.Creator<OffloadableMethod>() {

		@Override
		public OffloadableMethod createFromParcel(Parcel in) {
			return new OffloadableMethod(in);
		}

		@Override
		public OffloadableMethod[] newArray(int size) {
			return new OffloadableMethod[size];
		}

	};

	private OffloadableMethod(Parcel in) {
		appName = in.readString();
		methodPackage = (MethodPackage) in.readSerializable();
//		resultTicket = in.readParcelable(null);
		execDuration = in.readLong();
		energyConsumption = in.readLong();
		recordQuantity = in.readLong();
	}

}
