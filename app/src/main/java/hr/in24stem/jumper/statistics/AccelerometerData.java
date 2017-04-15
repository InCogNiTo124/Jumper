package hr.in24stem.jumper.statistics;

/**
 * Created by Smetko on 15.4.2017..
 */

public class AccelerometerData {

    private float x;
    private float y;
    private float z;

    AccelerometerData() {
        x = 0;
        y = 0;
        z = 0;
    }

    AccelerometerData(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public AccelerometerData(float[] a) {
        this.x = a[0];
        this.y = a[1];
        this.z = a[2];
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public double getRadius() {
        return Math.sqrt(x*x + y*y + z*z);
    }
}
