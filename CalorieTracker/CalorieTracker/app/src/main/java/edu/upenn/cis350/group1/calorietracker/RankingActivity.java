package edu.upenn.cis350.group1.calorietracker;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.RatingBar;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankingActivity extends CalorieTrackerActivity {

    private TextView[] user;
    private RatingBar[] ratingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        // get current user
        Bundle b = getIntent().getExtras();
        if (b != null) {
            currentUser = (User) b.getSerializable("currentUser");
        }

        user = new TextView[3];
        ratingBar = new RatingBar[3];

        user[0] = (TextView) findViewById(R.id.user1);
        user[1] = (TextView) findViewById(R.id.user2);
        user[2] = (TextView) findViewById(R.id.user3);

        user[0].setTypeface(null, Typeface.BOLD);
        user[1].setTypeface(null, Typeface.BOLD);
        user[2].setTypeface(null, Typeface.BOLD);

        user[0].setTextColor(Color.rgb(255, 215, 0));
        user[1].setTextColor(Color.rgb(205, 201, 201));
        user[2].setTextColor(Color.rgb(184, 134, 11));

        ratingBar[0] = (RatingBar) findViewById(R.id.ratingBar1);
        ratingBar[1] = (RatingBar) findViewById(R.id.ratingBar2);
        ratingBar[2] = (RatingBar) findViewById(R.id.ratingBar3);

        findTopThreeStars();
    }

    private void findTopThreeStars() {
        PaginatedScanList<User> psl = DynamoDB.getAllUsers();
        List<User> users = new ArrayList<User>();

        for (User u : psl) {
            if (currentUser.getFriends().contains(u.getEmail())) {
                users.add(u);
            }
        }

        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                int total1 = 0, total2 = 0;

                for (User.DateRecord dr : user1.getDateRecords()) {
                    for (User.Record r : dr.getRecords()) {
                        total1 += Math.abs(r.getCalories() - user1.getCaloriesTarget());
                        total1 += Math.abs(r.getProtein() - user1.getProteinTarget());
                        total1 += Math.abs(r.getCarbs() - user1.getCarbsTarget());
                        total1 += Math.abs(r.getSodium() - user1.getSodiumTarget());
                    }
                }

                for (User.DateRecord dr : user2.getDateRecords()) {
                    for (User.Record r : dr.getRecords()) {
                        total2 += Math.abs(r.getCalories() - user2.getCaloriesTarget());
                        total2 += Math.abs(r.getProtein() - user2.getProteinTarget());
                        total2 += Math.abs(r.getCarbs() - user2.getCarbsTarget());
                        total2 += Math.abs(r.getSodium() - user2.getSodiumTarget());
                    }
                }

                return total1 - total2;
            }
        });

        if (users.size() > 0) {
            user[0].setText(users.get(0).getEmail());
        }

        if (users.size() > 1) {
            user[1].setText(users.get(1).getEmail());
        }

        if (users.size() > 2) {
            user[2].setText(users.get(2).getEmail());
        }
    }
}
