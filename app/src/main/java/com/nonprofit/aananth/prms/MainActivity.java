package com.nonprofit.aananth.prms;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // **
    // * A native method that is implemented by the 'native-lib' native library,
    // * which is packaged with this application.
    // *
    public native String stringFromJNI();

    // Aananth added this
    public enum Mode {
        PERMISSION_GET, LOGIN, REND_PAT, VIEW_PAT,
        /* VIEW_DOCT, ADD_DOCT, UPDATE_DOCT,*/
        MAX_MODE
    }
    public static String PACKAGE_NAME;
    public static final int CREATE_FILE = 101;
    public static final int OPEN_FILE = 102;
    public static final int MY_PERMISSION_REQUEST = 103;
    public static final int LOGIN_ACTIVITY = 104;

    // Aananth added these member variables
    private String TAG = "PRMS-MainActivity";
    private String BACKUPFILE = "prms-backup.db";
    private int currLayout;
    private Doctor mDoctor;
    private DoctorDB doctorDB;
    private List<Doctor> mDocList;
    Spinner spinner;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PatRecyclerAdapter mPatRcAdapter;
    private List<Patient> mPatientList;
    private Patient mCurrPatient;
    private PatientDB patientDB;
    private TreatmentDB treatmentDB;
    private Mode mMode, mModePrev;
    private Menu mMenu;
    private String mSrchstr;
    private Boolean initialPatRenderingDone = false;

    private Handler importDB_Handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "-----------------\nWelcome to PRMS!!\n---");
        super.onCreate(savedInstanceState);

        // Aananth added these lines
        PACKAGE_NAME = getApplicationContext().getPackageName();
        mMode = Mode.PERMISSION_GET;
        currLayout = R.layout.login;
        patientDB = new PatientDB(this);
        treatmentDB = new TreatmentDB(this);
        doctorDB = new DoctorDB(this);
        mDoctor = new Doctor("Dr. Jegadish", "0", "");

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        if (mMode == Mode.PERMISSION_GET) {
            getDynamicFilePermission();
        }
        else if (mMode == Mode.LOGIN) {
            setupLoginScreen();
        }
        else if (mMode == Mode.REND_PAT) {
            renderPatientview(); //Patient view is rendered for the first time, here
        }
        else {
            refreshPatRecycleView();
        }
    }


    private void setupLoginScreen() {
        Log.d(TAG, "setupLoginScreen()");

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("doctor", mDoctor);
        intent.putExtra(EXTRA_MESSAGE, stringFromJNI());
        startActivityForResult(intent, LOGIN_ACTIVITY);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        mDoctor = mDocList.get(position);

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_SHORT).show();
    }


    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }


    public void getDynamicFilePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // TODO: Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.d(TAG, "getDynamicFilePermission() - Needs Explanation!");
                // FIXME: Let us request again..
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        MY_PERMISSION_REQUEST
                );
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        MY_PERMISSION_REQUEST
                );
            }
        }
        else {
            //permission already granted, so go to Login Screen
            mMode = Mode.LOGIN;
            setupLoginScreen();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    // setupLoginScreen();
                    mMode = Mode.LOGIN;

                } else {
                    // permission denied, boo! Lets exit.
                    mMode = Mode.PERMISSION_GET;
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    // Added by Aananth: button handlers
    public void SearchPatNamePhone(View view) {
        EditText srchtxt;

        srchtxt = (EditText) findViewById(R.id.search_txt);
        mSrchstr = srchtxt.getText().toString();

        Log.d(TAG, "SearchPatNamePhone(\"" + mSrchstr + "\")");
        refreshPatRecycleView();

        Button srchbtn = (Button) findViewById(R.id.search_btn);
        if (mSrchstr.length() > 0) {
            srchbtn.setText("Refresh");
        } else {
            srchbtn.setText("Search");
        }

        hideKeyboard();
    }


    public void AddNewPatient(View view) {
        Intent intent = new Intent(this, PatientAddEditActivity.class);
        intent.putExtra(EXTRA_MESSAGE, "add patient");
        intent.putExtra("patient", mCurrPatient);
        startActivity(intent);
    }


    // interface class
    public static abstract class ClickListener{
        public abstract void onClick(View view, int position);
        public abstract void onLongClick(View view, int position);
    }

    public void renderPatRecycleView(final List<Patient> patlist) {
        // set patient list layout
        currLayout = R.layout.patients;
        setContentView(currLayout);
        setTitle("Patient List");
        Log.d(TAG, "renderPatRecycleView()");

        mRecyclerView = (RecyclerView) findViewById(R.id.pat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderPatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPatRcAdapter = new PatRecyclerAdapter(mPatientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mPatRcAdapter);

        mPatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // show patient history view
                Log.d(TAG, "onClick()");
                mCurrPatient = patlist.get(position);
                if (!mCurrPatient.Name.equals("Empty")) {
                    Intent intent = new Intent(MainActivity.this, TreatmentViewActivity.class);
                    intent.putExtra("patient", mCurrPatient);
                    intent.putExtra("doctor", mDoctor);
                    startActivity(intent);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                mCurrPatient = patlist.get(position);
                //EditPatientRecord();
                Intent intent = new Intent(MainActivity.this, PatientAddEditActivity.class);
                intent.putExtra(EXTRA_MESSAGE, "update patient");
                intent.putExtra("patient", mCurrPatient);
                startActivity(intent);
            }
        }));
        initialPatRenderingDone = true;

        EditText srchBox = (EditText) findViewById(R.id.search_txt);
        srchBox.setText(mSrchstr);
        srchBox.invalidate();
        srchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean handled = false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_SEARCH:
                            SearchPatNamePhone(v.getRootView());
                            handled = true;
                        default:
                            Log.d(TAG, " setOnKeyListener search patient: unknown key " + keyCode);
                            break;
                    }
                }
                return handled;

            }
        });
    }


    public void refreshPatRecycleView() {
        if (!initialPatRenderingDone) {
            return;
        }

        setTitle("Patients List");
        Log.d(TAG, "refreshPatRecycleView()");

        mPatientList.clear();
        mPatientList.addAll(patientDB.GetPatientList(mSrchstr, ListOrder.REVERSE));
        mPatRcAdapter.notifyDataSetChanged();
        update_mode(Mode.VIEW_PAT);
    }




    //   P A T I E N T   S T A T I S T I C S
    public void ShowPatientStatistics() {
        List<Treatment> tlist;
        int max_treats, males, females, total_pat;
        Patient top_pat;

        males = females = max_treats = 0;
        total_pat = mPatientList.size();
        top_pat = mCurrPatient;

        for (Patient pat: mPatientList) {
            if (pat.Gender.equals("Male")) {
                males++;
            }
            else if (pat.Gender.equals("Female")) {
                females++;
            }
            tlist = treatmentDB.GetTreatmentList(pat, ListOrder.ASCENDING);
            if (tlist.size() > max_treats) {
                top_pat = pat;
                max_treats = tlist.size();
            }
        }

        PatStatistics pstat = new PatStatistics(total_pat, males, females, top_pat.Name, max_treats);

        Intent intent = new Intent(this, PatStatisticsActivity.class);
        intent.putExtra("patient statistics", pstat);
        startActivity(intent);
    }



    // M E N U   H A N D L I N G
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.export_db:
                createFileOpenDialog(CREATE_FILE);
                return true; // say to android that this menu event is handled here...
            case R.id.import_db:
                createFileOpenDialog(OPEN_FILE);
                return true; // say to android that this menu event is handled here...
            case R.id.complaint_search:
                SwitchToSearchActivity("complaint search");
                return true; // say to android that this menu event is handled here...
            case R.id.prescription_search:
                SwitchToSearchActivity("prescription search");
                return true; // say to android that this menu event is handled here...
            case R.id.statistics_pat:
                ShowPatientStatistics();
                return true; // say to android that this menu event is handled here...
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void SwitchToSearchActivity(String searchType) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra(EXTRA_MESSAGE, searchType);
        intent.putExtra("doctor", mDoctor);
        startActivity(intent);
    }


    //M O D E   M A N A G E M E N T   F U N C T I O N S
    private void update_mode(Mode mode) {
        mModePrev = mMode;
        mMode = mode;
        Log.d(TAG, "mMode = " + mMode + ", mModePrev = " + mModePrev);
    }

    @Override
    public void onPause() {
        super.onPause();

        doDatabaseBackup(BACKUPFILE, false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void renderPatientview() {
        mPatientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
        renderPatRecycleView(mPatientList);
        update_mode(Mode.VIEW_PAT);
    }


    private void myOnBackPressed() {
        refreshPatRecycleView();
    }

    public void QueryBeforeHandleBackPress() {
        Log.d(TAG, "QueryBeforeHandleBackPress: mMode = " + mMode);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to save this record?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                /*
                if ((mMode == Mode.ADD_DOCT) || (mMode == Mode.UPDATE_DOCT)) {
                    SaveDocRecord(findViewById(android.R.id.content));
                }
                */
                myOnBackPressed();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                myOnBackPressed();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        if (mMode == Mode.LOGIN) {
            finish();
        }
        //else if (mMode == Mode.VIEW_PAT_STAT) {
        //    myOnBackPressed();
        //}
        else if (mMode != Mode.VIEW_PAT) {
            QueryBeforeHandleBackPress();
        }
        else {
            if (exit) {
                finish(); // finish activity
            } else {
                Toast.makeText(this, "Press Back again to Exit.",
                        Toast.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                    }
                }, 3 * 1000);
            }
        }
    }


    private void hideKeyboard() {
        Context context = this;
        View view = getCurrentFocus().getRootView();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


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

            case LOGIN_ACTIVITY:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "onActivityResult()::LOGIN_ACTIVITY");
                    String strLoginResult = resultData.getStringExtra("login result");

                    if (strLoginResult.equalsIgnoreCase("login exit")) {
                        finish();
                    }
                    else {
                        MenuInflater inflater = getMenuInflater();
                        inflater.inflate(R.menu.main_menu, mMenu);
                        //MenuItem doctMenuItem = mMenu.findItem(R.id.add_doctor);
                        //doctMenuItem.setVisible(false);
                        update_mode(Mode.REND_PAT); // will be rendered inside onResume
                    }
                }
                break;
        }
    }


    // D A T A B A S E   E X P O R T   /   I M P O R T   O P E R A T I O N S
    public void doDatabaseBackup(String backupfile, boolean force) {
        String backuppath = getBackupFilePath(backupfile);

        // save backups if any database failed
        if (patientDB.isDbChanged() || treatmentDB.isDbChanged() || doctorDB.isDbChanged() || force) {
            Log.d(TAG, "onPause: creating " + backuppath);

            // backup databases and notify the event to all DB interface classes
            this.exportDB(backuppath);
            patientDB.DbSaved();
            treatmentDB.DbSaved();
            doctorDB.DbSaved();
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

    /*
    private Date getFileDate(String fpath) {
        File file = new File(fpath);
        Date lastModDate = new Date(file.lastModified());

        Log.d(TAG, "File " + fpath + " modified date: " + lastModDate.toString());
        return lastModDate;
    }
    */

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
    }

    // This function merges databases
    public void importDB(String inpath) {
        // first backup the current data
        doDatabaseBackup("prms-last-backup.db", true);
        Toast.makeText(getBaseContext(), "Merging databases is happening in background",
                Toast.LENGTH_LONG).show();
        final String inPath = inpath;

        importDB_Handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                // merging starts from Patients so that their treatment and doctors are merged
                final String merged_filepath = patientDB.mergeDB(inPath, treatmentDB);

                // closes databases so that the user will be forced to re-login to re-init databases
                treatmentDB.close();
                patientDB.close();
                doctorDB.close();

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

                Log.d(TAG, "importDB(): Finished import+merge; restarting MainActivity!");
                // Restart the app to read the new imported Database
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
                startActivity(i);
            }
        };
        importDB_Handler.postDelayed(r, 100);
    }
}
