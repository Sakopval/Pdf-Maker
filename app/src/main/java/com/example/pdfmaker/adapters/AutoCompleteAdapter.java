package com.example.pdfmaker.adapters;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.core.db.WorkSpaceEntity;
import com.example.pdfmaker.databinding.AutocompleteItemBinding;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AutoCompleteAdapter implements Filterable, ListAdapter {

    private MutableLiveData<List<WorkSpaceEntity>> list = new MutableLiveData<>(new ArrayList<>());
    private AutoCompleteTextView autoCompleteTextView = null;
    private LifecycleOwner owner;
    @Inject
    protected AutoCompleteAdapter() {
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                filterResults.values = constraint;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                results.values = constraint;
            }
        };
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return list.getValue().size();
    }

    @Override
    public Object getItem(int position) {
        return list.getValue().get(position).getName();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AutocompleteItemBinding binding;
        if (convertView == null){
            binding = AutocompleteItemBinding.inflate(LayoutInflater.from(parent.getContext()));
        }else {
            binding = AutocompleteItemBinding.bind(convertView);
        }
        binding.item.setText(((String) getItem(position)));
        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (autoCompleteTextView != null){
                    autoCompleteTextView.setText(list.getValue().get(position).getName());
                }
            }
        });
        return binding.getRoot();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return list.getValue().isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    public void setList(MutableLiveData<List<WorkSpaceEntity>> list) {
        this.list = list;
    }

    public void setAutoCompleteTextView(AutoCompleteTextView autoCompleteTextView) {
        this.autoCompleteTextView = autoCompleteTextView;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return ListAdapter.super.getAutofillOptions();
    }

    public void setOwner(LifecycleOwner owner) {
        this.owner = owner;
    }
}
