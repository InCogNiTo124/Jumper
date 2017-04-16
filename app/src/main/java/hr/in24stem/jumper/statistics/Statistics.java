package hr.in24stem.jumper.statistics;

import android.util.Log;

import java.util.ArrayList;
import hr.in24stem.Constants;

public class Statistics {
    public static double MEAN = 0.0;
    public static double DEVIATION = 0.0;
    private static boolean FITTED = false;

    public static void fit(ArrayList<AccelerometerData> data) {
        int n = data.size();
        Log.d(Constants.TAG, "STATISTICS: size = " + n);
        double sum = 0;
        for (AccelerometerData a: data) {
            sum += a.getRadius();
        }
        MEAN = sum / n;

        sum = 0;
        for (AccelerometerData a: data) {
            sum += Math.pow((a.getRadius() - MEAN), 2);
        }
        DEVIATION = Math.sqrt(sum/n);
        FITTED = true;
    }

    public static double transform(AccelerometerData a) {
        return (a.getRadius() - MEAN) / DEVIATION;
    }
}
