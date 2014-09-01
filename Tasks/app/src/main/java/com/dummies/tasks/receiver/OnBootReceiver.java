package com.dummies.tasks.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dummies.tasks.db.TasksDatabaseHelper;
import com.dummies.tasks.model.Task;
import com.dummies.tasks.util.ReminderManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

/**
 * This class will be triggered when the phone first boots so that our
 * app can re-install any alarms that need to be set.  If we didn't do
 * this, then the phone would lose all of our alarms on reboot!
 *
 * Note: Because this receiver does I/O operations with the database,
 * it SHOULD do its work in a background service.  We're going to skip
 * that step for simplicity, but you shouldn't!  Check out
 * WakefulBroadcastReceiver for more details on how to do this.
 */
public class OnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Dao<Task,Long> taskDao =
                TasksDatabaseHelper.getSingleton(context)
                .getDao(Task.class);

        // Get all of the tasks from the database
        List<Task> tasks;
        try {
            tasks = taskDao.queryForAll();
        } catch( SQLException e ) {
            throw new RuntimeException(e);
        }

        // Loop over all of the tasks
        for( Task task : tasks ) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(task.getDateTime());

            // Set the reminder
            new ReminderManager(context).setReminder(task.getId(),
                    cal);
        }
    }
}
