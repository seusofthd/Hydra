package com.symlab.hydra.lib;

import java.io.Serializable;

import android.os.Parcelable;

public class MethodPackage implements Serializable {

	private static final long serialVersionUID = 1234736759181295962L;

	public Integer id;
	public Object object;
	public String methodName;
	public Class<?>[] paraTypes;
	public Object[] paraValues;

	public MethodPackage(Integer tid, Object object, String methodName, Class<?>[] paraTypes, Object[] paraValues) {
		this.id = tid;
		this.object = object;
		this.methodName = methodName;
		this.paraTypes = paraTypes;
		this.paraValues = paraValues;
	}
}
