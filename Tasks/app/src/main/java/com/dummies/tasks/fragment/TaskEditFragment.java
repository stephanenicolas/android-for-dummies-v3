package com.dummies.tasks.fragment;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.AsyncTaskLoader;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dummies.tasks.R;
import com.dummies.tasks.db.TasksDatabaseHelper;
import com.dummies.tasks.interfaces.OnEditFinished;
import com.dummies.tasks.model.Task;
import com.dummies.tasks.util.ReminderManager;
import com.j256.ormlite.android.apptools.OrmLiteCursorLoader;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TaskEditFragment extends Fragment implements
        OnDateSetListener, OnTimeSetListener,
        LoaderManager.LoaderCallbacks<Task> {

    public static final String DEFAULT_EDIT_FRAGMENT_TAG =
            "editFragmentTag";
    public static final String TASK_ID = "taskId";

    //
    // Dialog Constants
    //
    static final String YEAR = "year";
    static final String MONTH = "month";
    static final String DAY = "day";
    static final String HOUR = "hour";
    static final String MINS = "mins";
    static final String CALENDAR = "calendar";

    //
    // Date Format
    //
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "kk:mm";

    Dao<Task,Long> taskDao;
    EditText titleText;
    EditText bodyText;
    Button dateButton;
    Button timeButton;
    long taskId;
    Calendar calendar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get a Data Access Object to help us manage our Tasks
        taskDao = TasksDatabaseHelper.getSingleton(getActivity())
                .getDao(Task.class);

        // If we're restoring state from a previous activity, restore the
        // previous date as well
        if (savedInstanceState != null) {
            calendar = (Calendar) savedInstanceState.getSerializable
                    (CALENDAR);
        }

        // If we didn't have a previous date, use "now"
        if( calendar==null ) {
            calendar = Calendar.getInstance();
        }

        // Set the task id from the intent arguments, if available.
        Bundle arguments = getArguments();
        if (arguments != null) {
            taskId = arguments.getLong(TASK_ID,0L);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout and set the container. The layout is the
        // view that we will return.
        View v = inflater.inflate(R.layout.task_edit_fragment,
                container, false);

        // From the layout, get a few views that we're going to work with
        titleText = (EditText) v.findViewById(R.id.title);
        bodyText = (EditText) v.findViewById(R.id.body);
        dateButton = (Button) v.findViewById(R.id.task_date);
        timeButton = (Button) v.findViewById(R.id.task_time);

        // Tell the date and time buttons what to do when we click on
        // them.
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        // Tell the confirmation button what to do when we click on it.
        Button confirmButton = (Button) v.findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    // taskId==0 when we create a new task,
                    // otherwise it's the id of the task being edited.
                    if (taskId == 0) {

                        // Create the new task and set taskId to the id of
                        // the new task.
                        Task task = new Task();
                        task.setTitle(titleText.getText().toString());
                        task.setBody(bodyText.getText().toString());
                        task.setDateTime(calendar.getTime());

                        taskDao.create(task);

                        // Update the id field based on what Dao.create
                        // tells us the new ID is.
                        taskId = task.getId();

                    } else {

                        Task task = taskDao.queryForId(taskId);
                        task.setTitle(titleText.getText().toString());
                        task.setBody(bodyText.getText().toString());
                        task.setDateTime(calendar.getTime());

                        taskDao.update(task);
                    }

                    // Notify the user of the change using a Toast
                    Toast.makeText(getActivity(),
                            getString(R.string.task_save_success),
                            Toast.LENGTH_SHORT).show();

                    // Create a reminder for this task
                    new ReminderManager(getActivity()).setReminder(taskId,
                            calendar);

                    // Tell our enclosing activity that we are done so that
                    // it can cleanup whatever it needs to clean up.
                    ((OnEditFinished) getActivity()).finishEditingTask();

                } catch( SQLException e ) {
                    Toast.makeText(getActivity(),
                            getString(R.string.task_save_error),
                            Toast.LENGTH_LONG).show();
                }

            }

        });

        if (taskId == 0) {
            // This is a new task - add defaults from preferences if set.
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            String defaultTitleKey = getString(R.string
                    .pref_task_title_key);
            String defaultTimeKey = getString(R.string
                    .pref_default_time_from_now_key);

            String defaultTitle = prefs.getString(defaultTitleKey, null);
            String defaultTime = prefs.getString(defaultTimeKey, null);

            if (defaultTitle != null)
                titleText.setText(defaultTitle);

            if (defaultTime != null && defaultTime.length() > 0)
                calendar.add(Calendar.MINUTE,
                        Integer.parseInt(defaultTime));

            updateButtons();

        } else {

            // Fire off a background loader to retrieve the data from the
            // database
            getLoaderManager().initLoader(0, null, this);

        }

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the calendar instance in case the user changed it
        outState.putSerializable(CALENDAR, calendar);
    }

    private void showDatePicker() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = new DatePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(YEAR, calendar.get(Calendar.YEAR));
        args.putInt(MONTH, calendar.get(Calendar.MONTH));
        args.putInt(DAY, calendar.get(Calendar.DAY_OF_MONTH));
        newFragment.setArguments(args);
        newFragment.show(ft, "datePicker");
    }

    private void showTimePicker() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = new TimePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(HOUR, calendar.get(Calendar.HOUR_OF_DAY));
        args.putInt(MINS, calendar.get(Calendar.MINUTE));
        newFragment.setArguments(args);
        newFragment.show(ft, "timePicker");
    }

    private void updateButtons() {
        // Set the time button text
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        String timeForButton = timeFormat.format(calendar.getTime());
        timeButton.setText(timeForButton);

        // Set the date button text
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String dateForButton = dateFormat.format(calendar.getTime());
        dateButton.setText(dateForButton);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear,
                          int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateButtons();
    }

    @Override
    public void onTimeSet(TimePicker view, int hour, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        updateButtons();
    }

    @Override
    public Loader<Task> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Task>(getActivity()) {
            @Override
            public Task loadInBackground() {
                try {
                    return taskDao.queryForId(taskId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Task> loader, Task task) {
        // Close this fragmentClass down if the item we're editing was
        // deleted
        if (task==null) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    ((OnEditFinished) getActivity()).finishEditingTask();
                }
            });
            return;
        }

        titleText.setText(task.getTitle());
        bodyText.setText(task.getBody());
        calendar.setTime(task.getDateTime());

        updateButtons();
    }

    @Override
    public void onLoaderReset(Loader<Task> ignored) {
        // nothing to reset for this fragmentClass
    }
}
