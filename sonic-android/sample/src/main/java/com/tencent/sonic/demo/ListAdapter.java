package com.tencent.sonic.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.tencent.sonic.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tongzhan on 2017/8/14.
 */

public class ListAdapter extends BaseAdapter {
    public static final String PREFERENCE_URLS = "urls";
    public static final String PREFERENCE_CHECKED_INDEX = "checked_index";

    public static final int MODE_NORMAL = 1;
    public static final int MODE_EDIT = 2;
    private ArrayList<String> urls;
    private LayoutInflater mInflater;
    private int checkedIndex;
    private int mode = MODE_NORMAL;

    SharedPreferences sharedPreferences;

    public ListAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        sharedPreferences = context.getSharedPreferences("list_adapter", 0);
        init();
    }

    void init() {
        try {
            urls = (ArrayList<String>) ObjectSerializer.deserialize(sharedPreferences.getString(PREFERENCE_URLS, ObjectSerializer.serialize(new ArrayList<String>())));
            if (urls.isEmpty()) {
                urls.add("http://mc.vip.qq.com/demo/indexv3");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        checkedIndex = sharedPreferences.getInt(PREFERENCE_CHECKED_INDEX, 0);

        toggleNormalMode();
    }

    String getCheckedUrl() {
        return (checkedIndex >= 0 && checkedIndex < urls.size()) ? urls.get(checkedIndex) : null;
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
        checkedIndex = index;
        notifyDataSetChanged();
        System.out.println("checked changed");
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
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.list_item, null);
            holder.radioButton = (RadioButton) view.findViewById(R.id.radio);
            holder.textUrl = (TextView) view.findViewById(R.id.text_url);
            holder.btnDelete = (ImageButton) view.findViewById(R.id.btn_delete);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

//        holder.btnDelete.setVisibility(mode == MODE_EDIT ? View.VISIBLE : View.GONE);
        if (mode == MODE_EDIT && i == 0) {
            holder.btnDelete.setVisibility(View.INVISIBLE);
        } else {
            holder.btnDelete.setVisibility(mode == MODE_EDIT ? View.VISIBLE : View.INVISIBLE);
        }

        holder.radioButton.setVisibility(mode == MODE_EDIT ? View.GONE : View.VISIBLE);
        holder.textUrl.setText(urls.get(i));
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (i == checkedIndex) {
                    checkedIndex = 0;
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
                System.out.println("checked changed");
            }
        };
        holder.radioButton.setOnCheckedChangeListener(null);
        holder.radioButton.setChecked(checkedIndex == i);
        holder.radioButton.setOnCheckedChangeListener(listener);
        return view;
    }

    void persist() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            editor.putString(PREFERENCE_URLS, ObjectSerializer.serialize(urls));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.putInt(PREFERENCE_CHECKED_INDEX, checkedIndex);
        editor.apply();
    }

    private class ViewHolder {
        RadioButton radioButton;
        TextView textUrl;
        ImageButton btnDelete;
    }
}
