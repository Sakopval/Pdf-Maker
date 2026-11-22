package com.example.pdfmaker;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.example.pdfmaker.databinding.FontTypeChooserBinding;
import com.example.pdfmaker.databinding.SettingActivityBinding;
import com.example.core.FileTools;
import com.example.pdfmaker.utils.LatexSettings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {
    private SettingActivityBinding binding;
    private ActivityResultLauncher<Uri> addPkgLauncher;
    private ActivityResultLauncher<Uri> addFontLauncher;
    private final Context context = this;
    @Inject
    ThreadPoolExecutor poolExecutor;
    @Inject
    Handler handler;
    @Inject
    LatexSettings latexSettings;
    @Inject
    FileTools fileTools;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SettingActivityBinding.inflate(getLayoutInflater());
        boolean kpathsea_depug = Boolean.TRUE.equals(fileTools.getViewModel().getKpathsea_depug().getValue());
        binding.kpathseaDepugSwitch.setChecked(kpathsea_depug);
        addPkgLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), (uri)->{
            if (uri != null){
                latexSettings.copyPkg(DocumentFile.fromTreeUri(this, uri), binding.loadingLay);
            }
        });
        addFontLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), (uri)->{
            if (uri != null){
                showFontTypeDialog(uri);
            }
        });
        setContentView(binding.settingsActivity);
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.resetDirTreeLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.loadingLay.setVisibility(View.VISIBLE);
                latexSettings.setLaTeXDirectoryTree(binding.loading, binding.stateText, binding.loadingLay);
                binding.loadingLay.setVisibility(View.GONE);
            }
        });
        binding.showVersionLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poolExecutor.execute(() -> {
                    try {
                        HashMap<String, String> hashMap = latexSettings.showPdfTeXVersion();
                        handler.post(() -> {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            AlertDialog dialog = builder.create();
                            dialog.setTitle("PdfTeX Version");
                            dialog.setMessage("\n" + hashMap.get("output") + "\n");
                            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        });
                    } catch (Exception e) {
                        handler.post(() -> {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            AlertDialog dialog = builder.create();
                            dialog.setTitle("Unknown error occured");
                            dialog.setMessage(e.toString());
                            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        });
                    }
                });
            }
        });
        binding.kpathseaDepugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                fileTools.getViewModel().getKpathsea_depug().setValue(isChecked);
                poolExecutor.execute(()->{
                    File jsonFile = new File(getFilesDir(), "settings");
                    boolean created = false;
                    boolean exists = false;
                    if (!(exists = jsonFile.exists())){
                        try {
                            created = jsonFile.createNewFile();
                        }catch (Exception e){
                            Log.e("SettingsFile","", new Throwable(e));
                            Toast.makeText(context, "failed to set settings", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (exists || created){
                        fileTools.addValueToJson(jsonFile, "kpathsea_depug", String.valueOf(isChecked));
                    }
                });
            }
        });
        binding.addAPackageRely.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.settingsActivity.getContext());
                AlertDialog dialog = builder.create();
                dialog.setTitle("Note!!");
                dialog.setMessage("""
                    Please Note that:
                    1. To add a package make sure that the package is inside a folder named with the same name of the package\
                     to store it correctly.
                    2. And to add multiple packages u must put each one as above in one folder and chose this folder that contains all packages and make sure \
                     that folder only contains folders not files to store all ur packages correctly.""");
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addPkgLauncher.launch(null);
                    }
                });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
        binding.addAFontRely.setOnClickListener((v)->{
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.settingsActivity.getContext());
            AlertDialog dialog = builder.create();
            dialog.setTitle("Note!!");
            dialog.setMessage("""
                    Please Note that:
                    1. To add a Font make sure that the font is inside a folder named with the same name of the font\
                     to store it correctly.\
                 
                    2. And to add multiple fonts u must put each one as above in one folder and chose this folder that contains all fonts and make sure \
                     that main folder only contains folders not files to store all ur fonts correctly.""");
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    addFontLauncher.launch(null);
                }
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        });
    }

    private void showFontTypeDialog(Uri uri){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.addAPackageRely.getContext());
        AlertDialog dialog = builder.create();
        dialog.setTitle("Choose font type");
        FontTypeChooserBinding typeChooserBinding = FontTypeChooserBinding.inflate(getLayoutInflater());
        dialog.setView(typeChooserBinding.getRoot());
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", (d,w)->{
            if (typeChooserBinding.getRoot().getCheckedRadioButtonId() == -1){
                Toast.makeText(this, "u didn't choose a font type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (typeChooserBinding.getRoot().getCheckedRadioButtonId() == R.id.open_type){
                latexSettings.copyFont(DocumentFile.fromTreeUri(this, uri), binding.loadingLay, "opentype");
            } else {
                latexSettings.copyFont(DocumentFile.fromTreeUri(this, uri), binding.loadingLay, "truetype");
            }
        });
        dialog.show();
    }
}
