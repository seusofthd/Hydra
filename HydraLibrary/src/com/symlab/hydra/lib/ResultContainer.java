package com.symlab.hydra.lib;

import java.io.Serializable;

public class ResultContainer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7407243547470872113L;

	public int id;
	public boolean isExceptionOrError;
	public Object caller;
	public Object result;
	public long pureExecutionDuration;

	public long energyConsumption;

	public ResultContainer() {

	}

	public ResultContainer(boolean failed, Object caller, Object result, Long duration, Long consumption, int id) {
		isExceptionOrError = failed;
		this.caller = caller;
		this.result = result;
		pureExecutionDuration = duration;
		energyConsumption = consumption;
	}

	public void setResult(boolean failed, Object caller, Object result, Long duration, Long consumption) {
		isExceptionOrError = failed;
		this.caller = caller;
		this.result = result;
		pureExecutionDuration = duration;
		energyConsumption = consumption;
	}
}
