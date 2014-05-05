package com.cobra.iradar.messaging;

public enum ConnectivityStatus {
	UNKNOWN(0),
	CONNECTING(1),
	DISCONNECTED(2),
	CONNECTED(3);
	
	private int code;
	
	private ConnectivityStatus(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
	
	public static ConnectivityStatus fromCode(int c) {
		for ( ConnectivityStatus s : ConnectivityStatus.values() ) {
			if ( s.getCode() == c ) 
				return s;
		}
		return null;
	}

}