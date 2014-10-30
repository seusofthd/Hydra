package com.symlab.hydra.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;

public class SVMDB {
	private ArrayList<String> arrayKeys = null;
	private ArrayList<String> arrayValues = null;
	private ArrayList<String> databaseKeys = null;
	private ArrayList<String> databaseKeyOptions = null;
	private DBAdapter database;
	
	public SVMDB(Context context){
		databaseKeys = new ArrayList<String>();
		databaseKeyOptions = new ArrayList<String>();
		
//		t_c is the server connection time
//		databaseKeys.add("t_c");
//		databaseKeyOptions.add("text");
		//s is the sending data size
		databaseKeys.add("s");
		databaseKeyOptions.add("text");
		//rssi is the signal strenght which indicates the bandwidth
		databaseKeys.add("rssi");
		databaseKeyOptions.add("text");
		//r is the return parameter size
		databaseKeys.add("r");
		databaseKeyOptions.add("text");
		//current bandwidth
		databaseKeys.add("b");
		databaseKeyOptions.add("text");
		//inum is the instruction count
		databaseKeys.add("inum");
		databaseKeyOptions.add("text");
		//util is the current utilization of the CPU
		databaseKeys.add("util");
		databaseKeyOptions.add("text");

		databaseKeys.add("label");
		databaseKeyOptions.add("text");
		
		database = new DBAdapter(context, "SVMTime", databaseKeys, databaseKeyOptions);
		database.open();
		arrayKeys = new ArrayList<String>();
		arrayValues = new ArrayList<String>();
	}
	
	public void appendData(String key, String value){
		arrayKeys.add(key);
		arrayValues.add(value);
	}
	
	public void addRow(){
	
		long jud = database.insertEntry(arrayKeys, arrayValues);
		if(jud==-1)
			System.out.println("insertion failed");		
		arrayKeys.clear();
		arrayValues.clear();
	}
	
	public void updateRow(){
		database.updateEntry(arrayKeys, arrayValues);
		arrayKeys.clear();
		arrayValues.clear();
	}
	
	public ArrayList<String> getData(){
		ArrayList<String> list = new ArrayList<String> ();
		Cursor results = database.getAllEntries(null, null, null);
		
		while (results.moveToNext()){
			String str = results.getString(results.getColumnIndex("s"))
					+", "+ results.getString(results.getColumnIndex("rssi"))
					+", "+ results.getString(results.getColumnIndex("inum"))
					+", "+ results.getString(results.getColumnIndex("r"))
					+", "+ results.getString(results.getColumnIndex("util"))
					+", "+ results.getString(results.getColumnIndex("b"))
					+", "+ results.getString(results.getColumnIndex("label"));
			list.add(str);
		}
		
		try{
			results.close();
		} finally{
			
		}
		return list;
	}
	public void deleteTable(){
		database.clearTable();
	}
	public void destroy() throws Throwable {
		database.close();
	}
}
