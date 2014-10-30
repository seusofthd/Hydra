package com.symlab.hydrasvmservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.symlab.hydra.HydraHelper;
import com.symlab.hydra.OffloadingService;
import com.symlab.hydra.lib.MethodPackage;
import com.symlab.hydra.lib.OffloadableMethod;
import com.symlab.hydra.network.Msg;
import com.symlab.hydraapp.NQueens;
import com.symlab.hydraapp.Sorting;
import com.symlab.hydraapp.Sudoku;


public class MainActivity extends Activity {

	private transient HydraHelper hydraHelper;
	private TextView tv;
	public static String out = "HydraClient";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        tv = (TextView) findViewById(R.id.output);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        hydraHelper = new HydraHelper(this);
        
        CheckBox cb;
        cb = (CheckBox) findViewById(R.id.helping);
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener(){
        	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
        		if(isChecked){
        			hydraHelper.startHelping();
        		} else{
        			hydraHelper.stopHelping();
        		}
        	}
        });
        
		intent = new Intent(this, OffloadingService.class);
		println("Hydra");
		
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    Intent intent;
    public void startService(View v) {
		if (!hydraHelper.serviceIsStart) {
			hydraHelper.startService(intent);
			println("service is started");
			if (!hydraHelper.mIsBound) {
				hydraHelper.bindService();
				println("service is binded");
			} else {
				println("service is already binded");
			}
		} else {
			println("service is already started");

		}
	}

	public void stopService(View v) {
		if (hydraHelper.serviceIsStart) {
			hydraHelper.stopHelping();
			hydraHelper.stopService(intent);
			println("service is stopped");
			if (hydraHelper.mIsBound) {
				hydraHelper.unbindService();
				println("service is unbinded");
			} else {
				println("service is already unbinded");
			}
		} else {
			println("service is already stopped");
		}
	}
    
    public void execute(final View v){
    	new Thread(){
    		public void run() {
    			RandomArray rndArray = new RandomArray();
    	    	for(int i=0;i<rndArray.nqueen_rnd.length;i++)
    	    		try {
    	    			println("start a new nqueen:" + i);
    	    			execute_nqueen(v,rndArray.nqueen_rnd[i]);
    	    			System.out.println("---------------execute "+i+"th: "+rndArray.nqueen_rnd[i]+"-queens");
    	    		} catch (Exception e) {
    				// TODO Auto-generated catch block
    	    			e.printStackTrace();
    	    	}
    		};
    	}.start();
    }
    
	public void execute_nqueen(View v, final int n) throws Exception {
		NQueens subtasks;
		final String classMethodName = Sorting.class.getName() + "#" + "solveNQueens" + "#" + 0 + "#" + 1;
		hydraHelper.startProfiling(classMethodName);
		subtasks = new NQueens();
		final Class<?>[] paramTypes = { int.class, int.class, int.class };
		Object[] paramValues = { n, 0, n };
		
		String apkPath = "/sdcard/Hydra/HydraApp.apk";
		
		final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 10000000), subtasks, "solveNQueens", paramTypes, paramValues);
		final OffloadableMethod offloadableMethod = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage, Boolean.class);
		offloadableMethod.offloadingMethod = Msg.LOCAL;
		hydraHelper.postTask(offloadableMethod, offloadableMethod.apkPath);
		
		
		final MethodPackage methodPackage_remote = new MethodPackage((int) (Math.random() * 10000000), subtasks, "solveNQueens", paramTypes, paramValues);
		final OffloadableMethod offloadableMethod_remote = new OffloadableMethod(hydraHelper.getPackageName(), apkPath, methodPackage_remote, Boolean.class);
		offloadableMethod_remote.offloadingMethod = Msg.CLOUD;
		hydraHelper.postTask(offloadableMethod_remote, offloadableMethod_remote.apkPath);
		synchronized(offloadableMethod){
			try {
				offloadableMethod.wait();				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized(offloadableMethod_remote){
			try {
				offloadableMethod_remote.wait();				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		

		hydraHelper.stopProfiling();
	}

//	public void execute_sort(View v, final int n) throws Exception {
//		final String classMethodName = Sorting.class.getName() + "#" + "qSort" + "#" + 0 + "#" + 1;
//		hydraHelper.startProfiling(classMethodName);
//		Sorting subtasks = new Sorting();
//		final Class<?>[] paramTypes = {int.class};
//		Object[] paramValues = {n};
//		final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "qSort", paramTypes, paramValues);
//		final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, void.class);
//		final OffloadableMethod offloadableMethod_remote = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, void.class);
//		hydraHelper.postTask(offloadableMethod);
//		hydraHelper.postTask(offloadableMethod_remote);
//		
//		synchronized(offloadableMethod){
//			try {
//				offloadableMethod.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		synchronized(offloadableMethod_remote){
//			try {
//				offloadableMethod_remote.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		hydraHelper.stopProfiling();
//	}

//	public void execute_sudoku(View v) throws Exception {
//		println("Sudoku");
//		final int num = 1;
//		final String classMethodName = Sorting.class.getName() + "#" + "hasSolution" + "#" + 0 + "#" + 1;
//		hydraHelper.startProfiling(classMethodName);
//		Sudoku subtasks = new Sudoku();
//		final Class<?>[] paramTypes = {};
//		Object[] paramValues = {};
//		final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "hasSolution", paramTypes, paramValues);
//		final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, Boolean.class);
//		final OffloadableMethod offloadableMethod_remote = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, void.class);
//		hydraHelper.postTask(offloadableMethod);
//		hydraHelper.postTask(offloadableMethod_remote);
//
//		synchronized(offloadableMethod){
//			try {
//				offloadableMethod.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		synchronized(offloadableMethod_remote){
//			try {
//				offloadableMethod_remote.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		hydraHelper.stopProfiling();
//}
	
//	public void execute_face(View v, final int n) throws Exception {
//		println("FaceDetection " + n);
//
//		final String classMethodName = Sorting.class.getName() + "#" + "detect_faces" + "#" + 0 + "#" + 1;
//		hydraHelper.startProfiling(classMethodName);
//		TestFaceDetection subtasks = new TestFaceDetection();
//	    final Class<?>[] paramTypes = { int.class, int.class };
//		Object[] paramValues = { 20, n };
//		final MethodPackage methodPackage = new MethodPackage((int) (Math.random() * 100000), subtasks, "detect_faces", paramTypes, paramValues);
//		final OffloadableMethod offloadableMethod = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, Boolean.class);
//		final OffloadableMethod offloadableMethod_remote = new OffloadableMethod(MainActivity.this, getPackageName(), methodPackage, void.class);
//		hydraHelper.postTask(offloadableMethod);
//		hydraHelper.postTask(offloadableMethod_remote);
//
//		synchronized(offloadableMethod){
//			try {
//				offloadableMethod.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		synchronized(offloadableMethod_remote){
//			try {
//				offloadableMethod_remote.wait();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//		hydraHelper.stopProfiling();
//	}


public void println(final String s) {
	new Thread() {
		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tv.setText(s + "\n" + tv.getText());
				}
			});
		}
	}.start();

}

public void clear_screen() {
	new Thread() {
		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tv.setText("");
				}
			});
		}
	}.start();
}
}
