package com.dummies.tasks.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dummies.tasks.R;
import com.dummies.tasks.activity.TaskPreferencesActivity;
import com.dummies.tasks.db.TasksDatabaseHelper;
import com.dummies.tasks.interfaces.OnEditTask;
import com.dummies.tasks.model.Task;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;
import java.util.List;

public class TaskListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<Task>> {


    Dao<Task,Long> taskDao;
    TaskListAdapter adapter;
    RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get a Data Access Object to help us manage our Tasks
        taskDao = TasksDatabaseHelper.getSingleton(getActivity())
                .getDao(Task.class);

        // Create the adapter that will produce views for each
        // task in the database
        adapter = new TaskListAdapter(taskDao);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.task_list_fragment,
                container, false);
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_insert:
                ((OnEditTask) getActivity()).editTask(0);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(getActivity(),
                        TaskPreferencesActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<Task>> onCreateLoader(int ignored, Bundle args) {
        try {
            return new OrmLiteListLoader<Task>(getActivity(), taskDao,
                    taskDao.queryBuilder().prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Task>> loader,
                               List<Task> tasks) {
        adapter.setTasks(tasks);
    }

    @Override
    public void onLoaderReset(Loader<List<Task>> loader) {
        adapter.setTasks(null);
    }
}



// TODO move to ormlite
// Need to see what happens with Android L first, are they going to add
// their own cursor adapter for recyclerview?
class OrmLiteListLoader<T> extends AsyncTaskLoader<List<T>>
        implements Dao.DaoObserver {

    protected Dao<T, ?> dao;
    protected PreparedQuery<T> query;
    protected List<T> results;

    public OrmLiteListLoader(Context context, Dao<T, ?> dao,
                          PreparedQuery<T> query) {
        super(context);
        this.dao = dao;
        this.query = query;
        dao.registerObserver(this);
    }

    @Override
    public List<T> loadInBackground() {
        try {
            return dao.query(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deliverResult(List<T> newResults) {
        if (isReset()) {
            return;
        }

        results = newResults;

        if (isStarted()) {
            super.deliverResult(results);
        }
    }

    @Override
    protected void onStartLoading() {
        if (results == null) {
            forceLoad();
        } else {
            deliverResult(results);
            if (takeContentChanged()) {
                forceLoad();
            }
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        results = null;
    }

    public void onChange() {
        onContentChanged();
    }

    public PreparedQuery<T> getQuery() {
        return query;
    }

    public void setQuery(PreparedQuery<T> mQuery) {
        this.query = mQuery;
    }
}






class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {
    List<Task> tasks;
    Dao<Task,Long> taskDao;


    TaskListAdapter( Dao<Task,Long> taskDao ) {
        this.taskDao = taskDao;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_row, parent, false);

        // wrap it in a ViewHolder
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final long id = getItemId(i);
        final Context context = viewHolder.title.getContext();

        // set the text
        viewHolder.title.setText(tasks.get(i).getTitle());

        // Set the click action
        viewHolder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((OnEditTask) context).editTask(id);
            }
        });

        viewHolder.title.setOnLongClickListener(new View
                .OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.delete_q)
                        .setMessage(viewHolder.title.getText())
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface
                                                                dialogInterface, int i) {

                                        try {
                                            taskDao.delete(tasks.get(i));
                                        } catch (SQLException e) {
                                            Toast.makeText(context,
                                                    R.string.task_save_error,
                                                    Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    }
                                })
                        .show();
                return true;
            }
        });

    }

    @Override
    public long getItemId(int position) {
        return tasks.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return tasks!=null ? tasks.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public ViewHolder(TextView itemView) {
            super(itemView);
            title = itemView;
        }

    }
}