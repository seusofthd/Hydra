package com.symlab.hydracloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

import com.symlab.hydra.network.DataPackage;
import com.symlab.hydra.network.Msg;

public class DataPackage2 implements Serializable {

	private static final long serialVersionUID = -3825185830334989901L;

	public Msg what;
	public InetAddress source;
	public Object data;
	public byte[] dataByte;
	public Long rttManagerToVM = new Long(0);
	public Long rttRouterToVM = new Long(0);
	public Long rttDeviceToVM = new Long(0);
	public Long pureExecTime = new Long(0);
	

	private DataPackage2(Msg what, Object data, InetAddress source) {
		this.what = what;
		this.data = data;
		this.source = source;
	}

	
//	public static DataPackage obtain() {
//		return new DataPackage(null, null, null);
//	}
//	
//	public static DataPackage obtain(Msg what) {
//		return new DataPackage(what, null, null);
//	}
//
//	public static DataPackage obtain(Msg what, Object data) {
//		return new DataPackage(what, data, null);
//	}
//
//	public static DataPackage obtain(Msg what, Object data, InetAddress source) {
//		return new DataPackage(what, data, source);
//	}
	
	public byte[] serialize() throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(this);
        dataByte = b.toByteArray();
        return dataByte;
    }

    public Object deserialize() throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(dataByte);
        ObjectInputStream o = new ObjectInputStream(b);
        data = o.readObject();
        return data; 
    }
}
