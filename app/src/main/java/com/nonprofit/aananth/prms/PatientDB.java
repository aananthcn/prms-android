package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by aananth on 04/04/17.
 */

public class PatientDB extends SQLiteOpenHelper{
    public static final String TABLE_PATIENTS = "patientlist";
    public static final String COLUMN_ID = "_id";
    public static final String DATABASE_NAME = "PRMS.db";
    public static final int DATABASE_VERSION = 4;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_PATIENTS +
            " ( " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), " +
            "phone VARCHAR(30), email VARCHAR(60), gender VARCHAR(20), uid VARCHAR(100))";

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

        // TODO: In future we should add saving/exporting old tables before delete
        String query;
        List<Patient> patientList = GetPatientList(null);

        // Delete all treatment history of all patients
        for (Patient pat: patientList) {
            query = "DROP TABLE IF EXISTS '" + pat.Uid + "'";
            db.execSQL(query);
        }

        // Delete the patient table and then create it in the end
        query = "DROP TABLE IF EXISTS " + TABLE_PATIENTS;
        db.execSQL(query);
        onCreate(db);
    }

    public void AddPatient(Patient pat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String uid = (pat.Name+ new Date()).replaceAll("\\s+","");
        String query = "INSERT INTO " + TABLE_PATIENTS + " (name, phone, email, gender, uid) VALUES ('"+
                pat.Name + "', '" + pat.Phone +"', '" + pat.Email + "', '" + pat.Gender + "', '"+ uid + "')";

        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public void UpdatePatient(Patient pat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_PATIENTS + " SET name = '" + pat.Name + "', phone = '" + pat.Phone +
                "', email = '" + pat.Email + "', gender = '" + pat.Gender + "' WHERE uid = '" +
                pat.Uid + "';";

        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public void DeletePatient(Patient pat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query;

        // delete treatment history
        query = "DROP TABLE IF EXISTS '" + pat.Uid + "'";
        Log.d("PatientDB", query);
        db.execSQL(query);

        // delete the patient
        query = "DELETE FROM " + TABLE_PATIENTS + " WHERE  uid = '" + pat.Uid + "';";
        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public List<Patient> GetPatientList(String search) {
        List<Patient> patientList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String name, phone, email, query, pid, gender, uid;
        Patient pat;
        Cursor res;

        if (search == null) {
            query = "SELECT * FROM " + TABLE_PATIENTS + " ORDER BY " + COLUMN_ID + " DESC";
        } else {
            query = "SELECT * FROM " + TABLE_PATIENTS +
                            " WHERE name LIKE '%"+search+"%' OR phone LIKE '%"+search+"%'";
        }
        Log.d("PatientDB", query);
        res = db.rawQuery(query, null);
        res.moveToFirst();

        if (res.getCount() <= 0) {
            pat = new Patient("Empty", "", "", "", "", "");
            patientList.add(pat);

            res.close();
            return patientList;
        }

        Log.d("PatientDB", "Number of patients = "+res.getCount());
        while(!res.isAfterLast()){
            name = res.getString(res.getColumnIndex("name"));
            phone = res.getString(res.getColumnIndex("phone"));
            email = res.getString(res.getColumnIndex("email"));
            gender = res.getString(res.getColumnIndex("gender"));
            uid = res.getString(res.getColumnIndex("uid"));
            pid = res.getString(res.getColumnIndex(COLUMN_ID));


            Log.d("PatientDB", "Name = "+ name + ", Phone: "+phone+", Email = "+email);
            pat = new Patient(name, phone, email, gender, pid, uid);
            patientList.add(pat);

            res.moveToNext();
        }

        res.close();
        return patientList;
    }
}
