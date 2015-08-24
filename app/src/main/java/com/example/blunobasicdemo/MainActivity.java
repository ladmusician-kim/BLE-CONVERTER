package com.example.blunobasicdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity  extends BlunoLibrary {

	public TextView blunoTextView;
	public TextView blueSwitchTextView;
	public TextView receivedDataTextView;
	public TextView printDataTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        onCreateProcess();														//onCreate Process by BlunoLibrary
 
        blunoTextView = (TextView) findViewById(R.id.blunoText);
		blueSwitchTextView = (TextView) findViewById(R.id.blueswitchText);
		receivedDataTextView = (TextView) findViewById(R.id.receivedData);
		printDataTextView = (TextView) findViewById(R.id.printData);
		
        scanLeDevice(true);
	}

	protected void onResume(){
		super.onResume();
		System.out.println("BlUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        //onPauseProcess();														//onPause Process by BlunoLibrary
    }
	
	protected void onStop() {
		super.onStop();
		//onStopProcess();														//onStop Process by BlunoLibrary
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();	
        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(int state) {//Once connection state changes, this function will be called
		switch (state) {
		case 1:
			blunoTextView.setText("Bluno is Connected");
			break;
		case 2:
			blunoTextView.setText("Bluno is Disconnected");
			break;
		case 3:
			blueSwitchTextView.setText("BlueSwitch is Connected");
			break;
		case 4:
			blueSwitchTextView.setText("BlueSwitch is Disconnected");
			break;
		case 5:
			receivedDataTextView.setText("Receive data? : true");
			break;
		default:
			break;
		}
	}

	@Override
	public void onSerialReceived(byte data) {
		//String str  = new String(data,0,1);
		printDataTextView.setText("Data : " + data);
	}

}