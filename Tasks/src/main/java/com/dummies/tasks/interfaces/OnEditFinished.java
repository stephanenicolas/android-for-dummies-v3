package com.dummies.tasks.interfaces;

//Just a think about : 
//this is the conventional way of communicating with activity
//but it forces activities to implement the callback
//a normal listener interface would require a bit more code to set a listener
//but would give more flexibility and make the fragments usable outside 
//of activities (e.g. as nested fragments)
public interface OnEditFinished {
    /**
     * Called when the user finishes editing a task.
     */
    public void finishEditingTask();
}
