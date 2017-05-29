package com.penn.ppj.util;

import com.penn.ppj.PPApplication;

/**
 * Created by penn on 08/04/2017.
 */

public class PPWarn {
    public int code;
    public String msg;

    public PPWarn(String string) {
        code = PPApplication.ppFromString(string, "code").getAsInt();
        msg = PPApplication.ppFromString(string, "msg").getAsString();
    }
}
