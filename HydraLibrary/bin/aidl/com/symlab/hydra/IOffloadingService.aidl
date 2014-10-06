package com.symlab.hydra;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.profilers.LogRecord;

interface IOffloadingService {
	void addTaskToQueue(in OffloadableMethod offloadableMethod);
	String getDeviceId();
	void startProfiling(in String methodName);
	LogRecord stopProfiling(in boolean receivedTask);
	//void globalStartQueueHandler();
	void startHelping();
	void stopHelping();
}