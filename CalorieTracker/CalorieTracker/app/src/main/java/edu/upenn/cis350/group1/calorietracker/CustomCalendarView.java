package edu.upenn.cis350.group1.calorietracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by jibreel on 2/21/16.
 *
 * Custom calendar view to support showing if there's data for a particular date
 */
public class CustomCalendarView extends LinearLayout {


    //Context
    private Context context;

    // row of day labels
    private LinearLayout weekDays;

    // back button (last month)
    private ImageView prevButton;

    // forward button (next month)
    private ImageView nextButton;

    // Month and Year display
    private TextView calendarMonth;

    // Grid display of dates in selected month
    private GridView calendarGrid;

    // Calendar object to keep track of currently displayed month
    private Calendar dateHolder;

    // Number of days to include in grid
    static final int NUM_DAYS =  42;

    // Format for dates
    static final String DATE_FORMAT = "MMMM yyyy";

    //Date Change Listener
    private OnDateChangeListener listener;

    //Database Handler
    //private DatabaseHandler dbHandler;
    private DynamoDB dbHandler;



    interface OnDateChangeListener {

        void onSelectedDayChange(CustomCalendarView view, int year, int month,
                                                  int dayOfMonth);
    }

    private class GridAdapter extends BaseAdapter {

        ArrayList<Calendar> dates;
        Context context;
        DatabaseHandler dbHandler;
        LayoutInflater inflater;

        //Constructor for gridAdapter
        public GridAdapter(Context context, ArrayList<Calendar> dates, DatabaseHandler dbHandler){
            this.dates = dates;
            this.context = context;
            this.dbHandler = dbHandler;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return dates.size();
        }

        @Override
        public Object getItem(int position) {
            return dates.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         *  Determines the correct color and font to display the date, based on the caloric
         *  intake for the day.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Calendar date = dates.get(position);
            TextView view = (convertView != null) ? (TextView) convertView :
                    (TextView) inflater.inflate(R.layout.date_view, parent, false);
            view.setText(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));

            view.setTag(R.id.date_key, date.clone());

            //booleans to store information about the caloric and temporal information of the date
            boolean aboveLimit = false;
            boolean belowLimit = false;
            boolean isEmpty = false;
            boolean inMonth = false;
            boolean isToday = false;

            //find caloric limit specified by user (defaults to 2000);
            int caloricLimit = dbHandler.getSetting("calories");
            caloricLimit = (caloricLimit > 0) ? caloricLimit : SettingsActivity.caloricDefault;

            //Does the database contain information for this date?
            Date sqlDate = new java.sql.Date(date.getTimeInMillis());
            if (dbHandler != null && dbHandler.getDateID(sqlDate) != -1) {
                int totalCals = 0;
                List<Meal> meals = dbHandler.getAllMealsList(sqlDate);
                for (Meal meal : meals) {
                    totalCals += meal.getCalories();
                }
                if (totalCals == 0) isEmpty = true;
                else if (totalCals > caloricLimit) aboveLimit = true;
                else belowLimit = true;
            }

            //is this date in the current month?
            if(date.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {

                inMonth = true;

                //is this date today?
                if (date.get(Calendar.DAY_OF_MONTH) ==
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                    isToday = true;
                }
            }

            //is this date selected?
            if(date.get(Calendar.YEAR) == dateHolder.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == dateHolder.get(Calendar.MONTH) &&
                    date.get(Calendar.DAY_OF_MONTH) == dateHolder.get(Calendar.DAY_OF_MONTH)) {
                view.setTypeface(null, Typeface.BOLD);
            }


            //change color based on database information
            if (isToday) view.setTextColor(getResources().getColor(R.color.colorAccent));
            else if (aboveLimit) view.setTextColor(getResources().getColor(R.color.aboveLimit));
            else if (belowLimit && !isEmpty) view.setTextColor(getResources().getColor(R.color
                    .belowLimit));
            else if (inMonth) view.setTextColor(getResources().getColor(R.color.colorText));

            return view;
        }
    }

    /**
     * Single argument constructor takes in only Context
     * @param context
     */
    public CustomCalendarView(Context context) {
        this(context, null);
    }

    /**
     * Two argument constructor takes in Context and AttributeSet
     * @param context
     * @param attrs
     */
    public CustomCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        dateHolder = Calendar.getInstance();
        inflateCalendar(context);
        setListeners();
        updateCalendar(false);
    }


    /**
     * Private method to inflate calendar components and initialize instance variables
     * containing layout components.
     * @param context
     */
    private void inflateCalendar(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);

        inflater.inflate(R.layout.custom_calendar_view, this);

        calendarMonth = (TextView) findViewById(R.id.curr_month);
        weekDays =(LinearLayout) findViewById(R.id.days_of_the_week);
        prevButton = (ImageView) findViewById(R.id.last_month);
        nextButton = (ImageView) findViewById(R.id.next_month);
        calendarGrid = (GridView) findViewById(R.id.days_grid);

    }

    private void setListeners() {
        prevButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dateHolder.add(Calendar.MONTH, -1);
                updateCalendar(true);
            }
        });

        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dateHolder.add(Calendar.MONTH, 1);
                updateCalendar(true);
            }
        });
    }

    /**
     * Called when calendar is first displayed or month is changed, calculates
     * which days to display for current month
     */
    private void updateCalendar(boolean changeMonth) {

        //List for each date in the view
        ArrayList<Calendar> dates = new ArrayList<>();

        Calendar calendar = (Calendar) dateHolder.clone();
        if (!changeMonth) {
            if (listener != null) {
                listener.onSelectedDayChange(this, calendar.get(calendar.YEAR), calendar.get(calendar.MONTH),
                        calendar.get(calendar.DAY_OF_MONTH));
            }
        }

        //Find first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1);


        //What day of the week is the first of the month
        int firstVisibleDate = calendar.get(Calendar.DAY_OF_WEEK) - 1;


        //Display current month at top of view
        calendarMonth.setText(new SimpleDateFormat(DATE_FORMAT).format(calendar.getTime()));

        //Add the days of the last month that will bring us to a full week
        calendar.add(Calendar.DAY_OF_YEAR, -firstVisibleDate);


        //Add the rest of the days to the list, totalling 6 weeks
        while(dates.size() < NUM_DAYS) {
            dates.add((Calendar) calendar.clone());
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        calendarGrid.setAdapter(new GridAdapter(getContext(), dates,
                new DatabaseHandler(context.getApplicationContext())));


        calendarGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = (TextView) view;
                //textView.setTypeface(null, Typeface.BOLD);
                Calendar selectedDate = (Calendar) textView.getTag(R.id.date_key);
                dateHolder = selectedDate;
                updateCalendar(false);
            }
        });
    }

    /**
     * Returns the current date (in milliseconds) as a long
     * @return
     */
    public long getDate() {
        return dateHolder.getTimeInMillis();
    }

    public void setOnDateChangeListener(OnDateChangeListener listener) {
        this.listener = listener;
    }

    public void setDataBaseHandler(DynamoDB dbHandler) {
        this.dbHandler = dbHandler;
    }




}
