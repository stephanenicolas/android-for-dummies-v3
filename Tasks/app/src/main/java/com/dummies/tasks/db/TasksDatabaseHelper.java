package com.dummies.tasks.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.dummies.tasks.model.Task;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class TasksDatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "tasks";
    private static final int DATABASE_VERSION = 1;
    private static TasksDatabaseHelper SINGLETON;

    TasksDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource
            connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Task.class);
        } catch (SQLException e) {
            throw new UnsupportedOperationException(e);
        }

    }

    @Override
    public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) {
        try {
            return super.getDao(clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database,
                          ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {

        throw new UnsupportedOperationException();
    }

    public static synchronized TasksDatabaseHelper getSingleton(
            Context context) {

        if( SINGLETON==null ) {
            Context applicationContext = context.getApplicationContext();
            SINGLETON = new TasksDatabaseHelper(applicationContext);
        }

        return SINGLETON;
    }

}
