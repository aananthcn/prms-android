package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by aananth on 04/04/17.
 */

public class PatientDB extends SQLiteOpenHelper{
    public static final String TABLE_PATIENTS = "patientlist";
    public static final String COLUMN_ID = "_id";
    private static final String DATABASE_NAME = "PRMS.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_PATIENTS + " ( " + COLUMN_ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), phone VARCHAR(30), email VARCHAR(60), gender VARCHAR(20))";

    public PatientDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.d("PatientDB", "Creating database: " + DATABASE_NAME);
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PatientDB.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
        onCreate(db);
    }

    public void InitDatabase(SQLiteDatabase db) {
        //db.execSQL("CREATE DATABASE IF NOT EXISTS "+ DATABASE_NAME);
        //db.execSQL("USE " + DATABASE_NAME);
        //db.execSQL("CREATE TABLE IF NOT EXISTS " + PATIENT_TABLE + " ( pid INT NOT NULL AUTO_INCREMENT, name VARCHAR(100), phone VARCHAR(30), email VARCHAR(60), gender VARCHAR(20), PRIMARY KEY (pid) )");
    }

    public void AddPatient(String name, String gender, String phone, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("PatientDB", name + ", " + gender + ", " + phone + ", " + email);
        db.execSQL("INSERT INTO " + TABLE_PATIENTS + " (name, phone, email, gender) VALUES ('"+ name +
                "', '" + phone +"', '" + email + "', '" + gender + "')");
    }
}
