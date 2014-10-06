package com.symlab.hydra.lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class ByteFile implements Serializable {

	private static final long serialVersionUID = 5509625572772951472L;
	private byte[] file;

	public ByteFile(byte[] data) {
		file = data;
	}

	public int length() {
		return file.length;
	}

	public byte[] toByteArray() {
		return file;
	}

	public static ByteFile createFromFile(File f) {
		byte[] tempArray = null;
		BufferedInputStream bis = null;
		try {
			FileInputStream fin = new FileInputStream(f);
			bis = new BufferedInputStream(fin);
			tempArray = new byte[(int) f.length()];
			bis.read(tempArray, 0, tempArray.length);
		} catch (FileNotFoundException e) {
			return new ByteFile(null);
		} catch (IOException e) {
			return new ByteFile(null);
		} finally {
			try {
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new ByteFile(tempArray);
	}

	public void writeToFile(File f) {
		FileOutputStream fout;
		BufferedOutputStream bout = null;
		try {
			fout = new FileOutputStream(f);
			bout = new BufferedOutputStream(fout, Constants.BUFFER);
			bout.write(toByteArray());
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (bout != null)
					bout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void recycle() {
		file = null;
	}
}
