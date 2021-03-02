package com.example.TATP_practice;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


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

/**
        if (rensyuuflg) {
            // ZIP化実施フォルダを取得
            File dir = new File("/sdcard/Download/" + tsk_filename);

            // ZIP保存先を取得
            File destination = new File("/sdcard/Download");

            // 圧縮実行
            compressDirectory(destination, dir);

            //textView.setText("データ圧縮完了");

        }
 */
        Button bkbutton = findViewById(R.id.b);
        bkbutton.setOnClickListener(v ->
                finish());
    }
    /**
     * 指定したフォルダをZIPファイルに圧縮
     * @param destination ZIP保存先ファイル
     * @param dir         圧縮対象のルートフォルダパス
     * @throws IOException
     */
    private void compressDirectory(final File destination, final File dir) {

        // 変数宣言
        byte[] buf = new byte[1024];
        ZipOutputStream zos = null;
        InputStream is = null;

        // ZIP対象フォルダ配下の全ファイルを取得
        List<File> files = new ArrayList<File>();
        getFiles(dir, files);

        try {
            // ZIP出力オブジェクトを取得（日本語の文字化けに対応するために文字コードは Shift-JIS を指定）
            zos = new ZipOutputStream(
                    new BufferedOutputStream(new FileOutputStream(destination)), Charset.forName("Shift-JIS"));

            // 全ファイルをZIPに格納
            for (File file : files) {

                // ZIP化実施ファイルの情報をオブジェクトに設定
                ZipEntry entry = new ZipEntry(
                        file.getAbsolutePath().replace(dir.getAbsolutePath() + File.separator, ""));
                zos.putNextEntry(entry);

                // ZIPファイルに情報を書き込む
                is = new BufferedInputStream(new FileInputStream(file));
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }

                // ストリームを閉じる
                is.close();
            }
            // 処理の最後にストリームは常に閉じる
            if (zos != null) {
                zos.close();
            }
            if (is != null) {
                is.close();
            }
        }catch (IOException ioException){
            //textView.setText("データ圧縮できなかった");
            Log.d("asyuku","圧縮できなかった");
        }
    }

    /**
     * 指定したフォルダ配下の全ファイルを取得
     * @param parentDir ファイル取得対象フォルダ
     * @param files     ファイル一覧
     */
    private void getFiles(final File parentDir, final List<File> files) {

        // ファイル取得対象フォルダ直下のファイル,ディレクトリを走査
        for (File f : parentDir.listFiles()) {

            // ファイルの場合はファイル一覧に追加
            if (f.isFile()) {
                files.add(f);

                // ディレクトリの場合は再帰処理
            } else if (f.isDirectory()) {
                getFiles(f, files);
            }
        }
    }
}