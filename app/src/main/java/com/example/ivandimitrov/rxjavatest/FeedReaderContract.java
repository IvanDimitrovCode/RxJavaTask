package com.example.ivandimitrov.rxjavatest;

import android.provider.BaseColumns;

/**
 * Created by Ivan Dimitrov on 1/26/2017.
 */

public final class FeedReaderContract {
    private FeedReaderContract() {
    }

    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME  = "ChuckNorrisDB2";
        public static final String COLUMN_JOKE = "CNJoke";
    }
}
