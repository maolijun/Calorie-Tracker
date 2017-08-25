package edu.upenn.cis350.group1.calorietracker;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import java.sql.Date;
import java.util.List;

/**
 * Created by joseovalle on 4/2/16.
 */
public class ProgressActivity extends CalorieTrackerActivity {

    //private DatabaseHandler db;
    private DynamoDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        // adding up total values for each of the nutrients
        int calorieStatus = 0;
        int proteinStatus = 0;
        int sodiumStatus = 0;
        int carbsStatus = 0;

        //db = new DatabaseHandler(getApplicationContext());
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );

        db = new DynamoDB(credentialsProvider);

        List<Meal> meals = db.getAllMealsList(new Date(System.currentTimeMillis()));
        for(Meal meal: meals) {
            calorieStatus += meal.getCalories();
            proteinStatus += meal.getProtein();
            sodiumStatus += meal.getSodium();
            carbsStatus += meal.getCarbs();

        }

        //sets the bar data for each bar, geting the progressbar
        //and edittext objects from xml
        createBar((ProgressBar) findViewById(R.id.calories_bar) ,
                  (TextView) findViewById(R.id.calories_text),
                   calorieStatus, "Calories", Color.RED);
        createBar((ProgressBar) findViewById(R.id.protein_bar),
                  (TextView) findViewById(R.id.protein_text),
                   proteinStatus, "Protein", Color.CYAN);
        createBar((ProgressBar) findViewById(R.id.sodium_bar),
                  (TextView) findViewById(R.id.sodium_text),
                   sodiumStatus, "Sodium", Color.BLUE);
        createBar((ProgressBar) findViewById(R.id.carbs_bar),
                  (TextView) findViewById(R.id.carbs_text),
                   carbsStatus, "Carbs", Color.MAGENTA);

    }

    /*
    params: progressbar to edit, textview to edit, progress thus far to use,
    type of nutrient bar relates to.
    This methods just uses progressbar methods to input the required data.
     */
    private void createBar(ProgressBar bar, TextView text, int progress, String type, int color) {
        // style progress bar
        bar.setMax(db.getSetting(type.toLowerCase()));
        bar.setProgress(progress);
        bar.setProgressTintList(ColorStateList.valueOf(color));

        // style text
        text.setText(type + " " +  progress + "/" + bar.getMax());
        text.setTypeface(null, Typeface.BOLD);
        text.setTextColor(Color.BLACK);
    }

}
