package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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

    public void AddPatient(String name, String gender, String phone, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("PatientDB", name + ", " + gender + ", " + phone + ", " + email);
        db.execSQL("INSERT INTO " + TABLE_PATIENTS + " (name, phone, email, gender) VALUES ('"+ name +
                "', '" + phone +"', '" + email + "', '" + gender + "')");
    }

    public List<Patient> GetPatientList(String search) {
        List<Patient> patientList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String name, phone, email, query;
        Patient pat;
        Cursor res;

        if (search == null) {
            query = new String("SELECT * FROM " + TABLE_PATIENTS + " ORDER BY " + COLUMN_ID + " DESC");
        } else {
            query = new String("SELECT * FROM " + TABLE_PATIENTS +
                            " WHERE name LIKE '"+search+"' OR phone LIKE '"+search+"'");
        }
        Log.d("PatientDB", query);
        res = db.rawQuery(query, null);
        res.moveToFirst();

        if (res.getCount() <= 0) {
            pat = new Patient("Empty", "", "");
            patientList.add(pat);

            return patientList;
        }

        Log.d("PatientDB", "Number of patients = "+res.getCount());
        while(res.isAfterLast() == false){
            name = new String(res.getString(res.getColumnIndex("name")));
            phone = new String(res.getString(res.getColumnIndex("phone")));
            email = new String(res.getString(res.getColumnIndex("email")));

            Log.d("PatientDB", "Name = "+ name + ", Phone: "+phone+", Email = "+email);
            pat = new Patient(name, phone, email);
            patientList.add(pat);

            res.moveToNext();
        }

        return patientList;
    }
}
