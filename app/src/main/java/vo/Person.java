package vo;

public class Person {
    private int _id;
    private String name;
    private int trial;
    private double timestamp;
    private double orientR;
    private double orientP;
    private double accX;
    private double accY;
    private double accZ;

    public int get_id() {
        return _id;
    }
    public String getName() {return name;}
    public int getTrial() { return trial; }
    public double getTimestamp() {return timestamp;}
    public double getOrientR() {return orientR;}
    public double getOrientP() {return orientP;}
    public double getAccX () {return accX;}
    public double getAccY () {return accY;}
    public double getAccZ () {return accZ;}
    public void set_id(int _id) {
        this._id = _id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setTrial(int trial) { this.trial =  trial; }
    public void setTimestamp(double timestamp) {this.timestamp = timestamp;}
    public void setOrientR(double orientR) {this.orientR = orientR;}
    public void setOrientP(double orientP) {this.orientP = orientP;}
    public void setAccX (double accX) {this.accX = accX;}
    public void setAccY (double accY) {this.accY = accY;}
    public void setAccZ (double accZ) {this.accZ = accZ;}
}
