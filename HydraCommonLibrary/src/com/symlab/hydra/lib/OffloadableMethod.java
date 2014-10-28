package com.symlab.hydra.lib;

import java.io.Serializable;

import android.content.Context;

public class OffloadableMethod implements Serializable {

	public String appName;
	public String apkPath;
	public MethodPackage methodPackage;
	public Class<?> reutrnType;
	public Object result;

	public long execDuration;
	public long energyConsumption;
	public long recordQuantity;

	public OffloadableMethod(Context context, String appName, String apkPath, MethodPackage methodPackage, Class<?> reutrnType) {
		this.appName = appName;
		this.apkPath = apkPath;
		this.methodPackage = methodPackage;
		this.reutrnType = reutrnType;
	}

}
