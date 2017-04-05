package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by aananth on 04/04/17.
 */

public class PatientDB extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Patient.db";

    public PatientDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS patientdb.patientlist ( pid INT NOT NULL AUTO_INCREMENT, name VARCHAR(100), phone VARCHAR(30), email VARCHAR(60), gender VARCHAR(20), PRIMARY KEY (pid) )");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
