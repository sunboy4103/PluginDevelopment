//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.tcl.dc.internal;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import com.tcl.dc.ConfigurationCrawler;
import com.tcl.dc.PLog;
import com.tcl.dc.PluginContext;
import com.tcl.dc.PluginStub;
import com.tcl.dc.ConfigurationCrawler.Callback;
import com.tcl.dc.internal.RemoteConfigurationCrawler.ConfigurationResult;
import com.tcl.dc.utils.CollectionUtils;
import com.tcl.dc.utils.FileUtils;
import com.tcl.dc.utils.MD5Util;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

public class LocalConfigurationCrawler implements ConfigurationCrawler {
    private static final String LOCAL_PLUGIN_DIR = ".plugins";
    private static final String PLUGIN_SUFFIX = ".tdp";
    private static final String CONFIG_MANIFEST_FILE = "plugins.json";
    private static final int LOCAL_PLUGIN_ID_BASE = 200000;
    final PluginContext mContext;
    Handler mHandler;

    public LocalConfigurationCrawler(PluginContext context) {
        this.mContext = context;
        this.mHandler = new Handler(context.getIoLooper());
    }

    private void checkResult(File dir, ConfigurationResult cr) throws Exception {
        if (cr == null) {
            throw new NullPointerException("configuration result is null!");
        } else if (!CollectionUtils.isEmpty(cr.plugins)) {
            ArrayList<PluginStub> removes = new ArrayList();
            Iterator var4 = cr.plugins.iterator();

            while (true) {
                while (var4.hasNext()) {
                    PluginStub stub = (PluginStub) var4.next();
                    if (stub.id <= 200000) {
                        removes.add(stub);
                        PLog.w("plugin %d path empty or md5 empty", new Object[] {Integer.valueOf(stub.id)});
                    } else if (!TextUtils.isEmpty(stub.path) && !TextUtils.isEmpty(stub.md5)) {
                        File file = new File(dir, stub.path);
                        if (file.exists() && file.getName().endsWith(".tdp") && file.length() == stub.size) {
                            String md5 = MD5Util.getFileMD5(file.getAbsolutePath());
                            if (!stub.md5.equalsIgnoreCase(md5)) {
                                removes.add(stub);
                                PLog.w("plugin %d md5(%s) not match, calc md5 is %s",
                                        new Object[] {Integer.valueOf(stub.id), stub.md5, md5});
                            } else {
                                stub.path = file.getAbsolutePath();
                                stub.ready = true;
                            }
                        } else {
                            removes.add(stub);
                            PLog.w("plugin %d file not exist or length not match",
                                    new Object[] {Integer.valueOf(stub.id)});
                        }
                    } else {
                        removes.add(stub);
                        PLog.w("plugin %d path empty or md5 empty", new Object[] {Integer.valueOf(stub.id)});
                    }
                }

                if (removes.size() > 0) {
                    cr.plugins.removeAll(removes);
                }

                return;
            }
        }
    }

    public int crawlConfiguration(final Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("the parameter callback invalid!");
        } else {
            Context context = this.mContext.getApplicationContext();
            File[] dirs = ContextCompat.getExternalFilesDirs(context, ".plugins");
            if (dirs != null && dirs.length != 0) {
                File dir = dirs[0];
                File[] files = dir.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        if (pathname.length() <= 100L) {
                            return false;
                        } else {
                            String name = pathname.getName();
                            return name.toLowerCase().endsWith(".tdp");
                        }
                    }
                });
                if (files != null && files.length != 0) {
                    File file = new File(dir, "plugins.json");
                    if (!file.exists()) {
                        PLog.w("local plugins manifest file %s not exists.", new Object[] {"plugins.json"});
                        return -1;
                    } else {
                        try {
                            String s = FileUtils.readString(file);
                            if (TextUtils.isEmpty(s)) {
                                PLog.w("manifest file %s content empty.", new Object[] {"plugins.json"});
                                return -12;
                            } else {
                                JSONObject jsonObject = new JSONObject(s);
                                final ConfigurationResult cr = RemoteConfigurationCrawler.parseResult(jsonObject);
                                this.checkResult(dir, cr);
                                this.mHandler.post(new Runnable() {
                                    public void run() {
                                        callback.onConfigurationResult(0, cr.plugins, cr.ts);
                                    }
                                });
                                return 0;
                            }
                        } catch (JSONException var10) {
                            PLog.printStackTrace(var10);
                            return -12;
                        } catch (IOException var11) {
                            PLog.printStackTrace(var11);
                            return -12;
                        } catch (Exception var12) {
                            PLog.printStackTrace(var12);
                            return -12;
                        }
                    }
                } else {
                    PLog.w("none local plugin exists.", new Object[0]);
                    return -1;
                }
            } else {
                PLog.w("local plugin dir not exists.", new Object[0]);
                return -1;
            }
        }
    }

    public void cancel() {}
}
