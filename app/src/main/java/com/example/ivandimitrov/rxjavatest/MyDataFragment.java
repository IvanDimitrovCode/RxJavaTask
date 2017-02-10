package com.example.ivandimitrov.rxjavatest;

import android.app.Fragment;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.ivandimitrov.rxjavatest.retrofit.ApiClient;
import com.example.ivandimitrov.rxjavatest.retrofit.ApiInterface;

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
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ivan Dimitrov on 2/7/2017.
 */

public class MyDataFragment extends Fragment {
    public static final String MY_URL = "http://api.icndb.com/jokes/random";

    private String                              mCurrentJoke;
    private Disposable                          mReaderSubscriber;
    private Observable<Long>                    mObservableReader;
    private WeakReference<JokeReceivedListener> mListenerRef;

    private ArrayList<DownloadTask> mDownloadTasks   = new ArrayList<>();
    private Random                  mRandomGenerator = new Random();

    //SQL
    private DBCWriter      mDBCWriterThread;
    private SQLiteDatabase mDataBase;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDataBase != null) {
            mDataBase.close();
        }
    }

    public void startSession(ArrayList<CountdownIndicator> indicators) {
        mDBCWriterThread = new DBCWriter(getActivity());
        mDBCWriterThread.start();
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(getActivity());
        mDataBase = mDbHelper.getReadableDatabase();

        //===============================
        //RETROFIT + RXJAVA
        //===============================
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        apiService.getJoke()
                .subscribeOn(Schedulers.io())
                .doOnNext(s -> Log.d("SHOW", s.getValue().getJoke()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        //===============================

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

        mDownloadTasks.add(new DownloadTask(observableFunction, indicators.get(0)));
        mDownloadTasks.add(new DownloadTask(observableFunction, indicators.get(1)));
        mDownloadTasks.add(new DownloadTask(observableFunction, indicators.get(2)));

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
//        Log.i("parseMessage", "executed on " + Thread.currentThread());
        try {
            JSONObject jsonObj = new JSONObject(result);
            value = jsonObj.getJSONObject("value");
            return value.getString("joke");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    void setIndicator(ArrayList<CountdownIndicator> indicators) {
        for (int i = 0; i < mDownloadTasks.size(); i++) {
            mDownloadTasks.get(i).setIndicator(indicators.get(i));
        }
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
