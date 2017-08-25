package edu.upenn.cis350.group1.calorietracker;

/**
 * Created by yishang on 10/30/2016.
 */

import android.util.Log;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;
import java.sql.Date;
import java.util.List;
import java.io.Serializable;

@DynamoDBTable(tableName = "User")
public class User implements Serializable {

    private String email;
    private String password;
    private int caloriesTarget = 2000;
    private int proteinTarget = 100;
    private int carbsTarget = 275;
    private int sodiumTarget = 2000;
    private List<DateRecord> dateRecords = new ArrayList<>();
    private List<String> friends = new ArrayList<>();

    @DynamoDBHashKey(attributeName = "email")
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDBAttribute(attributeName = "password")
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) { this.password = password;}

    @DynamoDBAttribute(attributeName = "caloriesTarget")
    public int getCaloriesTarget() {return caloriesTarget;}
    public void setCaloriesTarget(int caloriesTarget) { this.caloriesTarget = caloriesTarget;}

    @DynamoDBAttribute(attributeName = "proteinTarget")
    public int getProteinTarget() {
        return proteinTarget;
    }
    public void setProteinTarget(int proteinTarget) { this.proteinTarget = proteinTarget;}

    @DynamoDBAttribute(attributeName = "carbsTarget")
    public int getCarbsTarget() {
        return carbsTarget;
    }
    public void setCarbsTarget(int carbsTarget) { this.carbsTarget = carbsTarget;}

    @DynamoDBAttribute(attributeName = "sodiumTarget")
    public int getSodiumTarget() {
        return sodiumTarget;
    }
    public void setSodiumTarget(int sodiumTarget) { this.sodiumTarget = sodiumTarget;}

    @DynamoDBAttribute(attributeName="DateRecord")
    public List<DateRecord> getDateRecords() {return dateRecords;}
    public void setDateRecords(List<DateRecord> records) {this.dateRecords = records;}

    @DynamoDBAttribute(attributeName="Friend")
    public List<String> getFriends() {return friends;}
    public void setFriends(List<String> friends) {this.friends = friends;}

    public DateRecord getDateRecordByDate(String dateID) {
        List<DateRecord> dateRecords = getDateRecords();
        int i = 0;
        for(i = 0; i < dateRecords.size(); i++){
            DateRecord d = dateRecords.get(i);
            if(d.getDateID().equals(dateID)) return d;
            if(Integer.parseInt(d.getDateID()) > Integer.parseInt(dateID)) {
                break;
            }
        }
        DateRecord d = new DateRecord(dateID);
        dateRecords.add(i, d);
        return d;
    }

    public void addRecord(Meal meal) {
        Record record = new Record(meal.getName(), meal.getDateEaten().getTime(), meal.getTypeCode(), meal.getMealID());
        DateRecord d = getDateRecordByDate(getDateID(meal));
        d.addRecord(record);
    }

    public void addFriend(String user) {
        this.friends.add(user);
    }

    public String getDateID(Meal meal) {
        Date mealDate = meal.getDateEaten();
        return DynamoDB.getDateID(mealDate);
    }

    @DynamoDBDocument
    public static class DateRecord implements Serializable{
        private String dateID;
        private Double weight;
        private Double water;
        private List<Record> records = new ArrayList<>();

        public DateRecord() {}

        public DateRecord(String dateID){
            this.dateID = dateID;
            this.weight = 0.0;
            this.water = 0.0;
        }

        @DynamoDBAttribute(attributeName = "DateID")
        public String getDateID() { return dateID; }
        public void setDateID(String dateID) { this.dateID = dateID; }

        @DynamoDBAttribute(attributeName = "Weight")
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }

        @DynamoDBAttribute(attributeName = "Water")
        public Double getWater() { return water; }
        public void setWater(Double water) { this.water = water; }

        @DynamoDBAttribute(attributeName="Records")
        public List<Record> getRecords() {return records;}
        public void setRecords(List<Record> records) {this.records = records;}

        public void addRecord(Record record){
            String recordID = "0";
            if(records.size() == 0) {
                recordID = dateID + "00";
            } else {
                recordID = (records.get(records.size() - 1).getRecordID() + 1) + "";
            }

            record.setRecordID(Integer.parseInt(recordID));
            this.records.add(record);
        }
    }

    @DynamoDBDocument
    public static class Record implements Serializable{
        private String mealName;
        private Long date;
        private Integer typeCode;
        private Integer recordID;
        private Double calories;
        private Double protein;
        private Double carbs;
        private Double sodium;

        public Record() {}

        public Record(String mealName, Long date, Integer typeCode, Integer recordID) {
            this.mealName = mealName;
            this.date = date;
            this.typeCode = typeCode;
            this.recordID = recordID;
            this.calories = 0.0;
            this.protein = 0.0;
            this.carbs = 0.0;
            this.sodium = 0.0;
        }

        @DynamoDBAttribute(attributeName = "MealName")
        public String getMealName() { return mealName; }
        public void setMealName(String mealName) { this.mealName = mealName; }

        @DynamoDBAttribute(attributeName = "Date")
        public Long getDate() { return date; }
        public void setDate(Long date) { this.date = date; }

        @DynamoDBAttribute(attributeName = "TypeCode")
        public Integer getTypeCode() { return typeCode; }
        public void setTypeCode(Integer typeCode) { this.typeCode = typeCode; }

        @DynamoDBAttribute(attributeName = "RecordID")
        public Integer getRecordID() { return recordID; }
        public void setRecordID(Integer recordID) { this.recordID = recordID; }

        @DynamoDBAttribute(attributeName = "Calories")
        public Double getCalories() { return calories; }
        public void setCalories(Double calories) { this.calories = calories; }

        @DynamoDBAttribute(attributeName = "Protein")
        public Double getProtein() { return protein; }
        public void setProtein(Double protein) { this.protein = protein; }

        @DynamoDBAttribute(attributeName = "Carbs")
        public Double getCarbs() { return carbs; }
        public void setCarbs(Double carbs) { this.carbs = carbs; }

        @DynamoDBAttribute(attributeName = "Sodium")
        public Double getSodium() { return sodium; }
        public void setSodium(Double sodium) { this.sodium = sodium; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("mealName: " + mealName + "\n");
            sb.append("Date: " + date + "\n");
            sb.append("TypeCode: " + typeCode + "\n");
            sb.append("RecordID: " + recordID + "\n");
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return email;
    }
}
