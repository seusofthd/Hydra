package com.symlab.hydra.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Utils {
	public static Byte[] serialize(Object data) {
		Byte[] dataByte;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(data);
			byte[] dataByte2 = b.toByteArray();
			dataByte = new Byte[dataByte2.length];
			for (int i = 0; i < dataByte2.length; i++) {
				dataByte[i] = dataByte2[i];
			}
		} catch (IOException e) {
			dataByte = new Byte[1];
			e.printStackTrace();
		}
		return dataByte;
	}
	
	public static byte[] serialize2(Object data) {
		byte[] dataByte;
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(data);
			dataByte = b.toByteArray();
		} catch (IOException e) {
			dataByte = new byte[1];
			e.printStackTrace();
		}
		return dataByte;
	}

	public static Object deserialize(Byte[] dataByte) {
		Object data = null;
		try {
			byte[] dataByte2 = new byte[dataByte.length];
			for (int i = 0; i < dataByte.length; i++) {
				dataByte2[i] = dataByte[i];
			}
			ByteArrayInputStream b = new ByteArrayInputStream(dataByte2);
			ObjectInputStream o = new ObjectInputStream(b);
			data = o.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public static Object deserialize(Byte[] dataByte, File dexFile, File optimizedDir) {
		Object data = null;
		try {
			byte[] dataByte2 = new byte[dataByte.length];
			for (int i = 0; i < dataByte.length; i++) {
				dataByte2[i] = dataByte[i];
			}
			ByteArrayInputStream b = new ByteArrayInputStream(dataByte2);
			DynamicObjectInputStream o = new DynamicObjectInputStream(b);
			o.addDex(dexFile, optimizedDir);
			data = o.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
	public static Object deserialize(byte[] dataByte, File dexFile, File optimizedDir) {
		Object data = null;
		try {
			ByteArrayInputStream b = new ByteArrayInputStream(dataByte);
			DynamicObjectInputStream o = new DynamicObjectInputStream(b);
			o.addDex(dexFile, optimizedDir);
			data = o.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
	
}
