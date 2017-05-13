package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by aananth on 04/04/17.
 */

public class PatientDB extends SQLiteOpenHelper{
    public static final String PATIENT_LIST = "patientlist";
    public static final String PKEY = "_id";
    public static final String MAIN_DATABASE = "PRMS.db";
    public static final int DATABASE_VERSION = 4;

    public PatientDB(Context context) {
        super(context, MAIN_DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String query = getCreateTableStr(null, PATIENT_LIST);
        Log.d("PatientDB", query);
        database.execSQL(query);
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
        query = "DROP TABLE IF EXISTS " + PATIENT_LIST;
        db.execSQL(query);
        onCreate(db);
    }

    // Database creation sql statement
    private String getCreateTableStr(String dbname, String table) {
        String query, tablename;

        if (dbname == null) {
            tablename = table;
        } else {
            tablename = dbname + "." + table; // here dbname is the logical name!!
        }

        query = "CREATE TABLE IF NOT EXISTS " + tablename + " ( " + PKEY +
                " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), phone VARCHAR(30), " +
                " email VARCHAR(60), gender VARCHAR(20), uid VARCHAR(100) )";

        return query;
    }

    public void AddPatient(Patient pat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String uid = (pat.Name+ new Date()).replaceAll("\\s+","");
        String query = "INSERT INTO " + PATIENT_LIST + " (name, phone, email, gender, uid) VALUES ('"+
                pat.Name + "', '" + pat.Phone +"', '" + pat.Email + "', '" + pat.Gender + "', '"+ uid + "')";

        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public void UpdatePatient(Patient pat) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + PATIENT_LIST + " SET name = '" + pat.Name + "', phone = '" + pat.Phone +
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
        query = "DELETE FROM " + PATIENT_LIST + " WHERE  uid = '" + pat.Uid + "';";
        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public List<Patient> GetPatientList(String search) {
        return GetPatientListFromDB(search, null);
    }

    public List<Patient> GetPatientListFromDB(String search, String dbname) {
        List<Patient> patientList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String name, phone, email, pid, gender, uid;
        String query, tablename;
        Patient pat;
        Cursor res;

        if (dbname == null) {
            tablename = PATIENT_LIST;
        } else {
            tablename = dbname + "." + PATIENT_LIST; // here dbname is the logical name!!
        }

        if (search == null) {
            query = "SELECT * FROM " + tablename + " ORDER BY " + PKEY + " DESC";
        } else {
            query = "SELECT * FROM " +  tablename +
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
            pid = res.getString(res.getColumnIndex(PKEY));


            Log.d("PatientDB", "Name = "+ name + ", Phone: "+phone+", Email = "+email);
            pat = new Patient(name, phone, email, gender, pid, uid);
            patientList.add(pat);

            res.moveToNext();
        }

        res.close();
        return patientList;
    }

    public String mergeDB(String inpath) {
        SQLiteDatabase db = this.getWritableDatabase();
        String outdb = "TEMP.db";
        String importdbpath = Environment.getExternalStorageDirectory().toString() + inpath;
        String query;

        List<Patient> mainPatList = new ArrayList<>();
        List<Patient> impoPatList = new ArrayList<>();

        query = "PRAGMA foreign_keys = on";
        Log.d("PatientDB", query);
        db.execSQL(query);

        query = "ATTACH DATABASE '"+ importdbpath +"' as impdb"; // db that will be imported
        Log.d("PatientDB", query);
        db.execSQL(query);

        impoPatList = GetPatientListFromDB(null, "impdb");
        Log.d("PatientDB", "Size of imported patient list: "+ impoPatList.size());


        mainPatList = GetPatientListFromDB(null, null);

        query = "ATTACH DATABASE '"+ outdb + "' as newdb"; // db that will be created
        query = "BEGIN";
        query = getCreateTableStr("newdb", PATIENT_LIST);


        query = "COMMIT";

        query = "DETACH impdb";
        Log.d("PatientDB", query);
        db.execSQL(query);

        query = "DETACH newdb";


        return null;
    }
}
