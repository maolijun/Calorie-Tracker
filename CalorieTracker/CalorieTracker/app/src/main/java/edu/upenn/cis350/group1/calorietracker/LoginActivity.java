package edu.upenn.cis350.group1.calorietracker;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail;
    private EditText loginPassword;
    private Button loginButton;
    private TextView signupLink;
    private TextView loginPrompt;
    private DynamoDB ddb;
    private User currentUser;
    private LoginButton fbLoginButton;
    private CallbackManager callbackManager;
    private AccessToken accessToken;

    private static final int FB_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize fb sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        accessToken = AccessToken.getCurrentAccessToken();
        callbackManager = CallbackManager.Factory.create();
        FacebookSdk.setClientToken("");

        setContentView(R.layout.activity_login);

        loginEmail = (EditText) findViewById(R.id.loginEmail);
        loginPassword = (EditText) findViewById(R.id.loginPassword);
        loginButton = (Button) findViewById(R.id.loginButton);
        signupLink = (TextView) findViewById(R.id.signupLink);
        loginPrompt = (TextView) findViewById(R.id.loginPrompt);
        fbLoginButton = (LoginButton) findViewById(R.id.fb_login_button);

        addListeners();

        setUpDatabase();

        setFBLogin();
    }

    private void setFBLogin() {

        AppEventsLogger.activateApp(this.getApplication());
        if (accessToken != null) {
            fbLoginButton.performClick();
        }

        fbLoginButton.setReadPermissions("emails");
        fbLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("email"));
            }
        });

        // Callback registration
        fbLoginButton.registerCallback(callbackManager , new FacebookCallback<LoginResult>(){
            @Override
            public void onSuccess(LoginResult loginResult) {
                String email = "huangli@facebook.com";
                currentUser = ddb.load(email);
                Intent dailyIntent = new Intent(LoginActivity.this, DailyActivity.class);
                dailyIntent.putExtra("currentUser", currentUser);
                startActivity(dailyIntent);
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), "cancel", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                //Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                exception.printStackTrace();
                return;
            }
        });


    }

    public void gotoDailyActivity() {
        Intent intent = new Intent(LoginActivity.this, CalendarActivity.class);
        startActivity(intent);
    }


    private void addListeners() {
        signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                Intent signupIntent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(signupIntent);
            }

        });

        loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    finish();
                    Intent dailyIntent = new Intent(LoginActivity.this, DailyActivity.class);
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
        String email = loginEmail.getText().toString();
        String password = loginPassword.getText().toString();

        if (email == null || email.isEmpty()) {
            loginPrompt.setText("Email cannot be empty");
            return false;
        }

        if (password == null || password.isEmpty()) {
            loginPrompt.setText("Password cannot be empty");
            return false;
        }

        User user = ddb.load(email);
        if (user == null) {
            // no this user exists
            loginPrompt.setText("No account exists!");
            return false;
        } else {
            // this user already exists, check if password matches
            if (!password.equals(user.getPassword())) {
                loginPrompt.setText("Password does not match");
                return false;
            } else {
                currentUser = user;
                return true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
