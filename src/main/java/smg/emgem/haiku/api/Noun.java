package smg.emgem.haiku.api;

public class Noun extends Word {

	private int gender = 0;//0-we do not know
	
	public Noun(String value) {
		super(value);
	}
	
	public Noun(String value, int gender) {
		super(value);
		this.gender = gender;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}
	
	

}
