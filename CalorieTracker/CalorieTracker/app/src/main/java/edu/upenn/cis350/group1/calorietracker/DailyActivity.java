package edu.upenn.cis350.group1.calorietracker;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareButton;

import java.math.RoundingMode;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

// daily activity responsible for Daily view

public class DailyActivity extends CalorieTrackerActivity {

    // database handler
    //protected DatabaseHandler dbHandler;
    protected DynamoDB dbHandler;
    protected BottomMenu bottomMenu;

    // keys for various maps
    protected static final String KEY_MEAL_NAME = "mealName";
    protected static final String KEY_MEAL_TYPE = "type";
    protected static final String KEY_MEAL_CALORIES = "calories";
    protected static final String KEY_MEAL_ID = "mealID";
    protected static final int EDIT_RESULT_OK = 400;
    protected static final int ACTIVITY_DAILY = 1;
    protected static final int SPEAK_REQUEST_CODE = 233;


    // rigid meal type array inherited from Meal.java
    protected static final String[] types = {"Breakfast", "Lunch", "Dinner", "Snack"};

    protected TextView comment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily);

        String contentDescription = ((TextView)findViewById(R.id.comment)).getText().toString();
        ShareButton shareButton = (ShareButton)findViewById(R.id.fb_share_button);
        ShareLinkContent linkContent = new ShareLinkContent.Builder()
                .setContentTitle("I am using CalorieTracker")
                .setContentDescription(contentDescription)
                .setContentUrl(Uri.parse("http://developers.facebook.com/android"))
                .setShareHashtag(new ShareHashtag.Builder()
                        .setHashtag("#CalorieTracker")
                        .build())
                .build();
        shareButton.setShareContent(linkContent);

        // get current user
        Bundle b = getIntent().getExtras();
        if (b != null) {
            currentUser = (User) b.getSerializable("currentUser");
        }

        //Log.d("[Daily] Current User", currentUser.getEmail());

        // create database handler
        //dbHandler = new DatabaseHandler(getApplicationContext());
        // create bottom menu

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
                updateLabels();
            }
        };

        // update daily intakes of weight and water
        updateLabels();

        // update and expand Daily list view
        updateAndExpandListView();

        ExpandableListView view = (ExpandableListView) findViewById(R.id.daily_list);
        view.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                ExpandableListAdapter adapter = parent.getExpandableListAdapter();

                HashMap<String, String> data =
                        (HashMap) adapter.getChild(groupPosition, childPosition);

                int mealID = Integer.parseInt(data.get(KEY_MEAL_ID));

                Intent i = new Intent(DailyActivity.this, InputActivity.class);
                i.putExtra("EXISTING", true);
                i.putExtra("MEAL_ID", mealID);
                startActivityForResult(i, ACTIVITY_DAILY);
                return true;
            }
        });

        comment = (TextView) findViewById(R.id.comment);
    }

    // update daily intakes and list view when back to this activity
    @Override
    public void onResume() {
        super.onResume();
        updateLabels();
        updateAndExpandListView();

//        User user = new User();
//        user.setEmail("tianxiang@seas.upenn.edu");
//        user.setPassword("password");
//        user.addRecord(new Meal("sandwich", new Date(System.currentTimeMillis()), 0, 3));
//        user.addRecord(new Meal("Roasted Turkey", new Date(System.currentTimeMillis()), 1, 6));
//        user.addFriend("NewUserAsFriend@upenn.edu");
//        DynamoDB.save(user);
//        User userLoading = DynamoDB.load("tianxiang@cis.upenn.edu");
//        List<User.DateRecord> dateRecords = userLoading.getDateRecords();
//        User.DateRecord d = dateRecords.get(0);
//        Log.d("--> Database: ->" , d.getRecords().get(0).toString());
//        Log.d("--> Friends: ->" , user.getFriends().get(0));
    }

    // helper func to update daily intake labels
    private void updateLabels() {
        // get today's date
        Date date = new Date(System.currentTimeMillis());
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        // get water and weight values
        double weight = dbHandler.getWeight(date);
        double water = dbHandler.getWater(date);

        // set weight label if necessary
        if (weight >= 0.0) {
            TextView weightLabel = (TextView) findViewById(R.id.weight_counter);
            String weightString = df.format(dbHandler.getWeight(date)) + " lbs.";
            weightLabel.setText(weightString);
        }

        // set water label if necessary
        if (water >= 0.0) {
            TextView waterLabel = (TextView) findViewById(R.id.water_counter);
            String waterString = df.format(dbHandler.getWater(date)) + " oz.";
            waterLabel.setText(waterString);
        }

        /* Update the daily amount of nutrition facts, notice the Daily View has been changed, as well as newly added TextViews - Tianxiang Dong */
        // get list of meal
        //List<Meal> meals = dbHandler.getAllMealsList(date);
        List<Meal> meals = dbHandler.getAllMealsList(date);

        double[] nutrition = new double[4];
        for(Meal m : meals) {
            nutrition[0] += m.getCalories();
            nutrition[1] += m.getSodium();
            nutrition[2] += m.getCarbs();
            nutrition[3] += m.getProtein();
        }

        if(nutrition[0] >= 0.0 ) ((TextView)findViewById(R.id.calorie_counter)).setText(df.format(nutrition[0]) + "");
        if(nutrition[1] >= 0.0 ) ((TextView)findViewById(R.id.sodium_counter)).setText(df.format(nutrition[1]) + " mg");
        if(nutrition[2] >= 0.0 ) ((TextView)findViewById(R.id.carbs_counter)).setText(df.format(nutrition[2]) + " g");
        if(nutrition[3] >= 0.0 ) ((TextView)findViewById(R.id.protein_counter)).setText(df.format(nutrition[3]) + " g");
    }

    // click handler for adding new meal from Daily Screen
    public void onMealButtonClick(View v) {
        Intent mealInputScreen = new Intent(DailyActivity.this, InputActivity.class);
        currentUser = (User)getIntent().getSerializableExtra("currentUser");
        mealInputScreen.putExtra("currentUser", currentUser);

        startActivityForResult(mealInputScreen, ACTIVITY_DAILY);
        updateLabels();
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
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "How do you feel today?");

        try {
            startActivityForResult(i, SPEAK_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(DailyActivity.this, "Don't touch me!", Toast.LENGTH_LONG).show();
        }
    }

    // click handler for water button
    public void onWaterButtonClick(View v) {
        bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WATER_BOTTON);
    }

    // click handler for setting weight from Daily Screen
    public void onWeightButtonClick(View v) {
        bottomMenu.onButtonClick(this, dbHandler, BottomMenu.WEIGHT_BOTTON);
    }


    // fetch and prepare data for the listview
    private ExpandableListAdapter prepareListData() {
        // get current time
        Date date = new Date(System.currentTimeMillis());

        // get list of meals
        //List<Meal> meals = dbHandler.getAllMealsList(date);
        List<Meal> meals = dbHandler.getAllMealsList(date);

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

        updateLabels();  // newly added to update daily amount at the bottom

        // return the actual adapter
        return new SimpleExpandableListAdapter(DailyActivity.this, parentMapList,
                groupLayout, groupFrom, groupTo, childListOfListOfMaps, childLayout, childFrom,
                childTo);
    }

    // update data in list view and expand categories with data
    private void updateAndExpandListView() {
        // get adapter and view
        ExpandableListAdapter adapter = prepareListData();
        ExpandableListView view = (ExpandableListView) findViewById(R.id.daily_list);

        // show the actual view
        view.setAdapter(adapter);

        // expand categories that contain data
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            if (adapter.getChildrenCount(i) > 0) view.expandGroup(i);
        }
        updateLabels();
    }

    // called when a new meal is input using InputActivity
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);

        // if launched InputActivity returned properly then update list
        if(requestCode == ACTIVITY_DAILY && resultCode == EDIT_RESULT_OK){
            updateAndExpandListView();
        }

        if(requestCode == SPEAK_REQUEST_CODE && resultCode == RESULT_OK && intent != null) {
            ArrayList<String> res = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            comment.setText(res.get(0));
        }
    }
}
