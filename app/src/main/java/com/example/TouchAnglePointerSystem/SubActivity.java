package com.example.TouchAnglePointerSystem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SubActivity extends AppCompatActivity {
    //public TextView textView = findViewById(R.id.textView);//csv出力とzip圧縮できたら完了コメント出す
    //public TextView textView4 = findViewById(R.id.textView4);
    public String statustext;
    public List<String> taskcountlist = new ArrayList<>();
    public List<String> targetpointlist = new ArrayList<>();
    public List<String> sousatimelist = new ArrayList<>();
    public List<String> errorlist = new ArrayList<>();
    public String errorpercent;
    public  List<List<String>> trajectorylist = new ArrayList<>();
    public List<String> firsttouchpointlist = new ArrayList<>();
    public List<String> firstyo_list = new ArrayList<>();
    public List<String> firsthiritulist = new ArrayList<>();
    public List<String> imagenamearray = new ArrayList<>();
    public List<byte[]> imagearray = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        Intent intent = this.getIntent();
        //被験者情報、(1セクション＝全体通して更新されない)
        statustext = intent.getStringExtra("statusText");
        // タスク番号
        taskcountlist = intent.getStringArrayListExtra("taskCount");
        // ターゲット座標(x,y)
        targetpointlist = intent.getStringArrayListExtra("targetPoint");
        // 操作時間
        sousatimelist = intent.getStringArrayListExtra("sousaTime");
        // エラー
        errorlist = intent.getStringArrayListExtra("errorString");
        //エラー回数/35=1セクションのエラー率
        errorpercent = intent.getStringExtra("errorCount");
        // ポインター軌跡
        //trajectorylist = intent.getStringArrayListExtra("trajectory");//おそらく、2次元リストを配列になおしてインテントができてない。
        // 初期指座標
        firsttouchpointlist = intent.getStringArrayListExtra("firstTouchPoint");
        // 初期指ヨー角
        firstyo_list = intent.getStringArrayListExtra("firstYo");
        // 初期指の長軸短軸（３５個分）。比率＝長軸長さ/短軸長さ。
        firsthiritulist = intent.getStringArrayListExtra("firstHiritu");
        // 画像名前（全画像分）
        imagenamearray = intent.getStringArrayListExtra("imageName");

        // 画像byte配列（全画像分）
        MainActivity mainActivity = new MainActivity();
        imagearray = mainActivity.sectionimagearray; //多分ここの画像データに名前はついているはずだけど、確認しないとわからないので、imagenamearrayもインテントしてる
        trajectorylist = mainActivity.trajectorylist;
        //上２つの変数受け渡しできていない。
        Log.d("csvdata",statustext);
        Log.d("csvdata",taskcountlist.get(0));
        Log.d("csvdata",targetpointlist.get(0));
        Log.d("csvdata",sousatimelist.get(0));
        Log.d("csvdata", String.valueOf(trajectorylist)); //エラーの原因。出力結果ー＞ [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], []]
        Log.d("csvdata", String.valueOf(imagearray));
        ///35タスクが終了し画面が遷移したら、画像フォルダを圧縮し、データを削除//
        //compress(imagearray, statustext+"images");

        /**
         * データをcsvファイルに変換ー＞１セクション毎に一つのｃｓｖファイルが作成されることになる。
         */
        //exportCsv();


        Button backbutton = findViewById(R.id.b);
        backbutton.setOnClickListener(v -> {
            Intent restartintent = new Intent(getApplication(), StartActivity.class);
            startActivity(restartintent);
                }
        );
    }

    /**
     * @param inputFiles : 圧縮したいJPEGファイルのリストー＞byte配列で行けるのか微妙？？
     * @param outputFile : 出力先となるZIPファイルのファイル名
     */

    public void compress(List inputFiles, String outputFile) {
        // ZIP形式の出力ストリーム
        ZipOutputStream zos = null;

        // 入出力用のバッファを作成
        byte[] buf = new byte[1024];

        // ZipOutputStreamオブジェクトの作成
        try {
            zos = new ZipOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            for (int i = 0; i < inputFiles.size(); i++) {
                // 入力ストリームのオブジェクトを作成
                InputStream is = new FileInputStream((String) inputFiles.get(i));

                // Setting Filename
                String filename = imagenamearray.get(i);
                // String filename = String.format("img_%02d.jpg", i);

                // ZIPエントリを作成
                ZipEntry ze = new ZipEntry(filename);

                // 作成したZIPエントリを登録
                zos.putNextEntry(ze);

                // 入力ストリームからZIP形式の出力ストリームへ書き出す
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }

                // 入力ストリームを閉じる
                is.close();

                // エントリをクローズする
                zos.closeEntry();
            }

            // 出力ストリームを閉じる
            zos.close();
            //textView.setText("画像データ圧縮＆保存完了");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportCsv(){
        try {
            // 出力ファイルの作成
            PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + statustext + ".csv", false),"UTF-8")));

            // ヘッダーを指定する
            p.print("被験者情報");
            p.print(",");
            p.print("タスク番号");
            p.print(",");
            p.print("ターゲット座標");
            p.print(",");
            p.print("操作時間");
            p.print(",");
            p.print("エラー");
            p.print(",");
            p.print("エラー回数"+errorpercent);//1セクションで一つなので、列だけ作って同じもの入れる
            p.print(",");
            p.print("初期指座標");
            p.print(",");
            p.print("初期ヨー角");
            p.print(",");
            p.print("初期指比率");
            p.print(",");
            p.print("ポインター軌跡");//１タスク毎に要素にjはいるデータが長いので最後。
            p.print(",");
            p.println();//画像は別で保存するのでcsvには含まない
            // 内容をセットする
            for(int i = 0; i < taskcountlist.size(); i++){
                p.print(statustext);
                p.print(",");
                p.print(taskcountlist.get(i));
                p.print(",");
                p.print(targetpointlist.get(i));
                p.print(",");
                p.print(sousatimelist.get(i));
                p.print(",");
                p.print(errorlist.get(i));
                p.print(",");
                p.print(errorpercent);//1セクションで一つなので、列だけ作って同じもの入れる
                p.print(",");
                p.print(firsttouchpointlist.get(i));
                p.print(",");
                p.print(firstyo_list.get(i));
                p.print(",");
                p.print(firsthiritulist.get(i));
                p.print(",");
                p.print(trajectorylist.get(i));//１タスク毎に要素にjはいるデータが長いので最後。
                p.print(",");
                p.println();    // 改行
            }
            // ファイルに書き出し閉じる
            p.close();
            //textView4.setText("csvファイル出力完了");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}

