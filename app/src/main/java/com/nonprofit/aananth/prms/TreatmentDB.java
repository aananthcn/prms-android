package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.nonprofit.aananth.prms.PatientDB.DATABASE_VERSION;
import static com.nonprofit.aananth.prms.PatientDB.TABLE_PATIENTS;
import static com.nonprofit.aananth.prms.PatientDB.DATABASE_NAME;

/**
 * Created by aananth on 06/05/17.
 */

public class TreatmentDB extends SQLiteOpenHelper{
    public static final String TREAT_ID = "tid";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_PATIENTS +
            " ( " + TREAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), " +
            "phone VARCHAR(30), email VARCHAR(60), gender VARCHAR(20), uid VARCHAR(100))";

    public TreatmentDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        //database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Log.w(TreatmentDB.class.getName(),
        //        "Upgrading database from version " + oldVersion + " to "
        //                + newVersion + ", which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
        //onCreate(db);
    }

    public void createDatabaseifNotExist(SQLiteDatabase db, Treatment treat) {
        String tablename = treat.patient.Uid;
        String query = "CREATE TABLE IF NOT EXISTS " + tablename +
            " ( " + TREAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, date VARCHAR(40), " +
            "complaint VARCHAR(2048), prescription VARCHAR(2048))";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
    }

    public void AddTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();

        createDatabaseifNotExist(db, treat);
        String tablename = treat.patient.Uid;

        String query = "INSERT INTO " + tablename + " (date, complaint, prescription) VALUES ('"+
                treat.date + "', '" + treat.complaint +"', '" + treat.prescription + "')";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
    }

    public void UpdateTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = treat.patient.Uid;

        String query = "UPDATE " + tablename + " SET date = '" + treat.date +
                "', complaint = '" + treat.complaint + "', prescription = '" + treat.prescription +
                "' WHERE " + TREAT_ID + " = '" + treat.tid + "';";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
    }

    public void DeleteTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = treat.patient.Uid;

        String query = "DELETE FROM " + tablename + " WHERE " + TREAT_ID + " = '" + treat.tid + "';";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
    }

    public List<Treatment> GetTreatmentList(Patient pat) {
        List<Treatment> treatmentList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String tid, date, complaint, prescription, query;
        Cursor res;
        Treatment treat;
        String tablename = pat.Uid;

        query = "SELECT * FROM " + tablename + " ORDER BY " + TREAT_ID + " DESC";
        Log.d("TreatmentDB", query);
        res = db.rawQuery(query, null);
        res.moveToFirst();

        if (res.getCount() <= 0) {
            treat = new Treatment(pat, "0", "Empty", "Empty");
            treatmentList.add(treat);

            res.close();
            return treatmentList;
        }

        Log.d("TreatmentDB", "Number of treatments = "+res.getCount());
        while(!res.isAfterLast()){
            date = res.getString(res.getColumnIndex("date"));
            complaint = res.getString(res.getColumnIndex("complaint"));
            prescription = res.getString(res.getColumnIndex("prescription"));
            tid = res.getString(res.getColumnIndex(TREAT_ID));


            Log.d("TreatmentDB", "Date = "+ date + ", Complaint = " + complaint +
                    ", Prescription = " + prescription);
            treat = new Treatment(pat, tid, complaint, prescription);
            treatmentList.add(treat);

            res.moveToNext();
        }

        res.close();
        return treatmentList;
    }
}
