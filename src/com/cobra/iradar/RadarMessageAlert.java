package com.cobra.iradar;

/**
 * Adds alert type, frequency, strength to base RadarMessage
 * @author pzeltins
 *
 */
public class RadarMessageAlert extends RadarMessage {
	
	private static final long serialVersionUID = 1L;
	
	public int alertCode;
	public int strength;
	public float frequency;
	public Alert alert;
	
	public RadarMessageAlert(byte[] packet) throws Exception {
		super(packet);
		if ( type != RadarMessage.TYPE_ALERT ) {
			throw new Exception("Invalid packet for alert message");
		}
		alertCode = packet[5];
		strength = packet[6] - 48;
		frequency = calcFrequency();
		alert = Alert.fromRadarCode(alertCode);
	}
	
	public RadarMessageAlert(Alert alert, int strength, float frequency) {
		super(RadarMessage.TYPE_ALERT);
		this.alert = alert;
		alertCode = alert.getCode();
		this.strength = strength;
		this.frequency = frequency;
	}

	
	public float calcFrequency() {
		int l2 = 256 * (256 * packet[8]) + 256 * packet[9] + packet[10];
        return (float)l2 / 1000.0F;
	}
	
	public static final int ALERT_VOLUME_CHANGE = 118;
	public static final int ALERT_X = 88;
	public static final int ALERT_K = 75;
	public static final int ALERT_KA = 65;
	public static final int ALERT_KU = 85;
	public static final int ALERT_POP = 80;
	public static final int ALERT_SPECTOR = 98;
	public static final int ALERT_VG2 = 86;
	public static final int ALERT_EMERGENCY_VEH = 69;
	public static final int ALERT_ROAD_HAZARD = 79;
	public static final int ALERT_TRAIN_APPROACHING = 82;
	public static final int ALERT_LASER_20_20 = 73;
	public static final int ALERT_LASER_PRO_LASER = 66;
	public static final int ALERT_LASER_ULTRALATE = 74;
	public static final int ALERT_LASER_PRO_LASER_3 = 71;
	public static final int ALERT_LASER_STALKER = 81;
	public static final int ALERT_LASER_MOB_SPEED = 76;
	public static final int ALERT_LASER_STRELKA = 107;
	public static final int ALERT_LASER_AMATA = 77;
	public static final int ALERT_LASER_LISD = 83;

	public static final int ALERT_TYPE_RADAR = 0;
	public static final int ALERT_TYPE_HAZARD = 1;
	public static final int ALERT_TYPE_LASER = 2;
	public static final int ALERT_TYPE_RDD = 3;
	public static final int ALERT_TYPE_AUTOMUTE = 3;
	
	public static final String ALERT_SOUND_KA = "Ka";
	public static final String ALERT_SOUND_KU = "Ku";
	public static final String ALERT_SOUND_K = "K";
	public static final String ALERT_SOUND_X = "X";
	public static final String ALERT_SOUND_POP = "Pop";
	public static final String ALERT_SOUND_RDD = "RDD";
	public static final String ALERT_SOUND_LASER = "Laser";
	public static final String ALERT_SOUND_HAZARD = "Hazard";
	
	public enum Alert {
		X(ALERT_X,ALERT_TYPE_RADAR,"X",ALERT_SOUND_X),
		K(ALERT_K,ALERT_TYPE_RADAR,"K",ALERT_SOUND_K),
		Ka(ALERT_KA,ALERT_TYPE_RADAR,"Ka",ALERT_SOUND_KA),
		Ku(ALERT_KU,ALERT_TYPE_RADAR,"Ku",ALERT_SOUND_KU),
		POP(ALERT_POP,ALERT_TYPE_RADAR,"POP",ALERT_SOUND_POP),
		RDD1(ALERT_VG2,ALERT_TYPE_RDD,"VG2","VG2",ALERT_SOUND_RDD),
		RDD2(ALERT_SPECTOR,ALERT_TYPE_RDD,"Spector","Spector",ALERT_SOUND_RDD),
		HAZARD1(ALERT_TRAIN_APPROACHING,ALERT_TYPE_HAZARD,"Road Hazard",ALERT_SOUND_HAZARD,"Train Approaching"),
		HAZARD2(ALERT_ROAD_HAZARD,ALERT_TYPE_HAZARD,ALERT_SOUND_HAZARD,"Road Hazard"),
		HAZARD3(ALERT_EMERGENCY_VEH,ALERT_TYPE_HAZARD,ALERT_SOUND_HAZARD,"Road Hazard","Emergency Vehicle"),
		LASER1(ALERT_LASER_20_20,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","20-20"),
		LASER2(ALERT_LASER_PRO_LASER,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Pro Laser"),
		LASER3(ALERT_LASER_ULTRALATE,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Ultralate"),
		LASER4(ALERT_LASER_PRO_LASER_3,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Pro Laser 3"),
		LASER5(ALERT_LASER_STALKER,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Stalker"),
		LASER6(ALERT_LASER_MOB_SPEED,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Mobile Speed"),
		LASER7(ALERT_LASER_STRELKA,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Strelka"),
		LASER8(ALERT_LASER_AMATA,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","Amata"),
		LASER9(ALERT_LASER_LISD,ALERT_TYPE_LASER,ALERT_SOUND_LASER,"Laser","LISD"),
		CANCEL(ALERT_VOLUME_CHANGE, ALERT_TYPE_AUTOMUTE, "", "", "");
		
		private int code;
		private int type;
		private String name = "";
		private String additionalName = "";
		private String sound;
		
		Alert(int code, int type, String name, String sound) {
			this.code = code;
			this.type = type;
			this.name = name;
			this.sound = sound;
		}
		
		Alert(int code, int type, String name, String sound, String additionalName) {
			this(code,type,name,sound);
			this.additionalName = additionalName;
		}
		
		public static Alert fromRadarCode(int code) {
			for (Alert a : Alert.values()) {
				if ( a.getCode() == code )
					return a;
			}
			return null;
		}
		
		/**
		 * Code as transmitted by iRadar
		 * @return
		 */
		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		/**
		 * Alert category code
		 * @return
		 */
		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		/**
		 * Additional name (alert subtype) 
		 * @return
		 */
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAdditionalName() {
			return additionalName;
		}

		public void setAdditionalName(String additionalName) {
			this.additionalName = additionalName;
		}

		public String getSound() {
			return sound;
		}

		public void setSound(String sound) {
			this.sound = sound;
		}

	}
	
	
}
