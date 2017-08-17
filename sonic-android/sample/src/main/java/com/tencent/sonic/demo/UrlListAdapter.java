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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.tencent.sonic.R;

import java.util.ArrayList;
import java.util.Arrays;


public class UrlListAdapter extends BaseAdapter {
    public static final String PREFERENCE_URLS = "urls";
    public static final String PREFERENCE_CHECKED_INDEX = "checked_index";

    public static final int MODE_NORMAL = 1;
    public static final int MODE_EDIT = 2;

    private static final String DEFAULT_URL = "http://mc.vip.qq.com/demo/indexv3";

    private ArrayList<String> urls;
    private LayoutInflater mInflater;
    private int checkedIndex;
    private int mode = MODE_NORMAL;

    private SharedPreferences sharedPreferences;

    public UrlListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        sharedPreferences = context.getSharedPreferences("list_adapter", 0);
        init();
    }

    void init() {
        restore();
        toggleNormalMode();
    }

    private void restore() {
        urls = deserialize(sharedPreferences.getString(PREFERENCE_URLS, ""));
        if (urls.isEmpty()) {
            urls.add(DEFAULT_URL);
        }

        checkedIndex = sharedPreferences.getInt(PREFERENCE_CHECKED_INDEX, 0);
    }

    private String serialize(ArrayList<String> stringArrayList) {
        return TextUtils.join(";", stringArrayList);
    }

    private ArrayList<String> deserialize(String serializedString) {
        if (serializedString.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(serializedString.split(";")));
    }

    String getCheckedUrl() {
        return (checkedIndex >= 0 && checkedIndex < urls.size()) ? urls.get(checkedIndex) : DEFAULT_URL;
    }

    void addNewItem(String url) {
        urls.add(url);
        notifyDataSetChanged();
    }

    void toggleNormalMode() {
        mode = MODE_NORMAL;
        notifyDataSetChanged();
    }

    void toggleEditMode() {
        mode = MODE_EDIT;
        notifyDataSetChanged();
    }

    void setChecked(int index) {
        if (mode == MODE_NORMAL) {
            checkedIndex = index;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_item_url, null);
            holder.radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            holder.textUrl = (TextView) convertView.findViewById(R.id.text_url);
            holder.btnDelete = (ImageButton) convertView.findViewById(R.id.btn_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mode == MODE_EDIT && position == 0) {
            holder.btnDelete.setVisibility(View.INVISIBLE);
        } else {
            holder.btnDelete.setVisibility(mode == MODE_EDIT ? View.VISIBLE : View.INVISIBLE);
        }

        if (mode == MODE_EDIT) {
            holder.radioButton.setVisibility(View.GONE);
        } else {
            holder.radioButton.setChecked(checkedIndex == position);
            holder.radioButton.setVisibility(View.VISIBLE);
        }

        holder.textUrl.setText(urls.get(position));

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (position == checkedIndex) {
                    checkedIndex = 0;
                }
                urls.remove(position);
                notifyDataSetChanged();
            }
        });

        return convertView;
    }

    void persist() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREFERENCE_URLS, serialize(urls));
        editor.putInt(PREFERENCE_CHECKED_INDEX, checkedIndex);
        editor.apply();
    }

    private class ViewHolder {
        RadioButton radioButton;
        TextView textUrl;
        ImageButton btnDelete;
    }
}
