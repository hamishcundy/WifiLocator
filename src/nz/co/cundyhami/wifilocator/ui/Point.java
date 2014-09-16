package nz.co.cundyhami.wifilocator.ui;

public class Point {
	
	private String bssidPrefix;
	private int x, y, floor;
	private double measuredDistance;
	public String getBssidPrefix() {
		return bssidPrefix;
	}
	public void setBssidPrefix(String bssidPrefix) {
		this.bssidPrefix = bssidPrefix;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public double getMeasuredDistance() {
		return measuredDistance;
	}
	public void setMeasuredDistance(double measuredDistance) {
		this.measuredDistance = measuredDistance;
	}
	public int getFloor() {
		return floor;
	}
	public void setFloor(int floor) {
		this.floor = floor;
	}
	public String getString() {
		return "(" + x + "," + y + "," + floor + ")=" + measuredDistance + "m\n";
	}

}
