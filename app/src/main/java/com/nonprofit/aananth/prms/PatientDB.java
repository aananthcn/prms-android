package com.nonprofit.aananth.prms;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatatypeMismatchException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PatientDB extends SQLiteOpenHelper{
    public static final String PATIENT_LIST = "patientlist";
    public static final String PKEY = "_id";
    public static final String MAIN_DATABASE = "PRMS.db";
    public static final int DATABASE_VERSION = 4;

    private Context mContext;

    public PatientDB(Context context) {
        super(context, MAIN_DATABASE, null, DATABASE_VERSION);
        mContext = context;
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
        List<Patient> patientList = GetPatientList(null, ListOrder.ASCENDING);

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

    // Patient table creation sql statement
    private String getCreateTableStr(String dbname, String table) {
        String query, tablename;

        if (dbname == null) {
            tablename = table;
        } else {
            tablename = dbname + "." + table; // here dbname is the logical name!!
        }

        query = "CREATE TABLE IF NOT EXISTS " + tablename + " ( " + PKEY +
                " INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(100), phone VARCHAR(30), " +
                " email VARCHAR(60), gender VARCHAR(20), uid VARCHAR(100) UNIQUE )";

        return query;
    }

    // return 1 on success, 0 on failure
    public int AddPatient(Patient pat) {
        return AddPatientToDB(pat, null);
    }

    private int AddPatientToDB(Patient pat, String dbname) {
        SQLiteDatabase db = this.getWritableDatabase();
        String uid, tablename;
        int retval = 1;

        if (dbname == null) {
            // new patient added from UI
            tablename = PATIENT_LIST;
            uid = (pat.Name+"_"+ new Date()).replaceAll("[^a-zA-Z0-9]+","_");;
        } else {
            // patient from other db is copied to new db
            tablename = dbname + "." + PATIENT_LIST; // here dbname is the logical name!!
            uid = pat.Uid;
        }


        String query = "INSERT INTO " + tablename + " (name, phone, email, gender, uid) VALUES ('"+
                pat.Name + "', '" + pat.Phone +"', '" + pat.Email + "', '" + pat.Gender + "', '"+ uid + "')";

        Log.d("PatientDB", query);
        try {
            db.execSQL(query);
        }
        catch (SQLException mSQLException) {
            if(mSQLException instanceof SQLiteConstraintException){
                //some toast message to user.
                Log.d("PatientDB", "AddPatient: SQLiteConstraintException");
            }else if(mSQLException instanceof SQLiteDatatypeMismatchException) {
                //some toast message to user.
                Log.d("PatientDB", "AddPatient: SQLiteDatatypeMismatchException");
            }else {
                throw mSQLException;
            }
            retval = 0; // add failure!!
        }

        // If this is new patient, then create Treatment Table
        if (retval > 0) {
            TreatmentDB treatDB = new TreatmentDB(mContext);
            String treatTable;

            if (dbname == null)
                treatTable = uid;
            else
                treatTable = dbname + "." + uid;

            treatDB.createTreatmentTableIfNotExist(db, treatTable);
        }

        return retval;
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
        query = "DELETE FROM " + PATIENT_LIST + " WHERE " + PKEY + " = '" + pat.Pid + "';";
        Log.d("PatientDB", query);
        db.execSQL(query);
    }

    public List<Patient> GetPatientList(String search, ListOrder order) {
        return GetPatientListFromDB(search, null, order);
    }

    private List<Patient> GetPatientListFromDB(String search, String dbname, ListOrder order) {
        List<Patient> patientList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        String name, phone, email, pid, gender, uid, desc;
        String query, tablename;
        Patient pat;
        Cursor res;

        onCreate(db); //// TODO: 15/05/17 Fix this bug!! Following is executed before table is created!!

        if (dbname == null)
            tablename = PATIENT_LIST;
        else
            tablename = dbname + "." + PATIENT_LIST; // here dbname is the logical name!!

        if (order == ListOrder.REVERSE)
            desc = " DESC";
        else
            desc = "";

        if (search == null)
            query = "SELECT * FROM " + tablename + " ORDER BY " + PKEY + desc;
        else
            query = "SELECT * FROM " +  tablename +
                            " WHERE name LIKE '%"+search+"%' OR phone LIKE '%"+search+"%'" +
                    " ORDER BY " + PKEY + desc;

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

    private boolean isTableExists(SQLiteDatabase db, String tableName)
    {
        if (tableName == null || db == null) {
            return false;
        }

        String query = "SELECT * FROM " + tableName;
        Log.d("PatientDB", query);
        Cursor cursor = db.rawQuery(query, null);

        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }
        int count = cursor.getInt(0);
        cursor.close();
        Log.d("PatientDB", "isTableExists() query returned "+ count + " results!");

        return count > 0;
    }


    // This function copies all patient data from first argument to destination database (4th arg)
    // This function also merges treatments by checking for duplicate entries
    private int CopyPatListToDB(List<Patient> patList, SQLiteDatabase db, String srcnm, String dstnm) {
        int copy_count = 0;
        String src_table;
        TreatmentDB treatDB = new TreatmentDB(mContext);

        // Merge treatments for every patients
        for (Patient pat: patList) {
            if (pat.Name.equals("Empty"))
                continue;

            // Copy patient details
            copy_count += AddPatientToDB(pat, dstnm);

            // Find the name of treatment table for this patient
            if (srcnm == null)
                src_table = pat.Uid;
            else
                src_table = srcnm + "." + pat.Uid;

            // Copy treatments if the treatment table is valid
            if (isTableExists(db, src_table)) {
                treatDB.mergeTreatments(db, pat, srcnm, dstnm);
            }
        }

        return copy_count;
    }


    public String mergeDB(String inpath) {
        SQLiteDatabase db = this.getWritableDatabase();
        String storagepath = Environment.getExternalStorageDirectory().toString();
        String exportdbpath = storagepath + "/Download/TEMP.db";
        String importdbpath = storagepath + inpath;
        String IMPDB_LN = "impdb";
        String NEWDB_LN = "newdb";
        String query;
        int total_records, copied_records = 0;
        List<Patient> mainPatList, impoPatList;

        query = "PRAGMA foreign_keys = on";
        Log.d("PatientDB", query);
        db.execSQL(query);

        // Attach to new and imported databases
        query = "ATTACH DATABASE '"+ importdbpath +"' as " + IMPDB_LN; // db that will be imported
        Log.d("PatientDB", query);
        db.execSQL(query);
        query = "ATTACH DATABASE '"+ exportdbpath + "' as "+ NEWDB_LN; // db that will be created
        Log.d("PatientDB", query);
        db.execSQL(query);
        query = "BEGIN";
        Log.d("PatientDB", query);
        db.execSQL(query);

        // Create patientlist table in the new database
        query = getCreateTableStr(NEWDB_LN, PATIENT_LIST);
        Log.d("PatientDB", query);
        db.execSQL(query);


        // Copy patient records to new database
        mainPatList = GetPatientListFromDB(null, null, ListOrder.ASCENDING); // main database
        copied_records += CopyPatListToDB(mainPatList, db, null, NEWDB_LN);
        impoPatList = GetPatientListFromDB(null, IMPDB_LN, ListOrder.ASCENDING); // to be imported database
        copied_records += CopyPatListToDB(impoPatList, db, IMPDB_LN, NEWDB_LN);
        total_records = mainPatList.size() + impoPatList.size();
        Log.d("PatientDB", "Total patients added: "+ copied_records + " out of " + total_records);

        // Copy doctor records to new database
        DoctorDB dctDB = new DoctorDB(mContext);
        dctDB.mergeDoctors(db, null, NEWDB_LN);
        dctDB.mergeDoctors(db, IMPDB_LN, NEWDB_LN);

        // Commit and detach
        query = "COMMIT";
        Log.d("PatientDB", query);
        db.execSQL(query);
        query = "DETACH " + IMPDB_LN;
        Log.d("PatientDB", query);
        db.execSQL(query);
        query = "DETACH "+ NEWDB_LN;
        Log.d("PatientDB", query);
        db.execSQL(query);


        return exportdbpath;
    }
}
