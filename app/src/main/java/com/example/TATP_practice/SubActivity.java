package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class SubActivity extends AppCompatActivity {
    //public TextView textView = findViewById(R.id.textView);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        MainActivity mainActivity = new MainActivity();
        String tsk_filename = mainActivity.ftext;
        Boolean rensyuuflg = mainActivity.rensyuflg;

///35タスクが終了し画面が遷移したら、データを保存したフォルダを圧縮し、データを削除//
        if (rensyuuflg) {
            // ZIP化実施フォルダを取得
            //File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + tsk_filename);

            //final File downloadDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + tsk_filename);

            String in = /*Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+"/" +*/"/sdcard/Download/"+ tsk_filename;
            String out = /*Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+"/" + */"/sdcard/Download/"+tsk_filename+".zip";

//圧縮する対象のファイル
            File[] files = new File(in).listFiles();

            ZipParameters params = new ZipParameters();
//圧縮アルゴリズムはDEFLATE
            params.setCompressionMethod(CompressionMethod.DEFLATE);
//圧縮レベルは高速
            params.setCompressionLevel(CompressionLevel.FAST);

//ファイルが存在しなければ新規、存在すれば追加となる
            ZipFile zip = new ZipFile(out);
            try {
                for (File f : files) {
                    System.out.println(f.getPath());

                    if (f.isDirectory()) {
                        //addFolder は配下の階層ごと追加する
                        zip.addFolder(f, params);
                    } else {
                        //addFile はファイル単体を追加する
                        zip.addFile(f, params);
                    }
                }
            } catch (IOException e) {
                //エラー処理（Zipファイルをを削除するなど）
            }

            /**
            ArrayList<File> files
                    = new ArrayList<File>() {
                {
                    add(downloadDir);
                }
            };

            String zipFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + tsk_filename;
            try {
                ZipFile zipFile = new ZipFile(zipFilePath);

                /** パラメータ
                ZipParameters params = new ZipParameters();
                params.setCompressionMethod(CompressionMethod.DEFLATE);
                params.setCompressionLevel(CompressionLevel.FAST);//圧縮レベルは高速

                /** ファイルの圧縮
                for (File file : files) {
                    if (file.isFile())
                        zipFile.addFile(file, params);
                    else
                        zipFile.addFolder(file, params);
                }
            }catch (IOException e) {
                //System.out.println("Most probably wrong password.");
                e.printStackTrace();
            }
            //} catch(ZipException e) {
                //e.printStackTrace();
            //}
        */
        }
        Button bkbutton = findViewById(R.id.b);
        bkbutton.setOnClickListener(v ->
                finish());
    }
    /**
     * ファイルとフォルダを圧縮する
     * @param 圧縮するFile
     * @param zipName 生成されるzipファイルのパス
     *
    public void compressFiles(
            List<File> files, String zipFilePath)
    {

    }*/

}