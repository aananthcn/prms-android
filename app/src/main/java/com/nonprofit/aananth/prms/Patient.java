package com.nonprofit.aananth.prms;

import android.util.Log;

import java.util.Date;

/**
 * Created by aananth on 09/04/17.
 */

public class Patient {
    String Name;
    String Phone;
    String Email;
    String Id;

    public Patient(String name, String phone, String email) {
        Name = name;
        Phone = phone;
        Email = email;

        Id = (name+ new Date()).replaceAll("\\s+","");
        Log.d("Patient", Id);
    }
}
