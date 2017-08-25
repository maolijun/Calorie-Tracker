package edu.upenn.cis350.group1.calorietracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Switch;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yishang on 10/30/2016.
 */
public class DynamoDB {

    protected static DynamoDBMapper mapper;
    protected static User currentUser;
    protected static PaginatedScanList<User> allUsers;

    public DynamoDB(CognitoCachingCredentialsProvider credentialsProvider) {
        AmazonDynamoDBClient ddbClient = Region.getRegion(Regions.US_WEST_2) // CRUCIAL
            .createClient(
                AmazonDynamoDBClient.class,
                credentialsProvider,
                new ClientConfiguration()
            );

        mapper = new DynamoDBMapper(ddbClient);
    }

    public DynamoDBMapper getMapper() {
        return mapper;
    }

    public static void save(final User user) {
        currentUser = user;

        Runnable runnable = new Runnable() {
            public void run() {
                mapper.save(user);
            }
        };

        Thread ddbThread = new Thread(runnable);
        ddbThread.start();
        try {
            ddbThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static User load(final String email) {
        currentUser = new User();

        Runnable runnable = new Runnable() {
            public void run() {
                currentUser = mapper.load(User.class, email);
            }
        };

        Thread ddbThread = new Thread(runnable);
        ddbThread.start();
        try {
            ddbThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return currentUser;
    }

    public static PaginatedScanList<User> getAllUsers() {
        allUsers = null;

        Runnable runnable = new Runnable() {
            public void run() {
                allUsers = mapper.scan(User.class, new DynamoDBScanExpression());
            }
        };

        Thread ddbThread = new Thread(runnable);
        ddbThread.start();

        while (allUsers == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        return allUsers;
    }


    public static void addMeal(Meal meal){
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        String dateID = getDateID(meal.getDateEaten());
        boolean creating = true;
        User.DateRecord dateRecord = null;

        for(User.DateRecord d : dateRecords) {
            if(d.getDateID().equals(dateID)) {
                creating = false;
                dateRecord = d;
            }
        }
        if(dateRecords.isEmpty() || creating){
            dateRecords.add(new User.DateRecord(dateID));
            dateRecord = dateRecords.get(dateRecords.size() - 1);
        }

        User.Record record = new User.Record(meal.getName(), meal.getDateEaten().getTime(), meal.getTypeCode(), meal.getMealID());
        record.setCalories(meal.getCalories());
        record.setCarbs(meal.getCarbs());
        record.setProtein(meal.getProtein());
        record.setSodium(meal.getSodium());
        dateRecord.addRecord(record);
        save(currentUser);
    }

    public static void setWaterForDate(Date date, double water) {
        String dateID = getDateID(date);
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        int i;
        for(i = 0; i < dateRecords.size(); i++) {
            if(Integer.parseInt(dateID) == Integer.parseInt(dateRecords.get(i).getDateID())) break;

//            if(Integer.parseInt(dateID) > Integer.parseInt(dateRecords.get(i).getDateID())) {
//                dateRecords.add(i, new User.DateRecord(dateID));
//                break;
//            }
        }


        if(i == dateRecords.size()) dateRecords.add(new User.DateRecord(dateID));
        User.DateRecord dateRecord = dateRecords.get(i);
        dateRecord.setWater(water);
        save(currentUser);
    }

    public static void setWeightForDate(Date date, double weight) {
        String dateID = getDateID(date);
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        int i;
        for(i = 0; i < dateRecords.size(); i++) {
            if(Integer.parseInt(dateID) == Integer.parseInt(dateRecords.get(i).getDateID())) break;

//            if(Integer.parseInt(dateID) > Integer.parseInt(dateRecords.get(i).getDateID())) {
//                dateRecords.add(i, new User.DateRecord(dateID));
//                break;
//            }
        }
        if(i == dateRecords.size()) dateRecords.add(new User.DateRecord(dateID));
        User.DateRecord dateRecord = dateRecords.get(i);
        dateRecord.setWeight(weight);
        save(currentUser);
    }
    public double getWater(java.sql.Date date) {
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        String dateID = getDateID(date);
        for(User.DateRecord d : dateRecords) {
            if(d.getDateID().equals(dateID)) {
                return d.getWater();
            }
        }
        return -1;
    }

    public double getWeight(java.sql.Date date) {
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        String dateID = getDateID(date);
        for(User.DateRecord d : dateRecords) {
            if(d.getDateID().equals(dateID)) {
                return d.getWeight();
            }
        }
        return -1;
    }

    public void updateMeal(Meal meal) {
        String dateID = getDateID(meal.getDateEaten());
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        int i = 0;
//        for(i = 0; i < dateRecords.size(); i++) {
//            if(Integer.parseInt(dateID) == Integer.parseInt(dateRecords.get(i).getDateID())) break;
//        }
        Log.d("meal to update ---> D:", getDateID(meal.getDateEaten()));
        Log.d("meal to update ---> N:", meal.getName());
        Log.d("meal to update ---> ID:", meal.getMealID()+"");
        if(i != dateRecords.size()) {
            //User.DateRecord dateRecord = dateRecords.get(i);
            User.DateRecord dateRecord = currentUser.getDateRecordByDate(recordIDtoDateID(meal.getMealID()));
            //User.DateRecord dateRecord = dateRecords.get(dateRecords.size() - 1);
            for (User.Record record : dateRecord.getRecords()) {
                //Log.d("RecordID ---> ID:", record.getRecordID() + "");
                if (record.getRecordID().intValue() == meal.getMealID()) {
                    Log.d("meal to update ---> ", meal.getCalories() + "");
                    Log.d("meal to update ---> ", meal.getSodium() + "");
                    Log.d("meal to update ---> ", meal.getProtein() + "");
                    Log.d("meal to update ---> ", meal.getCarbs() + "");

                    record.setCalories(meal.getCalories());
                    record.setSodium(meal.getSodium());
                    record.setProtein(meal.getProtein());
                    record.setCarbs(meal.getCarbs());
                    record.setTypeCode(meal.getTypeCode());
                    record.setMealName(meal.getName());
                }
            }
        }
        save(currentUser);
    }



    public Meal getMeal(int id) {
        if (id == -1) return null;
        String dateID = recordIDtoDateID(id);
        List<Meal> meals = getAllMealsListByDateID(dateID);
        for(Meal meal : meals) {
            if(meal.getMealID() == id) return meal;
        }
        return null;
    }

    public void deleteMeal(int id) {
        if (id == -1) return;
        String dateID = recordIDtoDateID(id);
        User.DateRecord dateRecord = currentUser.getDateRecordByDate(dateID);
        for(User.Record r : dateRecord.getRecords()) {
            if(r.getRecordID() == id) {
                dateRecord.getRecords().remove(r);
                save(currentUser);
                return;
            }
        }
    }

    private String recordIDtoDateID(int mealID) {
        String recordID = mealID + "";
        return recordID.substring(0, 8);
    }

    public static String getDateID(Date date) {
        int dateInt = date.getDate();
        String dateStr = dateInt + "";
        if(dateStr.length() == 1) dateStr = "0" + dateStr;

        int monthInt = date.getMonth() + 1;
        String monthStr = monthInt + "";
        if(monthStr.length() == 1) monthStr = "0" + monthStr;
        String dateID = (date.getYear()+1900) + "" + monthStr + dateStr;
        return dateID;
    }

    public void addSetting(String setting, int defaultGoal) {
        updateSettings(setting, defaultGoal);
    }

    // update existing settings
    public void updateSettings(String setting, int newGoal) {
        switch (setting) {
            case "calories" :
                currentUser.setCaloriesTarget(newGoal);
                save(currentUser);
                break;
            case "protein" :
                currentUser.setProteinTarget(newGoal);
                save(currentUser);
                break;
            case "carbs" :
                currentUser.setCarbsTarget(newGoal);
                save(currentUser);
                break;
            case "sodium" :
                currentUser.setSodiumTarget(newGoal);
                save(currentUser);
                break;
            default:
                break;
        }
    }

    // get value for given setting, returns -1 if setting not in database
    public int getSetting(String setting) {
        switch (setting) {
            case "calories" :
                return currentUser.getCaloriesTarget();
            case "protein" :
                return currentUser.getProteinTarget();
            case "carbs" :
                return currentUser.getCarbsTarget();
            case "sodium" :
                return currentUser.getSodiumTarget();
            default:
                return -1;
        }
    }

    public List<Meal> getAllMealsList(Date date) {
        String dateID = DynamoDB.getDateID(date);
        return getAllMealsListByDateID(dateID);
    }

    public List<Meal> getAllMealsListByDateID(String dateID){
        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        List<Meal> res = new ArrayList<>();
        User.DateRecord dateRecord = null;
        for(User.DateRecord d : dateRecords) {
            if(d.getDateID().equals(dateID)) {
                dateRecord = d;
                break;
            }
        }

        if(dateRecord != null) {
            List<User.Record> records = dateRecord.getRecords();
            for(User.Record r : records) {
                Meal meal = new Meal(r.getMealName(), new java.sql.Date(r.getDate()), r.getTypeCode(), r.getRecordID());
                meal.setCalories(r.getCalories());
                meal.setCarbs(r.getCarbs());
                meal.setProtein(r.getProtein());
                meal.setSodium(r.getSodium());
                res.add(meal);
            }
        }
        return res;
    }

    public List<Meal> getFrequentMeals(){
        List<Meal> list = new ArrayList<>();
        Map<String, Integer> frequency = new HashMap<>();
        Map<String, Meal> map = new HashMap<>();

        List<User.DateRecord> dateRecords = currentUser.getDateRecords();
        for(User.DateRecord d : dateRecords) {
            String dateID = d.getDateID();
            List<Meal> meals = getAllMealsListByDateID(dateID);
            for(Meal m : meals) {
                String mealName = m.getName();
                if(!frequency.containsKey(mealName)) {
                    frequency.put(mealName, 0);
                }
                frequency.put(mealName, frequency.get(mealName) + 1);
                map.put(mealName, m);
            }
        }

        while(frequency.size() > 0 && list.size() < 5) {
            int max = 0;
            String mealName = null;
            for(String key : frequency.keySet()) {
                if(frequency.get(key) > max) {
                    max = frequency.get(key);
                    mealName = key;
                }
            }
            if(mealName == null) break;
            frequency.remove(mealName);
            list.add(map.get(mealName));
        }
        return list;
    }
}
