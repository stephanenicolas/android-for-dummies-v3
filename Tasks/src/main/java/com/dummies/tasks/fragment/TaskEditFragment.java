package com.dummies.tasks.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.PaletteItem;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dummies.tasks.R;
import com.dummies.tasks.db.TasksDatabaseHelper;
import com.dummies.tasks.interfaces.OnEditFinished;
import com.dummies.tasks.model.Task;
import com.dummies.tasks.util.ReminderManager;
import com.j256.ormlite.android.apptools.OrmLitePreparedQueryLoader;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class TaskEditFragment extends Fragment implements
        OnDateSetListener, OnTimeSetListener,
        LoaderManager.LoaderCallbacks<List<Task>> {

    public static final String DEFAULT_EDIT_FRAGMENT_TAG =
            "editFragmentTag";
    public static final String TASK_ID = "taskId";
    public static final String TASK_TITLE = "taskTitle";

    static final String YEAR = "year";
    static final String MONTH = "month";
    static final String DAY = "day";
    static final String HOUR = "hour";
    static final String MINS = "mins";
    static final String CALENDAR = "calendar";

    View rootView;
    EditText titleText;
    EditText notesText;
    ImageView imageView;
    Button dateButton;
    Button timeButton;
    ActionBar actionBar;

    Dao<Task,Long> taskDao;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout and set the container. The layout is the
        // view that we will return.
        View v = inflater.inflate(R.layout.task_edit_fragment,
                container, false);

        // From the layout, get a few views that we're going to work with
        actionBar = getActivity().getActionBar();
        rootView = v.getRootView();
        titleText = (EditText) v.findViewById(R.id.title);
        notesText = (EditText) v.findViewById(R.id.notes);
        dateButton = (Button) v.findViewById(R.id.task_date);
        timeButton = (Button) v.findViewById(R.id.task_time);
        imageView = (ImageView) v.findViewById(R.id.image);

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

    private void save() {
        try {
            // taskId==0 when we create a new task,
            // otherwise it's the id of the task being edited.
            if (taskId == 0) {

                // Create the new task and set taskId to the id of
                // the new task.
                Task task = new Task();
                task.setTitle(titleText.getText().toString());
                task.setBody(notesText.getText().toString());
                task.setDateTime(calendar.getTime());

                taskDao.create(task);

                // Update the id field based on what Dao.create
                // tells us the new ID is.
                taskId = task.getId();

            } else {

                Task task = taskDao.queryForId(taskId);
                task.setTitle(titleText.getText().toString());
                task.setBody(notesText.getText().toString());
                task.setDateTime(calendar.getTime());

                taskDao.update(task);
            }

            // Notify the user of the change using a Toast
            Toast.makeText(getActivity(),
                    getString(R.string.task_save_success),
                    Toast.LENGTH_SHORT).show();

            // Create a reminder for this task
            new ReminderManager(getActivity()).setReminder(taskId,
                    titleText.getText().toString(), calendar);

        } catch( SQLException e ) {
            Toast.makeText(getActivity(),
                    getString(R.string.task_save_error),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the calendar instance in case the user changed it
        outState.putSerializable(CALENDAR, calendar);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, 1, 0, R.string.confirm).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Save button
        if( item.getItemId() == 1) {
            save();

            // Tell our enclosing activity that we are done so that
            // it can cleanup whatever it needs to clean up.
            ((OnEditFinished) getActivity()).finishEditingTask();

            return true;
        }

        // If we can't handle this menu item, see if our parent can
        return super.onOptionsItemSelected(item);
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
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        String timeForButton = timeFormat.format(calendar.getTime());
        timeButton.setText(timeForButton);

        // Set the date button text
        DateFormat dateFormat = DateFormat.getDateInstance();
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
    public Loader<List<Task>> onCreateLoader(int id, Bundle args) {
        try {
            return new OrmLiteEntityLoader<Task,Long>(
                    getActivity(),
                    taskDao,
                    taskDao.queryBuilder().where().idEq(taskId).prepare()
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Task>> loader,
                               List<Task> tasks) {
        // Close this fragmentClass down if the item we're editing was
        // deleted
        if (tasks==null || tasks.size()!=1 ) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    ((OnEditFinished) getActivity()).finishEditingTask();
                }
            });
            return;
        }

        // Get the first and only task in the list
        Task task = tasks.iterator().next();

        titleText.setText(task.getTitle());
        notesText.setText(task.getBody());
        calendar.setTime(task.getDateTime());

        // set the thumbnail image
        Picasso.with(getActivity())
                .load(getImageUrlForTask(getActivity(),taskId))
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        // Set the colors of the activity based on the
                        // colors of the image, if available
                        Bitmap bitmap = ((BitmapDrawable)imageView
                                .getDrawable())
                                .getBitmap();
                        Palette palette = Palette.generate(bitmap,32);

                        PaletteItem bgColor =
                                palette.getLightMutedColor();
                        PaletteItem actionbarColor =
                                palette.getDarkVibrantColor();
                        PaletteItem statusColor =
                                palette.getDarkMutedColor();

                        if( bgColor!=null && actionbarColor!=null ) {
                            rootView.setBackgroundColor(bgColor.getRgb());
                            actionBar.setBackgroundDrawable(
                                    new ColorDrawable(actionbarColor
                                            .getRgb())
                            );
                            ((Activity)rootView.getContext())
                                    .getWindow()
                                    .setStatusBarColor(
                                            (statusColor!=null ?
                                                    statusColor :
                                                    actionbarColor )
                                    .getRgb());
                        }
                    }

                    @Override
                    public void onError() {
                        // do nothing
                    }
                });


        updateButtons();
    }

    @Override
    public void onLoaderReset(Loader<List<Task>> ignored) {
        // nothing to reset for this fragmentClass
    }

    // TODO move this somewhere else
    public static String getImageUrlForTask(
            Context context, long taskId) {

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x; // in pixels
        int height = width * 2 / 3; // like a 4 x 6 photo

        return "http://lorempixel.com/" + width + "/" + height +
                "/cats/?fakeId=" + taskId;
    }
}


// TODO Remove this class once https://github.com/j256/ormlite-android/pull/22 is accepted
class OrmLiteEntityLoader<T,ID> extends
        OrmLitePreparedQueryLoader<T,ID> implements Dao.DaoObserver {


    OrmLiteEntityLoader(Context context, Dao<T, ID> dao,
                        PreparedQuery<T> preparedQuery) {
        super(context, dao, preparedQuery);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        dao.registerObserver(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        dao.unregisterObserver(this);
    }

    @Override
    public void onChange() {
        onContentChanged();
    }
}