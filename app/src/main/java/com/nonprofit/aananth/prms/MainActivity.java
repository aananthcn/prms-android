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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.String;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Aananth added this
    public enum Mode {LOGIN, NORMAL, ADD_PAT, UPDATE_PAT, VIEW_TREAT, ADD_TREAT, UPDATE_TREAT}
    public static String PACKAGE_NAME;
    public static final int CREATE_FILE = 101;
    public static final int OPEN_FILE = 102;
    public static final int MY_PERMISSION_REQUEST = 103;

    // Aananth added these member variables
    private int currLayout;
    private List doctors = Arrays.asList("Jegadish", "Rama");
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
    private Mode mMode;
    private Menu mMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Aananth added these lines
        PACKAGE_NAME = getApplicationContext().getPackageName();
        currLayout = R.layout.login;
        patientDB = new PatientDB(this);
        treatmentDB = new TreatmentDB(this);
        doctorDB = new DoctorDB(this);
        mDoctor = new Doctor("Dr. Jegadish", "0", "");
        mMode = Mode.LOGIN;

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

    public void onLogin(View view) {
        Log.d("Main Activity", "Successful login");
        currLayout = R.layout.patients;
        setContentView(currLayout);
        mMode = Mode.NORMAL;

        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
        renderPatRecycleView(patientList);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, mMenu);
        getDynamicFilePermission();
    }

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
        Log.d("Main Activity", "Layout set to current layout");
        setContentView(currLayout);
    }

    // Added by Aananth: button handlers
    public void SearchPatNamePhone(View view) {
        EditText srchtxt;
        String srchstr;

        srchtxt = (EditText) findViewById(R.id.search_txt);

        Log.d("Main Activity", "Search patient records");
        srchstr = srchtxt.getText().toString();
        patientList = patientDB.GetPatientList(srchstr, ListOrder.REVERSE);
        renderPatRecycleView(patientList);

        Button srchbtn = (Button) findViewById(R.id.search_btn);
        if (srchstr.length() > 0) {
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
        mMode = Mode.ADD_PAT;
    }

    public void SavePatientRecord(View view) {
        String gender;
        RadioButton patgender;

        patname = (EditText)findViewById(R.id.fullName);
        patphone = (EditText)findViewById(R.id.phoneNo);
        patmail = (EditText)findViewById(R.id.emailID);
        patgender = (RadioButton)findViewById(R.id.radioMale);

        if (patgender.isChecked())
            gender = "Male";
        else
            gender = "Female";

        if (mMode == Mode.ADD_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, "", "");
            patientDB.AddPatient(pat);
            Log.d("Main Activity", "Added patient '" + pat.Name + "'");
        }
        else if (mMode == Mode.UPDATE_PAT) {
            Patient pat = new Patient(patname.getText().toString(), patphone.getText().toString(),
                    patmail.getText().toString(), gender, mCurrPatient.Pid, mCurrPatient.Uid);
            patientDB.UpdatePatient(pat);
            Log.d("Main Activity", " Updated patient '" + pat.Name + "'");
        }
        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
        renderPatRecycleView(patientList);
        mMode = Mode.NORMAL;
    }

    public void CancelPatientRecordEdit(View view) {
        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
        renderPatRecycleView(patientList);
        mMode = Mode.NORMAL;
    }

    public void DeleteCurrPatient(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure to delete this patient?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked ok button
                        patientDB.DeletePatient(mCurrPatient);
                        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
                        renderPatRecycleView(patientList);
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
                        renderPatRecycleView(patientList);
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
        mMode = Mode.NORMAL;
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
            Log.d("Main Activity", "renderPatRecycleView: mRecylerView is null!!");
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
                    mMode = Mode.VIEW_TREAT;
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // edit patient data
                mMode = Mode.UPDATE_PAT;
                currLayout = R.layout.add_edit_pat;
                setContentView(currLayout);
                setTitle("Update patient");

                mCurrPatient = patlist.get(position);
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
        }));
    }


    public void renderTreatRecycleView(final List<Treatment> treatlist) {
        // set treatment list layout
        currLayout = R.layout.treatments;
        setContentView(currLayout);
        setTitle(treatlist.get(0).patient.Name+"'s history");

        mRecyclerView = (RecyclerView) findViewById(R.id.treat_rv);
        if (mRecyclerView == null) {
            Log.d("Main Activity", "renderTreatRecycleView: mRecylerView is null!!");
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

    public void onBackPressed() {
        if (mMode == Mode.UPDATE_TREAT || mMode == Mode.ADD_TREAT) {
            mMode = Mode.VIEW_TREAT;
            treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
            renderTreatRecycleView(treatmentList);
        }
        else {
            patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
            renderPatRecycleView(patientList);
            mMode = Mode.NORMAL;
        }
    }

    public void AddNewTreatment(View view) {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "New treatment for " + mCurrPatient.Name;
        setTitle(title);
        mMode = Mode.ADD_TREAT;

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

        if (mMode == Mode.ADD_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, "", comp.getText().toString(),
                    pres.getText().toString(), mDoctor.name );
            treatmentDB.AddTreatment(treat);
            Log.d("Main Activity", "Added treatment for " + treat.patient.Name);
        }
        else if (mMode == Mode.UPDATE_TREAT) {
            Treatment treat = new Treatment(mCurrPatient, mCurrTreatment.tid,
                    comp.getText().toString(), pres.getText().toString(), mDoctor.name);
            treat.date = date.getText().toString();
            treatmentDB.UpdateTreatment(treat);
            Log.d("Main Activity", "Updated treatment for " + treat.patient.Name);
        }

        treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
        renderTreatRecycleView(treatmentList);
        mMode = Mode.VIEW_TREAT;
    }

    public void EditTreatmentRecord() {
        currLayout = R.layout.add_edit_treat;
        setContentView(currLayout);

        String title = "Update treatment for " + mCurrPatient.Name;
        setTitle(title);
        mMode = Mode.UPDATE_TREAT;

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

    public void CancelTreatmentEdit(View view) {
        // print delete option if in update treatment mode!!
        if (mMode == Mode.UPDATE_TREAT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure to delete this treatment?");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked ok button
                    treatmentDB.DeleteTreatment(mCurrTreatment);
                    mMode = Mode.VIEW_TREAT;
                    treatmentList = treatmentDB.GetTreatmentList(mCurrPatient, ListOrder.REVERSE);
                    renderTreatRecycleView(treatmentList);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        onBackPressed();
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
            default:
                return super.onOptionsItemSelected(item);
        }
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
            Log.d("Main Activity", "Illegal action passed to createFileOpenDialog()");
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
                    Log.d("Main Activity", "convertUriToFilePath(): path = "+path);
                }
            } finally {
                metaCursor.close();
            }
        }
        else {
            Log.d("Main Activity", "convertUriToFilePath(): metaCursor is null!!");
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
                        Log.i("Main Activity", "Create file: " + path);
                        exportDB(path);
                    }
                }
                break;
            case OPEN_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        uri = resultData.getData();
                        path = convertUriToFilePath(uri);
                        Log.i("Main Activity", "Open file: " + path);
                        importDB(path);
                    }
                }
                break;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private String createBackupFile(String filename) {
        String storagepath = Environment.getExternalStorageDirectory().toString();
        String outpath = storagepath + "/Download/" + filename;

        File file = new File(outpath);
        try {
            file.createNewFile();
            if (file.exists()) {
                OutputStream fo = new FileOutputStream(file);
                byte data[] = {'d', 'u', 'm', 'm', 'y'};
                fo.write(data);
                fo.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outpath;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (patientDB.isDbChanged() || treatmentDB.isDbChanged() || doctorDB.isDbChanged()) {
            String outpath = createBackupFile("prms-backup.db");
            Log.d("Main Activity", "onPause: creating " + outpath);

            // backup databases and notify the event to all DB interface classes
            this.exportDB(outpath);
            patientDB.DbSaved();
            treatmentDB.DbSaved();
            doctorDB.DbSaved();
        }
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
                Log.i("Main Activity", "Exporting "+ src.size()+ " bytes from "+appDBpath+ " to "+ outpath);
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Toast.makeText(getBaseContext(), out_file.toString(),
                        Toast.LENGTH_SHORT).show();
            }
            else {
                Log.d("Main Activity", "exportDB(): Can't write into external storage!");
            }
        } catch (Exception e) {
            Log.d("Main Activity", "Got exception! "+ e.toString());
        }
    }

    // This function merges databases
    public void importDB(String inpath) {
        // merging starts from Patients so that their treatment and doctors are merged
        String merged_filepath = patientDB.mergeDB(inpath);

        // closes databases so that the user will be forced to re-login to re-init databases
        treatmentDB.close();
        patientDB.close();
        doctorDB.close();

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data  = Environment.getDataDirectory();

            if (isExternalStorageWritable()) {
                String  appDBpath = "/data/" + PACKAGE_NAME
                        + "/databases/" + MAIN_DATABASE;
                File out_file = new File(data, appDBpath );
                //out_file.delete();
                File in_file = new File(merged_filepath);

                FileChannel dst = new FileOutputStream(out_file).getChannel();
                FileChannel src = new FileInputStream(in_file).getChannel();
                Log.i("Main Activity", "Importing "+ src.size()+ " bytes from "+merged_filepath+
                        " to "+ appDBpath );
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Toast.makeText(getBaseContext(), in_file.toString(),
                        Toast.LENGTH_SHORT).show();
                in_file.delete(); // delete temp file
            }
            else {
                Log.d("Main Activity", "exportDB(): Can't write into external storage!");
            }
        } catch (Exception e) {
            Log.d("Main Activity", "Got exception! "+ e.toString());
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

        if (mMode == Mode.LOGIN) {
            currLayout = R.layout.login;
            setContentView(currLayout);
            setupDoctorLoginSpinner();
        }
        else {
            currLayout = R.layout.patients;
            setContentView(currLayout);

            patientList = patientDB.GetPatientList(null, ListOrder.REVERSE);
            renderPatRecycleView(patientList);
            mMode = Mode.NORMAL;
        }
    }
}
