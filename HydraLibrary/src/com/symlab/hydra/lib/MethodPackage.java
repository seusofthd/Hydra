package com.symlab.hydra.lib;

import java.io.Serializable;

public class MethodPackage implements Serializable {

	private static final long serialVersionUID = 1234736759181295962L;

	public int id;
	public Object receiver;
	public String methodName;
	public Class<?>[] paraTypes;
	public Object[] paraValues;

	public MethodPackage(int tid, Object receiver, String methodName, Class<?>[] paraTypes, Object[] paraValues) {
		this.id = tid;
		this.receiver = receiver;
		this.methodName = methodName;
		this.paraTypes = paraTypes;
		this.paraValues = paraValues;
	}
}
