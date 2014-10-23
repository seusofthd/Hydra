package com.symlab.hydraapp;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class FaceDetection implements Serializable {

	private static final String TAG = "TestFaceDetection";

	private byte[] bytesar;

	int rowBytes;
	int heightImage;
	int widthImage;
	int dstCapacity;

	private static final long serialVersionUID = 1470242663091042025L;

	public int detect_faces(int num, int n) {
		String imageToCheck = "/sdcard/Pictures/image" + n + ".jpg";
		getImage(imageToCheck);
		Bitmap bitmap;
		if (new File(imageToCheck).exists()) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inPreferredConfig = Bitmap.Config.RGB_565;
			bitmap = BitmapFactory.decodeFile(imageToCheck, opts);
		} else {
			ByteBuffer dst = ByteBuffer.allocate(dstCapacity);
			dst.position(0);
			dst.put(bytesar);

			dst.position(0);
			bitmap = Bitmap.createBitmap(widthImage, heightImage, Bitmap.Config.RGB_565);
			bitmap.copyPixelsFromBuffer(dst);
		}

		FaceDetector fd = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), num);
		Face[] faces = new Face[num];
		int foundNum = 0;
		foundNum = fd.findFaces(bitmap, faces);
		bytesar = null;
		return foundNum;
	}

	public void getImage(String imageToCheck) {
		Bitmap sourceImage;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;

		sourceImage = BitmapFactory.decodeFile(imageToCheck, opts);

		rowBytes = sourceImage.getRowBytes();
		heightImage = sourceImage.getHeight();
		widthImage = sourceImage.getWidth();

		int bmSize = sourceImage.getRowBytes() * sourceImage.getHeight();

		ByteBuffer dst = ByteBuffer.allocate(bmSize);

		dstCapacity = dst.capacity();

		dst.position(0);

		sourceImage.copyPixelsToBuffer(dst);

		bytesar = new byte[bmSize];

		Log.d(TAG, "Size of the image to send: " + dstCapacity);

		dst.position(0);
		dst.get(bytesar);

		sourceImage.recycle();
	}

}
