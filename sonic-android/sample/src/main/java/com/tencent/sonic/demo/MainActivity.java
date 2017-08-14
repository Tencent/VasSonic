/*
 * Tencent is pleased to support the open source community by making VasSonic available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package com.tencent.sonic.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.tencent.sonic.R;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSessionConfig;


/**
 *  main activity of this sample
 */
public class MainActivity extends Activity {

    public static final int MODE_DEFAULT = 0;

    public static final int MODE_SONIC = 1;

    public static final int MODE_SONIC_WITH_OFFLINE_CACHE = 2;

    private static final int PERMISSION_REQUEST_CODE_STORAGE = 1;

    private static final String DEFAULT_URL = "http://mc.vip.qq.com/demo/indexv3";
    private String DEMO_URL = DEFAULT_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        // clean up cache btn
        Button btnReset = (Button) findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SonicEngine.getInstance().cleanCache();
            }
        });

        // default btn
        Button btnDefault = (Button) findViewById(R.id.btn_default_mode);
        btnDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBrowserActivity(MODE_DEFAULT);
            }
        });

        // preload btn
        Button btnSonicPreload = (Button) findViewById(R.id.btn_sonic_preload);
        btnSonicPreload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SonicSessionConfig sessionConfig = new SonicSessionConfig.Builder().build();
                boolean preloadSuccess = SonicEngine.getInstance().preCreateSession(DEMO_URL, sessionConfig);
                Toast.makeText(getApplicationContext(), preloadSuccess ? "Preload start up success!" : "Preload start up fail!", Toast.LENGTH_LONG).show();
            }
        });

        // sonic mode load btn
        Button btnSonic = (Button) findViewById(R.id.btn_sonic);
        btnSonic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBrowserActivity(MODE_SONIC);
            }
        });

        // load sonic with offline cache
        Button btnSonicWithOfflineCache = (Button) findViewById(R.id.btn_sonic_with_offline);
        btnSonicWithOfflineCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBrowserActivity(MODE_SONIC_WITH_OFFLINE_CACHE);
            }
        });

        if (hasPermission()) {
            init();
        } else {
            requestPermission();
        }

        FloatingActionButton btnFab = (FloatingActionButton) findViewById(R.id.btn_fab);
        btnFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedFab();
            }
        });
    }

    private void clickedFab() {
        View convertView = getLayoutInflater().inflate(R.layout.dialog, null);
        ListView listView = (ListView) convertView.findViewById(R.id.listView);
        final ListAdapter listAdapter = new ListAdapter(MainActivity.this);
        listView.setAdapter(listAdapter);
        Button btnAddItem = (Button) convertView.findViewById(R.id.btn_add_item);
        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listAdapter.addNewItem();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.set_custom_url);
        builder.setView(convertView);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                listAdapter.persist();
                String url = listAdapter.getCheckedUrl();
                DEMO_URL = url == null ? DEFAULT_URL : url;
                Toast.makeText(MainActivity.this, DEMO_URL, Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void init() {
        // init sonic engine
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }
    }


    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PERMISSION_REQUEST_CODE_STORAGE == requestCode) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            } else {
                init();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startBrowserActivity(int mode) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.PARAM_URL, DEMO_URL);
        intent.putExtra(BrowserActivity.PARAM_MODE, mode);
        intent.putExtra(SonicJavaScriptInterface.PARAM_CLICK_TIME, System.currentTimeMillis());
        startActivityForResult(intent, -1);
    }

}
