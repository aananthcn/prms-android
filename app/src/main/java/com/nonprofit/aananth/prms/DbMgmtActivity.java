package com.nonprofit.aananth.prms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static com.nonprofit.aananth.prms.MainActivity.CREATE_FILE;
import static com.nonprofit.aananth.prms.MainActivity.OPEN_FILE;
import static com.nonprofit.aananth.prms.MainActivity.PACKAGE_NAME;

import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;


public class DbMgmtActivity extends AppCompatActivity {
    private String TAG = "PRMS-DbMgmtActivity";
    private String BACKUPFILE = "prms-backup.db";
    private EditText mDbActMessageText;
    private String mDialogStr;
    private String mEditBoxStr;
    private Boolean mActivityStarted = false;
    private enum findDuplStates {FD_IDLE, FD_GETUI, FD_GETUI_INPROG, FD_ACTION_REQUESTED};
    private findDuplStates mFdState = findDuplStates.FD_IDLE;
    private AsyncTask<Void, Void, Void> mFindDuplBkGndTask;

    static private Boolean dbOperationActive = false;
    static private Boolean fDupOperationActive = false;
    static private Boolean patStatOperationActive = false;

    private DoctorDB mDoctorDB;
    private PatientDB mPatientDB;
    private TreatmentDB mTreatmentDB;

    //private Handler importDB_Handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_mgmt);

        // Establish connections to databases
        mPatientDB = new PatientDB(this);
        mTreatmentDB = new TreatmentDB(this);
        mDoctorDB = new DoctorDB(this);
    }


    private void myfinish() {
        Log.d(TAG, "myfinsh()");
        dbOperationActive = false;
        fDupOperationActive = false;
        patStatOperationActive = false;
        mFdState = findDuplStates.FD_IDLE;
        finish();
    }

    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        if (mActivityStarted) {
            return;
        }
        else {
            mActivityStarted = true;
        }

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String str = intent.getStringExtra(EXTRA_MESSAGE);
        mDbActMessageText = (EditText) findViewById(R.id.dbActMessage);

        if (str.equals("import database")) {
            Log.d(TAG, "onResume(): Import Database");
            mDbActMessageText.setText("Importing Database ...");
            int action = (int) intent.getSerializableExtra("action");
            setTitle("Import Database");
            createFileOpenDialog(action);
        }
        else if (str.equals("export database")) {
            Log.d(TAG, "onResume(): Export Database");
            mDbActMessageText.setText("Exporting Database ...");
            int action = (int) intent.getSerializableExtra("action");
            setTitle("Export Database");
            createFileOpenDialog(action);
        }
        else if (str.equals("backup database")) {
            Log.d(TAG, "onResume(): Backup Database");
            mDbActMessageText.setText("Backing up database...");
            setTitle("Backup Database");
            doDatabaseBackup(BACKUPFILE, true);
            myfinish();
        }
        else if (str.equals("find duplicates")) {
            Log.d(TAG, "onResume(): Find Duplicates");
            mDbActMessageText.setText("Finding duplicates in Patient Database ...");
            setTitle("Find Duplicates");
            FindDuplicatePatients();
        }
        else if (str.equals("patient statistics")) {
            Log.d(TAG, "onResume(): Patient Statistics");
            mDbActMessageText.setText("Parsing patient database ...");
            String searchStr = intent.getStringExtra("search string");
            setTitle("Patient Statistics");
            ShowPatientStatistics(searchStr);
        }
        else {
            Log.d(TAG, "onResume(): Error illegal EXTRA_MESSAGE");
        }
    }


    // D A T A B A S E   E X P O R T   /   I M P O R T   O P E R A T I O N S
    public void doDatabaseBackup(String backupfile, boolean force) {
        String backuppath = getBackupFilePath(backupfile);

        // save backups if any database failed
        if (mPatientDB.isDbChanged() || mTreatmentDB.isDbChanged() || mDoctorDB.isDbChanged() || force) {
            Log.d(TAG, "onPause: creating " + backuppath);

            // backup databases and notify the event to all DB interface classes
            this.exportDB(backuppath);
            mPatientDB.DbSaved();
            mTreatmentDB.DbSaved();
            mDoctorDB.DbSaved();
            Toast.makeText(getBaseContext(), "Database backed up to " + backupfile,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // invoked from menu to import or export database
    private void createFileOpenDialog(int action) {
        Intent intent = new Intent()
                .setType("*/*");
        String msg;

        if (action == CREATE_FILE) {
            intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
            msg = "Create file";
        }
        else if (action == OPEN_FILE){
            intent.setAction(Intent.ACTION_GET_CONTENT);
            msg = "Select a file";
        }
        else {
            Log.d(TAG, "Illegal action passed to createFileOpenDialog()");
            return;
        }

        startActivityForResult(Intent.createChooser(intent, msg), action);
    }

    private String convertUriToFilePath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        String path = null;

        ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor metaCursor = cr.query(uri, projection, null, null, null);
        if (metaCursor != null) {
            try {
                if (metaCursor.moveToFirst()) {
                    metaCursor.moveToFirst();
                    path = metaCursor.getString(0);
                    Log.d(TAG, "convertUriToFilePath(): path = "+path);
                }
            } finally {
                metaCursor.close();
            }
        }
        else {
            Log.d(TAG, "convertUriToFilePath(): metaCursor is null!!");
        }

        //TODO: Add designs to find full path, till then use "Download" dir
        return "/Download/"+path;
    }

    static public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private String getBackupFilePath(String filename) {
        String storagepath = Environment.getExternalStorageDirectory().toString();
        String outpath = storagepath + "/Download/" + filename;

        File file = new File(outpath);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    OutputStream fo = new FileOutputStream(file);
                    byte data[] = {'d', 'u', 'm', 'm', 'y'};
                    fo.write(data);
                    fo.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outpath;
    }


    // This function copies the MAIN_DATABASE used by this app to external storage path
    private void exportDB(String outpath) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (isExternalStorageWritable()) {
                String  appDBpath = "/data/" + PACKAGE_NAME
                        + "/databases/" + MAIN_DATABASE;
                File in_file = new File(data, appDBpath);
                File out_file = new File(sd, outpath);
                if (!out_file.exists()) {
                    out_file = new File(outpath);
                }
                if (out_file.exists()) {
                    out_file.delete();
                }

                FileChannel src = new FileInputStream(in_file).getChannel();
                FileChannel dst = new FileOutputStream(out_file).getChannel();
                Log.i(TAG, "Exporting "+ src.size()+ " bytes from "+appDBpath+ " to "+ outpath);
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            }
            else {
                Log.d(TAG, "exportDB(): Can't write into external storage!");
            }
        } catch (Exception e) {
            Log.d(TAG, "Got exception! "+ e.toString());
        }
        Toast.makeText(getBaseContext(), "Export database completed!",
                Toast.LENGTH_SHORT).show();
    }

    // This function merges databases
    public void importDB(String inpath) {
        // first backup the current data
        doDatabaseBackup("prms-last-backup.db", true);
        Toast.makeText(getBaseContext(), "Merging databases is happening in background",
                Toast.LENGTH_SHORT).show();
        final String inPath = inpath;

        dbOperationActive = true;
        runImpExpUiUpdateThread();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // merging starts from Patients so that their treatment and doctors are merged
                final String merged_filepath = mPatientDB.mergeDB(inPath, mTreatmentDB);

                // closes databases so that the user will be forced to re-login to re-init databases
                mTreatmentDB.close();
                mPatientDB.close();
                mDoctorDB.close();

                try {
                    File data  = Environment.getDataDirectory();

                    if (isExternalStorageWritable()) {
                        String  appDBpath = "/data/" + PACKAGE_NAME + "/databases/" + MAIN_DATABASE;
                        File out_file = new File(data, appDBpath);
                        File in_file = new File(merged_filepath);

                        FileChannel dst = new FileOutputStream(out_file).getChannel();
                        FileChannel src = new FileInputStream(in_file).getChannel();
                        Log.i(TAG, "Importing "+ src.size()+ " bytes from "+merged_filepath+
                                " to "+ appDBpath );
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();
                        in_file.delete(); // delete temp file
                        doDatabaseBackup(BACKUPFILE, true);
                    }
                    else {
                        Log.d(TAG, "importDB(): Can't write into external storage!");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "importDB(): Got exception! "+ e.toString());
                }

                dbOperationActive = false;
                Log.d(TAG, "importDB(): Finished import+merge; restarting MainActivity!");
                // Restart the app to read the new imported Database
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                myfinish();
                startActivity(i);
            }
        });
    }

    private void runImpExpUiUpdateThread() {
        new Thread() {
            public void run() {
                while (dbOperationActive) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String str = mPatientDB.getStatusString();
                                mDbActMessageText.setText(str);
                            }
                        });
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    //   P A T I E N T   S T A T I S T I C S
    private void ShowPatientStatistics(String searchStr) {
        patStatOperationActive = true;
        runPatStatUiUpdateThread();
        mEditBoxStr = "Patient statistics: " + "...";

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String status = "started!";
                List<Patient> patientList;
                List<Treatment> tlist;
                int max_treats, males, females, total_pat, i;
                Patient top_pat;

                patientList = mPatientDB.GetPatientList(searchStr, ListOrder.REVERSE);
                males = females = max_treats = i = 0;
                total_pat = patientList.size();
                top_pat = patientList.get(0);
                status = "... ...";

                for (Patient pat : patientList) {
                    mEditBoxStr = "Parsing " + i + " of " + total_pat + "!";
                    i++;
                    if (pat.Gender.equals("Male")) {
                        males++;
                    } else if (pat.Gender.equals("Female")) {
                        females++;
                    }
                    tlist = mTreatmentDB.GetTreatmentList(pat, ListOrder.ASCENDING);
                    if (tlist.size() > max_treats) {
                        top_pat = pat;
                        max_treats = tlist.size();
                    }
                }

                PatStatistics pstat = new PatStatistics(total_pat, males, females, top_pat.Name, max_treats);
                Intent intent = new Intent();
                intent.putExtra("patient statistics", pstat);
                setResult(RESULT_OK, intent);
                patStatOperationActive = false;
                myfinish();
            }
        });
    }


    private void runPatStatUiUpdateThread() {
        new Thread() {
            public void run() {
                while (patStatOperationActive) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDbActMessageText.setText(mEditBoxStr);
                            }
                        });
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    // Activity Results
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        String path = null;
        Uri uri = null;
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        switch (requestCode) {
            case CREATE_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    // The document selected by the user won't be returned in the intent.
                    // Instead, a URI to that document will be contained in the return intent
                    // provided to this method as a parameter.
                    // Pull that URI using resultData.getData().
                    if (resultData != null) {
                        uri = resultData.getData();
                        path = convertUriToFilePath(uri);
                        Log.i(TAG, "Create file: " + path);
                        exportDB(path);
                        myfinish();
                    }
                }
                break;

            case OPEN_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        uri = resultData.getData();
                        path = convertUriToFilePath(uri);
                        Log.i(TAG, "Open file: " + path);
                        importDB(path);
                    }
                }
                break;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void FindDuplicatePatients() {
        Log.d(TAG, "FindDuplicatePatients()");
        final List<Patient> patientList = mPatientDB.GetPatientList(null, ListOrder.REVERSE);

        mEditBoxStr = "...";
        fDupOperationActive = true;
        runUiUpdateFindDuplThread();

        mFindDuplBkGndTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Log.d(TAG, "FindDuplicatePatients() : entering doInBackground() thread!");
                List<Patient> removedPats = new ArrayList<>();
                List<Patient> duplicaList;
                int i = 0, total;
                Boolean exitBackgroundTask = false;
                Boolean dbDirtyFlag = false;

                total = patientList.size();
                mEditBoxStr = "Parsing: " + i + " out of " + total + " patients";
                // find all duplicates...
                for (final Patient pat : patientList) {
                    // This function finds duplicates based on phone number. A valid phone number is
                    // critical to find duplicates.
                    i++;
                    final String phone = pat.Phone;
                    if (phone.length() < 8 || phone.length() > 15) {
                        continue; // invalid phone, so move to next patient
                    }

                    duplicaList = mPatientDB.GetPatientList(phone, null);

                    for (final Patient dup : duplicaList) {
                        Boolean is_removed;

                        if (duplicaList.size() <= 1) {
                            continue; // duplicate makes sense only if there are more than 1
                        }

                        // ignore if the duplicate and patient are same
                        if (dup.Pid.equals(pat.Pid)) {
                            continue;
                        }

                        // ignore if a patient is already removed in previous iteration
                        is_removed = false;
                        for (final Patient remPat : removedPats) {
                            if (dup.Pid.equals(remPat.Pid)) {
                                is_removed = true;
                                break;
                            }
                        }
                        if (is_removed) {
                            continue;
                        }

                        // get ready to prompt user for merge
                        mDialogStr = "Shall I merge \"" + dup.Name + ": " + dup.Phone + "\" " +
                                " ==> \"" + pat.Name + ": " + pat.Phone + "\"" + " ?";
                        mFdState = findDuplStates.FD_GETUI;

                        // wait infinitely till user say 'merge' or 'skip' in UI dialog raised
                        // by runUiUpdateFindDuplThread() function
                        while (mFdState == findDuplStates.FD_GETUI ||
                                mFdState == findDuplStates.FD_GETUI_INPROG) {
                            try {
                                Thread.sleep(200);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (isCancelled()) {
                                Log.d(TAG, "Exiting doInBackground()::while loop!");
                                exitBackgroundTask = true;
                                break;
                            }
                        }

                        if (exitBackgroundTask) {
                            Log.d(TAG, "Exiting doInBackground()::for loop (duplicate)!");
                            break;
                        }

                        if (mFdState == findDuplStates.FD_ACTION_REQUESTED) {
                            List<Treatment> dupTrtList = mTreatmentDB.GetTreatmentList(dup, null);
                            for (Treatment trt : dupTrtList) {
                                if (!trt.complaint.equalsIgnoreCase("Empty")) {
                                    mTreatmentDB.AddTreatmentToPatient(trt, pat);
                                }
                            }

                            // Remove patient from main database
                            mPatientDB.DeletePatient(dup);
                            dbDirtyFlag = true;
                            removedPats.add(dup);
                            removedPats.add(pat);
                            mFdState = findDuplStates.FD_IDLE;
                        }
                    }
                    mEditBoxStr = "Parsing: " + i + " out of " + total + " patients";
                    if (exitBackgroundTask) {
                        Log.d(TAG, "Exiting doInBackground()::for loop (main loop)!");
                        break;
                    }
                }
                Log.d(TAG, "FindDuplicatePatients(): exiting doInBackground() thread!");
                fDupOperationActive = false;
                if (dbDirtyFlag) {
                    dbDirtyFlag = false;
                    Intent intent = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    myfinish();
                    startActivity(intent);
                }
                return null;
            }
        }.execute();

        /*
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
            }
        });
        */
    }

    private void runUiUpdateFindDuplThread() {
        Log.d(TAG, "runUiUpdateFindDuplThread()");
        new Thread() {
            public void run() {
                Log.d(TAG, "runUiUpdateFindDuplThread() : run() thread!");
                while (fDupOperationActive) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDbActMessageText.setText(mEditBoxStr);

                                if (mFdState == findDuplStates.FD_GETUI) {
                                    mFdState = findDuplStates.FD_GETUI_INPROG;
                                    AlertDialog.Builder builder = new AlertDialog.Builder(DbMgmtActivity.this);
                                    builder.setMessage(mDialogStr);
                                    builder.setPositiveButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // User clicked ok button
                                            mFdState = findDuplStates.FD_IDLE;
                                        }
                                    });
                                    builder.setNegativeButton("Merge", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // User cancelled the dialog
                                            mFdState = findDuplStates.FD_ACTION_REQUESTED;
                                        }
                                    });

                                    AlertDialog dialog = builder.create();
                                    dialog.setOnKeyListener(new Dialog.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface arg0, int keyCode,
                                                             KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                mFindDuplBkGndTask.cancel(true);
                                                dialog.dismiss();
                                                myfinish();
                                            }
                                            return true;
                                        }
                                    });
                                    dialog.show();
                                }
                            }
                        });
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "runUiUpdateFindDuplThread() : exiting while loop!");
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        myfinish();
    }
}
