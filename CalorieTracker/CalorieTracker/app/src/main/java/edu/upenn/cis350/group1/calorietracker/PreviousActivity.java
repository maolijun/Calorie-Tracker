package edu.upenn.cis350.group1.calorietracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huangli1 on 11/1/2016.
 */
public class PreviousActivity extends CalorieTrackerActivity {

    //protected DatabaseHandler dbHandler;
    protected DynamoDB dbHandler;
    protected BottomMenu bottomMenu;
    protected static final String KEY_MEAL_NAME = "mealName";
    protected static final String KEY_MEAL_TYPE = "type";
    protected static final String KEY_MEAL_CALORIES = "calories";
    protected static final String KEY_MEAL_ID = "mealID";
    protected static final int RESULT_OK = 400;
    protected static final int ACTIVITY_PREVIOUS = 1;
    protected static final String[] types = {"Breakfast", "Lunch", "Dinner", "Snack"};

    private Date select;
    private static final String DATE_KEY = "date";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous);

        Long time = getIntent().getLongExtra("selectedDate", System.currentTimeMillis());
        select = new Date(time);
        Log.d("-------> Selected Date" , DynamoDB.getDateID(select));
        Toast.makeText(this.getApplicationContext(),select.toString(), Toast.LENGTH_SHORT).show();
        setTitle(select.toString());

//        dbHandler = new DatabaseHandler(getApplicationContext());

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );
        dbHandler = new DynamoDB(credentialsProvider);

        bottomMenu = new BottomMenu(this){
            @Override
            public void update(int bottonNum) {
                if (bottonNum == BottomMenu.WATER_BOTTON){
                    dbHandler.setWaterForDate(bottomMenu.getDate(), bottomMenu.getValue());
                } else {
                    dbHandler.setWeightForDate(bottomMenu.getDate(), bottomMenu.getValue());
                }
                intakeSummary();
            }
        };

        updateAndExpandListView();
    }

    // fetch and prepare data for the listview
    private ExpandableListAdapter prepareListData() {

        // get list of meals
        List<Meal> meals = dbHandler.getAllMealsList(select);

        // list of maps to hold categories of meals
        ArrayList<HashMap<String, String>> parentMapList = new ArrayList<>();

        /* Get Calorie for each meal Type respectively */
        double[] calories = new double[4];
        for(Meal m : meals) {
            calories[m.getTypeCode()] += m.getCalories();
        }

        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // add a map entry for every parent to the above list
        for (int i = 0; i < types.length; i++) {
            HashMap<String, String> group = new HashMap<>();
            group.put(KEY_MEAL_TYPE, types[i] + " (Calorie: " + df.format(calories[i]) + ")");   // add text to Daily View
            parentMapList.add(group);
        }

        // parameters required by list adapter - shows value of key "type in mealtype_header
        int groupLayout = R.layout.meal_group;
        String[] groupFrom = new String[] { KEY_MEAL_TYPE };
        int[] groupTo = new int [] { R.id.mealtype_header };

        // create list of list of maps for child view's purposes
        ArrayList<ArrayList<HashMap<String, String>>> childListOfListOfMaps = new ArrayList<>();

        // create empty lists of maps for children of each category
        for (int i = 0; i < parentMapList.size(); i++) {
            ArrayList<HashMap<String, String>> children = new ArrayList<>();
            childListOfListOfMaps.add(children);
        }

        // add each individual meal as a child of appropriate parent
        for (int i = 0; i < meals.size(); i++) {
            Meal m = meals.get(i);
            int typeCode = m.getTypeCode();
            ArrayList<HashMap<String, String>> listOfMaps = childListOfListOfMaps.get(typeCode);

            HashMap<String, String> mealProperties = new HashMap<>();
            mealProperties.put(KEY_MEAL_NAME, m.getName());
            mealProperties.put(KEY_MEAL_CALORIES, String.valueOf(m.getCalories()));
            mealProperties.put(KEY_MEAL_ID, String.valueOf(m.getMealID()));

            listOfMaps.add(mealProperties);
        }

        // parameters required by list adapter
        int childLayout = R.layout.meal_item;
        String[] childFrom = new String[] { KEY_MEAL_NAME, KEY_MEAL_CALORIES };
        int[] childTo = new int [] { R.id.textview_meal_title, R.id.textview_meal_calories };

        intakeSummary();  // newly added to update daily amount at the bottom

        // return the actual adapter
        return new SimpleExpandableListAdapter(PreviousActivity.this, parentMapList,
                groupLayout, groupFrom, groupTo, childListOfListOfMaps, childLayout, childFrom,
                childTo);
    }

    // update data in list view and expand categories with data
    private void updateAndExpandListView() {
        // get adapter and view
        ExpandableListAdapter adapter = prepareListData();
        ExpandableListView view = (ExpandableListView) findViewById(R.id.previous_list);

        // show the actual view
        view.setAdapter(adapter);

        // expand categories that contain data
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            if (adapter.getChildrenCount(i) > 0) view.expandGroup(i);
        }
        intakeSummary();
    }

    public void intakeSummary() {
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // get water value
        double water = dbHandler.getWater(select);
        if (water >= 0.0) {
            TextView waterVal = (TextView) findViewById(R.id.water_val);
            String waterString = df.format(dbHandler.getWater(select)) + " oz.";
            waterVal.setText(waterString);
        }

        // get list of meal
        List<Meal> meals = dbHandler.getAllMealsList(select);
        double[] nutrition = new double[4];
        for(Meal m : meals) {
            nutrition[0] += m.getCalories();
            nutrition[1] += m.getSodium();
            nutrition[2] += m.getCarbs();
            nutrition[3] += m.getProtein();
        }

        if(nutrition[0] >= 0.0 ) ((TextView)findViewById(R.id.cal_val)).setText(df.format(nutrition[0]) + "");
        if(nutrition[1] >= 0.0 ) ((TextView)findViewById(R.id.sod_val)).setText(df.format(nutrition[1]) + " mg");
        if(nutrition[2] >= 0.0 ) ((TextView)findViewById(R.id.carb_val)).setText(df.format(nutrition[2]) + " g");
        if(nutrition[3] >= 0.0 ) ((TextView)findViewById(R.id.prot_val)).setText(df.format(nutrition[3]) + " g");

    }

    public void onMealButtonClick(View v) {
        Intent inputActivity = new Intent(PreviousActivity.this, InputActivity.class);
        inputActivity.putExtra(DATE_KEY, select.getTime());
        currentUser = (User)getIntent().getSerializableExtra("currentUser");
        inputActivity.putExtra("currentUser", currentUser);

        startActivityForResult(inputActivity, ACTIVITY_PREVIOUS);
    }

    public void onWaterButtonClick(View v) {
        bottomMenu.setDate(select);
        bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WATER_BOTTON);
    }

    public void onWeightButtonClick(View v) {
        bottomMenu.setDate(select);
        bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WEIGHT_BOTTON);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // if launched InputActivity returned properly then update list
        if(requestCode == ACTIVITY_PREVIOUS && resultCode == RESULT_OK){
            updateAndExpandListView();
        }
    }
}
