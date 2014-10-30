package com.symlab.hydra.lib;

import java.io.Serializable;

import com.symlab.hydra.network.Msg;

public class OffloadableMethod implements Serializable {

	private static final long serialVersionUID = -5388638223556142224L;
	public String appName;
	public String apkPath;
	public MethodPackage methodPackage;
	public Msg offloadingMethod;
	public Class<?> reutrnType;
	public Object result;

	public long execDuration;
	public long energyConsumption;
	public long recordQuantity;

	public OffloadableMethod(String appName, String apkPath, MethodPackage methodPackage, Class<?> reutrnType) {
		this.appName = appName;
		this.apkPath = apkPath;
		this.methodPackage = methodPackage;
		this.reutrnType = reutrnType;
	}

}
