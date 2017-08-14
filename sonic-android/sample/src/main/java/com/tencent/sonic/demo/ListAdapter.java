package com.tencent.sonic.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;

import com.tencent.sonic.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tongzhan on 2017/8/14.
 */

public class ListAdapter extends BaseAdapter {
    public static final String PREFERENCE_URLS = "urls";
    public static final String PREFERENCE_CHECKED_INDEX = "checked_index";
    private ArrayList<String> urls;
    private LayoutInflater mInflater;
    private int checkedIndex;

    SharedPreferences sharedPreferences;

    public ListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        sharedPreferences = context.getSharedPreferences("list_adapter", 0);
        try {
            urls = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString(PREFERENCE_URLS, ObjectSerializer.serialize(new ArrayList<String>())));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        checkedIndex = sharedPreferences.getInt(PREFERENCE_CHECKED_INDEX, -1);
    }

    public String getCheckedUrl() {
        return (checkedIndex >= 0 && checkedIndex < urls.size()) ? urls.get(checkedIndex) : null;
    }

    public void addNewItem() {
        urls.add("");
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        // here we won't use ViewHolder.
        // because the conflicts between RadioButtons and TextWatcher.
        view = mInflater.inflate(R.layout.list_item, null);
        EditText textUrl = (EditText) view.findViewById(R.id.text_url);
        RadioButton radioButton = (RadioButton) view.findViewById(R.id.radio);
        Button btnDelete = (Button) view.findViewById(R.id.btn_delete);
        textUrl.setText(urls.get(i));
        textUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                urls.set(i, editable.toString());
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (i == checkedIndex) {
                    checkedIndex = -1;
                }
                urls.remove(i);
                notifyDataSetChanged();
            }
        });
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    checkedIndex = i;
                    notifyDataSetChanged();
                }
            }
        };
        radioButton.setOnCheckedChangeListener(null);
        radioButton.setChecked(checkedIndex == i);
        radioButton.setOnCheckedChangeListener(listener);
        return view;
    }

    public void persist() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            editor.putString(PREFERENCE_URLS, ObjectSerializer.serialize(urls));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.putInt(PREFERENCE_CHECKED_INDEX, checkedIndex);
        editor.apply();
    }
}
