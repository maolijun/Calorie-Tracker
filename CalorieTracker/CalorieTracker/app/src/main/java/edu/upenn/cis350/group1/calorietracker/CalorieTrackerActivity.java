package edu.upenn.cis350.group1.calorietracker;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.facebook.login.widget.LoginButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jibreel on 3/20/16.
 */
public abstract class CalorieTrackerActivity extends AppCompatActivity{

    // class variables necessary for photo saving
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 0;
    String mCurrentPhotoPath;

    // current user
    protected User currentUser;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tracker_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        boolean res = toActivity(id, getCurrentFocus());
        if (res) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // open the activity that is chosen in action bar
    public boolean toActivity(int id, View v) {
        Intent intent = null;
        switch (id) {
            case R.id.menu_today :
                intent = new Intent(this, DailyActivity.class);
                break;
            case R.id.menu_calendar :
                intent = new Intent(this, CalendarActivity.class);
                break;
            case R.id.menu_settings :
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.menu_weight :
                intent = new Intent(this, WeightTrackingActivity.class);
                break;
            case R.id.menu_progress :
                intent = new Intent(this, ProgressActivity.class);
                break;
            case R.id.menu_barcode :
                intent = new Intent(this, BarcodeActivity.class);
                break;
            case R.id.menu_friend :
                intent = new Intent(this, FriendActivity.class);
                break;
            case R.id.menu_ranking :
                intent = new Intent(this, RankingActivity.class);
                break;
            case R.id.menu_logout :
                // log out through facebook account
                if (AccessToken.getCurrentAccessToken() != null) {
                    LoginManager.getInstance().logOut();
                }
                intent = new Intent(this, LoginActivity.class);
                break;
            default:
                break;
        }



        if (intent != null) {

            currentUser = (User)getIntent().getSerializableExtra("currentUser");
            // put extra current user
            intent.putExtra("currentUser", currentUser);
            // close prior activity
            startActivity(intent);

            if (v != null) {
                if (!(v.getContext() instanceof DailyActivity)) {
                    finish();
                }
            } else {
                finish();
            }
            return true;
        }
        return false;

    }
}
