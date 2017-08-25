package edu.upenn.cis350.group1.calorietracker;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by jibreel on 4/3/16.
 */
public class WeightTrackingActivity extends CalorieTrackerActivity{

    private DynamoDB dbHandler;
    private GraphView graph;
    private static DecimalFormat df = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_weight_tracking);

        //create database handler
        //dbHandler = new DatabaseHandler(this.getApplicationContext());
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:fe3c44bd-0f02-4cb5-8e3d-2aeecca86628", // Identity Pool ID
                Regions.US_WEST_2 // Region
        );
        dbHandler = new DynamoDB(credentialsProvider);

        //find graph view
        graph = (GraphView) findViewById(R.id.graph);

        buildGraphAndList();
    }

    private void buildGraphAndList() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -30);
        ListView list = (ListView) findViewById(R.id.weight_table);

        // column names and view ids to update
        String[] arrayColumns = new String[] {"date", "weight", "_id"};
        int[] viewIDs = {R.id.textview_weight_date, R.id.textview_weight_value};

        MatrixCursor c = new MatrixCursor(arrayColumns);

        Map<Long, Double> entries = new TreeMap<>();


        //add entries for each day of last month that has weight data
        for (int i = 0; i <= 30; i++) {
            long millis = calendar.getTimeInMillis();
            Date date = new Date(millis);
            double weight = dbHandler.getWeight(date);
            if (weight > 0) {
                entries.put(millis, weight);
                String[] values = {date.toString(), df.format(weight) + " lbs",
                        dbHandler.getDateID(date)};   // Is this dateID a little different from previous Integer DateID ?
                c.addRow(values);
            } else {
                entries.put(millis, 0.0);
            }

            calendar.add(Calendar.DATE, 1);
        }

        // update the adapter
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.weight_item, c,
                arrayColumns, viewIDs);
        list.setAdapter(adapter);

        ArrayList<Long> entryDates = new ArrayList<>(entries.keySet());
        Collections.sort(entryDates);

        Long[] dates = entryDates.toArray(new Long[entryDates.size()]);
        DataPoint[] dataPoints = new DataPoint[dates.length];

        for (int i = 0; i < dataPoints.length; i++) {
            dataPoints[i] = new DataPoint(dates[i], entries.get(dates[i]));
        }

        //Set graph to display dates on X axis
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setColor(Color.MAGENTA);
        graph.addSeries(series);


        formatGraph(dates);
    }

    public void formatGraph(Long[] dates) {
        //modifications to grid
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(2);
        graph.getGridLabelRenderer().setGridColor(Color.BLACK);
        graph.getGridLabelRenderer().setPadding(25);

        //modifications to viewport
        graph.getViewport().setBackgroundColor(Color.LTGRAY);



        //manual X bounds
        graph.getViewport().setMinX(dates[0]);
        graph.getViewport().setMaxX(dates[dates.length - 1]);
        graph.getViewport().setXAxisBoundsManual(true);
    }
}
