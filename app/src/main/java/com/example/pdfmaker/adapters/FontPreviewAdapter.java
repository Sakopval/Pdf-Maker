package com.example.pdfmaker.adapters;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pdfmaker.R;
import com.example.pdfmaker.databinding.FontPreviewItemBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Inflater;

public class FontPreviewAdapter extends RecyclerView.Adapter<FontPreviewAdapter.VH> {
    private HashMap<String, Typeface> fonts;
    private ArrayList<String> names;
    private ArrayList<Typeface> typefaces;

    public FontPreviewAdapter(HashMap<String, Typeface> fonts) {
        this.fonts = fonts;
        names = new ArrayList<>(fonts.keySet());
        typefaces = new ArrayList<>(fonts.values());
    }

    class VH extends RecyclerView.ViewHolder{
        private TextView name;
        public VH(@NonNull View itemView) {
            super(itemView);
            FontPreviewItemBinding binding = FontPreviewItemBinding.bind(itemView);
            name = binding.item;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.font_preview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        int pos = holder.getAbsoluteAdapterPosition();
        Typeface typeface = typefaces.get(pos);
        String  name = names.get(pos);
        if (typeface != null){
            holder.name.setTypeface(typeface);
        }
        holder.name.setText(name);
    }

    @Override
    public int getItemCount() {
        return fonts.size();
    }
}
