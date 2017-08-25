package edu.upenn.cis350.group1.calorietracker;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBDocument;

import java.sql.Date;

/**
 * Created by Jussi Lundstedt on 2/20/2016.
 *
 * custom class to represent one meal
 */
public class Meal {
    private static final String[] types = {"Breakfast", "Lunch", "Dinner", "Snack"};

    private int type;
    private String name;
    private double calories;
    private double protein;
    private double carbs;
    private double sodium;
    private Date dateEaten;
    private int mealID;

    // construct a meal object with only name & type - passing in 0 thru 3 in accordance with
    // types[] - 0: breakfast 1: lunch 2: dinner 3: snacks
    public Meal(String name, Date dateEaten, int typeCode, int mealID) {
        checkConstructorValidity(name,typeCode);
        this.name = name;
        this.dateEaten = dateEaten;
        this.type = typeCode;
        this.mealID = mealID;
    }


    private void checkConstructorValidity(String name, int typeCode) {
        if (typeCode < 0 || typeCode > 3) {
            throw new IllegalArgumentException("invalid type code, must be between 0 and 3");
        }
        if (name == null) {
            throw new IllegalArgumentException("invalid name, name can't be null");
        }
    }


    // getters and setters
    public int getTypeCode() {
        return type;
    }

    public String getType() {
        return types[type];
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getSodium() {
        return sodium;
    }

    public void setSodium(double sodium) {
        this.sodium = sodium;
    }

    public Date getDateEaten() {
        return dateEaten;
    }

    public int getMealID() {
        return mealID;
    }

    public void setMealID(int mealID) {
        this.mealID = mealID;
    }

    // show meal names in frequent meal dropdown list
    @Override
    public String toString() {
        return getName();
    }
}
