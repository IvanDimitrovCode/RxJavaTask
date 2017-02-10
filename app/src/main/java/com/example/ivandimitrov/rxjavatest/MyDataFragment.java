package com.example.ivandimitrov.rxjavatest;

import android.app.Fragment;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

/**
 * Created by Ivan Dimitrov on 2/7/2017.
 */

public class MyDataFragment extends Fragment {
    public static final String MY_URL             = "http://api.icndb.com/jokes/random";
    public static final int    TIMER_REFRESH_RATE = 20;

    private WeakReference<JokeReceivedListener> mListenerRef;
    private String                              mCurrentJoke;
    private Random mRandomGenerator = new Random();
    private DBCWriter        mDBCWriterThread;
    private SQLiteDatabase   mDataBase;
    private Observable<Long> mObservableReader;
    private Disposable       mReaderSubscriber;
    private DownloadTask     mDownloadTask;
    private ArrayList<DownloadTask> mDownloadTasks = new ArrayList<>();

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDataBase != null) {
            mDataBase.close();
        }
//        if (mDisposable != null && !mDisposable.isDisposed()) {
//            mDisposable.dispose();
//            Log.d("CLEARING", "DISPOSED");
//        }
    }

    public void startSession(ArrayList<CountdownIndicator> indicators) {
        mDBCWriterThread = new DBCWriter(getActivity());
        mDBCWriterThread.start();
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getActivity());
        mDataBase = mDbHelper.getReadableDatabase();

        Function<Boolean, Observable<Boolean>> observableFunction = s -> Observable.just(s)
                .observeOn(AndroidSchedulers.from(mDBCWriterThread.getLooper()))
                .map(isTimeFinished -> {
                    if (isTimeFinished) {
                        mCurrentJoke = parseMessage(getJoke(MY_URL));
                        if (!mDBCWriterThread.writeToDB(mCurrentJoke)) {
                            for (DownloadTask task : mDownloadTasks) {
                                task.stopTask();
                            }
                        }
                    }
                    return isTimeFinished;
                }).subscribeOn(AndroidSchedulers.from(mDBCWriterThread.getLooper()));

        mDownloadTasks.add(mDownloadTask = new DownloadTask(observableFunction, indicators.get(0)));
        mDownloadTasks.add(mDownloadTask = new DownloadTask(observableFunction, indicators.get(1)));
        mDownloadTasks.add(mDownloadTask = new DownloadTask(observableFunction, indicators.get(2)));

        for (DownloadTask task : mDownloadTasks) {
            task.startTask((mRandomGenerator.nextInt(10) + 1) * 1000);
        }

        mObservableReader = Observable.interval(3000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread());
        mReaderSubscriber = mObservableReader.subscribe(s -> readRandomJoke());
    }

    private void readRandomJoke() {
        try {
            int currentDBCSize = getDBJokeCount();
            if (currentDBCSize > 0) {
                String selectQuery = "SELECT  * FROM " + FeedReaderContract.FeedEntry.TABLE_NAME + " WHERE "
                        + FeedReaderContract.FeedEntry._ID + " = " + (mRandomGenerator.nextInt((currentDBCSize - 1)) + 1);
                Cursor c = mDataBase.rawQuery(selectQuery, null);
                if (c != null) {
                    c.moveToFirst();
                    String result = c.getString(c.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_JOKE));
                    if (mListenerRef != null) {
                        JokeReceivedListener listener = mListenerRef.get();
                        listener.onJokeReceived(result);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopReader() {
        mReaderSubscriber.dispose();
    }

    public void resumeReader() {
        mReaderSubscriber = mObservableReader.subscribe(s -> readRandomJoke());
    }

    public int getDBJokeCount() {
        int cnt = (int) DatabaseUtils.queryNumEntries(mDataBase, FeedReaderContract.FeedEntry.TABLE_NAME);
        return cnt;
    }

    private String getJoke(String selectedUrl) {
//        Log.i("GetList", "executed on " + Thread.currentThread());
        URL url = null;
        String joke = "";
        try {
            url = new URL(selectedUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            joke = readStream(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return joke;
    }

    private String readStream(InputStream in) {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        StringBuilder total = new StringBuilder();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total.toString();
    }

    private String parseMessage(String result) {
        JSONObject value = null;
        Log.i("parseMessage", "executed on " + Thread.currentThread());
        try {
            JSONObject jsonObj = new JSONObject(result);
            value = jsonObj.getJSONObject("value");
            return value.getString("joke");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    void setIndicator(CountdownIndicator indicator) {
        mDownloadTask.setIndicator(indicator);
    }

    public void setListener(JokeReceivedListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }

    public void clearListener() {
        mListenerRef = null;
    }

    public String pullResponse() {
        return mCurrentJoke;
    }

    interface JokeReceivedListener {
        void onJokeReceived(String joke);
    }
}
