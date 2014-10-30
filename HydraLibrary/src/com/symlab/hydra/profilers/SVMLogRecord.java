package com.symlab.hydra.profilers;

public class SVMLogRecord {
	public int s; //sending data size
	public int rssi;
	public int r; //return parameter size
	public long b; //current bandwidth
	public int inum;
	public float util;
	public long localTime;
	public long remoteTime;
	public int label;
	
	public void clearLog(){
		s = 0;
		rssi = 0;
		r = 0;
		b = 0;
		inum = 0;
		util = 0;
		localTime = 0;
		remoteTime = 0;
		label = 0;
	}
}
