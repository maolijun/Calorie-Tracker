package edu.upenn.cis350.group1.calorietracker;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import java.math.RoundingMode;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

// calendar activity responsible for Calendar window operation

public class CalendarActivity extends CalorieTrackerActivity {

    //private DatabaseHandler dbHandler;
    private DynamoDB dbHandler;
    private BottomMenu bottomMenu;
    private static final int RESULT_OK = 400;
    private static final int ACTIVITY_CALENDAR = 1;
    private static final String DATE_KEY = "date";
    private Date today;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        // create database handler
        //dbHandler = new DatabaseHandler(this.getApplicationContext());


        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );

        dbHandler = new DynamoDB(credentialsProvider);

        // create bottom menu
        bottomMenu = new BottomMenu(this){
            @Override
            public void update(int bottonNum) {
                if (bottonNum == BottomMenu.WATER_BOTTON){
                    dbHandler.setWaterForDate(bottomMenu.getDate(), bottomMenu.getValue());
                } else {
                    dbHandler.setWeightForDate(bottomMenu.getDate(), bottomMenu.getValue());
                }
                populateIntakeSummary(bottomMenu.getDate());
            }
        };

        // populate list view for the initial date
        CustomCalendarView calendarView = (CustomCalendarView) findViewById(R.id.calendar);
        calendarView.setDataBaseHandler(dbHandler);
        Date date = new Date(calendarView.getDate());
        today = date;
        //populateListView(date);
        populateIntakeSummary(date);

        // set listview click listener to enable editing meals from calendar
        ListView list = (ListView) findViewById(R.id.daily_summary);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                ListAdapter adapter = (ListAdapter) parent.getAdapter();

                Cursor c = (Cursor) adapter.getItem(position);

                int mealID = c.getInt(c.getColumnIndex("_id"));

                Intent mealEditingScreen = new Intent(CalendarActivity.this, InputActivity.class);
                mealEditingScreen.putExtra("EXISTING", true);
                mealEditingScreen.putExtra("MEAL_ID", mealID);
                startActivityForResult(mealEditingScreen, ACTIVITY_CALENDAR);
                c.close();
            }
        });

        // create change listener for calendar so that list view is populated with day's meals
        calendarView.setOnDateChangeListener(new CustomCalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CustomCalendarView view, int year, int month, int dayOfMonth) {
                Date d = new Date(year - 1900, month, dayOfMonth);
                //populateListView(d);
                populateIntakeSummary(d);

                Date select = new Date(view.getDate());
                Toast.makeText(view.getContext(), select.toString(), Toast.LENGTH_LONG).show();
                openPreviousActivity(select);

            }
        });
    }

    public void openPreviousActivity(Date date) {
        Intent intent = new Intent(this, PreviousActivity.class);
        intent.putExtra("selectedDate", date.getTime());

        currentUser = (User)getIntent().getSerializableExtra("currentUser");
        // put extra current user
        intent.putExtra("currentUser", currentUser);

        Calendar select = Calendar.getInstance();
        select.setTime(date);
        Calendar today = Calendar.getInstance();
        if (select.get(Calendar.YEAR) == today.get(Calendar.YEAR) && select.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                && select.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
            Intent intentForDaily = new Intent(this, DailyActivity.class);
            intentForDaily.putExtra("currentUser", currentUser);
            startActivity(intentForDaily);
        } else if (select.before(today)) {
            startActivity(intent);
        }
    }

    // populate list view
//    public void populateListView(Date date) {
//        // get cursor and list view objects
//        Cursor c = dbHandler.getAllMealsCursor(date);
//        ListView list = (ListView) findViewById(R.id.daily_summary);
//
//        // if query was empty nothing found and empty the listview
//        if (c == null || c.getCount() <= 0) {
//            list.setAdapter(null);
//            if(c != null){
//                c.close();
//            }
//            return;
//        }
//
//        // column names and view ids to update
//        String[] arrayColumns = new String[] {"name", "calories", "_id"};
//        int[] viewIDs = {R.id.textview_meal_title, R.id.textview_meal_calories};
//
//
//
//        // update the adapter
//        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.meal_item, c,
//                arrayColumns, viewIDs);
//
//        // display it in the listview
//        list.setAdapter(adapter);
//        c.close();
//
//    }

    // click handler for meal button
    public void onMealButtonClick(View v) {
        // get date currently in calendar view
        CustomCalendarView calendarView = (CustomCalendarView) findViewById(R.id.calendar);
        Date dateSelect = new Date(calendarView.getDate());
        if(dateSelect.toString().compareTo(today.toString())<=0){
            Intent inputActivity = new Intent(CalendarActivity.this, InputActivity.class);
            inputActivity.putExtra(DATE_KEY, calendarView.getDate());
            startActivityForResult(inputActivity, ACTIVITY_CALENDAR);
        }
    }

    // click handler for water button
    public void onWaterButtonClick(View v) {
        CustomCalendarView calendarView = (CustomCalendarView) findViewById(R.id.calendar);
        Date dateSelect = new Date(calendarView.getDate());
        if(dateSelect.toString().compareTo(today.toString())<=0){
            bottomMenu.setDate(dateSelect);
            bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WATER_BOTTON);
        }
    }

    // click handler for setting weight from Daily Screen
    public void onWeightButtonClick(View v) {
        CustomCalendarView calendarView = (CustomCalendarView) findViewById(R.id.calendar);
        Date dateSelect = new Date(calendarView.getDate());
        if(dateSelect.toString().compareTo(today.toString())<=0){
            bottomMenu.setDate(dateSelect);
            bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WEIGHT_BOTTON);
        }
    }


    public void populateIntakeSummary(Date date) {
        //get all meals for the given date
        List<Meal> meals = dbHandler.getAllMealsList(date);

        //create doubles to hold the nutrition information for all
        double cals = 0;
        double prot = 0;
        double sod = 0;
        double carbs = 0;
        double water;

        //Add nutrition information from each of the day's meals
        for (Meal m : meals) {
            cals += m.getCalories();
            prot += m.getProtein();
            sod += m.getSodium();
            carbs += m.getCarbs();
        }

        water = dbHandler.getWater(date);

        //find the TextView for each of the nutrition items
        TextView calsVal = (TextView) findViewById(R.id.cals_val);
        TextView protVal = (TextView) findViewById(R.id.prot_val);
        TextView sodVal = (TextView) findViewById(R.id.sod_val);
        TextView carbsVal = (TextView) findViewById(R.id.carb_val);
        TextView waterVal = (TextView) findViewById(R.id.water_val);

        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        //create strings to show that there is either no information, or to display the amount of
        // calories eaten
        String numCals = (cals == 0) ? "--" : df.format(cals);
        String numProt = (prot == 0) ? "--" : df.format(prot);
        String numSod = (sod == 0) ? "--" : df.format(sod);
        String numCarbs = (carbs == 0) ? "--" : df.format(carbs);
        String numWater = (water == -1) ? "--" : df.format(water);

        //Set the textViews with the strings created above
        calsVal.setText(numCals);
        protVal.setText(numProt);
        sodVal.setText(numSod);
        carbsVal.setText(numCarbs);
        waterVal.setText(numWater);

        //Find the user specified max values for each nutrition item
        int calMax = dbHandler.getSetting("calories");
        int protMax = dbHandler.getSetting("protein");
        int sodMax = dbHandler.getSetting("sodium");
        int carbMax = dbHandler.getSetting("carbs");

        //If the user has not specified max values, default to presets
        calMax = (calMax > 0) ? calMax : SettingsActivity.caloricDefault;
        protMax = (protMax > 0) ? protMax : SettingsActivity.proteinDefault;
        sodMax = (sodMax > 0) ? sodMax : SettingsActivity.sodiumDefault;
        carbMax = (carbMax > 0) ? carbMax : SettingsActivity.carbDefault;

        //change text color for each of the nutrition items based on its value
        if (cals > 0 && cals <= calMax) {
            calsVal.setTextColor(getResources().getColor(R.color.belowLimit));
        } else if (cals > calMax) {
            calsVal.setTextColor(getResources().getColor(R.color.aboveLimit));
        } else {
            calsVal.setTextColor(getResources().getColor(R.color.colorText));
        }

        if (prot > 0 && prot <= protMax) {
            protVal.setTextColor(getResources().getColor(R.color.belowLimit));
        } else if (prot > protMax) {
            protVal.setTextColor(getResources().getColor(R.color.aboveLimit));
        } else {
            protVal.setTextColor(getResources().getColor(R.color.colorText));
        }

        if (sod > 0 && sod <= sodMax) {
            sodVal.setTextColor(getResources().getColor(R.color.belowLimit));
        } else if (sod > sodMax) {
            sodVal.setTextColor(getResources().getColor(R.color.aboveLimit));
        } else {
            protVal.setTextColor(getResources().getColor(R.color.colorText));
        }

        if (carbs > 0 && carbs <= carbMax) {
            carbsVal.setTextColor(getResources().getColor(R.color.belowLimit));
        } else if (carbs > carbMax) {
            carbsVal.setTextColor(getResources().getColor(R.color.aboveLimit));
        } else {
            carbsVal.setTextColor(getResources().getColor(R.color.colorText));
        }
    }

    // called when a new meal is input using InputActivity
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        // if launched InputActivity returned properly then update list
        if(requestCode == ACTIVITY_CALENDAR && resultCode == RESULT_OK){
            // populate list view for the initial date
            CustomCalendarView calendarView = (CustomCalendarView) findViewById(R.id.calendar);
            Date date = new Date(calendarView.getDate());
            //populateListView(date);
            populateIntakeSummary(date);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent, Date date){
        super.onActivityResult(requestCode, resultCode, intent);

        // if launched InputActivity returned properly then update list
        if(requestCode == ACTIVITY_CALENDAR && resultCode == RESULT_OK){
            // populate list view for the initial date
            //populateListView(date);
            populateIntakeSummary(date);
        }
    }
}
