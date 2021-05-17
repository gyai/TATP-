package com.example.TouchAnglePointerSystem;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import org.hcilab.libftsp.LocalDeviceHandler;
import org.hcilab.libftsp.capacitivematrix.capmatrix.CapacitiveImageTS;
import org.hcilab.libftsp.listeners.LocalCapImgListener;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;



public class MainActivity extends AppCompatActivity{
    static {
        System.loadLibrary("opencv_java3");
    }
//実験用保存先フォルダ名+練習か本番か
    public String ftext = "1-1";//被験者番号-実験回数
    public Boolean rensyuflg = false;
    public String statustext;

    //グローバル変数
    public static int[][] capmatrix = null;//静電容量値
    public static int[] flattermatrix = null;//1次元配列静電容量値
    public static double yo = 0;//OpenCVで求めたヨー角格納用。ピッチトリガーで使う

    public static float size = 0;//楕円サイズ

    /////フラグ/////
    public static Boolean systemTrigger_flag = false;//押されている間かどうか=転送システムが起動しているか
    public LocalDeviceHandler localDeviceHandler = new LocalDeviceHandler();
    public Boolean animation_flg = false;
    public Boolean errorflg = false;//楕円が認識されなかった時用

    ///OpenCV///
    public RotatedRect box;
    public Bitmap bmpimage;

    //touch転送用変数
    public float syoki_touch_x, syoki_touch_y;
    public float touchcentx, touchcenty;
    public float move_x, move_y;
    public float kyori_x,kyori_y;
    public float pointer_x, pointer_y;//渡すx,y座標
    public float syoki_pointerx;//初期位置
    public float syoki_pointery;
    public float syoki_finalx,syoki_finaly;
    public float syoki_yo;
    public String syoki_hiritu;
    public String hiritu;
    public float pointer_finalx, pointer_finaly;//受け取った最終的なポインタ位置
    public float distance;
    public float size_height;


    static final int SAIBYOUGA_KANKAKU_MS = 50; //pointerの再描画の間隔（ms）。小さいほどアニメーションが滑らかに。

    //ポインター画像表示用
    public static ImageView pointerimage;
    public static ImageView waku;

    //OpenCV画像用
    private int[] pix;
    private int r,g,b;//RGBデータ用

    //randomButton設定用
    public Button button;
    public static int buttonx = 0;
    public static int buttony = 0;
    public static int bwidth = 150;
    public static int bheight = 150;
    public static int bx = 0;
    public static int by = 0;
    public ArrayList<Integer> arrayindex = new ArrayList<Integer>();

    //データ保存用//
    public int task_count = 0;
    public long task_starttime;
    public long task_endtime;
    public String error = "-";
    public int errorcount = 0;
    public List<String> pointer_kiseki = new ArrayList<>();
    public String task_kekka;
    public int imagecount = 1;

    public  List<String> taskcountlist = new ArrayList<>();
    public  List<String> targetpointlist = new ArrayList<>();
    public  List<String> sousatimelist = new ArrayList<>();
    public  List<String> errorlist = new ArrayList<>();
    public  List<List<String>> trajectorylist = new ArrayList<>();
    public  List<String> firsttouchpointlist = new ArrayList<>();
    public  List<String> firstyo_list = new ArrayList<>();
    public  List<String> firsthiritulist = new ArrayList<>();
    public List<String> imagenamearray = new ArrayList<>();
    public List<byte[]> sectionimagearray = new ArrayList<>();

////////////onCreate()-start/////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//画面上部の邪魔なステータスバーなどを非表示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//同上
        setContentView(R.layout.activity_main);

        /*
        start画面から情報受け取り
        被験者番号-セクション番号->statustext
        練習か本番か->rensyuflg(デフォルトは本番(true))
        */
        Intent intent = this.getIntent();
        statustext = intent.getStringExtra("statusText");
        rensyuflg = intent.getBooleanExtra("practice",false);
        TextView text = findViewById(R.id.textView2);
        text.setTextSize(30);
        if (rensyuflg) {
            text.setText("練習中"+statustext);
        }else{
            text.setText("本実験中"+statustext);
        }
        
        layoutSetting(); //ボタンと画像などの見た目諸々設定関数
        
        //タッチ時に指の接触面の大きさを計測したいので、最初にリスナー起動必要あり//
        localDeviceHandler.startHandler();//静電容量リスナー（スレッド）起動
        seidenListener();
    }

    public void layoutSetting(){
        //ランダムな位置にボタン//
        for (int a=0; a<35; a++) {
            arrayindex.add(a);//0~34まで
        }
        Collections.shuffle(arrayindex);//35個の要素をランダムにシャッフル
        buttonSet();

        //ポインター画像と初期位置枠画像の設定///
        ConstraintLayout.LayoutParams imagelp = new ConstraintLayout.LayoutParams(100,100);
        pointerimage = findViewById(R.id.pointerimg);
        pointerimage.setImageResource(R.drawable.pointer);
        pointerimage.setLayoutParams(imagelp);
        pointerimage.setVisibility(View.GONE);//なかったようにする,非表示

        ConstraintLayout.LayoutParams wakulp = new ConstraintLayout.LayoutParams(110,110);
        waku = findViewById(R.id.wakuimage);
        waku.setImageResource(R.drawable.waku);
        waku.setLayoutParams(wakulp);
        waku.setVisibility(View.GONE);
    }

    public void buttonSet() {
        ///(呼び出される時(初回とタスク終了時点で呼ばれる)、各配列のindexをランダムに取り出して、それに対応する画面上の位置を決定)//
        if (task_count <= 34 ){
            //randomButton設定
            Resources res = getResources();
            Drawable shape = ResourcesCompat.getDrawable(res, R.drawable.stroke, getTheme());
            button = findViewById(R.id.button);
            ConstraintLayout.LayoutParams buttonlp = new ConstraintLayout.LayoutParams(bwidth, bheight);
            button.setLayoutParams(buttonlp);
            button.setBackground(shape);

            int index = arrayindex.get(task_count);//タスク番目の要素を取り出して、indexにする
            bx = 150 * ((index % 7));//ボタンx座標→indexを列要素数の7で割ったあまり。(例:index=1なら 1%7=0..1つめり1列目。index =34 34%7=4..6つまり6列目_
            by = 150 * ((int) Math.floor(index / 7.0));//ボタンy座標
            //Log.d("ボタン座標", String.valueOf(bx) + " , " + String.valueOf(by) + "インデックス: " + String.valueOf(index));


            button.setX((float) bx);
            button.setY((float) by);
            buttonx = bx + bwidth;
            buttony = by + bheight;
        }
    }
////////////onCreate{}--end//////////////////////

////////////onTouchEvent()-start////////////////////////

    public double[] sousaarray = new double[35];
    public double[] syujyukudo = new double[35];

    @Override
    public boolean onTouchEvent (MotionEvent e){//何らかのタッチ操作中ずっと動いている
        AnimationThread animationThread = new AnimationThread();

        ///pointer描画を50ミリ秒間隔でアニメーションさせる///////////////
        ///更新されたpointer_x,yを受け取って描画するだけ///
        Handler pointerhandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (systemTrigger_flag) {
                    ///ポインターが画面外に行かないように最終的な見た目の閾値設定///
                    if (pointer_x <= 50) {
                        pointer_x = 50;
                    } else if (pointer_x >= 1030) {
                        pointer_x = 1030;
                    }
                    if (pointer_y < 50) {
                        pointer_y = 50;
                    } else if (pointer_y >= 1350) {
                        pointer_y = 1350;
                    }
                    if (imagecount == 1 || imagecount%5 == 0) {//imagecountが1か5の倍数の時だけ保存→画像データ量1/3に
                        pointer_kiseki.add(pointer_x + "," + pointer_y); //ポインター軌跡取得
                    }
                    if (syoki_pointerx <= 65) {
                        syoki_pointerx = 65;
                    } else if (syoki_pointerx >= 1045) {
                        syoki_pointerx = 1045;
                    }
                    if (syoki_pointery < 45) {
                        syoki_pointery = 45;
                    } else if (syoki_pointery >= 1355) {
                        syoki_pointery = 1355;
                    }

                    pointer_finalx = pointer_x - 50;//受け取った転送先座標からポインター画像の幅/2を引いて、座標を画像の真ん中に。
                    pointer_finaly = pointer_y - 50;

                    syoki_finalx = syoki_pointerx - 55;
                    syoki_finaly = syoki_pointery - 55;

                    if (!rensyuflg) {
                        //静電容量画像保存
                        saveimageFile();
                        imagecount += 1;
                    }

                    //ポインター,初期枠画像位置設定+表示//
                    pointerimage.setTranslationX(pointer_finalx);
                    pointerimage.setTranslationY(pointer_finaly);
                    pointerimage.setVisibility(View.VISIBLE);

                    waku.setTranslationX(syoki_finalx);
                    waku.setTranslationY(syoki_finaly);
                    waku.setVisibility(View.VISIBLE);

                    //Log.d("pointer", String.valueOf(pointer_finalx) + " , " + String.valueOf(pointer_finaly));
                    //Log.d("syokipointer",String.valueOf(syoki_pointerx) +" , "+ String.valueOf(syoki_pointery));

                    pointerhandler.postDelayed(this, SAIBYOUGA_KANKAKU_MS);//0.05秒間隔でハンドラ開始してアニメーションしてる

                }else{//指が離れている時
                    pointerimage.setVisibility(View.GONE);//指が離れたらポインタ隠す
                    waku.setVisibility(View.GONE);
                }
            }
        };

        ////タッチ時トリガー:0.5秒長押ししたらトリガー起動したいので長押しハンドラ挟む///
        long LONG_PRESS_TIME = 500;    // 長押し時間（ミリ秒）
        Handler long_press_handler = new Handler();
        Runnable long_press_receiver = () -> {
            ///タッチダウン後0.5秒後、サイズ30以上なら起動→サイズの微調整は必要かも
            if (size >= 30 && !systemTrigger_flag) {//タッチした時の輪郭サイズが30以上=指の腹で押されたならシステム開始

                ///初期タッチ時系変数：初期化///
                syoki_touch_x = e.getX();//初期位置計算で使う初期タッチX座標
                syoki_touch_y = e.getY();//初期位置計算で使う初期タッチY座標

                move_x = 0;
                move_y = 0;
                kyori_y = 0;
                kyori_x = 0;

                //size_first = size; //初期タッチ時のサイズ
                //size_height_first = size_height; //初期タッチ時のサイズ高さ

                ///データ保存用、システム起動時に1タスクの操作時間計測用に時間計測///
                task_starttime = System.nanoTime();//システム起動時のシステム時間

                ///初期タッチ時系変数///
                syoki_yo = (float) yo;
                syoki_hiritu = hiritu;
                float tan_theta = (float)Math.tan(Math.toRadians(syoki_yo));

                //タッチした際のポインター初期位置表示//
                pointer_x = syoki_touch_x - ((syoki_touch_y - 600) * tan_theta);
                pointer_y = 600;//指の方向上の画面高さ半分くらいの位置に表示させたい

                syoki_pointerx = pointer_x;//初期位置のpointerのx座標
                syoki_pointery = pointer_y;//y座標

                systemTrigger_flag = true;//システムトリガーフラグをtrueにするのはここだけ→システム起動はここだけ
                //ポインター移動ハンドラ起動
                pointerhandler.post(runnable);//ポインター描画ハンドラon
                animationThread.start();//アニメーションスレッドをon

            }
        };


        //楕円形が認識されなかったら、指を中心に戻すよう指示//
        if (errorflg) {
            Toast.makeText(getApplicationContext(), "指が認識されませんやり直してください:画面端に寄りすぎです", Toast.LENGTH_SHORT).show();
            errorflg = false;
        }

        switch (e.getAction()) {
/////////タッチダウン////////////
            case MotionEvent.ACTION_DOWN:

                long_press_handler.postDelayed( long_press_receiver, LONG_PRESS_TIME);  // 0.5秒長押し判定

                break;

//////タッチアップ////////
            case MotionEvent.ACTION_UP:

                if (systemTrigger_flag) {
                    //systemTrigger_flag=trueの時、離れたらその点にタッチ転送

                    ////データ保存用：操作終了時間////
                    task_endtime = System.nanoTime();//システム終了時間計測

                    ////ポインター操作時に指が離れたらタッチ転送->「ボタンがタップされたか」ではなく、「指が離れたときにポインターがターゲットボタン内なら」にしないとエラーが測れない
                    if (by <= pointer_finaly+50 && pointer_finaly+50 <= buttony && bx <= pointer_finalx +50 && pointer_finalx+50 <= buttonx){
                        Log.d("systemtouch","システムタッチ成功");
                        trans_touchevent();
                    }else{
                        //データ保存用：error回数カウント
                        errorcount += 1;
                        error = "error";
                        Log.d("systemtouch","システムタッチ失敗");
                        trans_touchevent();
                    }
                    //システムトリガー終了し、諸々の処理を動かさないように//
                    systemTrigger_flag = false;

                    //1タスク終了//
                    task_count += 1;

                    ///でーた保存///

                    if (rensyuflg) {
                        ///習熟度計算////
                        syujyuku_do();
                    }else{
                        dataOfTask(); //1タスク毎の諸々のデータを保存

                        //saveFile();
                        //画像データ保存もimageview配列使ってここにしたい（画像データ1万枚近くを操作中に保存するのは処理思い原因になりそう）//
                    }


                    if (task_count == 35){
                        Toast.makeText(this, "セクション終了:お疲れさまでした", Toast.LENGTH_SHORT).show();
                        Intent finishintent = new Intent(getApplication(), SubActivity.class);
                        //データセット
                        /*
                        必要な初期情報
                        ・DBに保存する1セクション分のテキストデータ(被験者情報、タスク番号、ターゲット座標、操作時間、エラー、ポインター軌跡、初期指座標、初期指ヨー角、初期指の長軸短軸)
                        ・保存する画像データ（静電容量画像）
                         */
                        //被験者情報、(1セクション＝全体通して更新されない)
                        finishintent.putExtra("statusText",statustext);
                        // タスク番号、（３５個分＝リストに格納して渡さないとだめ。。？->putStringArrayListExtraなどで可能）
                        finishintent.putStringArrayListExtra("taskCount",(ArrayList)taskcountlist);
                        // ターゲット座標(x,y)、（３５個分）
                        finishintent.putStringArrayListExtra("targetPoint",(ArrayList)targetpointlist);
                        // 操作時間、（３５個分）
                        finishintent.putStringArrayListExtra("sousaTime",(ArrayList)sousatimelist);
                        // エラー、（３５個分）
                        finishintent.putStringArrayListExtra("errorString",(ArrayList)errorlist);
                        //エラー回数/35=1セクションのエラー率
                        finishintent.putExtra("errorCount",String.valueOf(errorcount/35.0));
                        // ポインター軌跡、（３５回分）
                        finishintent.putStringArrayListExtra("trajectory",(ArrayList)trajectorylist);
                        // 初期指座標、（３５個分）
                        finishintent.putStringArrayListExtra("firstTouchPoint",(ArrayList)firsttouchpointlist);
                        // 初期指ヨー角、（３５個分）
                        finishintent.putStringArrayListExtra("firstYo",(ArrayList)firstyo_list);
                        // 初期指の長軸短軸（３５個分）。比率＝長軸長さ/短軸長さ。
                        finishintent.putStringArrayListExtra("firstHiritu",(ArrayList)firsthiritulist);
                        // 画像名前（全画像分）
                        finishintent.putStringArrayListExtra("imageName",(ArrayList)imagenamearray);
                        // 画像byte配列（全画像分）//データ型がintent不可だったので、アクティビティ呼び出しで受け渡す
                        //finishintent.putStringArrayListExtra("imageByte",(ArrayList)sectionimagearray);
                        startActivity(finishintent);
                    }
                    buttonSet();
                }

                ///フラグや変数諸々初期化
                long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                pointerimage.setVisibility(View.GONE);
                systemTrigger_flag = false;
                //xmove_flg = false;
                //ymove_flg = false;
                animation_flg = false;
                error ="-";
                kyori_y = 0;
                kyori_x = 0;
                animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。→止まってない。改善する


                break;
/////ムーブ///////////
            case MotionEvent.ACTION_MOVE:
                move_y = e.getY();
                move_x = e.getX();
                break;

        }
        ///タッチ時条件分岐終了//

        //////button押された時の動き////
        //ボタンの範囲でタッチ判定起きたらよびだされる呼び出される→多分もっとスマートな実装ある
        // lambda式
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    //////button押された時の動き////
                    //Log.d("systouch", "systemタッチした");
                    systemTrigger_flag = false;

                }else if (event.getAction() == MotionEvent.ACTION_UP){
                    Log.d("アップ", "システムアップ");
                    ///楕円検出できなかったらこれを呼び出す。全リセット//
                    systemTrigger_flag = false;
                    //ymove_flg = false;
                    //xmove_flg = false;
                    animation_flg = false;
                    long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                    pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                    animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。

                }
                Log.d("tag","buttonTouch");
                return false;
            }
        });

        return false;
    }
////////////////ontouch{}--end/////////////////////////

///////////////////////////////////////////////関数//////////////////////////////////////
    //習熟度計算関数//
    public void syujyuku_do() {
        int rot = task_count + 1;
        for (int i = 0; i < 35; i++) {
            if (rot == i) {
                sousaarray[i] = (double)(task_endtime - task_starttime) / (double)1000000000;
            }
        }
        if ((rot % 2) == 0) {
            syujyukudo[rot] = (sousaarray[rot] / sousaarray[rot / 2]) * 100;
            if (rot >= 2) {
                if (Math.floor(syujyukudo[rot]) <= Math.floor(syujyukudo[(rot - 2)]) + 3 && Math.floor(syujyukudo[rot]) >= Math.floor(syujyukudo[(rot - 2)]) - 3) {
                    Toast.makeText(this, "習熟度発散: ", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    //1タスク終了時のタスクデータ保存と１タスク毎に更新されるデータをリストに追加＋初期化
    public void dataOfTask() {
        taskcountlist.add(String.valueOf(task_count+1)); //taskcountlist[0~34]に[1~35]が入る
        targetpointlist.add(bx +","+ by);
        sousatimelist.add(String.valueOf((double)(task_endtime - task_starttime) / (double)1000000000));
        errorlist.add(String.valueOf(error));
        error = "-";
        trajectorylist.add(pointer_kiseki);
        imagecount = 1;
        pointer_kiseki.clear();
        firsttouchpointlist.add(syoki_touch_x +","+ syoki_touch_y);
        firstyo_list.add(String.valueOf(syoki_yo));
        firsthiritulist.add(syoki_hiritu);

    }

    // ファイルを保存関数//
    public void saveFile() {
        //ファイル保存用//

        String fileName = "task" + String.format("%02d", task_count + 1) + ".txt";

        try {
            //text保存
            File extStrageDir =Environment.getExternalStorageDirectory();
            File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+ "/"+ftext, fileName);//練習

            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            //FileWriter writer = new FileWriter(file);
            writer.write(task_kekka);
            writer.flush();
            writer.close();


        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 画像ファイルを保存関数//
    //現状、画像サイズが小さい。画像が粗い（解像度が悪すぎる）、DB使えないうえで効率的な保存＋管理方法模索中
    public void saveimageFile() {
        //ファイル保存用//
        //画像ファイル名"1_1task01_0_150_001"　＝　被験者情報_タスク番号_ターゲット座標(x,y)_枚目
        String image_fileName = statustext + "_" + String.format("%02d",task_count+1) + "_(" + bx +"_"+ by + ")_" + String.format("%03d",imagecount) + ".jpeg";

       //try {
            if (imagecount == 1 || imagecount%5 == 0) {//imagecountが1か5の倍数の時だけ保存→画像データ量1/3に
                //画像保存
                /*
                File extStrageDir = Environment.getExternalStorageDirectory();
                File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + ftext, image_fileName);

                FileOutputStream outStream = new FileOutputStream(i_file);
                bmpimage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.close();
                 */
                imagenamearray.add(image_fileName);//画像保存する際にファイル名も保存
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmpimage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] jpgarr = baos.toByteArray();
                sectionimagearray.add(jpgarr);//imagenamearrayリストに対応した要素番号に、jpeg画像のbyte配列データ格納
            }
        //}
        //catch (IOException ioExceptione) {
        //    ioExceptione.printStackTrace();
        //}
    }



///タッチ点転送関数//
    public void trans_touchevent(){
        if (systemTrigger_flag) {
            ///// タップなしでonTouchEventを発生させる////////
            MotionEvent trans_event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, pointer_x, pointer_y, 0);
            button.dispatchTouchEvent(trans_event);//ここでbuttonタッチ発生している
        }else {
            MotionEvent trans_event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, pointer_x, pointer_y, 0);
            button.dispatchTouchEvent(trans_event);//ここでbuttonタッチ発生している
            errorflg = true;
        }
    }


//////静電容量取得し、指の向きyoとsizeを返す。この中のCapImgリスナーでxとyのpointer座標も決定する//////
    public void seidenListener() {
        /////////////////////////---以下静電容量取得したときの処理-----/////////////////////////////
        //静電容量の配列などのデータを取得するリスなー
        localDeviceHandler.setLocalCapImgListener(new LocalCapImgListener() {
            @Override
            public void onLocalCapImg(CapacitiveImageTS capImg) {// called approximately every 50ms

                capmatrix = capImg.getMatrix(); // get the 27x15 capacitive image(2次元配列)
                int width = capmatrix[0].length;
                int height = capmatrix.length;
                flattermatrix = capImg.getFlattenedMatrix(); // get a flattened 27x15 capacitive image(1次元配列)

                for (int j = 0; j < flattermatrix.length; j++) {//1次元配列ノイズ除去
                    if (flattermatrix[j] < 50) {//ノイズを3以下に設定するとご認識静電容量が多すぎるので、50以下をノイズとして設定(デバイスの精度によるものだとは思う)
                        flattermatrix[j] = 0;
                    }
                }
                pix = new int[width * height];//画像データを格納15*27
                pix = getPix();//画面下半分の範囲でタッチされた場所にRGB代入された配列

                ///////////////////////画像化処理→ヨー角と輪郭size取得////////////////////////////////

                //静電容量が生成された(実質タップされた)時//
                bmpimage = Bitmap.createBitmap(capmatrix[0].length, capmatrix.length, Bitmap.Config.ARGB_8888);
                bmpimage.setPixels(pix, 0, width, 0, 0, width, height);//bmpimageにsetPixel()で画像データに合わせている（幅や高さなど）


                //OpenCV//
                Mat mat_img = new Mat();
                Utils.bitmapToMat(bmpimage, mat_img);//mat_imageにbmpimageをMatに変換
                Mat grayMat = new Mat();//グレースケール変数
                Imgproc.cvtColor(mat_img, grayMat, Imgproc.COLOR_RGB2GRAY);//mat_imgをgrayMatにグレースケール変換
                Mat mat_msk = new Mat();//二値化データ変数
                Imgproc.threshold(grayMat, mat_msk, 0, 255.0, Imgproc.THRESH_BINARY_INV);//グレースケールのgrayMatをmat_mskに二値化
                // 明るさ0以外を白に、タッチ点を黒に？逆かも。（静電容量部分が黒くなる）→ここでもとのbmpimageが濃淡のない二値化された白黒画像になった

                //////////////楕円の輪郭を取得////////////////////////////////////////
                //輪郭画像データ(楕円パラメータなど)//
                List<MatOfPoint> contours = new ArrayList<>();//contour = 輪郭List
                Mat hierarchy = new Mat();//ヒエラルキー
                Imgproc.findContours(mat_msk, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
                for (int i = 0; i < contours.size(); i++) {//輪郭の長さ分ループ
                    Size count = contours.get(i).size();//輪郭の大きさを取得

                    // (小さすぎる|大きすぎる)　輪郭を除外//
                    if (count.height < 80 && i == 0) {//<80だと下の処理通るが、<85に設定すると下の処理行かない。境界が80~85?
                        //size = count.height;//heightしか値変わらない90: 11 ~ 0: 21
                        //Log.d("ピッチ", String.valueOf(size));
                        Mat poMat = new Mat();
                        contours.get(i).convertTo(poMat, CvType.CV_32F);//contoursリストのi番目を浮動小数点32bitにしてpoMatに変換？出力してる？
                        List<Point> pointsf = new ArrayList<Point>();
                        Converters.Mat_to_vector_Point(poMat, pointsf);//List<Point>をpoMat(Mat型)をList<Post>に変換+代入(コンバート)
                        MatOfPoint2f matOfInterest = new MatOfPoint2f();
                        matOfInterest.fromList(pointsf);//pointfs(list<Point>)をmatOfInterest(MatOfPoint2f)にコンバート
                        ///////////////////////// 楕円フィッティング/////////////////////////
                        /// エラー回避用,楕円が取得できているときだけうごく////
                        try {
                            box = Imgproc.fitEllipse(matOfInterest);//楕円検出、フィッティング
                            size_height = (float) box.size.height;
                            hiritu = String.valueOf(box.size.height) + "/" + String.valueOf(box.size.width);
                            size = (float)((box.size.height/2) * (box.size.width) * Math.PI);//楕円の面積で推定
                            //Log.d("パラメータ、サイズ", String.valueOf(size));
                            //Log.d("パラメータ", String.valueOf(size_first) + " , " +String.valueOf(size));

                            touchcentx = (float)box.center.x;
                            touchcenty = (float)box.center.y;
                            //Log.d("x,y", String.valueOf(touchcentx) +" , "+String.valueOf(touchcenty));
                            yo = 180 - box.angle;//yoに真上を0として左90の角度しまう

/**
                            /////カーソル位置調整//////
                            y_kettei();//返り値:y_after
                            x_kettei();//返り値:pointer_x
*/
                        } catch (Exception e) {//楕円取得できないときはExceptionできてる
                            //Toast.makeText(getApplicationContext(), "指が認識できません:画面端を触りすぎです",Toast.LENGTH_SHORT).show();
                            //systemTrigger_flag = false;
                            //trans_touchevent();
                            Log.d("exception", "楕円取得できずキャッチ");
                        }
                    }
                }
            }
        });
        /////////////静電容量リスナー処理終了。静電容量画像生成の度(50ms秒毎？)起動する。ここで取得するのは「ヨー角」と「輪郭の高さ」////////////
    }
    //ポインター座標(pointer_x,y)を返す関数。アニメーションも通常もこの処理に集約//
    public void PointerSet(){
        /////ポインター位置調整。
        ///// アニメーションフラグがfalseの通常時は「x_kettei()」「y_kettei()」でポインター座標を決定。
        // 初期タッチ座標と現在のムーブ座標との距離を計算し、その距離を定数倍したものをpointer座標にする。
        ///// アニメーションフラグがtrueのアニメーション時は↓
        // 初期座標(x,y)とアニメーション開始する計測時点の座標(x',y')がある。
        // ２つの座標があれば２点間の角度(radian)と距離(distance)がわかる。
        // つまり、アニメーションして(x',y')を遠くへ伸ばしたいなら、距離distanceにインクリメントし続ければ全方位にポインターが伸びることになる。


        //必要な変数準備
        Point2DFloat syokipoint = new Point2DFloat(syoki_touch_x, syoki_touch_y);
        distance = (float) Math.sqrt((move_x - syokipoint.x) * (move_x - syokipoint.x) + (move_y - syokipoint.y) * (move_y - syokipoint.y)); // 2点の距離
        float radian = (float) Math.atan2(move_y - syokipoint.y,move_x - syokipoint.x); //2点の角度ラジアン
        if (animation_flg){//アニメーションの時
            //アニメーションフラグの設定部分でアニメーション範囲などの条件はクリアしている。
            //ここでは単純な、アニメーション時のpointer_x,yの決定（制御処理）を実装
            //ポインター座標　＋＝　2点間の距離distance+20の三角関数でｘとｙに変換したもの
            // (座標上で言うマイナス方向も動くのか？？？？？→ラジアンがしっかり機能していれば.sinや.cosでマイナスの値になっているはず)
            pointer_y += (float) (Math.sin(radian) * distance*+20);
            pointer_x += (float) (Math.cos(radian) * distance*+20);

        }else{//通常時
            pointer_y = (float) (Math.sin(radian) * distance*3);
            pointer_x = (float) (Math.cos(radian) * distance*3);

        }
    }

    //bitmapに格納するpix配列にRGBデータ代入
    public int[] getPix() {
        int width = capmatrix[0].length;
        int height = capmatrix.length;
        //pix配列にcapmatrix>=0ならRGBデータ代入

        for (int i=height/2; i<height ; i++) {//画面下半分で動くようにしている
            for (int j=0; j<width; j++) {
                int index = i * width + j;

                r = (flattermatrix[index] >> 16) & 0xff;
                g = (flattermatrix[index] >> 8) & 0xff;
                b = flattermatrix[index] & 0xff;
                pix[index] = Color.argb(255, r, g, b);//RGBデータが入った1次元pix配列
            }
        }
        return pix;//タッチされたピクセル上に色データが入っている配列(画面下半分のみ)
    }

///完全別処理を行うアニメーションフラグ管理用Thread/////
    ////並行処理してくれる。主にフラグを管理する目的////

    /**
     * アニメーションと通常制御の切り替えラグの改善
     * 通常制御時、0.5秒間座標の差が±5以内ならフラグ=true、0.5秒の間(0.05秒＊１０)で範囲外にいったらフラグ=falseにしてループ強制で抜ける
     *
     * 案１：メインのUIスレッド以外で、0.1秒ごとに指の座標を1タスク分全部取得し続ける。（リスト？）
     * じゃあ0.5秒指が動いていないって？－＞要素の１～５要素前全部が今の要素の指座標と比較して±５以内。
     *
     * 案２：スレッドスリープ0.5秒を、0.05秒ｘ１０に分解し、分解したスリープ処理ごとに動いたかどうか判定。(0.5秒を１まとまりとする)
     * 0.5秒の間に動いたなら（これどうやって判定する？）アニメーション移行しない。
     * 0.5秒の間が全部範囲内だったら、アニメーション移行。
     */
    class AnimationThread extends Thread{
        @Override public void run() {

            //queue構造の配列を作る
            Queue<Point2DFloat> pointerlog = new ArrayDeque<>();
            int queuemax = 100;
            float inf = 99999.9f;
            for (int i=0; i < queuemax; i++){
                pointerlog.add(new Point2DFloat(inf, inf));
            }

            while (systemTrigger_flag){
                pointerlog.add(new Point2DFloat(move_x, move_y));
                float max_x=-inf,max_y=-inf,min_x=inf,min_y=inf;
                for (Point2DFloat p: pointerlog){
                    max_x=Math.max(max_x,p.x);
                    max_y=Math.max(max_y,p.y);
                    min_x=Math.min(min_x,p.x);
                    min_y=Math.min(min_y,p.y);
                }

                float x_sa = Math.abs(max_x-min_x);
                float y_sa = Math.abs(max_y-min_y);
                //ここで右側の初期座標と現在の座標との距離条件(アニメーション範囲にいるかどうか)を判定
                //指がプラマイ5いないの範囲で停止しており、初期座標とムーブ座標との距離が50以上=初期タッチ座標を原点とする半径50の円より外側ならアニメーション領域
                if ((x_sa <= 5 && y_sa <= 5) && distance > 50){
                    animation_flg = false;
                }else{
                    animation_flg = true;
                }

                pointerlog.poll(); //先頭取り出し
            }

/**
                while (true) {
                    //whileの中でqueueの中にｘとｙをひたすら入れる、例えば１００しか入らないようにしたら、１００超えたら先に入れたデータが消えていく（プログラム的には消す）。最初にバグらないように、あらかじめ変な座標データを入れてお必要あり。
                    //１００個データをそろえるまでにかかる時間が０．５秒であれば、０，５秒分のデータが常にたまっていることになる。
                    //やりたいことは、キューに入れようとするデータと、１００個からあふれて押し出されたデータを比較して、範囲内であればアニメーションフラグがtrueになる。
                    //仮に、１００個データを集める途中で範囲から外れたデータを入れようとしたなら、それは指を明示的に動かしたことになるので、フラグはfalseになる。

                    float startx = move_x;
                    float starty = move_y;
                    Boolean aniflg = false;
                    long startTime = System.nanoTime();
                    for (int i=0; i < 10; i++){ //10回繰り返す
                        Thread.sleep(50); //0.05秒止める
                        float x_sa = startx - move_x;
                        float y_sa = starty - move_y;
                        if (((-5 <= x_sa && x_sa <= 5)&&(-5 <= y_sa && y_sa <= 5)) && (pointer_x < syoki_pointerx-50||syoki_pointerx+50 < pointer_x || pointer_y < syoki_pointery-50||syoki_pointery+50 < pointer_y)){
                            aniflg = true;
                        }else{
                            aniflg = false;
                            break;
                        }
                    }
*/

                    /**
                    try {
                        float startx = move_x;
                        float starty = move_y;

                        Thread.sleep(500);//指の傾きが固定されているか判定、0.5秒待つ

                        float endx = move_x;
                        float endy = move_y;

                        //xもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//

                        float x_sa = startx - endx;

                        //yもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//
                        float y_sa = starty - endy;
                        
                        //アニメーションフラグ//
                        //0.5秒間指がほぼ動いていない、かつ、ポインターの位置が初期位置からプラマイ５０より外側の時
                        if (((-5 <= x_sa && x_sa <= 5)&&(-5 <= y_sa && y_sa <= 5))&&  (pointer_x < syoki_pointerx-50||syoki_pointerx+50 < pointer_x || pointer_y < syoki_pointery-50||syoki_pointery+50 < pointer_y)) {
                            animation_flg = true;

                        }else{
                            animation_flg = false;

                        }
                    } catch (InterruptedException interruptedException) {
                        Log.d("Thread", "スレッドを停止");
                        //このログを見たことは無い。機能していない//
                        return;
                    }
                    */

               // }

        }
    }

}