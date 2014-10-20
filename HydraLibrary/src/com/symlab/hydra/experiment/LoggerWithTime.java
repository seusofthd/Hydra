package com.symlab.hydra.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.text.format.Time;

public class LoggerWithTime {

	File logFileDir = Environment.getExternalStorageDirectory();
	File logFile = new File(logFileDir, "dandelion_log.csv");
	Time t = new Time();;
	
	public static void log(String who, String record) {
		long mtime = System.currentTimeMillis();
		LoggerWithTime l = new LoggerWithTime();
		l.t.set(mtime);
		String time = "" + l.t.hour/10 +l.t.hour%10  + ":" + l.t.minute/10 + l.t.minute%10 + ":" + l.t.second/10 + l.t.second%10;
		LoggerWithTime logger = new LoggerWithTime();
		if (!logger.logFile.exists()) {
			try {
				logger.logFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(logger.logFile, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter writer = new BufferedWriter(fileWriter);
		try {
			writer.write("" + mtime/1000 + "." + mtime%1000/100 + mtime%1000%100/10  + mtime%1000%100%10 +  "," + time + "," + who + "," + record + "\r\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
