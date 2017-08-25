package edu.upenn.cis350.group1.calorietracker;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import java.sql.Date;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InputActivity extends CalorieTrackerActivity {
    private boolean existing; // whether editing existing meal or adding a new meal
    private int mealID; // meal id of this meal
    private Meal m; // Meal object being edited
    //private DatabaseHandler db; // database handler
    private DynamoDB db;
    private Date date; // date meal was eaten
    private static final int EDIT_RESULT_OK = 400; // result ok code  //AnqiChen: Renamed it

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        // set up database handler and get intent extras
        //db = new DatabaseHandler(getApplicationContext());
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );
        db = new DynamoDB(credentialsProvider);


        existing = getIntent().getBooleanExtra("EXISTING", false);
        mealID = getIntent().getIntExtra("MEAL_ID", -1);
        long intentDate = getIntent().getLongExtra("date", System.currentTimeMillis());
        date = new Date(intentDate);


        if (existing) {
            Spinner spinner = (Spinner)findViewById(R.id.frequent_meal_spinner);
            spinner.setVisibility(View.GONE);
            // fetch meal from db
            m = db.getMeal(mealID);
            int type = m.getTypeCode();

            // set spinner value to the correct one
            Spinner mealTypeSpinner = (Spinner) findViewById(R.id.mealtype_spinner);
            mealTypeSpinner.setSelection(type);

            // EditText fields
            EditText calories = (EditText) findViewById(R.id.calories);
            EditText meal = (EditText) findViewById(R.id.meal);
            EditText sodium = (EditText) findViewById(R.id.sodium);
            EditText carbs = (EditText) findViewById(R.id.carbs);
            EditText protein = (EditText) findViewById(R.id.protein);

            // set text fields to the correct values
            meal.setText(m.getName());
            calories.setText(Double.toString(m.getCalories()));
            sodium.setText(Double.toString(m.getSodium()));
            carbs.setText(Double.toString(m.getCarbs()));
            protein.setText(Double.toString(m.getProtein()));

            /* Set the button of Submit to text "Update", to make it more clear for users */
            Button submitButton = (Button) findViewById(R.id.submit);
            submitButton.setText("Update");
        } else {
            // hide delete button if this meal is not new
            //frequent meal spinner
            setFrequentMeal();
            Button deleteButton = (Button) findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.INVISIBLE);
        }

    }

    private void setFrequentMeal() {
        Spinner frequentSpinner = (Spinner) findViewById(R.id.frequent_meal_spinner);
        ArrayAdapter adapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, getFrequentMeals());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequentSpinner.setAdapter(adapter);
        frequentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Meal meal = (Meal) parent.getItemAtPosition(position);
                EditText calories = (EditText) findViewById(R.id.calories);
                EditText mealName = (EditText) findViewById(R.id.meal);
                EditText sodium = (EditText) findViewById(R.id.sodium);
                EditText carbs = (EditText) findViewById(R.id.carbs);
                EditText protein = (EditText) findViewById(R.id.protein);
                mealName.setText(meal.getName());
                calories.setText(Double.toString(meal.getCalories()));
                sodium.setText(Double.toString(meal.getSodium()));
                carbs.setText(Double.toString(meal.getCarbs()));
                protein.setText((Double.toString(meal.getProtein())));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private List<Meal> getFrequentMeals() {
        List<Meal> list = new ArrayList<>();
        Meal dummy = new Meal("", new Date(System.currentTimeMillis()), 0, 2016112800);
        dummy.setCarbs(0);
        dummy.setSodium(0);
        dummy.setProtein(0);
        dummy.setCalories(0);
        dummy.setName("");
        list.add(dummy);
        List<Meal> res = db.getFrequentMeals();
        for(Meal m : res) list.add(m);
        return list;

    }

    // Helper function to take user to main menu
    private void toMainMenu() {
        Intent menu = new Intent(this, MainActivity.class);
        startActivity(menu);
    }

    public void onSpeakClick(View v) {
        if(v.getId() == R.id.speak) {
            promptSpeechInput();
        }
    }

    public void promptSpeechInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "What did you eat?");

        try {
            startActivityForResult(i, 233);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(InputActivity.this, "Don't touch me!", Toast.LENGTH_LONG).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);
        switch (requestCode) {
            case 233: if(resultCode == RESULT_OK && i != null) {
                ArrayList<String> res = i.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                EditText meal = (EditText) findViewById(R.id.meal);
                meal.setText(res.get(0));
            }
        }
    }

    // click handler for submit button
    public void onSubmitClick(View v) {
        // returning intent
        Intent i = new Intent();

        // EditText fields
        EditText calories = (EditText) findViewById(R.id.calories);
        EditText meal = (EditText) findViewById(R.id.meal);
        EditText sodium = (EditText) findViewById(R.id.sodium);
        EditText carbs = (EditText) findViewById(R.id.carbs);
        EditText protein = (EditText) findViewById(R.id.protein);

        if (!existing) {
            // get spinner value and set meal type correctly
            Spinner mealTypeSpinner = (Spinner) findViewById(R.id.mealtype_spinner);
            int typeCode = mealTypeSpinner.getSelectedItemPosition();

            if (meal.getText().toString().length() == 0) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                dialog.setMessage("Please enter a name for the meal.");

                // Set up the buttons
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                dialog.show();
                return;
            }

            Meal thisMeal = new Meal(meal.getText().toString().trim(), date, typeCode, 0);
            final DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.HALF_UP);

            double roundedCalories = Double.parseDouble(df.format(Double.parseDouble(calories.getText().toString())));
            double roundedCarbs = Double.parseDouble(df.format(Double.parseDouble(carbs.getText().toString())));
            double roundedProtein = Double.parseDouble(df.format(Double.parseDouble(protein.getText().toString())));
            double roundedSodium = Double.parseDouble(df.format(Double.parseDouble(sodium.getText().toString())));

            if (calories.length() != 0)
                thisMeal.setCalories(roundedCalories);
            if (carbs.length() != 0)
                thisMeal.setCarbs(roundedCarbs);
            if (protein.length() != 0)
                thisMeal.setProtein(roundedProtein);
            if (sodium.length() != 0)
                thisMeal.setSodium(roundedSodium);

            db.addMeal(thisMeal);

            toggleSubmitButton();
            setResult(EDIT_RESULT_OK, i);
            finish();
        } else {
            // get spinner value and set meal type correctly
            Spinner mealTypeSpinner = (Spinner) findViewById(R.id.mealtype_spinner);
            /* Originally we cannot update the meal type, now we can */
            int typeCode = mealTypeSpinner.getSelectedItemPosition();
            m.setType(typeCode);   // it seems not working actually, don't know why yet.

            // make sure meal name isn't empty
            if (meal.getText().toString().length() == 0) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);

                dialog.setMessage("Please enter a name for the meal.");

                // Set up the buttons
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                dialog.show();
                return;
            }

            /* Here we extract the name from updating page */
            if (meal.getText().toString().trim().length() != 0)
                m.setName(meal.getText().toString().trim());

            final DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.HALF_UP);

            double roundedCalories = Double.parseDouble(df.format(Double.parseDouble(calories.getText().toString())));
            double roundedCarbs = Double.parseDouble(df.format(Double.parseDouble(carbs.getText().toString())));
            double roundedProtein = Double.parseDouble(df.format(Double.parseDouble(protein.getText().toString())));
            double roundedSodium = Double.parseDouble(df.format(Double.parseDouble(sodium.getText().toString())));

            if (calories.length() != 0)
                m.setCalories(roundedCalories);
            if (carbs.length() != 0)
                m.setCarbs(roundedCarbs);
            if (protein.length() != 0)
                m.setProtein(roundedProtein);
            if (sodium.length() != 0)
                m.setSodium(roundedSodium);

            db.updateMeal(m);

            toggleSubmitButton();
            setResult(EDIT_RESULT_OK, i);
            finish();
        }
    }

    // click handler for click of delete button
    public void onDeleteClick(View v) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("Are you sure you want to delete this meal? " +
                "The meal will be permanently deleted.");

        // Set up the buttons
        dialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // returning intent
                Intent i = new Intent();

                if (existing) {
                    db.deleteMeal(mealID);
                    setResult(EDIT_RESULT_OK, i);
                    finish();
                } else {
                    finish();
                }
            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    /**
     * Add action listeners
     */
    private void toggleSubmitButton() {
        // change button color when clicked
        final Button submit = (Button) findViewById(R.id.submit);
        submit.setBackgroundColor(Color.rgb(0, 178, 238));
        submit.setText("Submitted");
    }
}
