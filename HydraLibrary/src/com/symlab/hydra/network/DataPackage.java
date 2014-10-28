package com.symlab.hydra.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

import com.symlab.hydra.lib.Utils;

public class DataPackage implements Serializable {

	private static final long serialVersionUID = -3825185830334989901L;
	public Integer id = new Integer(0);
	public Msg destination;
	public Msg what;

	public InetAddress source;
	public InetAddress dest;
	public Byte[] dataByte;
	public Long rttManagerToVM = new Long(0);
	public Long rttRouterToVM = new Long(0);
	public Long rttDeviceToVM = new Long(0);
	public Long pureExecTime = new Long(0);

	public Boolean finish = false;
	
	public DataPackage() {
	}

	private DataPackage(Msg what, Object data, InetAddress source) {
		this.what = what;
		this.dataByte = Utils.serialize(data);
		this.source = source;
		Utils.serialize(data);
	}

	public static DataPackage obtain() {
		return new DataPackage(null, null, null);
	}

	public static DataPackage obtain(Msg what) {
		return new DataPackage(what, null, null);
	}

	public static DataPackage obtain(Msg what, Object data) {
		return new DataPackage(what, data, null);
	}

	public static DataPackage obtain(Msg what, Object data, InetAddress source) {
		return new DataPackage(what, data, source);
	}
}
