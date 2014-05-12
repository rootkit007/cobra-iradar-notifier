package com.cobra.iradar.messaging;

public enum ConnectivityStatus {
	UNKNOWN(0,"Unknown"),
	CONNECTING(1,"Connecting to device"),
	DISCONNECTED(2,"Disconnected"),
	CONNECTED(3,"Connected"),
	PROTOCOL_ERROR(4,"Protocol error");
	
	private int code;
	private String statusName;
	
	private ConnectivityStatus(int code, String name) {
		this.code = code;
		this.statusName = name;
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

	public String getStatusName() {
		return statusName;
	}

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

}