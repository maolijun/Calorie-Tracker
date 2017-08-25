package edu.upenn.cis350.group1.calorietracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Regions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    private EditText signupEmail;
    private EditText signupPassword;
    private Button signupButton;
    private TextView signupPrompt;
    private DynamoDB ddb;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupEmail = (EditText) findViewById(R.id.signupEmail);
        signupPassword = (EditText) findViewById(R.id.signupPassword);
        signupButton = (Button) findViewById(R.id.signupButton);
        signupPrompt = (TextView) findViewById(R.id.signupPrompt);

        addListeners();

        setUpDatabase();
    }

    private void addListeners() {
        signupButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            if (validateInput()) {
                finish();
                Intent dailyIntent = new Intent(SignupActivity.this, DailyActivity.class);
                dailyIntent.putExtra("currentUser", currentUser);
                startActivity(dailyIntent);
            }
            }

        });
    }

    private void setUpDatabase() {
        // DynamoDB handler
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
            getApplicationContext(),
            "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
            Regions.US_WEST_2 // Region
        );

        ddb = new DynamoDB(credentialsProvider);
    }

    private boolean validateInput() {
        String email = signupEmail.getText().toString();
        String password = signupPassword.getText().toString();

        if (email == null || email.isEmpty()) {
            signupPrompt.setText("Email cannot be empty");
            return false;
        }

        Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
        Matcher m = p.matcher(email);
        if (!m.matches()) {
            signupPrompt.setText("Email format wrong");
            return false;
        }

        if (password == null || password.length() < 6) {
            signupPrompt.setText("Password too short");
            return false;
        }

        User user = ddb.load(email);
        if (user == null) {
            // no this user exists
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(password);
            ddb.save(newUser);
            currentUser = newUser;
            return true;
        } else {
            // this user already exists
            signupPrompt.setText("User already exists!");
            return false;
        }
    }

}
