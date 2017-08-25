package edu.upenn.cis350.group1.calorietracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends CalorieTrackerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // Function to create Intent to transfer user to daily activity on click
    public void toDaily(View view) {
        Intent menu = new Intent(this, DailyActivity.class);
        startActivity(menu);
    }

    // Function to create Intent to transfer user to calendar activity on click
    public void toCalendar(View view) {
        Intent menu = new Intent(this, CalendarActivity.class);
        startActivity(menu);
    }

    // Function to create Intent to transfer user to settings activity on click
    public void toSettings(View view) {
        Intent menu = new Intent(this, SettingsActivity.class);
        startActivity(menu);
    }
}
