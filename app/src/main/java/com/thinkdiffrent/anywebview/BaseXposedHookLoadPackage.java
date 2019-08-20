package com.thinkdiffrent.anywebview;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;

import static com.thinkdiffrent.anywebview.XposedHelpersWraper.setTAG;


public abstract class BaseXposedHookLoadPackage implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    protected Boolean iib;
    static {
        setTAG("anywebview");
    }
}
