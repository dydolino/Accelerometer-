package olek.pwr.lista101;

/**
 * Created by Win8 on 2017-12-28.
 */

public class AccelData {

    private double timestamp;
    private float aX;
    private float aY;
    private float aZ;


    public AccelData(double timestamp, float aX, float aY, float aZ ){
        this.aX=aX;
        this.aY=aY;
        this.aZ=aZ;
        this.timestamp=timestamp;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public float getaX() {
        return aX;
    }

    public float getaY() {
        return aY;
    }

    public float getaZ() {
        return aZ;
    }

    @Override
    public String toString() {
        return timestamp + ", " + aX + ", " + aY + ", " + aZ;
    }
}
