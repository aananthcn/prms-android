package com.nonprofit.aananth.prms;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Aananth added this
    public enum Mode {
        LOGIN,
        VIEW_PAT, ADD_PAT, UPDATE_PAT,
        VIEW_TREAT, ADD_TREAT, UPDATE_TREAT,
        VIEW_DOCT, ADD_DOCT, UPDATE_DOCT,
        VIEW_PAT_STAT, MAX_MODE
    }
    public static String PACKAGE_NAME;
    public static final int CREATE_FILE = 101;
    public static final int OPEN_FILE = 102;
    public static final int MY_PERMISSION_REQUEST = 103;

    // Aananth added these member variables
    private String TAG = "PRMS-MainActivity";
    private String BACKUPFILE = "prms-backup.db";
    private int currLayout;
    private Doctor mDoctor;
    private DoctorDB doctorDB;
    private List<Doctor> mDocList;
    private EditText user, patname, patphone, patmail;
    Spinner spinner;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private PatRecyclerAdapter mPatRcAdapter;
    private List<Patient> patientList;
    private Patient mCurrPatient;
    private PatientDB patientDB;
    private TreatRecyclerAdapter mTreatRcAdapter;
    private List<Treatment> treatmentList;
    private Treatment mCurrTreatment;
    private TreatmentDB treatmentDB;
    private Mode mMode, mModePrev;
    private Menu mMenu;
    private String mSrchstr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "-----------------\nWelcome to PRMS!!\n---");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Aananth added these lines
        PACKAGE_NAME = getApplicationContext().getPackageName();
        mMode = Mode.LOGIN;
        currLayout = R.layout.login;
        patientDB = new PatientDB(this);
        treatmentDB = new TreatmentDB(this);
        doctorDB = new DoctorDB(this);
        mDoctor = new Doctor("Dr. Jegadish", "0", "");

        setupDoctorLoginSpinner();
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

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public void getDynamicFilePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Layout set to current layout");
        if (mMode == Mode.VIEW_TREAT) {
            treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
            renderTreatRecycleView(treatmentList);
        }
        else {
            patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
            renderPatRecycleView(patientList);
            update_mode(Mode.VIEW_PAT);
        }
    }

    // Added by Aananth: button handlers
    public void SearchPatNamePhone(View view) {
        EditText srchtxt;

        srchtxt = (EditText) findViewById(R.id.search_txt);

        Log.d(TAG, "Search patient records");
        mSrchstr = srchtxt.getText().toString();
        patientList = patientDB.GetPatientList(mSrchstr, ListOrder.REVERSE);
        renderPatRecycleView(patientList);

        Button srchbtn = (Button) findViewById(R.id.search_btn);
        if (mSrchstr.length() > 0) {
            srchbtn.setText("Refresh");
        } else {
            srchbtn.setText("Search");
        }
    }

    public void AddNewPatient(View view) {
        currLayout = R.layout.add_edit_pat;
        setContentView(currLayout);
        setTitle("Add patient");

        Button delbtn = (Button) findViewById(R.id.pat_del);
        delbtn.setVisibility(View.INVISIBLE);
        update_mode(Mode.ADD_PAT);
    }

    public void EditPatientRecord() {
        update_mode(Mode.UPDATE_PAT);
        currLayout = R.layout.add_edit_pat;
        setContentView(currLayout);
        setTitle("Update patient");

        patname = (EditText) findViewById(R.id.fullName);
        patname.setText(mCurrPatient.Name);
        patphone = (EditText) findViewById(R.id.phoneNo);
        patphone.setText(mCurrPatient.Phone);
        patmail = (EditText) findViewById(R.id.emailID);
        patmail.setText(mCurrPatient.Email);
        if (mCurrPatient.Gender == "Male") {
            RadioButton gender = (RadioButton) findViewById(R.id.radioMale);
            gender.setChecked(true);
        }
        else {
            RadioButton gender = (RadioButton) findViewById(R.id.radioFemale);
            gender.setChecked(true);
        }

        Button patsav = (Button) findViewById(R.id.pat_save);
        patsav.setText("Update");
        Button patback = (Button) findViewById(R.id.pat_back);
        patback.setText("Cancel");
    }

    public void SavePatientRecord(View view) {
        String gender;
        RadioButton patgender;

        patname = (EditText)findViewById(R.id.fullName);
        patphone = (EditText)findViewById(R.id.phoneNo);
        patmail = (EditText)findViewById(R.id.emailID);
        patgender = (RadioButton)findViewById(R.id.radioMale);

        if (patname.getText().toString().length() == 0) {
            myOnBackPressed();
            return;
        }

        if (patgender.isChecked())
            gender = "Male";
        else
            gender = "Female";

        if (mMode == Mode.ADD_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, null, null);
            patientDB.AddPatient(pat);
            Log.d(TAG, "Added patient '" + pat.Name + "'");
        }
        else if (mMode == Mode.UPDATE_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, mCurrPatient.Pid, mCurrPatient.Uid);
            patientDB.UpdatePatient(pat);
            Log.d(TAG, " Updated patient '" + pat.Name + "'");
        }
        myOnBackPressed();
    }

    public void CancelPatientRecordEdit(View view) {
        myOnBackPressed();
    }

    public void DeleteCurrPatient(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this patient?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked ok button
                        patientDB.DeletePatient(mCurrPatient);
                        myOnBackPressed();
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        myOnBackPressed();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
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

        mRecyclerView = (RecyclerView) findViewById(R.id.pat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderPatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mPatRcAdapter = new PatRecyclerAdapter(patientList);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mPatRcAdapter);

        mPatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // show patient history view
                mCurrPatient = patlist.get(position);
                if (!mCurrPatient.Name.equals("Empty")) {
                    treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
                    renderTreatRecycleView(treatmentList);
                    update_mode(Mode.VIEW_TREAT);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                mCurrPatient = patlist.get(position);
                EditPatientRecord();
            }
        }));

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


    public void renderTreatRecycleView(final List<Treatment> treatlist) {
        // set treatment list layout
        currLayout = R.layout.treatments;
        setContentView(currLayout);
        setTitle(treatlist.get(0).patient.Name+"'s history");

        mRecyclerView = (RecyclerView) findViewById(R.id.treat_rv);
        if (mRecyclerView == null) {
            Log.d(TAG, "renderTreatRecycleView: mRecylerView is null!!");
            return;
        }
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mTreatRcAdapter = new TreatRecyclerAdapter(treatlist);
        mRecyclerView.addItemDecoration(new DividerItemDecorator(this, LinearLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mTreatRcAdapter);

        mTreatRcAdapter.notifyDataSetChanged();

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                // share treatment
                mCurrTreatment = treatlist.get(position);
                registerForContextMenu(view);
                openContextMenu(view);
                view.showContextMenu();
                unregisterForContextMenu(view);
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit treatment
                mCurrTreatment = treatlist.get(position);
                EditTreatmentRecord();
            }
        }));
    }

    public void AddNewTreatment(View view) {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "New treatment for " + mCurrPatient.Name;
        setTitle(title);
        update_mode(Mode.ADD_TREAT);

        // Add date according to the format defined in Treatment class
        Treatment treat = new Treatment(null, null, null, null, null);
        EditText date = (EditText) findViewById(R.id.treat_date);
        date.setText(treat.date);
    }

    public void SaveTreatmentRecord(View view) {
        EditText comp, pres, date;

        comp = (EditText)findViewById(R.id.complaint);
        pres = (EditText)findViewById(R.id.prescription);
        date = (EditText)findViewById(R.id.treat_date);

        if (comp.getText().toString().length() == 0 && pres.getText().toString().length() == 0) {
            myOnBackPressed();
            return;
        }

        if (mMode == Mode.ADD_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, "", comp.getText().toString(),
                    pres.getText().toString(), mDoctor.name );
            treatmentDB.AddTreatment(treat);
            Log.d(TAG, "Added treatment for " + treat.patient.Name);
        }
        else if (mMode == Mode.UPDATE_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, mCurrTreatment.tid,
                    comp.getText().toString(), pres.getText().toString(), mDoctor.name);
            treat.date = date.getText().toString();
            treatmentDB.UpdateTreatment(treat);
            Log.d(TAG, "Updated treatment for " + treat.patient.Name);
        }

        myOnBackPressed();
    }

    public void EditTreatmentRecord() {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "Update treatment for " + mCurrPatient.Name;
        setTitle(title);
        update_mode(Mode.UPDATE_TREAT);

        EditText compl = (EditText) findViewById(R.id.complaint);
        compl.setText(mCurrTreatment.complaint);
        EditText pres = (EditText) findViewById(R.id.prescription);
        pres.setText(mCurrTreatment.prescription);
        EditText date = (EditText) findViewById(R.id.treat_date);
        date.setText(mCurrTreatment.date);

        Button  savupd = (Button) findViewById(R.id.treat_sav_upd);
        savupd.setText("Update");
        Button  cancdel = (Button) findViewById(R.id.treat_canc_del);
        cancdel.setText("Delete");
    }

    public void CancDelTreatmentEdit(View view) {
        // print delete option if in update treatment mode!!
        if (mMode == Mode.UPDATE_TREAT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure to delete this treatment?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked ok button
                    treatmentDB.DeleteTreatment(mCurrTreatment);
                    myOnBackPressed();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    myOnBackPressed();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            myOnBackPressed();
        }
    }

    // M E N U   H A N D L I N G
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.export_db:
                createFileOpenDialog(CREATE_FILE);
                return true;
            case R.id.import_db:
                createFileOpenDialog(OPEN_FILE);
                return true;
            case R.id.add_doctor:
                AddNewDoctor();
                return true;
            case R.id.statistics_pat:
                ShowPatientStatistics();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void ShowPatientStatistics() {
        List<Treatment> tlist;
        int max_treats, males, total_pat;
        Patient top_pat;

        update_mode(Mode.VIEW_PAT_STAT);
        setContentView(R.layout.statistics_pat);

        males = max_treats = 0;
        total_pat = patientList.size();
        top_pat = mCurrPatient;

        TextView no_of_pat_v = (TextView) findViewById(R.id.total_pat);
        TextView male_pat_v = (TextView) findViewById(R.id.male_pat);
        TextView female_pat_v = (TextView) findViewById(R.id.female_pat);
        TextView top_pat_v = (TextView) findViewById(R.id.top_pat);

        for (Patient pat: patientList) {
            if (pat.Gender.equals("Male")) {
                males++;
            }
            tlist = treatmentDB.GetTreatmentList(pat, ListOrder.ASCENDING);
            if (tlist.size() > max_treats) {
                top_pat = pat;
                max_treats = tlist.size();
            }
        }

        no_of_pat_v.setText(String.format("%d",total_pat));
        male_pat_v.setText(String.format("%.2f", (males * 100.0)/total_pat) + "% Males  | ");
        female_pat_v.setText(String.format("%.2f", (100.0 * (total_pat - males))/total_pat) + "% Females");
        top_pat_v.setText(String.format("%s", top_pat.Name) + " is the Top Patient with " +
                String.format("%d", max_treats) + " visits");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.share_menu, menu);
        menu.setHeaderTitle("Share Treatment");
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.share_whatsapp:
                Intent wtspIntent = new Intent(Intent.ACTION_SEND);
                wtspIntent.setType("text/plain");
                wtspIntent.setPackage("com.whatsapp");
                wtspIntent.putExtra(Intent.EXTRA_TEXT, "*Date*: "+ mCurrTreatment.date
                        + "\n\n*Patient:* "+ mCurrTreatment.patient.Name
                        + "\n\n*Complaint*: "+ mCurrTreatment.complaint
                        + "\n\n*Prescription*: "+ mCurrTreatment.prescription);
                try {
                    this.startActivity(wtspIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getBaseContext(), "WhatsApp have not been installed.",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.share_sms:
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", mCurrTreatment.patient.Phone);
                smsIntent.putExtra("sms_body", mCurrTreatment.prescription);
                try {
                    this.startActivity(smsIntent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getBaseContext(), "Couldn't send SMS",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // D A T A B A S E   E X P O R T   /   I M P O R T   O P E R A T I O N S
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

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
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
        }
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

    private Date getFileDate(String fpath) {
        File file = new File(fpath);
        Date lastModDate = new Date(file.lastModified());

        Log.d(TAG, "File " + fpath + " modified date: " + lastModDate.toString());
        return lastModDate;
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
    }

    // This function merges databases
    public void importDB(String inpath) {
        // first backup the current data
        doDatabaseBackup("prms-last-backup.db", true);

        // merging starts from Patients so that their treatment and doctors are merged
        String merged_filepath = patientDB.mergeDB(inpath);

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
                Log.d(TAG, "exportDB(): Can't write into external storage!");
            }
        } catch (Exception e) {
            Log.d(TAG, "Got exception! "+ e.toString());
        }

        // Restart the app to read the new imported Database
        Intent i = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage( getBaseContext().getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(i);
    }


    // D O C T O R   H A N D L I N G
    private void setupDoctorLoginSpinner() {
        // Welcome message
        TextView tv = (TextView) findViewById(R.id.message_txt);
        tv.setText(stringFromJNI());

        // Set login options
        spinner = (Spinner)findViewById(R.id.doctor);
        List<String> loginList = new ArrayList<String>();
        mDocList = doctorDB.GetDoctorList(ListOrder.ASCENDING);
        for (Doctor doc : mDocList) {
            loginList.add(doc.name);
        }

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, loginList);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
    }

    public void AddNewDoctor() {
        currLayout = R.layout.add_edit_doc;
        setContentView(currLayout);
        Button del = (Button) findViewById(R.id.doc_del);
        del.setVisibility(View.INVISIBLE);
        update_mode(Mode.ADD_DOCT);
    }

    public void SaveDocRecord(View view) {
        EditText docName, docPhone, docEmail;
        String name, ph, email;

        docName = (EditText)findViewById(R.id.docName);
        docPhone = (EditText)findViewById(R.id.docPhone);
        docEmail = (EditText)findViewById(R.id.docEmail);

        name = docName.getText().toString();
        ph = docPhone.getText().toString();
        email = docEmail.getText().toString();

        Doctor doc = new Doctor(name, ph, email);
        doctorDB.AddDoctor(doc);

        myOnBackPressed();
    }

    public void DeleteCurrDoc(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this Doctor?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                doctorDB.DeleteDoctor(mDoctor);
                myOnBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                myOnBackPressed();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void CancelDocRecordEdit(View view) {
        myOnBackPressed();
    }



    //M O D E   M A N A G E M E N T   F U N C T I O N S
    private void update_mode(Mode mode) {
        if (mode != Mode.VIEW_PAT) {
            mSrchstr = ""; // clear the patient search once go out of View Patient mode.
        }
        mModePrev = mMode;
        mMode = mode;
        Log.d(TAG, "mMode = " + mMode + ", mModePrev = " + mModePrev);
    }

    public void onLogin(View view) {
        // setup main view
        currLayout = R.layout.patients;
        setContentView(currLayout);
        renderPatientview();

        // setup menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, mMenu);
        getDynamicFilePermission();

        //TODO: setup Google Drive
        //mDrive.connectToGoogleDrive(this);
        //Log.d(TAG, "Successful login");
    }

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

    @Override
    public void onPause() {
        super.onPause();

        doDatabaseBackup(BACKUPFILE, false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void renderTreatmentView() {
        treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
        renderTreatRecycleView(treatmentList);
        update_mode(Mode.VIEW_TREAT);
    }

    public void renderPatientview() {
        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
        renderPatRecycleView(patientList);
        update_mode(Mode.VIEW_PAT);
    }


    public void myOnBackPressed() {
       if (mMode == Mode.ADD_DOCT && mModePrev == Mode.LOGIN) {
           currLayout = R.layout.login;
           setContentView(currLayout);
           setupDoctorLoginSpinner();
           update_mode(Mode.LOGIN);
       }
       else if ((mMode == Mode.ADD_DOCT) && (mModePrev != Mode.LOGIN)) {
           if (mModePrev == Mode.UPDATE_TREAT) {
               EditTreatmentRecord();
           }
           else if (mModePrev == Mode.VIEW_TREAT || mModePrev == Mode.ADD_TREAT) {
               renderTreatmentView();
           }
           else if (mModePrev == Mode.UPDATE_PAT) {
               EditPatientRecord();
           }
           else {
               renderPatientview();
           }
       }
       else if (mMode == Mode.UPDATE_TREAT || mMode == Mode.ADD_TREAT) {
           renderTreatmentView();
       }
       else {
           renderPatientview();
       }
   }

    public void QueryBeforeHandleBackPress() {

        if ((mMode != Mode.ADD_TREAT) && (mMode != Mode.UPDATE_TREAT) &&
                (mMode != Mode.ADD_PAT) && (mMode != Mode.UPDATE_PAT) &&
                (mMode != Mode.ADD_DOCT) && (mMode != Mode.UPDATE_DOCT)) {
            myOnBackPressed();
            return;
        }

        Log.d(TAG, "QueryBeforeHandleBackPress: mMode = " + mMode);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to save this record?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked ok button
                if ((mMode == Mode.ADD_TREAT) || (mMode == Mode.UPDATE_TREAT)) {
                    SaveTreatmentRecord(findViewById(android.R.id.content));
                }
                else if ((mMode == Mode.ADD_PAT) || (mMode == Mode.UPDATE_PAT)) {
                    SavePatientRecord(findViewById(android.R.id.content));
                }
                else if ((mMode == Mode.ADD_DOCT) || (mMode == Mode.UPDATE_DOCT)) {
                    SaveDocRecord(findViewById(android.R.id.content));
                }
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
}
