package game2024;

public class Player {
	String name;
	int xpos;
	int ypos;
	private int previousXpos;
	private int previousYpos;
	int point;
	String direction;

	public Player(String name, int xpos, int ypos, String direction) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.point = 0;
	}

	public int getPreviousXpos() {
		return previousXpos;
	}

	public void setPreviousXpos(int previousXpos) {
		this.previousXpos = previousXpos;
	}

	public int getPreviousYpos() {
		return previousYpos;
	}

	public void setPreviousYpos(int previousYpos) {
		this.previousYpos = previousYpos;
	}

	public int getXpos() {
		return xpos;
	}

	@Override
	public void setXpos(int xpos) {
		this.previousXpos = this.xpos;
		this.xpos = xpos;
	}

	public int getYpos() {
		return ypos;
	}

	@Override
	public void setYpos(int ypos) {
		this.previousYpos = this.ypos;
		this.ypos = ypos;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public void addPoints(int p) {
		point+=p;
	}

	public String toString() {
		return name+":   "+point;
	}
}
