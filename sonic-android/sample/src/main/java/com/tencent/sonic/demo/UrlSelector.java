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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.tencent.sonic.R;


public class UrlSelector {
    static void launch(final Context context, final UrlListAdapter urlListAdapter, final OnUrlChangedListener listener) {
        urlListAdapter.init();
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_url, null);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(urlListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                urlListAdapter.setChecked(position);
            }
        });

        final ImageButton btnAddItem = (ImageButton) view.findViewById(R.id.btn_add_item);
        btnAddItem.setActivated(false);

        final EditText textNewUrl = (EditText) view.findViewById(R.id.text_new_url);
        textNewUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                btnAddItem.setActivated(Patterns.WEB_URL.matcher(s.toString()).matches());
            }
        });

        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnAddItem.isActivated()) {
                    urlListAdapter.addNewItem(textNewUrl.getText().toString());
                    textNewUrl.setText(R.string.http_prefix);
                    textNewUrl.setSelection(textNewUrl.getText().length());
                } else {
                    Toast.makeText(context, R.string.illegal_url, Toast.LENGTH_SHORT).show();
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.set_custom_url);
        builder.setCancelable(false);
        builder.setView(view);
        builder.setPositiveButton(R.string.close, null);
        final AlertDialog alertDialog = builder.create();

        final View viewAddItem = view.findViewById(R.id.add_item);
        final Button btnEdit = (Button) view.findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnEdit.setVisibility(View.INVISIBLE);
                urlListAdapter.toggleEditMode();
                viewAddItem.setVisibility(View.VISIBLE);

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.finish);
            }
        });

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (okButton.getText().equals(context.getString(R.string.finish))) {
                            urlListAdapter.toggleNormalMode();
                            viewAddItem.setVisibility(View.GONE);
                            btnEdit.setVisibility(View.VISIBLE);
                            okButton.setText(R.string.close);
                        } else if (okButton.getText().equals(context.getString(R.string.close))) {
                            urlListAdapter.persist();
                            listener.urlChanged(urlListAdapter.getCheckedUrl());
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        alertDialog.show();

        // prevent keyboard from not showing
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    interface OnUrlChangedListener {
        void urlChanged(String url);
    }
}
