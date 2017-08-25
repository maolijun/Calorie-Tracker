package edu.upenn.cis350.group1.calorietracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jussi Lundstedt on 2/20/2016.
 *
 * SQLite database handler for Calorie Tracker
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    // database fields
    private static final int DATABASE_VERSION = 5; // database version, 1: initial, 2: incl. weight
    private static final String DATABASE_NAME = "calorieTracker"; // database name

    // date table name & fields
    private static final String TABLE_DATES = "dates";
    private static final String DATES_KEY_ID = "_id";
    private static final String DATES_KEY_DATE = "date";
    private static final String DATES_KEY_WEIGHT = "weight";
    private static final String DATES_KEY_WATER = "water";

    // goals table name & fields
    private static final String TABLE_GOALS = "goals";
    private static final String GOALS_KEY_ID = "_id";
    private static final String GOALS_KEY_SETTING = "setting";
    private static final String GOALS_KEY_VALUE = "value";

    // meal table name & fields
    private static final String TABLE_MEALS = "meals";
    private static final String MEALS_KEY_ID = "_id";
    private static final String MEALS_KEY_DATE_ID = "dateID";
    private static final String MEALS_KEY_TYPE = "mealType";
    private static final String MEALS_KEY_NAME = "name";
    private static final String MEALS_KEY_CALORIES = "calories";
    private static final String MEALS_KEY_PROTEIN = "protein";
    private static final String MEALS_KEY_CARBS = "carbs";
    private static final String MEALS_KEY_SODIUM = "sodium";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getTableMeals() {
        return TABLE_MEALS;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL query to create date table
        String CREATE_DATES_TABLE = "CREATE TABLE " + TABLE_DATES + "("
                + DATES_KEY_ID + " INTEGER PRIMARY KEY,"
                + DATES_KEY_DATE + " TEXT NOT NULL,"
                + DATES_KEY_WEIGHT + " REAL DEFAULT 0.0,"
                + DATES_KEY_WATER + " REAL DEFAULT 0.0"
                + ")";
        // create dates table
        db.execSQL(CREATE_DATES_TABLE);

        // SQL query to create meals table
        String CREATE_MEALS_TABLE = "CREATE TABLE " + TABLE_MEALS + "("
                + MEALS_KEY_ID + " INTEGER PRIMARY KEY,"
                + MEALS_KEY_DATE_ID + " INTEGER NOT NULL,"
                + MEALS_KEY_TYPE + " INTEGER NOT NULL,"
                + MEALS_KEY_NAME + " TEXT NOT NULL,"
                + MEALS_KEY_CALORIES + " REAL,"
                + MEALS_KEY_PROTEIN + " REAL,"
                + MEALS_KEY_CARBS + " REAL,"
                + MEALS_KEY_SODIUM + " REAL,"
                + "FOREIGN KEY(" + MEALS_KEY_DATE_ID + ") REFERENCES " + TABLE_DATES + "(" + DATES_KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_MEALS_TABLE);

        // SQL query to create goals table
        String CREATE_GOALS_TABLE = "CREATE TABLE " + TABLE_GOALS + "("
                + GOALS_KEY_SETTING + " TEXT PRIMARY KEY,"
                + GOALS_KEY_VALUE + " INTEGER"
                + ")";
        // create goals table
        db.execSQL(CREATE_GOALS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // switch with new version, facilitates new database versions if necessary
        switch (newVersion) {
            case 3:
                // SQL query to create goals table
                String CREATE_GOALS_TABLE = "CREATE TABLE " + TABLE_GOALS + "("
                        + GOALS_KEY_SETTING + " TEXT PRIMARY KEY,"
                        + GOALS_KEY_VALUE + " INTEGER"
                        + ")";
                // create goals table
                db.execSQL(CREATE_GOALS_TABLE);
                break;
            case 4:
                // SQL query to add weight column
                String ADD_WEIGHT_COLUMN = "ALTER TABLE " + TABLE_DATES
                        + " ADD COLUMN " + DATES_KEY_WEIGHT + " REAL DEFAULT 0.0";
                db.execSQL(ADD_WEIGHT_COLUMN);
                break;
            case 5:
                // SQL query to add water column
                String ADD_WATER_COLUMN = "ALTER TABLE " + TABLE_DATES
                        + " ADD COLUMN " + DATES_KEY_WATER + " REAL DEFAULT 0.0";
                db.execSQL(ADD_WATER_COLUMN);
                break;
            default:
                break;
        }
    }

    // add meal to database
    public int addMeal(Meal meal) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // get date of meal
        Date d = meal.getDateEaten();

        // insert date into database, doesn't matter if date already exists, method returns id either way
        int dateID = addDate(d);

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        values.put(MEALS_KEY_DATE_ID, dateID);
        values.put(MEALS_KEY_TYPE, meal.getTypeCode());
        values.put(MEALS_KEY_NAME, meal.getName());
        // only insert these if they've been inserted
        if (meal.getCalories() != 0.0) values.put(MEALS_KEY_CALORIES, meal.getCalories());
        if (meal.getProtein() != 0.0) values.put(MEALS_KEY_PROTEIN, meal.getProtein());
        if (meal.getCarbs() != 0.0) values.put(MEALS_KEY_CARBS, meal.getCarbs());
        if (meal.getSodium() != 0.0) values.put(MEALS_KEY_SODIUM, meal.getSodium());

        // put meal into external database
        DynamoDB.addMeal(meal);

        // insert to meals table
        return (int) db.insert(TABLE_MEALS, null, values);
    }

    // add date to database - returns id of new date or if found in table already will return id
    // of existing date
    public int addDate(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // query if date already exists
        Cursor c = db.query(TABLE_DATES, new String[] { DATES_KEY_ID }, DATES_KEY_DATE + "=?",
                new String[]{date.toString()}, null, null, null, null);

        int dateID = 0; // date ID to return
        if (c != null) { // check if cursor is null
            // if date not in table, insert date into table
            if (!c.moveToFirst()) {
                ContentValues dateContent = new ContentValues();
                dateContent.put(DATES_KEY_DATE, date.toString());
                dateID = (int) db.insert(TABLE_DATES, null, dateContent);
            } else { // otherwise just get the id of the date
                c.moveToFirst();
                dateID = c.getInt(0);
            }
            c.close(); // close cursor
        }

        return dateID;
    }

    // get the ID of this date in the database, returns -1 if doesn't exist
    public int getDateID(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();
        int dateID = -1; // date ID to return

        // query date
        Cursor c = db.query(TABLE_DATES, new String[]{DATES_KEY_ID}, DATES_KEY_DATE + "=?",
                new String[]{date.toString()}, null, null, null, null);

        if (c != null) { // if query not null
            if (!c.moveToFirst()) { // matching record not found, return default -1
                c.close();
                return dateID;
            } else { // else get the existing date id
                dateID = c.getInt(0); // get date ID if date was found
                c.close();
            }
        }

        return dateID; // return the date ID
    }

    // get weight for given date, returns -1 if date not in database
    // weight defaults to 0.0 = not set.
    public double getWeight(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // get date id of given date
        int dateID = getDateID(date);

        // if date not found return -1
        if (dateID == -1) return dateID;

        // query database
        Cursor c = db.query(TABLE_DATES, null, DATES_KEY_ID + "=?",
                new String[] { Integer.toString(dateID) }, null, null, null);

        if (c.moveToFirst()) {
            double retValue = c.getDouble(2);
            c.close();
            return retValue;
        } else {
            c.close();
            return -1;
        }
    }

    // set weight for given date
    public void setWeightForDate(Date date, double weight) {
        // add date to db if not there already
        int dateID = getDateID(date);
        if (dateID == -1) {
            dateID = addDate(date);
        }

        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        values.put(DATES_KEY_WEIGHT, weight);

        // insert to dates table
        db.update(TABLE_DATES, values, "_id=" + dateID, null);

        DynamoDB.setWeightForDate(date, weight);
    }

    // get list of meals for a given date
    public List<Meal> getAllMealsList(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create list of meals
        ArrayList<Meal> meals = new ArrayList<>();

        // first query date to get dateID
        int dateID = getDateID(date);
        if (dateID != -1) { // if date doesn't exist return empty list otherwise get to doing stuff
            Cursor c = db.query(TABLE_MEALS, null, MEALS_KEY_DATE_ID + "=?",
                    new String[] { Integer.toString(dateID)}, null, null, MEALS_KEY_ID + " ASC");

            if (c.moveToFirst()) {
                do {
                    // create meal
                    Meal m = new Meal(c.getString(3), date, c.getInt(2), c.getInt(0));
                    // if these values are not null, add these values to the meal
                    if (!c.isNull(4)) m.setCalories(c.getDouble(4));
                    if (!c.isNull(5)) m.setProtein(c.getDouble(5));
                    if (!c.isNull(6)) m.setCarbs(c.getDouble(6));
                    if (!c.isNull(7)) m.setSodium(c.getDouble(7));
                    meals.add(m);
                } while (c.moveToNext());
            }

            c.close();
        }
        return meals;
    }

    // get cursor for all meals (just a helper to make it easier for populating ListView)
    public Cursor getAllMealsCursor(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // first query date to get dateID
        int dateID = getDateID(date);
        if (dateID != -1) { // if date doesn't exist return empty list otherwise get to doing stuff
            return db.query(TABLE_MEALS, null, MEALS_KEY_DATE_ID + "=?",
                    new String[] { Integer.toString(dateID) }, null, null, MEALS_KEY_ID + " ASC");
        }

        return null; // returns null if date is empty
    }

    // get meal with given ID
    public Meal getMeal(int id) {
        if (id == -1) return null;
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // query meals table
        Cursor c = db.query(TABLE_MEALS, null, MEALS_KEY_ID + "=?",
                new String[]{Integer.toString(id)}, null, null, null);
        if (c.getCount() == 1) { // if meal exists
            c.moveToFirst();
            int dateID = c.getInt(1); // get dateID and query dates table
            Cursor d = db.query(TABLE_DATES, null, DATES_KEY_ID + "=?",
                    new String[]{Integer.toString(dateID)}, null, null, null);
            d.moveToFirst();
            Date date = new Date(d.getLong(1));

            // generate meal
            int mealType = c.getInt(2);
            String mealName = c.getString(3);
            Meal m = new Meal(mealName, date, mealType, id);
            if (!c.isNull(4)) m.setCalories(c.getDouble(4));
            if (!c.isNull(5)) m.setProtein(c.getDouble(5));
            if (!c.isNull(6)) m.setCarbs(c.getDouble(6));
            if (!c.isNull(7)) m.setSodium(c.getDouble(7));
            c.close();
            d.close();
            return m;
        }
        c.close();
        return null; // if meal wasn't found or db is broken return null
    }

    // update existing meal
    public void updateMeal(Meal meal) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        //values.put(MEALS_KEY_NAME, meal.getName());
        // only insert these if they've been inserted

        /* We can update the name of every meal now */
        if (meal.getName() != null && meal.getName().length() != 0) {
            values.put(MEALS_KEY_NAME, meal.getName());
        } else {
            values.put(MEALS_KEY_NAME, "");
        }
        /* Update Type Code in Database, Now the type updating works - Tianxiang Dong */
        values.put(MEALS_KEY_TYPE, meal.getTypeCode());

        if (meal.getCalories() != 0.0) values.put(MEALS_KEY_CALORIES, meal.getCalories());
        if (meal.getProtein() != 0.0) values.put(MEALS_KEY_PROTEIN, meal.getProtein());
        if (meal.getCarbs() != 0.0) values.put(MEALS_KEY_CARBS, meal.getCarbs());
        if (meal.getSodium() != 0.0) values.put(MEALS_KEY_SODIUM, meal.getSodium());

        // insert to meals table
        db.update(TABLE_MEALS, values, "_id=" + meal.getMealID(), null);

        //DynamoDB.updateMeal(meal);
    }

    // add a setting to db
    public void addSetting(String setting, int defaultGoal) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        values.put(GOALS_KEY_SETTING, setting);
        values.put(GOALS_KEY_VALUE, defaultGoal);

        // insert to meals table
        db.insert(TABLE_GOALS, null, values);
    }

    // update existing settings
    public void updateSettings(String setting, int newGoal) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        values.put(GOALS_KEY_SETTING, setting);
        values.put(GOALS_KEY_VALUE, newGoal);

        // insert to meals table
        db.update(TABLE_GOALS, values, GOALS_KEY_SETTING + "='" + setting + "'", null);
    }

    // get value for given setting, returns -1 if setting not in database
    public int getSetting(String setting) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // query database
        Cursor c = db.query(TABLE_GOALS, null, GOALS_KEY_SETTING + "=?",
                new String[] { setting }, null, null, null);
        if (c.moveToFirst()) {
            int retValue = c.getInt(1);
            c.close();
            return retValue;
        } else {
            c.close();
            return -1;
        }
    }

    // delete existing meal by ID
    public void deleteMeal(int mealID) {
        SQLiteDatabase db = this.getWritableDatabase(); // get db
        int dateID; // date id for this meal's date

        // database safety clause - make sure deletion query only returns 1 result
        Cursor mealIDCursor = db.query(TABLE_MEALS, new String[] { MEALS_KEY_ID, MEALS_KEY_DATE_ID },
                MEALS_KEY_ID + "=?", new String[]{Integer.toString(mealID)}, null, null, null);

        // return if no result or more than 1 result found
        if (mealIDCursor.getCount() != 1) {
            mealIDCursor.close();
            return;
        } else {
            mealIDCursor.moveToFirst();
            // if found exactly 1 meal delete that meal
            dateID = mealIDCursor.getInt(1);
            db.delete(TABLE_MEALS, MEALS_KEY_ID + "=?", new String[] { Integer.toString(mealID) });
        }
        mealIDCursor.close();

        // query how many meals exist for this date after deletion
        Cursor dateIDCursor = db.query(TABLE_MEALS, new String[]{MEALS_KEY_ID},
                MEALS_KEY_DATE_ID + "=?", new String[]{Integer.toString(dateID)}, null, null, null);

        // if deleted meal was the only one for that day delete date from db too
        // requisite to keep CalendarView synced
        if (dateIDCursor.getCount() == 0) {
            db.delete(TABLE_DATES, DATES_KEY_ID + "=?", new String[]{Integer.toString(dateID)});
        }
        dateIDCursor.close();
    }

    // get water intake for given date, returns -1 if date not in database
    // water defaults to 0.0 = not set.
    public double getWater(Date date) {
        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // get date id of given date
        int dateID = getDateID(date);

        // if date not found return -1
        if (dateID == -1) return dateID;

        // query database
        Cursor c = db.query(TABLE_DATES, null, DATES_KEY_ID + "=?",
                new String[] { Integer.toString(dateID) }, null, null, null);
        if (c.moveToFirst()) {
            double retValue = c.getDouble(3);
            c.close();
            // return water intake if found
            return retValue;
        } else {
            c.close();
            return -1;
        }
    }

    // set water intake for given date
    public void setWaterForDate(Date date, double water) {
        // add date to db if not there already
        int dateID = getDateID(date);
        if (dateID == -1) {
            dateID = addDate(date);
        }

        // get db
        SQLiteDatabase db = this.getWritableDatabase();

        // create a content values bundle for insertion
        ContentValues values = new ContentValues();
        values.put(DATES_KEY_WATER, water);

        // insert to dates table
        db.update(TABLE_DATES, values, "_id=" + dateID, null);

        DynamoDB.setWaterForDate(date, water);
    }
}
