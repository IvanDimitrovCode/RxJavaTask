package com.example.ivandimitrov.rxjavatest;

import android.util.Pair;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ivan Dimitrov on 2/9/2017.
 */

public class DownloadTask {
    public static final int TIMER_REFRESH_RATE = 20;

    private Function<Boolean, Observable<Boolean>> mObservableFunction;
    private Disposable                             mDisposable;
    private double mCurrentTimeValue = 1;
    private CountdownIndicator mIndicator;
    private double  mTimerInterval = 5000;
    private boolean isTimeFinished = false;

    DownloadTask(Function<Boolean, Observable<Boolean>> observableFunction, CountdownIndicator indicator) {
        mObservableFunction = observableFunction;
        mIndicator = indicator;
    }

    public void startTask(int timerInterval) {
        mTimerInterval = timerInterval;
        double stepsNeeded = mTimerInterval / TIMER_REFRESH_RATE;
        double percent = 100 / stepsNeeded;
        final double step = percent / 100;
        mDisposable = Observable.interval(TIMER_REFRESH_RATE, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .flatMap(s -> {
                    if (isTimeFinished) {
                        isTimeFinished = false;
                        return mObservableFunction.apply(true);
                    } else {
                        return mObservableFunction.apply(false);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    mCurrentTimeValue -= step;
                    if (mCurrentTimeValue <= 0) {
                        mCurrentTimeValue = 1;
//                        Log.i("UI", "executed on " + Thread.currentThread());
                        isTimeFinished = true;
                    }
                    mIndicator.setPhase(mCurrentTimeValue);
                },
                Throwable::printStackTrace
        );
    }

    public void stopTask() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }


    public void setIndicator(CountdownIndicator indicator) {
        mIndicator = indicator;
        mIndicator.setPhase(mCurrentTimeValue);
    }
}
