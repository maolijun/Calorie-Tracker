package edu.upenn.cis350.group1.calorietracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;

import java.util.List;

public class FriendActivity extends CalorieTrackerActivity {

    private EditText[] friend;
    private Button[] friendButton;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        // get current user
        Bundle b = getIntent().getExtras();
        if (b != null) {
            currentUser = (User) b.getSerializable("currentUser");
        }

        Log.d("[Friend] Current User", currentUser.getEmail());

        friend = new EditText[4];
        friendButton = new Button[4];

        friend[0] = (EditText) findViewById(R.id.friend1);
        friend[1] = (EditText) findViewById(R.id.friend2);
        friend[2] = (EditText) findViewById(R.id.friend3);
        friend[3] = (EditText) findViewById(R.id.friend4);

        friendButton[0] = (Button) findViewById(R.id.friendButton1);
        friendButton[1] = (Button) findViewById(R.id.friendButton2);
        friendButton[2] = (Button) findViewById(R.id.friendButton3);
        friendButton[3] = (Button) findViewById(R.id.friendButton4);

        refreshButton = (Button) findViewById(R.id.refreshButton);

        setUpText();

        addListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        DynamoDB.save(currentUser);
    }

    private void setUpText() {
        PaginatedScanList<User> users = DynamoDB.getAllUsers();
        List<String> friends = currentUser.getFriends();

        int i = 0;
        for (User user : users) {
            if (!friends.contains(user.getEmail()) && !user.getEmail().equals(currentUser.getEmail())) {
                friend[i++].setText(user.getEmail());
                if (i >= 2) break;
            }
        }

        i = 2;
        for (String friendEmail : friends) {
            friend[i++].setText(friendEmail);
            if (i >= 4) break;
        }
    }

    private void addListeners() {
        friendButton[0].setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                List<String> friends = currentUser.getFriends();
                String friendEmail = friend[0].getText().toString();

                if (!friends.contains(friendEmail)) {
                    currentUser.addFriend(friendEmail);
                }

                friendButton[0].setText("Added");
                friendButton[0].setBackgroundColor(Color.rgb(0, 178, 238));
            }

        });

        friendButton[1].setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                List<String> friends = currentUser.getFriends();
                String friendEmail = friend[1].getText().toString();

                if (!friends.contains(friendEmail)) {
                    currentUser.addFriend(friendEmail);
                }

                friendButton[1].setText("Added");
                friendButton[1].setBackgroundColor(Color.rgb(0, 178, 238));
            }

        });

        friendButton[2].setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                List<String> friends = currentUser.getFriends();
                String friendEmail = friend[2].getText().toString();

                friends.remove(friendEmail);

                friendButton[2].setText("Removed");
                friendButton[2].setBackgroundColor(Color.rgb(255, 102, 102));
            }

        });

        friendButton[3].setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                List<String> friends = currentUser.getFriends();
                String friendEmail = friend[3].getText().toString();

                friends.remove(friendEmail);

                friendButton[3].setText("Removed");
                friendButton[3].setBackgroundColor(Color.rgb(255, 102, 102));
            }

        });

        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendActivity.this, FriendActivity.class);
                intent.putExtra("currentUser", currentUser);
                finish();
                startActivity(intent);
            }

        });
    }
}
