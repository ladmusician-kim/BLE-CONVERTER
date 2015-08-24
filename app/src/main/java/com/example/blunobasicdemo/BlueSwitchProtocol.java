package com.example.blunobasicdemo;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
 * byte[0] 	byte[1] 	byte[2] 	byte[3] 	byte[4] 	byte[5] 	byte[6] 	byte[7]
 *  STX		DATA[0] 	DATA[1] 	DATA[2] 	DATA[3] 	DATA[4]    CHECK_SUM 	ETX
 * 
 * 	STX = 0x03
 *  DATA[0] =  0	x		0			0			0			0			0			0			0			0
 *                   	Read/Write	Direction		 AbsenceMode		AlarmMode	1st Swtich	2nd Switch	3rd Switch
 * 
 *  # Read = 0, Write = 1
 * 	# Direction ��� �� �� ������..........
 * 		- 0 = SW -> PHONE
 * 		- 1 = PHONE -> SW
 * 	# AbsenceMode
 * 		- 00 = No Absence Operation
 * 		- 01 = Register Absence
 * 		- 10 = UnRegister Absence
 * 		- 11 = Sync Module Time
 * 	# AlarmMode
 * 		- 0 = AlarmSwitch Off
 * 		- 1 = AlarmSwitch On
 * 	# Switch
 * 		- 0 = Off;
 * 		- 1 = On;
 * 
 * 	CHECK_SUM = DATA ^ 0xFF
 * 	ETX= 0x02
 */
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BlueSwitchProtocol {
	private static final String TAG = "BLUE PROTOCOL OPERTAION";

	public final static int PACKET_HEADER_LENGTH = 4;
	public final static int DATA_LENGTH_ABSENCE = 6;
	public final static int DATA_LENGTH_NORMAL = 2;
	public final static int PACKET_LENGTH_ABSENCE = 10;
	public final static int PACKET_LENGTH_NORMAL = 6;
	//public final static int PACKET_LENGTH_NORMAL = 4;

	public final static int TRUE = 1;
	public final static int FALSE = 0;

	public static final String DEVICE_MODEL_NAME = "MODEL";

	private final static byte STX_BYTE = (byte) 0xF0;
	private final static byte ETX_BYTE = (byte) 0xE0;

	private final static byte TYPE_SWITCH = (byte) 0x01;
	private final static byte TYPE_ALARM = (byte) 0x02;
	private final static byte TYPE_TIMER = (byte) 0x04;
	private final static byte TYPE_WIDGET = (byte) 0x08;
	private final static byte TYPE_ABSENCE = (byte) 0x10;
	private final static byte TYPE_DIMMING = (byte) 0x20;

	public final static byte READ_DATA_BYTE = (byte) 0x00;
	public final static byte WRITE_DATA_BYTE = (byte) 0x80;

	public final static byte FIRST_SWITCH_ON_BYTE = (byte) 0x04;
	public final static byte SECOND_SWITCH_ON_BYTE = (byte) 0x02;
	public final static byte THIRD_SWITCH_ON_BYTE = (byte) 0x01;

	public final static byte CHECK_ABSENCE_BYTE = (byte) 0x00;
	public final static byte REGISTER_ABSENCE_BYTE = (byte) 0x10;
	public final static byte UNREGISTER_ABSENCE_BYTE = (byte) 0x20;
	public final static byte TIME_SYNC_BYTE = (byte) 0x30;

	public static final int SWITCH_IMAGE = 0;
	public static final int SWITCH_1 = 1;
	public static final int SWITCH_2 = 2;
	public static final int SWITCH_3 = 3;

	/*
	public static final int NO_OPERATION = -1;
	public static final int SWITCH_ONE_ON = 0;
	public static final int SWITCH_ONE_OFF = 1;
	public static final int SWITCH_ONE_ON_OFF = 10;
	public static final int SWITCH_TWO_ON = 2;
	public static final int SWITCH_TWO_OFF = 3;
	public static final int SWITCH_TWO_ON_OFF = 11;
	public static final int SWITCH_THREE_ON = 4;
	public static final int SWITCH_THREE_OFF = 5;
	public static final int SWITCH_THREE_ON_OFF = 12;
	public static final int ALARM_ON = 6;
	public static final int ALARM_OFF = 7;
	public static final int SWITCH_ALL_ON_OFF = 8;
	*/
	public static final int NO_OPERATION = -1;
	public static final int SWITCH_ONE_ON = 48;
	public static final int SWITCH_ONE_OFF = 49;
	public static final int SWITCH_ONE_ON_OFF = 10;
	public static final int SWITCH_TWO_ON = 50;
	public static final int SWITCH_TWO_OFF = 51;
	public static final int SWITCH_TWO_ON_OFF = 11;
	public static final int SWITCH_THREE_ON = 52;
	public static final int SWITCH_THREE_OFF = 53;
	public static final int SWITCH_THREE_ON_OFF = 12;
	public static final int ALARM_ON = 54;
	public static final int ALARM_OFF = 55;
	public static final int SWITCH_ALL_ON_OFF = 56;

	public final byte[] getReadPacket() {
		byte[] dataPacket = new byte[PACKET_LENGTH_NORMAL];
		dataPacket[0] = STX_BYTE;
		dataPacket[1] = DATA_LENGTH_NORMAL;
		dataPacket[2] = TYPE_SWITCH;
		dataPacket[3] = READ_DATA_BYTE;
		dataPacket[4] = getCheckSumByte(dataPacket[3]);
		dataPacket[5] = ETX_BYTE;
		return dataPacket;
	}

	private final byte getCheckSumByte(byte data) {
		return (byte) (data ^ 0xFF);
	}

	public final byte[] getWritePacketForApp(byte recvDataPacket, int opeartion) {
		byte[] dataPacket = new byte[PACKET_LENGTH_NORMAL];
		dataPacket[0] = STX_BYTE;
		dataPacket[1] = DATA_LENGTH_NORMAL;
		dataPacket[2] = TYPE_SWITCH;

		Log.e("PROTOCOL #####", "" + opeartion);


		switch(opeartion) {
		case NO_OPERATION :
			Log.i(TAG,"no operation");
			dataPacket[3] = (recvDataPacket);
			break;
		case SWITCH_ONE_ON:
			Log.i(TAG,"1 switch on");
			dataPacket[3] = (byte) (recvDataPacket|FIRST_SWITCH_ON_BYTE);
			break;
		case SWITCH_ONE_OFF:
			Log.i(TAG,"1 switch off");
			if(isTrueBit(recvDataPacket, 3)) {
				dataPacket[3] = (byte) ((recvDataPacket^FIRST_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (recvDataPacket);
			}
			break;
		case SWITCH_TWO_ON:
			Log.i(TAG,"2 switch on");
			dataPacket[3] = (byte) (recvDataPacket|SECOND_SWITCH_ON_BYTE);
			break;
		case SWITCH_TWO_OFF:
			Log.i(TAG,"2 switch off");
			if(isTrueBit(recvDataPacket, 2)) {
				dataPacket[3] = (byte) ((recvDataPacket^SECOND_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (recvDataPacket);
			}
			break;
		case SWITCH_THREE_ON:
			Log.i(TAG,"3 switch on");
			dataPacket[3] = (byte) (recvDataPacket|THIRD_SWITCH_ON_BYTE);
			break;
		case SWITCH_THREE_OFF:
			Log.i(TAG,"3 switch off");
			if(isTrueBit(recvDataPacket, 1)) {
				dataPacket[3] = (byte) ((recvDataPacket^THIRD_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (recvDataPacket);
			}
			break;

		case SWITCH_ONE_ON_OFF:
			Log.i(TAG,"1 switch on off");
			if(isTrueBit(recvDataPacket, 3)) {
				dataPacket[3] = (byte) ((recvDataPacket^FIRST_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (byte) (recvDataPacket|FIRST_SWITCH_ON_BYTE);
			}
			break;
		case SWITCH_TWO_ON_OFF:
			Log.i(TAG,"2 switch on off");
			if(isTrueBit(recvDataPacket, 2)) {
				dataPacket[3] = (byte) ((recvDataPacket^SECOND_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (byte) (recvDataPacket|SECOND_SWITCH_ON_BYTE);
			}
			break;
		case SWITCH_THREE_ON_OFF:
			Log.i(TAG,"3 switch on off");
			if(isTrueBit(recvDataPacket, 1)) {
				dataPacket[3] = (byte) ((recvDataPacket^THIRD_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (byte) (recvDataPacket|THIRD_SWITCH_ON_BYTE);
			}
			break;

		case SWITCH_ALL_ON_OFF:
			if(isTrueBit(recvDataPacket, 1) && isTrueBit(recvDataPacket, 2) && isTrueBit(recvDataPacket, 3)) {
				dataPacket[3] = (byte) ((recvDataPacket^FIRST_SWITCH_ON_BYTE^SECOND_SWITCH_ON_BYTE^THIRD_SWITCH_ON_BYTE));
			} else {
				dataPacket[3] = (byte) (recvDataPacket|FIRST_SWITCH_ON_BYTE|SECOND_SWITCH_ON_BYTE|THIRD_SWITCH_ON_BYTE);
			}
			break;
		default:
			Log.e("OPERATION TEST ###", "OPERATION IS WRONG");
			break;
		}

		dataPacket[3] = (byte)(dataPacket[3] | WRITE_DATA_BYTE);
		dataPacket[4] = getCheckSumByte(dataPacket[3]);
		dataPacket[5] = ETX_BYTE;

		return dataPacket;
	}

	public final byte[] getWritePacketForWidget(byte recvDataPacket, int operation) {
		byte[] dataPacket = new byte[PACKET_LENGTH_NORMAL];
		dataPacket[0] = STX_BYTE;
		dataPacket[1] = DATA_LENGTH_NORMAL;
		dataPacket[2] = TYPE_WIDGET;

		switch(operation) {
		case SWITCH_ONE_ON:
			Log.i(TAG,"1 switch on");
			dataPacket[3] = (byte) (recvDataPacket|FIRST_SWITCH_ON_BYTE);
			break;
		case SWITCH_ONE_OFF:
			Log.i(TAG,"1 switch off");
			if(isTrueBit(recvDataPacket, 3)) {
				dataPacket[3] = (byte) (recvDataPacket^FIRST_SWITCH_ON_BYTE);
			} else {
				dataPacket[3] = recvDataPacket;
			}
			break;
		case SWITCH_TWO_ON:
			Log.i(TAG,"2 switch on");
			dataPacket[3] = (byte) (recvDataPacket|SECOND_SWITCH_ON_BYTE);
			break;
		case SWITCH_TWO_OFF:
			Log.i(TAG,"2 switch off");
			if(isTrueBit(recvDataPacket, 2)) {
				dataPacket[3] = (byte) (recvDataPacket^SECOND_SWITCH_ON_BYTE);
			} else {
				dataPacket[3] = recvDataPacket;
			}
			break;
		case SWITCH_THREE_ON:
			Log.i(TAG,"3 switch on");
			dataPacket[3] = (byte) (recvDataPacket|THIRD_SWITCH_ON_BYTE);
			break;
		case SWITCH_THREE_OFF:
			Log.i(TAG,"3 switch off");
			if(isTrueBit(recvDataPacket, 1)) {
				dataPacket[3] = (byte) (recvDataPacket^THIRD_SWITCH_ON_BYTE);
			} else {
				dataPacket[3] = recvDataPacket;
			}
			break;
		default:
			Log.e("PROTOCOL", "WRONG OPERATION");
			break;
		}

		dataPacket[3] = (byte)(dataPacket[3] | WRITE_DATA_BYTE);
		dataPacket[4] = getCheckSumByte(dataPacket[3]);
		dataPacket[5] = ETX_BYTE;

		return dataPacket;
	}

	public final byte[] getWritePacketForTimer(int switchNum[]) {
		byte[] switchLocation = new byte[PACKET_LENGTH_NORMAL];

		switchLocation[0] = STX_BYTE;
		switchLocation[1] = DATA_LENGTH_NORMAL;
		switchLocation[2] = TYPE_TIMER;

		if(switchNum[0] == TRUE && switchNum[1] == FALSE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x03;
		} else if(switchNum[0] == FALSE && switchNum[1] == TRUE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x05;
		} else if(switchNum[0] == FALSE && switchNum[1] == FALSE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x06;
		} else if(switchNum[0] == TRUE && switchNum[1] == TRUE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x01;
		} else if(switchNum[0] == TRUE && switchNum[1] == FALSE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x02;
		} else if(switchNum[0] == FALSE && switchNum[1] == TRUE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x04;
		} else if(switchNum[0] == TRUE && switchNum[1] == TRUE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x00;
		} else {
			switchLocation[3] = (byte) 0x07;
		}

		switchLocation[4] = (byte) (switchLocation[3] ^ 0xff);
		switchLocation[5] = ETX_BYTE;

		return switchLocation;
	}

	public final byte[] getWritePacketForAlarm(int switchNum[], int isDevilAlarm) {
		byte[] switchLocation = new byte[PACKET_LENGTH_NORMAL];

		switchLocation[0] = STX_BYTE;
		switchLocation[1] = DATA_LENGTH_NORMAL;
		switchLocation[2] = TYPE_ALARM;

		if(switchNum[0] == TRUE && switchNum[1] == FALSE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x04;
		} else if(switchNum[0] == FALSE && switchNum[1] == TRUE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x02;
		} else if(switchNum[0] == FALSE && switchNum[1] == FALSE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x01;
		} else if(switchNum[0] == TRUE && switchNum[1] == TRUE && switchNum[2] == FALSE) {
			switchLocation[3] = (byte) 0x06;
		} else if(switchNum[0] == TRUE && switchNum[1] == FALSE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x05;
		} else if(switchNum[0] == FALSE && switchNum[1] == TRUE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x03;
		} else if(switchNum[0] == TRUE && switchNum[1] == TRUE && switchNum[2] == TRUE) {
			switchLocation[3] = (byte) 0x07;
		} else {
			switchLocation[3] = (byte) 0x00;
		}

		/*if(isDevilAlarm == AlarmService.OFF_USING_SWITCH) {
			switchLocation[3] |= (byte)0x08;
		}
		 */
		switchLocation[4] = (byte) (switchLocation[3] ^ 0xff);
		switchLocation[5] = ETX_BYTE;

		return switchLocation;
	}

	public byte[] getWritePacketForAbsenceRegister(int startHour, int startMinute, int endHour, int endMinute, int[] isCheckedSwitch) {
		byte[] sendData = new byte[PACKET_LENGTH_ABSENCE];

		sendData = calcAbsenseTime(startHour, startMinute, endHour, endMinute);

		sendData[0] = STX_BYTE;
		sendData[1] = DATA_LENGTH_ABSENCE;
		sendData[2] = TYPE_ABSENCE;

		if(isCheckedSwitch[2] == FALSE && isCheckedSwitch[1] == FALSE && isCheckedSwitch[0] == TRUE) {
			sendData[3] = (byte) 0x04;
		} else if(isCheckedSwitch[2] == FALSE && isCheckedSwitch[1] == TRUE && isCheckedSwitch[0] == FALSE) {
			sendData[3] = (byte) 0x02;
		} else if(isCheckedSwitch[2] == TRUE && isCheckedSwitch[1] == FALSE && isCheckedSwitch[0] == FALSE) {
			sendData[3] = (byte) 0x01;
		} else if(isCheckedSwitch[2] == FALSE && isCheckedSwitch[1] == TRUE && isCheckedSwitch[0] == TRUE) {
			sendData[3] = (byte) 0x06;
		} else if(isCheckedSwitch[2] == TRUE && isCheckedSwitch[1] == TRUE && isCheckedSwitch[0] == FALSE) {
			sendData[3] = (byte) 0x03;
		} else if(isCheckedSwitch[2] == TRUE && isCheckedSwitch[1] == FALSE && isCheckedSwitch[0] == TRUE) {
			sendData[3] = (byte) 0x05;
		} else if(isCheckedSwitch[2] == TRUE && isCheckedSwitch[1] == TRUE && isCheckedSwitch[0] == TRUE) {
			sendData[3] = (byte) 0x07;
		} else {
			sendData[3] = (byte) 0x00;
		}

		sendData[3] = (byte)(sendData[3] | REGISTER_ABSENCE_BYTE);
		sendData[8] = (byte)(sendData[3] ^ 0xff);
		sendData[9] = ETX_BYTE;

		return sendData;
	}

	public byte[] calcAbsenseTime(int startHH, int startMM, int endHH, int endMM) {
		long time = System.currentTimeMillis();
		Date date = new Date(time);
		SimpleDateFormat dateFormatHour = new SimpleDateFormat("HH");
		SimpleDateFormat dateFormatMinute = new SimpleDateFormat("mm");
		int currentHH = Integer.parseInt(dateFormatHour.format(date));
		int currentMM = Integer.parseInt(dateFormatMinute.format(date));
		byte sendStartHH = 0, sendStartMM = 0, sendEndHH = 0, sendEndMM = 0;

		if(startHH >= currentHH) {
			sendStartHH = (byte) (startHH - currentHH);
		}
		else if(startHH < currentHH) {
			sendStartHH = (byte) (24 + (startHH - currentHH));
		}

		if(startMM >= currentMM) {
			sendStartMM = (byte) (startMM - currentMM);
		}
		else if(startMM < currentMM) {
			sendStartHH = (byte) ((sendStartHH) - 1);
			sendStartMM = (byte) (60 + (startMM - currentMM));
		}

		if(endHH >= startHH) {
			sendEndHH = (byte) (endHH - startHH);
		}
		else if(endHH < currentHH) {
			sendEndHH = (byte) (24 + (endHH - startHH));
		}

		if(endMM >= startMM) {
			sendEndMM = (byte) (endMM - startMM);
		}
		else if(endMM < startMM) {
			sendEndHH = (byte) ((sendEndHH) - 1);
			sendEndMM = (byte) (60 + endMM - startMM);
		}

		byte[] sendData = new byte[PACKET_LENGTH_ABSENCE];
		sendData[4] = sendStartHH;
		sendData[5] = sendStartMM;
		sendData[6] = sendEndHH;
		sendData[7] = sendEndMM;

		Log.i(TAG, "StartHH : " + (int)sendStartHH + " StartMM : " + (int)sendStartMM + " EndHH : " + (int)sendEndHH + " EndMM : " + (int)sendEndMM);

		return sendData;
	}

	public byte[] getWritePacketForAbsenceOff() {
		byte[] setData = new byte[PACKET_LENGTH_ABSENCE];
		setData[0] = STX_BYTE;
		setData[1] = DATA_LENGTH_ABSENCE;
		setData[2] = TYPE_ABSENCE;
		setData[3] = UNREGISTER_ABSENCE_BYTE;
		setData[4] = 0x00;
		setData[5] = 0x00;
		setData[6] = 0x00;
		setData[7] = 0x00;
		setData[8] = (byte) (setData[3] ^ 0xff);
		setData[9] = ETX_BYTE;

		return setData;
	}

	public byte[] getWritePacketForAbsenceCheck() {
		byte[] setData = new byte[PACKET_LENGTH_ABSENCE];
		setData[0] = STX_BYTE;
		setData[1] = DATA_LENGTH_ABSENCE;
		setData[2] = TYPE_ABSENCE;
		setData[3] = CHECK_ABSENCE_BYTE;
		setData[4] = 0x00;
		setData[5] = 0x00;
		setData[6] = 0x00;
		setData[7] = 0x00;
		setData[8] = (byte) (setData[3] ^ 0xff);
		setData[9] = ETX_BYTE;

		return setData;
	}

	public final boolean isTrueBit(byte packet, int whereBit) {
		byte a = (byte) (packet>>(whereBit-1));
		byte b = (byte) (a<<(7));

		if(b==(byte)0x80) {
			return true;
		} else {
			return false;
		}
	}
/*
	public static String lefPad(String str, int size, char padChar) {
		return StringUtils.leftPad(str, size, padChar);
	}

	public void printBinaryData(byte[] data) {

		String[] s = {
				lefPad(Integer.toBinaryString(data[0]),8,'0').substring(lefPad(Integer.toBinaryString(data[0]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[0]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[1]),8,'0').substring(lefPad(Integer.toBinaryString(data[1]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[1]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[2]),8,'0').substring(lefPad(Integer.toBinaryString(data[2]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[2]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[3]),8,'0').substring(lefPad(Integer.toBinaryString(data[3]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[3]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[4]),8,'0').substring(lefPad(Integer.toBinaryString(data[4]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[4]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[5]),8,'0').substring(lefPad(Integer.toBinaryString(data[5]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[5]),8,'0').length())
		}; 		Log.i(TAG,"Data Packet : "+s[0]+"  " +s[1]+"  "+s[2]+"  "+s[3]+"  "+s[4]+"  "+s[5]);

	}

	public void printBinaryData_2(byte[] data) {

		String[] s = {
				lefPad(Integer.toBinaryString(data[0]),8,'0').substring(lefPad(Integer.toBinaryString(data[0]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[0]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[1]),8,'0').substring(lefPad(Integer.toBinaryString(data[1]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[1]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[2]),8,'0').substring(lefPad(Integer.toBinaryString(data[2]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[2]),8,'0').length())
		}; 		Log.i(TAG,"Data Packet : "+s[0]+"  " +s[1]+"  "+s[2]);

	}

	public void printBinaryAbsenceData(byte[] data) {
		/*
		String[] s = {
				lefPad(Integer.toBinaryString(data[0]),8,'0').substring(lefPad(Integer.toBinaryString(data[0]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[0]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[1]),8,'0').substring(lefPad(Integer.toBinaryString(data[1]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[1]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[2]),8,'0').substring(lefPad(Integer.toBinaryString(data[2]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[2]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[3]),8,'0').substring(lefPad(Integer.toBinaryString(data[3]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[3]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[4]),8,'0').substring(lefPad(Integer.toBinaryString(data[4]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[4]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[5]),8,'0').substring(lefPad(Integer.toBinaryString(data[5]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[5]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[6]),8,'0').substring(lefPad(Integer.toBinaryString(data[6]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[6]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[7]),8,'0').substring(lefPad(Integer.toBinaryString(data[7]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[7]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[8]),8,'0').substring(lefPad(Integer.toBinaryString(data[8]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[8]),8,'0').length()),
				lefPad(Integer.toBinaryString(data[9]),8,'0').substring(lefPad(Integer.toBinaryString(data[9]),8,'0').length()-8, lefPad(Integer.toBinaryString(data[9]),8,'0').length())
		}; 		Log.i(TAG,"Data Packet : "+s[0]+"  " +s[1]+"  "+s[2]+"  "+s[3]+"  "+s[4]+"  "+s[5]+"  "+s[6]+"  "+s[7]+"  "+s[8]+"  "+s[9]);
		 
	}
*/
}
