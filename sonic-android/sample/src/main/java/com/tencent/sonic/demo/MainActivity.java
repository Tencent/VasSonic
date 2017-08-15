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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.tencent.sonic.R;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSessionConfig;


/**
 * main activity of this sample
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
                Toast.makeText(MainActivity.this, DEMO_URL, Toast.LENGTH_LONG).show();
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
        final ListAdapter listAdapter = new ListAdapter(MainActivity.this);
        FloatingActionButton btnFab = (FloatingActionButton) findViewById(R.id.btn_fab);
        btnFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedFab(listAdapter);
            }
        });
        DEMO_URL = listAdapter.getCheckedUrl();
    }

    private void clickedFab(final ListAdapter listAdapter) {
        listAdapter.init();
        final View convertView = getLayoutInflater().inflate(R.layout.dialog, null);
        final Button btnEdit = (Button) convertView.findViewById(R.id.btn_edit);
        final View viewAddItem = convertView.findViewById(R.id.add_item);

        ListView listView = (ListView) convertView.findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("clicked");
                listAdapter.setChecked(i);
            }
        });
        final ImageButton btnAddItem = (ImageButton) convertView.findViewById(R.id.btn_add_item);
        btnAddItem.setActivated(false);
        final EditText textNewUrl = (EditText) convertView.findViewById(R.id.text_new_url);
        textNewUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                btnAddItem.setActivated(Patterns.WEB_URL.matcher(s.toString()).matches());

            }
        });
        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnAddItem.isActivated()) {
                    listAdapter.addNewItem(textNewUrl.getText().toString());
                    textNewUrl.setText("http://");
                    textNewUrl.setSelection(textNewUrl.getText().length());
                } else {
                    Toast.makeText(MainActivity.this, "不合法的URL", Toast.LENGTH_SHORT).show();
                }
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.set_custom_url);
        builder.setCancelable(false);
        builder.setView(convertView);

        builder.setPositiveButton("关闭", null);
        final AlertDialog alertDialog = builder.create();

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnEdit.setVisibility(View.INVISIBLE);
                listAdapter.toggleEditMode();
                viewAddItem.setVisibility(View.VISIBLE);
                okButton.setText("完成");
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (okButton.getText().equals("完成")) {
                            listAdapter.toggleNormalMode();
                            viewAddItem.setVisibility(View.GONE);
                            btnEdit.setVisibility(View.VISIBLE);
                            okButton.setText("关闭");
                        } else if (okButton.getText().equals("关闭")) {
                            listAdapter.persist();
                            DEMO_URL = listAdapter.getCheckedUrl();
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

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
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE);
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
