package com.connect.backend.base;

import android.util.Log;

import com.connect.core.OnCompletion;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tehilarozin on 04/12/2016.
 */

public abstract class CallableLoader implements Callable<Void> {

    protected final String loadId = this.toString() + ":" + System.currentTimeMillis();

    protected OnCompletion completion;
    private CountDownLatch waitCompletion;

    protected String TAG;
    protected final Object syncObject = new Object();
    protected boolean isCanceled = false;


    protected CallableLoader(String tag, OnCompletion completion) {
        this.completion = completion;
        this.TAG = tag;
    }

    abstract protected void load() throws InterruptedException;

    abstract protected void cancel();

    protected void notifyCompletion() {
        if (waitCompletion != null) {
            synchronized (syncObject) {
                Log.i(TAG, loadId + ": notifyCompletion: countDown =  " + waitCompletion.getCount());
                waitCompletion.countDown();
            }
        }
    }

    protected void waitCompletion() throws InterruptedException {
        synchronized (syncObject) {
            Log.i(TAG, loadId + ": waitCompletion: set new counDown" + (waitCompletion != null ? "already has counter " + waitCompletion.getCount() : ""));
            waitCompletion = new CountDownLatch(1);
        }
        waitCompletion.await();

    }

    @Override
    public Void call() {
        if (isCanceled()) { // needed in case cancel done before callable started
            Log.i(TAG, loadId + ": Loader call canceled");
            return null;
        }

        Log.i(TAG, loadId + ": Loader call started ");

        try {
            load();
            Log.i(TAG, loadId + ": load finished with no interruptions");
        } catch (InterruptedException e) {
            interrupted();
        }
        return null;
    }

    protected boolean isCanceled() {
        return isCanceled;// Thread.currentThread().isInterrupted();
    }

    protected void interrupted() {
        Log.i(TAG, loadId + ": loader operation interrupted ");
        cancel();
    }


}
