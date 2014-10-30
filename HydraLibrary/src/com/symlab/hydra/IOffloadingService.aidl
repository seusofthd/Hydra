package com.symlab.hydra;

import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.IOffloadingCallback;
import com.symlab.hydra.profilers.LogRecord;

interface IOffloadingService {
	void addTaskToQueue(in byte[] offloadableMethod, in String apkPath);
	void registerCallback(in IOffloadingCallback offloadingCallback);
	void unregisterCallback();
	String getDeviceId();
	void startHelping();
	void stopHelping();
	void startProfiling(in String methodName);
	LogRecord stopProfiling(in boolean receivedTask);
}