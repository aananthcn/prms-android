package com.nonprofit.aananth.prms;

import java.io.Serializable;

/**
 * Created by aananth on 14/05/17.
 */

public class Doctor implements Serializable {
    public String name;
    public String phone;
    public String email;
    public int id;

    public Doctor(String n, String p, String e) {
        name = n;
        phone = p;
        email = e;
        id = 0;
    }

    public Doctor(String n, String p, String e, int i) {
        name = n;
        phone = p;
        email = e;
        id = i;
    }
}
