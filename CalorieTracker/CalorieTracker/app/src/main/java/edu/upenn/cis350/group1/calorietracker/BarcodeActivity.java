package edu.upenn.cis350.group1.calorietracker;

/**
 * Created by maolijun on 10/27/2016.
 */

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.sql.Date;
import java.util.Calendar;

public class BarcodeActivity extends CalorieTrackerActivity{

    private Button button;
    private Button clearButton;
    private Button addButton;
    private TextView productTitle;
    private TextView calorieCount;
    private TextView sodiumCount;
    private TextView carbCount;
    private TextView proteinCount;
    private TextView weightCount;
    private TextView waterCount;

    private final String uriRoot = "https://api.nutritionix.com/v1_1/item?";
    private final String applicationId = "8f9bae48";
    private final String applicationKey = "072b4b6d306f60b05ab63dd104659d4b";
    private String upc = "";

    private DynamoDB db;
    private Date date;
    private Calendar dateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);
        button = (Button) this.findViewById(R.id.scan);
        clearButton = (Button) this.findViewById(R.id.clearButton);
        addButton = (Button) this.findViewById(R.id.addButton);
        productTitle = (TextView) this.findViewById(R.id.productName);
        calorieCount = (TextView) this.findViewById(R.id.productCalorie);
        sodiumCount = (TextView) this.findViewById(R.id.productSodium);
        carbCount = (TextView) this.findViewById(R.id.productCarbs);
        proteinCount = (TextView) this.findViewById(R.id.productProtein);
        weightCount = (TextView) this.findViewById(R.id.productWeight);
        waterCount = (TextView) this.findViewById(R.id.productWater);

        final Activity activity = this;
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan Barcode");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearAllField();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                        Regions.US_WEST_2 // Region
                );
                db = new DynamoDB(credentialsProvider);

                dateHolder = Calendar.getInstance();
                Date date = new Date(dateHolder.getTimeInMillis());

                String name = productTitle.getText().toString();
                double roundedCalories = Double.parseDouble(calorieCount.getText().toString());
                double roundedCarbs = Double.parseDouble(carbCount.getText().toString());
                double roundedProtein = Double.parseDouble(proteinCount.getText().toString());
                double roundedSodium = Double.parseDouble(sodiumCount.getText().toString());

                if(name.equals("none")){
                    Toast.makeText(getApplicationContext(), "No scanned product to be added", Toast.LENGTH_LONG).show();
                } else {
                    Spinner mealTypeSpinner = (Spinner) findViewById(R.id.scanner_mealtype);
                    int typeCode = mealTypeSpinner.getSelectedItemPosition();
                    Meal thisMeal = new Meal(name, date, typeCode, 0);
                    thisMeal.setCalories(roundedCalories);
                    thisMeal.setCarbs(roundedCarbs);
                    thisMeal.setProtein(roundedProtein);
                    thisMeal.setSodium(roundedSodium);
                    db.addMeal(thisMeal);
                    Toast.makeText(getApplicationContext(), "Product has been added to database", Toast.LENGTH_LONG).show();
                    clearAllField();
                }
            }
        });
    }

    public void clearAllField(){
        productTitle.setText("none");
        calorieCount.setText("0");
        sodiumCount.setText("0");
        carbCount.setText("0");
        proteinCount.setText("0");
        weightCount.setText("0");
        waterCount.setText("0");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null){
            if(result.getContents() == null){
                //test mode when camera unavailable
//                upc = "016000614604";
//                upc = "49000036756";
//                upc = "049000055412";
//                String resourceURL = uriRoot + "upc=" + upc + "&appId=" + applicationId + "&appKey=" + applicationKey;
//                Log.e("BarcodeActivity",resourceURL);
//                new JSONTask().execute(resourceURL);

                Log.d("BarcodeActivity","Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                upc = result.getContents().toString();
                String resourceURL = uriRoot + "upc=" + upc + "&appId=" + applicationId + "&appKey=" + applicationKey;
                new JSONTask().execute(resourceURL);

//                Log.d("BarcodeActivity","Scaned");
//                Toast.makeText(this, "Scaned " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    public class JSONTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... params){
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try{
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url .openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";
                while((line = reader.readLine())!=null){
                    buffer.append(line);
                }

                String jsonString = buffer.toString();
                JSONObject product = new JSONObject(jsonString);
                String productTitle = product.getString("item_name");
                if(productTitle == null || productTitle.equals("null")){
                    productTitle = "none";
                }

                String calorie = product.getString("nf_calories");
                if(calorie == null || calorie.equals("null")){
                    calorie = "0.0";
                }
                if(calorie.matches(".*[a-z].*")){
                    calorie = "0.0";
                }

                String sodium = product.getString("nf_sodium");
                if(sodium == null || sodium.equals("null")){
                    sodium = "0.0";
                }
                String carb = product.getString("nf_total_carbohydrate");
                if(carb == null || carb.equals("null")){
                    carb = "0.0";
                }
                String protein = product.getString("nf_protein");
                if(protein == null || protein.equals("null")){
                    protein = "0.0";
                }
                String weight = product.getString("nf_serving_weight_grams");
                if(weight == null || weight.equals("null")){
                    weight = "0.0";
                }
                String water = product.getString("nf_water_grams");
                if(water == null || water.equals("null")){
                    water = "0.0";
                }

                return productTitle + "-" + calorie + "-" + sodium + "-" + carb + "-" + protein + "-" + weight + "-" + water;

            } catch(MalformedURLException e) {
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            } catch(JSONException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
                try{
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            Log.e("BarcodeActivity", result);

            String[] data = result.split("-");
            try
            {
                double d = Double.parseDouble(data[1]);
            }
            catch(NumberFormatException nfe)
            {
                data[1] = "0.0";
            }

            productTitle.setText(data[0]);
            calorieCount.setText(data[1]);
            sodiumCount.setText(data[2]);
            carbCount.setText(data[3]);
            proteinCount.setText(data[4]);
            weightCount.setText(data[5]);
            waterCount.setText(data[6]);
        }

    }

}
