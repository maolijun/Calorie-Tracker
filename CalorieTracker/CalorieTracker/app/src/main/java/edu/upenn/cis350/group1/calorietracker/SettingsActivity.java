package edu.upenn.cis350.group1.calorietracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

public class SettingsActivity extends CalorieTrackerActivity {

    // class variables to define keys used to refer to respective nutritional values
    public static final String calorieKey = "calories";
    public static int caloricLimit;
    public static final int caloricDefault = 2000;

    public static final String proteinKey = "protein";
    public static int proteinLimit;
    public static final int proteinDefault = 50;

    public static final String carbKey = "carbs";
    public static int carbLimit;
    public static final int carbDefault = 275;

    public static final String sodiumKey = "sodium";
    public static int sodiumLimit;
    public static final int sodiumDefault = 2300;

//    private DatabaseHandler db;
    private DynamoDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //db = new DatabaseHandler(getApplicationContext());
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );
        db = new DynamoDB(credentialsProvider);

        /*
        Fills in textboxes with stored goals from the database
         */
        EditText setting = (EditText) findViewById(R.id.calorie_limit);
        caloricLimit = db.getSetting(calorieKey);
        if (caloricLimit == -1) {
            caloricLimit = caloricDefault;
            db.addSetting(calorieKey, caloricLimit);
        }
        setting.setText(Integer.toString(caloricLimit));

        EditText settingProt = (EditText) findViewById(R.id.protein_limit);
        proteinLimit = db.getSetting(proteinKey);
        if (proteinLimit == -1) {
            proteinLimit = proteinDefault;
            db.addSetting(proteinKey, proteinLimit);
        }
        settingProt.setText(Integer.toString(proteinLimit));

        EditText settingCarb = (EditText) findViewById(R.id.carb_limit);
        carbLimit = db.getSetting(carbKey);
        if (carbLimit == -1) {
            carbLimit = carbDefault;
            db.addSetting(carbKey, carbLimit);
        }
        settingCarb.setText(Integer.toString(carbLimit));

        EditText settingSodium = (EditText) findViewById(R.id.sodium_limit);
        sodiumLimit = db.getSetting(sodiumKey);
        if (sodiumLimit == -1) {
            sodiumLimit = sodiumDefault;
            db.addSetting(sodiumKey, sodiumLimit);
        }
        settingSodium.setText(Integer.toString(sodiumLimit));
    }

    public void saveSettings(View v) {
        /*
        updates each of the given goal settings based on value in edittext
         */
        EditText setting = (EditText) findViewById(R.id.calorie_limit);
        if (setting.length() != 0) caloricLimit = Integer.parseInt(setting.getText().toString());
        setting.setText(Integer.toString(caloricLimit));
        db.updateSettings(calorieKey, caloricLimit);

        EditText settingProt = (EditText) findViewById(R.id.protein_limit);
        if (settingProt.length() != 0) proteinLimit = Integer.parseInt(settingProt.getText().toString());
        settingProt.setText(Integer.toString(proteinLimit));
        db.updateSettings(proteinKey, proteinLimit);

        EditText settingSodium = (EditText) findViewById(R.id.sodium_limit);
        if (settingSodium.length() != 0) sodiumLimit = Integer.parseInt(settingSodium.getText().toString());
        settingSodium.setText(Integer.toString(sodiumLimit));
        db.updateSettings(sodiumKey, sodiumLimit);

        EditText settingCarb = (EditText) findViewById(R.id.carb_limit);
        if (settingCarb.length() != 0) carbLimit = Integer.parseInt(settingCarb.getText().toString());
        settingCarb.setText(Integer.toString(carbLimit));
        db.updateSettings(carbKey, carbLimit);

        /* Change image on save button click */
        final Button save = (Button) findViewById(R.id.save_settings);
        save.setBackgroundColor(Color.rgb(0, 178, 238));
        save.setText("Updated Successfully");
        finish();
    }

}
