package jp.ac.titech.itpro.sdl.machiawase;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by takahiro on 2017/07/08.
 */

public class Utills {
    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }

}
