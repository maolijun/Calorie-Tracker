package edu.upenn.cis350.group1.calorietracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;

import java.math.RoundingMode;
import java.sql.Date;
import java.text.DecimalFormat;

/**
 * Set button handlers for water button and weight button
 * DailyActivity and CalendarActivity use the BottomMenu
 */

public class BottomMenu extends CalorieTrackerActivity {

    protected Date date;                 // need this for AlertDialog
    protected double value;              // need this for AlertDialog
    private AlertDialog.Builder dialog;
    static final int WEIGHT_BOTTON = 1;
    static final int WATER_BOTTON = 2;

    // constructor
    public BottomMenu(Context context) {
        date = new Date(System.currentTimeMillis());
        dialog = new AlertDialog.Builder(context);
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return this.date;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    /**
     * This method sets the input box and AlertDialog
     * @param context
     * @param dbHandler
     * @param bottonNum weight botton is number 1, water botton is number 2
     */
    public void onButtonClick(Context context, DynamoDB dbHandler, final int bottonNum) {

        // set input box
        final EditText inputBox = new EditText(context);
        inputBox.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // set input align center
        inputBox.setGravity(Gravity.CENTER);
        double intake = 0.0;
        if(bottonNum == WATER_BOTTON){
            dialog.setTitle("Water intake on " + date.toString());
            intake = dbHandler.getWater(date);
        }
        if(bottonNum == WEIGHT_BOTTON){
            dialog.setTitle("Weight for " + date.toString());
            intake = dbHandler.getWeight(date);
        }
        if (intake == -1) {
            inputBox.setHint(Double.toString(0.0));
        } else {
            inputBox.setText(Double.toString(intake));
        }
        dialog.setView(inputBox);

        // set AlertDialog bottons
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        dialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // save value as either a water recording
                double input = Double.parseDouble(df.format(Double.parseDouble(inputBox.getText().toString())));
                setValue(input);
                if (value >= 0.0) {
                    update(bottonNum);
                } else {
                    dialog.cancel();
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
     * This method updates waterIntake / weight in database
     * Must override the method to do specific work in CalendarActivity and DailyActivity
     * @param bottonLabel
     */
    public void update(int bottonLabel) {
        return;
    }

}
