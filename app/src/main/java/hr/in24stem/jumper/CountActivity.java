package hr.in24stem.jumper;

import android.os.Bundle;
import android.app.Activity;

public class CountActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
