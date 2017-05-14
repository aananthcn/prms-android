package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.nonprofit.aananth.prms.PatientDB.DATABASE_VERSION;
import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;

/**
 * Created by aananth on 06/05/17.
 */

public class DoctorDB extends SQLiteOpenHelper{
    public static final String DOCT_ID = "did";
    public static final String DOCT_TABLE = "doclist";

    // Database creation sql statement
    public DoctorDB(Context context) {
        super(context, MAIN_DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        //database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Log.w(DoctorDB.class.getName(),
        //        "Upgrading database from version " + oldVersion + " to "
        //                + newVersion + ", which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + PATIENT_LIST);
        //onCreate(db);
    }

    public void createDoctorTableIfNotExist(SQLiteDatabase db, String tablename) {
        String query;

        query = "CREATE TABLE IF NOT EXISTS " + tablename +
                " ( " + DOCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(64), " +
                "phone VARCHAR(32), email VARCHAR(64) )";

        Log.d("DoctorDB", query);
        db.execSQL(query);
    }

    public void AddDoctor(Doctor doc) {
        SQLiteDatabase db = this.getWritableDatabase();

        AddDoctorToDB(db, doc, null);
    }

    public void AddDoctorToDB(SQLiteDatabase db, Doctor doc, String dbname) {
        String tablename;

        if (dbname == null)
            tablename = DOCT_TABLE;
        else
            tablename = dbname + "." + DOCT_TABLE; // here dbname is the logical name!!

        createDoctorTableIfNotExist(db, tablename);

        String query = "INSERT INTO " + tablename + " (name, phone, email) VALUES ('"+
                doc.name + "', '" + doc.phone +"', '" + doc.email + "')";

        Log.d("DoctorDB", query);
        db.execSQL(query);
    }

    public void UpdateDoctor(Doctor doc) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = DOCT_TABLE;

        String query = "UPDATE " + tablename + " SET name = '" + doc.name +
                "', phone = '" + doc.phone + "', email = '" + doc.email +
                "' WHERE " + DOCT_ID + " = '" + doc.id + "';";

        Log.d("DoctorDB", query);
        db.execSQL(query);
    }

    public void DeleteDoctor(Doctor doc) {
        SQLiteDatabase db = this.getWritableDatabase();
        String tablename = DOCT_TABLE;

        String query = "DELETE FROM " + tablename + " WHERE " + DOCT_ID + " = '" + doc.id + "';";

        Log.d("DoctorDB", query);
        db.execSQL(query);
    }

    public List<Doctor> GetDoctorList() {
        SQLiteDatabase db = this.getWritableDatabase();

        return GetDoctorListFromDB(db, null);
    }

    public List<Doctor> GetDoctorListFromDB(SQLiteDatabase db, String dbname) {
        List<Doctor> docList = new ArrayList<>();
        String query, name, email, phone;
        int id;
        Cursor res;
        Doctor doc;
        String tablename;


        Log.d("DoctorDB", "GetDoctorListFromdDB(): dbname = " + dbname);
        if (dbname == null)
            tablename = DOCT_TABLE;
        else
            tablename = dbname + "." + DOCT_TABLE; // here dbname is the logical name!!

        createDoctorTableIfNotExist(db, tablename);

        query = "SELECT * FROM " + tablename + " ORDER BY " + DOCT_ID + " DESC";
        Log.d("DoctorDB", query);
        res = db.rawQuery(query, null);
        res.moveToFirst();

        if (res.getCount() <= 0) {
            doc = new Doctor("Empty", "", "");
            docList.add(doc);

            res.close();
            return docList;
        }

        Log.d("DoctorDB", "Number of docments = "+res.getCount());
        while(!res.isAfterLast()){
            name = res.getString(res.getColumnIndex("name"));
            phone = res.getString(res.getColumnIndex("phone"));
            email = res.getString(res.getColumnIndex("email"));
            id = res.getInt(res.getColumnIndex(DOCT_ID));


            Log.d("DoctorDB", "Name = "+ name + ", Phone = " + phone + ", Email = " + email);
            doc = new Doctor(name, phone, email, id);
            docList.add(doc);

            res.moveToNext();
        }

        res.close();
        return docList;
    }

}
