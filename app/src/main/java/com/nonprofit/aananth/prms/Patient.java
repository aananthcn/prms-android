package com.nonprofit.aananth.prms;

import android.util.Log;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by aananth on 09/04/17.
 */

public class Patient implements Serializable {
    String Name;
    String Phone;
    String Email;
    String Gender;
    String Uid; // used to create tables for patient treatment records
    String Pid; // used by database module

    public Patient(String name, String phone, String email, String gender, String pid, String uid) {
        Name = name;
        Phone = phone;
        Email = email;
        Uid =  uid; // Unique ID
        Pid = pid;
        Gender = gender;
    }
}
