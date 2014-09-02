package com.dummies.tasks.interfaces;

import android.view.View;

public interface OnEditTask {
    /**
     * Called when the user asks to edit or insert a task.
     */
    public void editTask(long id, View view);
}
