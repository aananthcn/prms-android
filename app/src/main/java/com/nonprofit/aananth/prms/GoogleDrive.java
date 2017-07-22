package com.nonprofit.aananth.prms;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;

import static com.nonprofit.aananth.prms.MainActivity.PACKAGE_NAME;
import static com.nonprofit.aananth.prms.PatientDB.MAIN_DATABASE;


/**
 * Created by aananth on 15/07/17.
 */

public class GoogleDrive implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    // A t t r i b u t e s
    private GoogleApiClient mGoogleApiClient;
    private android.content.Context mContext;
    protected static final int REQUEST_CODE_RESOLUTION = 1; // for auto Google Play Services error resolution
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private String TAG = "PRMS-GoogleDrive";
    private String BACKUPFOLDER = "PRMS";
    private String BACKUPFILE = "prms-gdrive-backup.db";

    private DriveId mPRMS_folderId = null;
    private DriveId mPRMS_dbFileId = null;
    private Date    mPRMS_lfDbDate;
    private Date    mPRMS_gdDbDate;
    private boolean mSaveThreadRunning = false;

    // M e t h o d s
    protected void connectToGoogleDrive(android.content.Context context) {
        mContext = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
        Log.i(TAG, "GoogleDrive connection request issued");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showMessage("GoogleDrive connected");
        Log.i(TAG, "GoogleDrive connected");

        Drive.DriveApi.requestSync(getGoogleApiClient());
        DriveFolder folder = Drive.DriveApi.getRootFolder(getGoogleApiClient());
        folder.listChildren(getGoogleApiClient())
                .setResultCallback(RootfolderQueryCallBack);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleDrive connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, "GoogleDrive connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(GoogleApiAvailability.getInstance().getErrorString(0));
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        } else {
            try {
                // !!!
                result.startResolutionForResult((Activity) mContext, REQUEST_CODE_RESOLVE_ERR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        }
    }

    public void disconnectFromGoogleDrive() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    final ResultCallback<DriveApi.MetadataBufferResult> RootfolderQueryCallBack = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while querying root folder");
                        Log.d(TAG, "RootfolderQueryCallback error");
                        return;
                    }

                    final MetadataBuffer buffer = result.getMetadataBuffer();
                    if (buffer == null) {
                        Log.d(TAG, "RootfolderQueryCallback MetadataBufferResult is null");
                        return;
                    }
                    if (buffer.getCount() <= 0) {
                        Log.d(TAG, "RootfolderQueryCallback MetadataBufferResult is Empty");
                    }

                    // let us do the file search in a different thread
                    new Thread() {
                        @Override
                        public void run() {
                            if (mSaveThreadRunning)
                                return;

                            mSaveThreadRunning = true;
                            Log.d(TAG, "RootfolderQueryCallback running thread!");
                            boolean app_folder_exist = false;

                            // check if PRMS folder exists
                            for (Metadata md : buffer) {
                                if (md == null || !md.isDataValid())
                                    continue;
                                if ((md.getMimeType().equals("application/vnd.google-apps.folder")) &&
                                        (md.getTitle().equals(BACKUPFOLDER))) {
                                    app_folder_exist = true;
                                    mPRMS_folderId = md.getDriveId();
                                    Log.d(TAG, BACKUPFOLDER + " folder found in Google drive!");
                                }
                            }

                            // create PRMS folder if not exist
                            if (!app_folder_exist) {
                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(BACKUPFOLDER).build();
                                mPRMS_folderId = Drive.DriveApi.getRootFolder(getGoogleApiClient())
                                        .createFolder(getGoogleApiClient(), changeSet)
                                        .await()
                                        .getDriveFolder()
                                        .getDriveId();
                                Log.d(TAG, "Creating " + BACKUPFOLDER + " folder in Google drive");
                            }
                            mSaveThreadRunning = false;
                            Log.d(TAG, "RootfolderQueryCallback Thread exited...");
                        }
                    }.start();
                }
            };


    protected void saveToGoogleDrive(Date dbdate) {
        if (mPRMS_folderId == null)
            return;

        mPRMS_lfDbDate = dbdate;

        if (!getGoogleApiClient().isConnected()) {
            getGoogleApiClient().connect();
        }

        // before we save, we need to first find out if we are going to overwrite or create
        DriveFolder folder = mPRMS_folderId.asDriveFolder();
        folder.listChildren(getGoogleApiClient()).setResultCallback(PRMSfolderQueryCallBack);
        Log.d(TAG, "saveToGoogleDrive PRMS folder query triggered...");
    }

    final private ResultCallback<DriveApi.MetadataBufferResult> PRMSfolderQueryCallBack = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                public void onResult(final DriveApi.MetadataBufferResult result) {

                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while querying PRMS folder");
                        Log.d(TAG, "PRMSfolderQueryCallback error");
                        return;
                    }

                    final MetadataBuffer buffer = result.getMetadataBuffer();
                    if (buffer == null) {
                        Log.d(TAG, "PRMSfolderQueryCallBack MetadataBufferResult is null");
                        return;
                    }

                    // let us do the file search in a different thread
                    Log.d(TAG, "PRMSfolderQueryCallback: Starting new thread...");
                    new Thread() {
                        @Override
                        public void run() {
                            boolean db_file_exist = false;

                            // get drive ID if file exist
                            for (Metadata md : buffer) {
                                if (md == null || !md.isDataValid())
                                    continue;
                                if ((md.getMimeType().equals("text/plain")) &&
                                        (md.getTitle().equals(BACKUPFILE))) {
                                    db_file_exist = true;
                                    mPRMS_dbFileId = md.getDriveId();
                                    mPRMS_gdDbDate = md.getModifiedDate();
                                    Log.d(TAG, BACKUPFILE + " found in Google drive!");
                                    break;
                                }
                            }

                            // create new file and get drive ID
                            if (!db_file_exist) {
                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(BACKUPFILE)
                                        .setMimeType("text/plain")
                                        .build();
                                mPRMS_dbFileId = mPRMS_folderId.asDriveFolder()
                                        .createFile(getGoogleApiClient(), changeSet, null)
                                        .await().getDriveFile().getDriveId();
                                mPRMS_gdDbDate = new Date(0L); // let us go back to epoch!
                                Log.d(TAG, "Creating " + BACKUPFILE + " in Google drive...");
                            }

                            // update google drive backup file if phone has got the recent updates
                            Log.d(TAG, "GoogleDrive backup file date: " + mPRMS_gdDbDate.toString());
                            if (mPRMS_gdDbDate.after(mPRMS_lfDbDate)) {
                                Log.d(TAG, "GoogleDrive backup not done! Date is later than phone!");
                            }
                            else {
                                updateGoogleDriveFile();
                            }
                            Log.d(TAG, "PRMS folder query thread exited...");
                        }
                    }.start();
                }
            };

    private void updateGoogleDriveFile() {
        Log.d(TAG, "Updating " + BACKUPFILE + "to Google drive...");
        try {
            com.google.android.gms.common.api.Status status;
            DriveFile file = mPRMS_dbFileId.asDriveFile();
            DriveApi.DriveContentsResult driveContentsResult = file.open(
                    getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();

            if (driveContentsResult.getStatus() == null) {
                Log.d(TAG, "driveContentsResult.getStatus() is null!");
                return;
            }

            if (!driveContentsResult.getStatus().isSuccess()) {
                Log.d(TAG, "Can't open Google drive database file for writing!");
                Log.d(TAG, driveContentsResult.getStatus().getStatusMessage());
                return;
            }

            String appDBpath = "/data/" + PACKAGE_NAME + "/databases/" + MAIN_DATABASE;
            File data = Environment.getDataDirectory();
            File db_file = new File(data, appDBpath);

            if (db_file != null) {
                DriveContents driveContents = driveContentsResult.getDriveContents();
                OutputStream outputStream = driveContents.getOutputStream();
                FileInputStream inputStream = new FileInputStream(db_file);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                status = driveContents.commit(getGoogleApiClient(), null).await();
                if (status.isSuccess()) {
                    Log.d(TAG, "Success!! Database copied to Google drive");
                } else {
                    Log.d(TAG, "Google drive database copy fail! Reason: " + status.getStatusMessage());
                }
            } else {
                Log.d(TAG, "Can't access local database file for backup to Google drive!");
            }
        } catch (Exception e) {
            Log.d(TAG, "updateGoogleDriveFile(): Got exception! " + e.toString());
        }

    }

    public void showMessage(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }
}
