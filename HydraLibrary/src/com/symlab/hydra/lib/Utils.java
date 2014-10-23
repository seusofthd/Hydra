package com.symlab.hydra.lib;

import java.io.ByteArrayInputStream;
import java.io.File;

public class Utils {
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
}
