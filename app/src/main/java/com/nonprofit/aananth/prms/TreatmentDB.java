package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.nonprofit.aananth.prms.PatientDB.DATABASE_VERSION;
import static com.nonprofit.aananth.prms.PatientDB.PATIENT_LIST;
import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;

/**
 * Created by aananth on 06/05/17.
 */

public class TreatmentDB extends SQLiteOpenHelper{
    public static final String TREAT_ID = "tid";
    private boolean mDbChanged = false;


    // Database creation sql statement
    public TreatmentDB(Context context) {
        super(context, MAIN_DATABASE, null, DATABASE_VERSION);
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
        //db.execSQL("DROP TABLE IF EXISTS " + PATIENT_LIST);
        //onCreate(db);
    }


    public void createTreatmentTableIfNotExist(SQLiteDatabase db, String tablename) {
        String query;

        query = "CREATE TABLE IF NOT EXISTS " + tablename +
                " ( " + TREAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, date VARCHAR(40), " +
                "complaint VARCHAR(2048), prescription VARCHAR(2048), doctor VARCHAR(100) )";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
    }


    public void AddTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();

        AddTreatmentToDB(db, treat, null);
    }


    public void AddTreatmentToDB(SQLiteDatabase db, Treatment treat, String dbname) {
        String tablename;

        if (dbname == null)
            tablename = treat.patient.Uid;
        else
            tablename = dbname + "." + treat.patient.Uid; // here dbname is the logical name!!

        createTreatmentTableIfNotExist(db, tablename);

        String query = "INSERT INTO " + tablename + " (date, complaint, prescription, doctor) VALUES ('"+
                treat.date + "', '" + treat.complaint +"', '" + treat.prescription +"', '" +
                treat.doctor + "')";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
        mDbChanged = true;
    }


    public void UpdateTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = treat.patient.Uid;

        String query = "UPDATE " + tablename + " SET date = '" + treat.date +
                "', complaint = '" + treat.complaint + "', prescription = '" + treat.prescription +
                "' WHERE " + TREAT_ID + " = '" + treat.tid + "';";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
        mDbChanged = true;
    }


    public void DeleteTreatment(Treatment treat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = treat.patient.Uid;

        String query = "DELETE FROM " + tablename + " WHERE " + TREAT_ID + " = '" + treat.tid + "';";

        Log.d("TreatmentDB", query);
        db.execSQL(query);
        mDbChanged = true;
    }


    public List<Treatment> GetTreatmentList(Patient pat, ListOrder order) {
        SQLiteDatabase db = this.getWritableDatabase();

        return GetTreatmentListFromDB(db, pat, null, order);
    }


    public List<Treatment> GetTreatmentListFromDB(SQLiteDatabase db, Patient pat, String dbname, ListOrder order) {
        List<Treatment> treatmentList = new ArrayList<>();
        String tid, date, complaint, prescription, doctor, query, desc;
        Cursor res;
        Treatment treat;
        String tablename;

        Log.d("TreatmentDB", "GetTreatmentListFromdDB(): dbname = " + dbname);
        if (dbname == null)
            tablename = pat.Uid;
        else
            tablename = dbname + "." + pat.Uid; // here dbname is the logical name!!

        if (order == ListOrder.REVERSE)
            desc = " DESC";
        else
            desc = "";

        query = "SELECT * FROM " + tablename + " ORDER BY " + TREAT_ID + desc;
        Log.d("TreatmentDB", query);
        res = db.rawQuery(query, null);
        res.moveToFirst();

        if (res.getCount() <= 0) {
            treat = new Treatment(pat, "0", "Empty", "Empty", "");
            treatmentList.add(treat);

            res.close();
            return treatmentList;
        }

        Log.d("TreatmentDB", "Number of treatments = "+res.getCount());
        while(!res.isAfterLast()){
            date = res.getString(res.getColumnIndex("date"));
            complaint = res.getString(res.getColumnIndex("complaint"));
            prescription = res.getString(res.getColumnIndex("prescription"));
            doctor = res.getString(res.getColumnIndex("doctor"));
            tid = res.getString(res.getColumnIndex(TREAT_ID));


            Log.d("TreatmentDB", "Date = "+ date + ", Complaint = " + complaint +
                    ", Prescription = " + prescription);
            treat = new Treatment(pat, tid, complaint, prescription, doctor);
            treat.date = date;
            treatmentList.add(treat);

            res.moveToNext();
        }

        res.close();
        return treatmentList;
    }


    private boolean isTreatmentExist(Treatment inTrt, List<Treatment> trtList) {
        for (Treatment trt : trtList) {
            Log.d("TreatmentDB", trt.date+"=="+inTrt.date+" && "+ trt.complaint+"=="+inTrt.complaint);
            if ( trt.date.equals(inTrt.date) && trt.complaint.equals(inTrt.complaint) ) {
                return true;
            }
        }
        return false;
    }


    public void mergeTreatments(SQLiteDatabase db, Patient pat, String srcdbn, String dstdbn) {
        List<Treatment> srcList; // Treatment list from source database
        List<Treatment> dstList; // Treatment list from destination database

        Log.d("TreatmentDB", "Entering mergeTreatments()");
        srcList = GetTreatmentListFromDB(db, pat, srcdbn, ListOrder.ASCENDING);
        dstList = GetTreatmentListFromDB(db, pat, dstdbn, ListOrder.ASCENDING);

        // Check for redundancy and add imported records to destination database
        int count = 0;
        for (Treatment trt : srcList) {
            if (!isTreatmentExist(trt, dstList)) {
                if (trt.patient.Name.equals("Empty"))
                    continue;
                AddTreatmentToDB(db, trt, dstdbn);
                dstList.add(trt);
                count++;
            }
        }
        Log.d("TreatmentDB", "Added "+count+" treatments to " + dstdbn);
    }


    public boolean isDbChanged() {
        Log.d("TreatmentDB", "mDbChanged = " + mDbChanged);
        return mDbChanged;
    }


    public void DbSaved() {
        mDbChanged = false;
    }
}
