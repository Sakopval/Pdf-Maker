package com.example.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Handler;
import android.system.Os;
import android.system.StructStat;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.example.core.db.AppDataBase;
import com.example.core.db.FileEntity;
import com.example.core.db.FontEntity;
import com.example.core.db.PkgEntity;
import com.example.core.db.WorkSpaceEntity;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.Iterators;
import org.apache.commons.lang3.CharSet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;


@Singleton
public class FileTools {
    private final static int BUFFER_SIZE = 8192;
    public final String TYPE_VALID = "valid";
    public final String TYPE_INVALID = "invalid";
    private final Context context;
    private LinearProgressIndicator progressIndicator;
    private AlertDialog dialog;
    private final Handler handler;
    private final AppDataBase appDataBase;
    private final ThreadPoolExecutor poolExecutor;
    private MainActivityViewModel viewModel;

    @Inject
    public FileTools(@ApplicationContext Context context, Handler handler, AppDataBase dataBase, ThreadPoolExecutor poolExecutor) {
        this.context = context;
        this.handler = handler;
        this.appDataBase = dataBase;
        this.poolExecutor = poolExecutor;
    }


    public String checkDocTree(DocumentFile document) {
        if (document != null && document.isDirectory()) {
            return TYPE_VALID;
        }
        return TYPE_INVALID;
    }

    public File mkDirInTarget(DocumentFile documentFile, File target) {
        File file;
        String name = documentFile.getName();
        if (name != null && !name.isEmpty()) {
            file = new File(target, name);
            file.mkdirs();
        } else {
            file = new File(target, "Folder");
            file.mkdirs();
        }
        return file;
    }

    public void copyRecursive(DocumentFile documentFile, File target) throws Exception {
        if (checkDocTree(documentFile).equals(TYPE_VALID)) {
            for (DocumentFile file : documentFile.listFiles()) {
                //file is a directory so make a dir in target with the same name
                if (file.isDirectory()) {
                    File subDir = mkDirInTarget(file, target);
                    copyRecursive(file, subDir);
                } else {
                    //it is a file
                    copyFile(file, target);
                }
            }
        } else {
            throw new Exception("Not a directory");
        }
    }

    @SuppressLint("Range")
    public void copyFile(DocumentFile file, File target) throws Exception {
        try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri());
             FileOutputStream outputStream = new FileOutputStream(new File(target, file.getName() != null ? file.getName() :
                     String.valueOf(System.currentTimeMillis())))) {
            if (inputStream == null) {
                throw new IOException("couldn't open: " + file.getUri());
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
    }

    public int countTotalFiles(DocumentFile documentFile) {
        int total = 0;
        for (DocumentFile file : documentFile.listFiles()) {
            if (file.isDirectory()) {
                total += countTotalFiles(file);
            } else {
                total++;
            }
        }
        return total;
    }

    public void setDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public void setProgressIndicator(LinearProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public String getHTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        return formatter.format(time);
    }

    public String getHSize(long size) {
        if (-1000 < size && size < 1000) {
            return size + " B";
        }
        CharacterIterator si = new StringCharacterIterator("KMGTPE");
        while (size <= -999_950 || size >= 999_950) {
            size /= 1000;
            si.next();
        }
        return String.valueOf(size / 1000) + " " + si.current() + "B";
    }

    public String getSizeOfWorkSpace(WorkSpaceEntity workSpace) {
        List<FileEntity> fileEntities = appDataBase.workSpaceDao().getFilesForWorkSpace(workSpace.getWorkSpaceId());
        long totalSize = 0;
        for (FileEntity file : fileEntities) {
            totalSize += file.getSize();
        }
        return getHSize(totalSize);
    }

    @WorkerThread
    public String extractLaTeX(Object o, @Nullable LoadingIndicator loadingIndicator, File targetDir, boolean hideLoading) throws Exception {
        String createdFolderName = "null";
        if (loadingIndicator != null) {
            handler.post(() -> {
                loadingIndicator.setVisibility(View.VISIBLE);
                loadingIndicator.show();
            });
        }
        Thread.sleep(4000);
        InputStream compressorStream;
        if (o instanceof Uri) {
            DocumentFile latexFile = DocumentFile.fromSingleUri(context, ((Uri) o));
            if (latexFile.getName().endsWith(".xz") || latexFile.getName().endsWith(".XZ")) {
                compressorStream = new XZCompressorInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(((Uri) o))));
            } else {
                compressorStream = new GzipCompressorInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(((Uri) o))));
            }
        } else if (o instanceof AssetManager) {
            compressorStream = new GzipCompressorInputStream(new BufferedInputStream(((AssetManager) o).open("texmf.tar.gz")));
        } else {
            if (loadingIndicator != null) {
                handler.post(() -> {
                    loadingIndicator.hide();
                    loadingIndicator.setVisibility(View.GONE);
                });
            }
            throw new Exception("wrongObjectType this method only takes Uri and AssetManager as a first argument");
        }

        try (TarArchiveInputStream i = new TarArchiveInputStream(compressorStream)) {
            TarArchiveEntry tarArchiveEntry;
            while ((tarArchiveEntry = i.getNextEntry()) != null) {
                File destFile = new File(targetDir, tarArchiveEntry.getName());
                Log.d("ExtractingArchive", "DestFileToCreate is: ", new Throwable(destFile.getCanonicalPath() + "\n" + destFile.getAbsolutePath()));
                //not related to extraction it just tells what the name of the first folder that will contain the archive
                if (createdFolderName.equals("null")) {
                    createdFolderName = tarArchiveEntry.getName();
                }

                //prevents zip-slips
                String canonicalTargetlPath = targetDir.getCanonicalPath();
                String canonicalDestFile = destFile.getCanonicalPath();
                if (!canonicalDestFile.startsWith(canonicalTargetlPath + File.separator)) {
                    Log.e("ExtractingArchive", "Failed with message", new SecurityException("EntryOutsideTargetDir" + targetDir.getName() + "DestFile: " + canonicalDestFile
                            + "\n TargetDir: " + canonicalTargetlPath));
                }

                if (tarArchiveEntry.isDirectory()) {
                    //if  the file to be extracted is directory create it
                    boolean created = destFile.mkdirs();
                    Log.d("ExtractingArchive", created + " created destFile is Dir and name is: ", new Throwable(destFile.getName()));
                } else if (tarArchiveEntry.isSymbolicLink()) {
                    Log.d("ExtractingArchive", "destFile is Symlink and name is: ", new Throwable(destFile.getName()));
                    if (destFile.exists()) {
                        boolean deleted = destFile.delete();
                        Log.d("ExtractingArchive", deleted + " deleted destFile is Symlink and name is: ", new Throwable(destFile.getName()));
                    }
                    try {
                        Os.symlink(tarArchiveEntry.getLinkName().startsWith("/")
                                ? tarArchiveEntry.getLinkName().substring(1) :
                                tarArchiveEntry.getLinkName(), destFile.getAbsolutePath());
                        //check the symlink
                        StructStat stat = Os.lstat(destFile.getAbsolutePath());
                        Log.i("LinkStat", destFile.getName() + " created and stat is: ", new Throwable(
                                "Mode: " + Integer.toOctalString(stat.st_mode)
                                        + "Size: " + stat.st_size
                                        + "Target: " + Os.readlink(destFile.getAbsolutePath())
                        ));
                    } catch (Exception e) {
                        Log.w("ExtractingArchive", "Failed to construct symlink: " + tarArchiveEntry.getName(), e);
                    }
                } else if (tarArchiveEntry.isLink()) {
                    Log.d("ExtractingArchive", "destFile is Link and name is: ", new Throwable(destFile.getName()));
                    if (destFile.exists()) {
                        boolean deleted = destFile.delete();
                        Log.d("ExtractingArchive", deleted + " deleted destFile is Link and name is: ", new Throwable(destFile.getName()));
                    }
                    try {
                        Os.link(tarArchiveEntry.getLinkName(), destFile.getAbsolutePath());
                        //check the symlink
                        StructStat stat = Os.lstat(destFile.getAbsolutePath());
                        Log.i("LinkStat", destFile.getName() + " created and stat is: ", new Throwable(
                                "Mode: " + Integer.toOctalString(stat.st_mode)
                                        + "Size: " + stat.st_size
                                        + "Target: " + Os.readlink(destFile.getAbsolutePath())
                        ));
                    } catch (Exception e) {
                        Log.w("ExtractingArchive", "Failed to construct Link: " + tarArchiveEntry.getName(), e);
                    }
                } else {
                    File parent = destFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
                        byte[] bytes = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = i.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, read);
                        }
                    }
                    destFile.setExecutable(true, false);

                }
            }
        } finally {
            if (loadingIndicator != null && hideLoading) {
                handler.post(() -> {
                    loadingIndicator.hide();
                    loadingIndicator.setVisibility(View.GONE);
                });
            }
        }
        return createdFolderName;
    }

    public void unpackWithTar(DocumentFile tarFile, File targetDir) throws Exception {
        Log.e("ExtractingWithTar", "targetCreated: " + targetDir.getName());
        copyFile(tarFile, context.getCacheDir());
        Log.e("ExtractingWithTar", "fileToCopy: " + context.getCacheDir() + File.separator + tarFile.getName());
        ProcessBuilder pb = new ProcessBuilder(
                "tar",
                "-xzf", context.getCacheDir() + File.separator + tarFile.getName(),
                "-C", targetDir.getAbsolutePath(),
                "--no-same-owner"
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        String s;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((s = reader.readLine()) != null) {
            output.append(s);
        }

        int exitCode = process.waitFor();
        Log.e("ExtractingWithTar", "exitCode " + exitCode);
        Log.e("ExtractingWithTar", "output:\n " + output.toString());
    }

    public void addFileToDB(FileEntity file) {
        poolExecutor.execute(() -> {
            //check if it exists
            WorkSpaceEntity workSpace = appDataBase.workSpaceDao().getWorkSpaceById(file.getWorkSpaceOwnerId());
            boolean exists = fileEntityExists(file, workSpace.getWorkSpaceId());
            //add or update it
            if (exists) {
                //if file exists update it
                appDataBase.workSpaceDao().updateFileEntity(file);
                //update its workspace
                workSpace.setLastModified(System.currentTimeMillis());
                appDataBase.workSpaceDao().updateWorkSpace(workSpace);
            } else {
                //if doesn't exist
                appDataBase.workSpaceDao().insertFile(file);
                //update its workspace
                workSpace.setLastModified(System.currentTimeMillis());
                workSpace.setFilesNumber(workSpace.getFilesNumber() + 1);
            }
            updateViewModel();
        });
    }

    public void updateFile(Object file, boolean renameOnDisk, @Nullable String oldPath) {
        //this can be used for renaming or changing something in file
        poolExecutor.execute(() -> {
            if (file instanceof FileEntity) {
                try {
                    File newfile = new File(((FileEntity) file).getAbsolutePath());
                    if (renameOnDisk) {
                        File old = new File(oldPath);
                        if (old.renameTo(newfile)) {
                            Log.d("renaming", "Success");
                            ((FileEntity) file).setLastModified(newfile.lastModified());
                        }
                    }
                    //no need to check if it exists we are just renaming
                    WorkSpaceEntity workSpace = appDataBase.workSpaceDao().getWorkSpaceById(((FileEntity) file).getWorkSpaceOwnerId());
                    //add or update it we just need to update
                    //update it
                    appDataBase.workSpaceDao().updateFileEntity(((FileEntity) file));
                    //update its workspace
                    workSpace.setLastModified(System.currentTimeMillis());
                    appDataBase.workSpaceDao().updateWorkSpace(workSpace);
                    updateViewModel();
                } catch (Exception e) {
                    Log.e("Updating", "failed", e);
                }
            } else if (file instanceof WorkSpaceEntity) {
                try {
                    File newfile = new File(((WorkSpaceEntity) file).getAbsolutePath());
                    if (renameOnDisk) {
                        File old = new File(oldPath);
                        if (old.renameTo(newfile)) {
                            Log.d("renaming", "Success");
                        }
                    }
                    //no need to check if it exists we are just renaming
                    WorkSpaceEntity workSpace = ((WorkSpaceEntity) file);
                    //add or update it we just need to update
                    //update its workspace
                    workSpace.setLastModified(System.currentTimeMillis());
                    //update workspace files first
                    appDataBase.workSpaceDao().renameWorkSpaceFiles(workSpace.getName(), workSpace.getWorkSpaceId(), workSpace.getAbsolutePath());
                    appDataBase.workSpaceDao().updateWorkSpace(workSpace);
                    updateViewModel();
                } catch (Exception e) {
                    Log.e("Updating", "failed", e);
                }
            }
        });
    }

    public void deleteFileFromDB(FileEntity file) {
        //delete it on disk
        File fileToDelete = new File(((FileEntity) file).getAbsolutePath());
        //make sure it is deleted
        if (!fileToDelete.delete()) {
            Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
            return;
        }
        //no need to check if it exists we are just deleting
        WorkSpaceEntity workSpace = appDataBase.workSpaceDao().getWorkSpaceById(((FileEntity) file).getWorkSpaceOwnerId());
        //add or update it we just need to delete
        //delete it
        appDataBase.workSpaceDao().deleteFile(((FileEntity) file));
        //update its workspace
        workSpace.setLastModified(System.currentTimeMillis());
        workSpace.setFilesNumber(workSpace.getFilesNumber() - 1);
        appDataBase.workSpaceDao().updateWorkSpace(workSpace);
        updateViewModel();
    }

    public void deleteWorkSpace(WorkSpaceEntity workSpace) {
        //delete it on disk
        File wsToDelete = new File(workSpace.getAbsolutePath());
        //make sure it is deleted
        if (deleteAllFile(wsToDelete)) {
            //no need to check if it exists we are just deleting
            //delete it
            appDataBase.workSpaceDao().deleteWorkSpaceWithFiles(workSpace);
            //no need to update any thing
        }
        updateViewModel();
    }

    public void deletePkg(PkgEntity pkg){
        File file = new File(pkg.getPath());
        if (!file.isDirectory()){
            handler.post(()->{
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        if (file.listFiles() == null){
            handler.post(()->{
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        boolean deleted = deleteAllFile(file);
        if (deleted){
            appDataBase.workSpaceDao().deletePkg(pkg);
            viewModel.getPkgs().postValue(appDataBase.workSpaceDao().getAllPkgs());
        }
        Log.d("DeletingPkg",deleted+" "+pkg.getName());

    }
    public void deleteFont(FontEntity font){
        File file = new File(font.getPath());
        if (!file.isDirectory()){
            handler.post(()->{
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        if (file.listFiles() == null){
            handler.post(()->{
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        boolean deleted = deleteAllFile(file);
        if (deleted){
            appDataBase.workSpaceDao().deleteFont(font);
            viewModel.getFonts().postValue(appDataBase.workSpaceDao().getAllFonts());
        }
        Log.d("DeletingFont",deleted+" "+font.getName());

    }

    public void setViewModel(MainActivityViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void updateViewModel() {
        viewModel.setPdfsList(appDataBase.workSpaceDao().getAllPdfs());
        viewModel.setTexList(appDataBase.workSpaceDao().getAllTex());
        viewModel.setWorkSpaces(appDataBase.workSpaceDao().getAllWorkSpaces());
        viewModel.setLogsList(appDataBase.workSpaceDao().getAllLogs());
        viewModel.getAllFiles().postValue(appDataBase.workSpaceDao().getAllFiles());

    }

    public boolean fileEntityExists(Object file, long wsID) {
        boolean exists = false;

        if (file instanceof File) {
            ArrayList<FileEntity> fileEntities = new ArrayList<>(viewModel.getAllFiles().getValue());
            for (FileEntity f : fileEntities) {
                if (f.getName().equals(((File) file).getName())) {
                    exists = true;
                    break;
                }
            }
        } else if (file instanceof FileEntity) {
            ArrayList<FileEntity> fileEntities = new ArrayList<>(appDataBase.workSpaceDao().getAllFiles());
            for (FileEntity f : fileEntities) {
                if (f.getAbsolutePath().equals(((FileEntity) file).getAbsolutePath()) && f.getWorkSpaceOwnerId() == wsID) {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    private boolean deleteAllFile(File file) {
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                if (!file1.isDirectory()) {
                    if (file1.delete()) {
                        Log.d("deletingFile", "succeeded to delete " + file1.getName(), new Throwable());
                    } else {
                        Log.e("deletingFile", "Failed to delete " + file1.getName(), new Throwable());
                    }

                } else {
                    deleteAllFile(file1);
                }
            }
            if (file.delete()) {
                Log.d("deletingDir", "succeeded to delete " + file.getName(), new Throwable());
            } else {
                Log.e("deletingDir", "Failed to delete " + file.getName(), new Throwable());
            }
        } else {
            file.delete();
        }
        return true;
    }

    public MainActivityViewModel getViewModel() {
        return viewModel;
    }

    public String readFile(File file) {
        Log.d("ReadingFile", "starting to read " + file.getAbsolutePath());

        if (file.exists()) {
            Log.d("ReadingFile", "checking file exists or not and it exists");
            if (file.canRead()) {
                Log.d("ReadingFile", "checking file can be read or not and it can be read");
                try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder builder = new StringBuilder();
                    String s;
                    while ((s = reader.readLine()) != null) {
                        builder.append(s).append("\n");
                    }

                    return builder.toString();
                } catch (Exception e) {
                    Log.e("ReadingFile", "", new Throwable(e));
                    return null;
                }
            } else {
                Log.d("ReadingFile", "checking file can be read or not and it can not be read");
                return null;
            }
        } else {
            Log.d("ReadingFile", "checking file exists or not and it doesn't exist");
            return null;
        }
    }

    public boolean writeToFile(String contentToWrite, File fileToWriteTo) {
        Log.d("WritingToFile", "starting to write to " + fileToWriteTo.getAbsolutePath());

        if (fileToWriteTo.exists()) {
            Log.d("WritingToFile", "checking file exists or not and it exists");
            if (fileToWriteTo.canWrite()) {
                Log.d("ReadingFile", "checking file can be written or not and it can be written");
                try (FileOutputStream outputStream = new FileOutputStream(fileToWriteTo)) {
                    outputStream.write(contentToWrite.getBytes());

                    return true;
                } catch (Exception e) {
                    Log.e("WritingToFile", "", new Throwable(e));
                    return false;
                }
            } else {
                Log.d("ReadingFile", "checking file can be written or not and it can be not written");
                return false;
            }
        } else {
            Log.d("WritingToFile", "checking file exists or not and it does not exist");
            return false;
        }
    }

    public String getJsonString(File file, String stringToGet) {
        Log.d("GettingJson", "starting getting json string from " + file.getAbsolutePath());

        String jsonContent;
        if ((jsonContent = readFile(file)) != null) {
            try {
                JSONObject jsonObject;
                if (!jsonContent.isEmpty()) {
                    jsonObject = new JSONObject(jsonContent);
                } else {
                    jsonObject = new JSONObject();
                }

                String res = jsonObject.getString(stringToGet);
                Log.d("GettingJson", "successfully get key: " + stringToGet + " and it equals " + res);
                return res;
            } catch (Exception e) {
                Log.e("GettingJson", "", new Throwable(e));
                return null;
            }
        } else {
            Log.e("GettingJson", "failed To get json string");
            return null;
        }
    }

    public boolean writeJsonToFile(File fileToWriteTo, String stringToWrite) {
        Log.d("WritingJson", "starting to write to " + fileToWriteTo.getAbsolutePath());

        try {
            JSONObject jsonObject = new JSONObject(stringToWrite);

            return writeToFile(jsonObject.toString(), fileToWriteTo);
        } catch (Exception e) {
            Log.e("WritingJson", "", new Throwable(e));
            return false;
        }
    }

    public boolean addValueToJson(File jsonFile, String key, String value) {
        Log.d("AddingValueToJson", "adding key: " + key + ", with value: " + value);

        try {
            String jsonStringFromJsonFile;
            if ((jsonStringFromJsonFile = readFile(jsonFile)) != null) {
                JSONObject jsonObject;
                if (!jsonStringFromJsonFile.isEmpty()) {
                    jsonObject = new JSONObject(jsonStringFromJsonFile);
                } else {
                    jsonObject = new JSONObject();
                }
                jsonObject.put(key, value);

                return writeJsonToFile(jsonFile, jsonObject.toString());
            } else {
                Log.e("AddingValueToJson", "json file doesn't exist");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(key, value);

                return writeJsonToFile(jsonFile, jsonObject.toString());
            }
        } catch (Exception e) {
            Log.e("AddingValueToJson", "", new Throwable(e));
            throw new RuntimeException(e);
        }
    }

    public String generateName() {
        StringBuilder result = new StringBuilder();
        SecureRandom random = new SecureRandom();
        List<Character> characterList = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        for (char i = 'a'; i < 'z'; i++) {
            characterList.add(i);
            integers.add(Character.getNumericValue(i));
        }
        for (int i = 0; i < 5; i++) {
            result.append(characterList.get(random.nextInt(characterList.size())));
            result.append(integers.get(random.nextInt(integers.size())));
        }
        return result.toString();
    }
}
