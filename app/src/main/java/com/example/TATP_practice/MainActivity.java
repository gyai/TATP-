package com.example.TATP_practice;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    static {
        System.loadLibrary("opencv_java3");
    }
//実験用保存先フォルダ名+練習か本番か
    public String ftext = "3-3";//被験者番号-実験回数
    public Boolean rensyuflg = true;

    //グローバル変数
    public static int[][] capmatrix = null;//静電容量値
    public static int[] flattermatrix = null;//1次元配列静電容量値
    public static double yo = 0;//OpenCVで求めたヨー角格納用。ピッチトリガーで使う


    public static double final_yo = 0;//最終的なヨー角代入用
    public static double final_pitch = 500;//最終的なピッチ代入用
    public static float size = 0;//楕円サイズ

    /////フラグ/////
    public static Boolean systemTrigger_flag = false;//押されている間かどうか=転送システムが起動しているか
    public LocalDeviceHandler localDeviceHandler = new LocalDeviceHandler();
    public static Boolean ymove_flg = false;//指の傾きが最大のとき&&0.5秒固定していたら、それより奥に0.5秒間隔で20ピクセルずつ動く判定フラグ
    public static Boolean xmove_flg = false;//指の傾きが最大のとき&&0.5秒固定していたら、それより奥に0.5秒間隔で20ピクセルずつ動く判定フラグ
    public Boolean errorflg = false;//楕円が認識されなかった時用

    public Boolean yo_hoseiflg = false;//ヨー角の傾きを補正して真上方向にするかどうか。trueなら補正をかける
    public Boolean syoki_flg = true;
    ///OpenCV///
    public RotatedRect box;
    public Bitmap bmpimage;

    //touch転送用変数
    public float syoki_touch_x, syoki_touch_y;
    public float touchcentx, touchcenty;
    public float size_first;
    public float touchcentx_first, touchcenty_first;
    public float move_x, move_y;
    public float kyori_x,kyori_y;
    public float pointer_x, pointer_y;//渡すx,y座標
    public float keisan_x, keisan_y;//ヨー角調整時使用
    public float syoki_pointerx;//初期位置
    public float syoki_pointery;
    public float syoki_finalx,syoki_finaly;
    public float syoki_keisanx, syoki_keisany;
    public float syoki_yo;
    public float pointer_finalx, pointer_finaly;//受け取った最終的なポインタ位置
    public float sa_y;
    public float sa_x;
    public float size_height;
    public float size_height_first;

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
    public int seikoukaisuu = 0;
    public StringBuilder pointer_kiseki = new StringBuilder();
    public String task_kekka;
    //public View view;
    public int imagecount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);//画面上部の邪魔なステータスバーなどを非表示
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//同上
        setContentView(R.layout.activity_main);

        //ランダムな位置にボタン//
        for (int a=0; a<35; a++) {
            arrayindex.add(a);//0~34まで
        }
        Collections.shuffle(arrayindex);//35この要素をランダムにシャッフル
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


        localDeviceHandler.startHandler();//静電容量リスナー（スレッド）起動
        seidenListener();

        //view = new View(getApplicationContext());
        //view = findViewById(R.id.frameLayout);

        ///補正on/offボタン+テキスト設定//
        TextView hoseitext = findViewById(R.id.textView2);
        hoseitext.setTextSize(30);
        Button onbutton = findViewById(R.id.buttonon);
        onbutton.setOnClickListener(v -> {
            yo_hoseiflg = true;
            task_count = 1;
            hoseitext.setText("指の向き補正ON");

        });
        Button offbutton = findViewById(R.id.buttonoff);
        offbutton.setOnClickListener(v -> {
            yo_hoseiflg = false;
            task_count = 1;
            hoseitext.setText("指の向き補正OFF");

        });

    }

    ////////////onCreate{}--end//////////////////////////


    public void buttonSet() {
        ///呼び出される時、各配列のindexをランダムに取り出して、それに対応する画面上の位置を決定//
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
            by = 150 * ((int) Math.floor(index / 7));//ボタンy座標
            //Log.d("ボタン座標", String.valueOf(bx) + " , " + String.valueOf(by) + "インデックス: " + String.valueOf(index));


            button.setX((float) bx);
            button.setY((float) by);
            buttonx = bx + bwidth;
            buttony = by + bheight;
        }
    }

/////////////////ここからタッチ時処理//////////////////////////////////

    public double[] sousaarray = new double[35];
    public double[] syujyukudo = new double[35];

    @Override
    public boolean onTouchEvent (MotionEvent e){//何らかのタッチ操作中ずっと動いている？？
        AnimationThread animationThread = new AnimationThread();

        ///pointer描画を50ミリ秒間隔でアニメーションさせる///////////////
        ///更新されたpointer_x,yを受け取って描画するだけ///
        Handler pointerhandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (systemTrigger_flag) {
                    if (syoki_flg == false) {
                        if (!(ymove_flg || xmove_flg)) {//アニメーションフラグじゃない時
                            pointer_x = syoki_pointerx - kyori_x;
                            pointer_y = syoki_pointery - kyori_y;
                            if (yo_hoseiflg) {//補正フラグの時、最終的なpointer座標を回転させる。
                                pointer_x = (float) ((Math.cos(Math.toRadians(syoki_yo)) * pointer_x) + (-Math.sin(Math.toRadians(syoki_yo)) * pointer_y));
                                pointer_y = (float) ((Math.sin(Math.toRadians(syoki_yo)) * pointer_x) + (Math.cos(Math.toRadians(syoki_yo)) * pointer_y));
                                Log.d("補正直後", String.valueOf(pointer_x) + " , " + String.valueOf(pointer_y));
                                //初期地点どうする？？//
                                //syoki_pointerx = (float) ((Math.cos(Math.toRadians(syoki_yo)) * syoki_pointerx) + (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
                                //syoki_pointery = (float) ((Math.sin(Math.toRadians(syoki_yo)) * syoki_pointerx) + (Math.cos(Math.toRadians(syoki_yo)) * syoki_pointery));
                            }
                        }
                    }else{//初回のみ
                        pointer_x = syoki_pointerx - kyori_x;
                        pointer_y = syoki_pointery - kyori_y;
                        if (yo_hoseiflg) {//補正フラグの時、最終的なpointer座標を回転させる。
                            pointer_x = (float) ((Math.cos(Math.toRadians(syoki_yo)) * pointer_x) + (-Math.sin(Math.toRadians(syoki_yo)) * pointer_y));
                            pointer_y = (float) ((Math.sin(Math.toRadians(syoki_yo)) * pointer_x) + (Math.cos(Math.toRadians(syoki_yo)) * pointer_y));
                            Log.d("補正直後", String.valueOf(pointer_x) + " , " + String.valueOf(pointer_y));
                            //syoki_pointerx = (float) ((Math.cos(Math.toRadians(syoki_yo)) * syoki_pointerx) + (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
                            //syoki_pointery = (float) ((Math.sin(Math.toRadians(syoki_yo)) * syoki_pointerx) + (Math.cos(Math.toRadians(syoki_yo)) * syoki_pointery));
                        }
                        syoki_pointerx = pointer_x;
                        syoki_pointery = pointer_y;
                        syoki_flg = false;
                    }

                    ///ポインター画面外に行かないように閾値設定///
                    if (!yo_hoseiflg) {
                        //ヨー角そのままの時//
                        if (pointer_x <= 70) {
                            pointer_x = 70;
                        } else if (pointer_x >= 1080) {
                            pointer_x = 1080;
                        }
                        if (pointer_y < 90) {
                            pointer_y = 90;
                        } else if (pointer_y >= 1350) {
                            pointer_y = 1350;
                        }
                        pointer_kiseki.append(String.valueOf(pointer_x) + " , " + String.valueOf(pointer_y) + " : ");

                        if (syoki_pointerx <= 70) {
                            syoki_pointerx = 70;
                        } else if (syoki_pointerx >= 1080) {
                            syoki_pointerx = 1080;
                        }
                        if (syoki_pointery < 90) {
                            syoki_pointery = 90;
                        } else if (syoki_pointery >= 1350) {
                            syoki_pointery = 1350;
                        }

                        pointer_finalx = pointer_x - 50;//受け取った転送先座標から画像の幅/2を引いて、座標を画像の真ん中に。
                        pointer_finaly = pointer_y - 50;

                        syoki_finalx = syoki_pointerx - 55;
                        syoki_finaly = syoki_pointery - 55;
                    }else{

                        //ヨー角そのままの時//
                        if (pointer_x <= -230) {
                            pointer_x = -230;
                        } else if (pointer_x >= 780) {
                            pointer_x = 780;
                        }
                        if (pointer_y < 90) {
                            pointer_y = 90;
                        } else if (pointer_y >= 1350) {
                            pointer_y = 1350;
                        }
                        pointer_kiseki.append(String.valueOf(pointer_x) + " , " + String.valueOf(pointer_y) + " : ");

                        if (syoki_pointerx <= -230) {
                            syoki_pointerx = -230;
                        } else if (syoki_pointerx >= 780) {
                            syoki_pointerx = 780;
                        }
                        if (syoki_pointery < 90) {
                            syoki_pointery = 90;
                        } else if (syoki_pointery >= 1350) {
                            syoki_pointery = 1350;
                        }

                        pointer_finalx = pointer_x + 250;//受け取った転送先座標から画像の幅/2を引いて、座標を画像の真ん中に。
                        pointer_finaly = pointer_y - 50;

                        syoki_finalx = syoki_pointerx + 245;
                        syoki_finaly = syoki_pointery - 55;
                    }
/**
 ///ここで、初期位置から微小な範囲(20*20)しか動いてないときは位置を更新せず、初期位置に固定する//
 if (syoki_pointerx - 5 <= pointer_x && pointer_x <= syoki_pointerx + 5 && syoki_pointery - 5 <= pointer_y && pointer_y <= syoki_pointery + 5) {
 pointer_finalx = syoki_finalx;
 pointer_finaly = syoki_finaly;
 }*/

                    if (!rensyuflg) {
                        //静電容量画像保存
        /////imageView配列に保管して、タスク終了時(text保存時と同じタイミング)に移動させたい
                        saveimageFile();
                        imagecount += 1;
                    }

                    //ポインター,初期枠画像位置設定+表示//
                    pointerimage.setTranslationX(pointer_finalx);
                    pointerimage.setTranslationY(pointer_finaly);
                    pointerimage.setVisibility(View.VISIBLE);

                    waku.setTranslationX(syoki_finalx);
                    waku.setTranslationY(syoki_finaly);
                    //waku.setVisibility(View.VISIBLE);

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
        Runnable long_press_receiver = new Runnable() {
            @Override
            public void run()
            {
                ///タッチダウン後0.5秒後、サイズ30以上なら起動→サイズの微調整は必要かも
                if (size >= 30 && systemTrigger_flag == false) {//タッチした時の輪郭サイズが30以上=指の腹で押されたならシステム開始

                    ///初期タッチ時系変数///
                    syoki_touch_x = e.getX();//初期位置計算でのみ使うタッチX座標
                    syoki_touch_y = e.getY();//初期位置計算でのみ使うタッチY座標
                    //touchcentx_first = touchcentx;//タッチ時の中心x
                    //touchcenty_first = touchcenty;//タッチ時の中心y
                    move_x = 0;
                    move_y = 0;
                    kyori_y = 0;
                    kyori_x = 0;

                    //size_first = size;//初期タッチ時のサイズ
                    //size_height_first = size_height;//初期タッチ時のサイズ高さ

                    ///データ保存用、システム起動時に1タスクの操作時間計測用に時間計測///
                    task_starttime = System.nanoTime();//システム起動時のシステム時間

                    ///初期タッチ時系変数///
                  syoki_yo = (float) yo;
                  float tan_theta = (float)Math.tan(Math.toRadians(syoki_yo));

                    //タッチした際のポインター初期位置表示//
                    pointer_x = syoki_touch_x - ((syoki_touch_y - 600) * tan_theta);
                    pointer_y = 600;//指の方向上の画面高さ半分くらいの位置に表示させたい

                    syoki_pointerx = pointer_x;//初期位置のpointerのx座標
                    syoki_pointery = pointer_y;//y座標

                    systemTrigger_flag = true;//システムトリガーフラグをtrueにするのはここだけ→システム起動はここだけ
                    syoki_flg = true;
                    //ポインター移動ハンドラ起動
                    pointerhandler.post(runnable);//ポインター描画ハンドラon
                    animationThread.start();//アニメーションスレッドをon

                }
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
            case (MotionEvent.ACTION_UP):

                if (systemTrigger_flag) {//systemTrigger_flag=trueの時、離れたらその点にタッチ転送

                    ////データ保存用：操作終了時間////
                    task_endtime = System.nanoTime();//システム終了時間計測

                    if (by <= pointer_finaly+50 && pointer_finaly+50 <= buttony && bx <= pointer_finalx +50 && pointer_finalx+50 <= buttonx){
                    //if (pointer_finaly >= by && pointer_y <= buttony && pointer_x >= bx && pointer_x <= buttonx) {
                        //データ保存用：成功回数カウント
                        seikoukaisuu += 1;
                        Log.d("seikou","システムタッチ成功");
                        trans_touchevent();
                    }
                    //システムトリガー終了し、諸々の処理を動かさないように//
                    systemTrigger_flag = false;

                    if (rensyuflg) {
                        ///習熟度計算////
                        int rot = task_count + 1;
                        for (int i = 0; i < 35; i++) {
                            if (rot == i) {
                                sousaarray[i] = sousa_time;
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
                        //習熟度計算終わり//
                    }else{
                        ///でーた保存///
                        double sousa_time = (double)(task_endtime - task_starttime) / (double)1000000000;

                        task_kekka = "\r\n"+"タスク"+String.valueOf(task_count+1)+ "\r\n"+"ターゲット座標: "+String.valueOf(bx)+" , "+String.valueOf(by)+"\r\n"+"操作時間: "+ String.valueOf(sousa_time)+"\r\n"+"成功回数: "+String.valueOf(seikoukaisuu)+"\r\n"+"ポインター軌跡:"+pointer_kiseki.toString();

                        saveFile();
                        //画像データ保存もimageview配列使ってここにしたい（画像データ1万枚近くを操作中に保存するのは処理思い原因になりそう）//

                        ////1タスク完了後データ等の処理//
                        ///軌跡系変数を初期化or消す//
                        pointer_kiseki = new StringBuilder();//インスタンス再生成してデータ消す
                        imagecount = 1;
                    }

                    //1タスク終了//
                    task_count += 1;
                    if (task_count == 35){
                        Toast.makeText(this, "セクション終了:お疲れさまでした", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplication(), SubActivity.class);
                        startActivity(intent);
                    }
                    buttonSet();
                }

///フラグや変数諸々初期化
                long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                pointerimage.setVisibility(View.GONE);
                systemTrigger_flag = false;
                xmove_flg = false;
                ymove_flg = false;
                kyori_y = 0;
                kyori_x = 0;
                animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。→止まってない。改善する


                break;
/////ムーブ///////////
            case MotionEvent.ACTION_MOVE:
                move_y = e.getY();
                move_x = e.getX();
                break;

            default:///上の条件どれにも当てはまらかった時タッチアップと同じ
                if (systemTrigger_flag) {//systemTrigger_flag=trueの時、離れたらその点にタッチ転送

                    ////データ保存用：操作終了時間////
                    task_endtime = System.nanoTime();//システム終了時間計測

                    if (by <= pointer_finaly+50 && pointer_finaly+50 <= buttony && bx <= pointer_finalx +50 && pointer_finalx+50 <= buttonx){
                        //if (pointer_finaly >= by && pointer_y <= buttony && pointer_x >= bx && pointer_x <= buttonx) {
                        //データ保存用：成功回数カウント
                        seikoukaisuu += 1;
                        Log.d("seikou","システムタッチ成功");
                        trans_touchevent();
                    }
                    //システムトリガー終了し、諸々の処理を動かさないように//
                    systemTrigger_flag = false;

                    if (rensyuflg) {
                        ///習熟度計算////
                        int rot = task_count + 1;
                        for (int i = 0; i < 35; i++) {
                            if (rot == i) {
                                sousaarray[i] = sousa_time;
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
                        //習熟度計算終わり//
                    }else{
                        ///でーた保存///
                        double sousa_time = (double)(task_endtime - task_starttime) / (double)1000000000;

                        task_kekka = "\r\n"+"タスク"+String.valueOf(task_count+1)+ "\r\n"+"ターゲット座標: "+String.valueOf(bx)+" , "+String.valueOf(by)+"\r\n"+"操作時間: "+ String.valueOf(sousa_time)+"\r\n"+"成功回数: "+String.valueOf(seikoukaisuu)+"\r\n"+"ポインター軌跡:"+pointer_kiseki.toString();

                        saveFile();
                        //画像データ保存もimageview配列使ってここにしたい（画像データ1万枚近くを操作中に保存するのは処理思い原因になりそう）//

                        ////1タスク完了後データ等の処理//
                        ///軌跡系変数を初期化or消す//
                        pointer_kiseki = new StringBuilder();//インスタンス再生成してデータ消す
                        imagecount = 1;
                    }

                    //1タスク終了//
                    task_count += 1;
                    if (task_count == 35){
                        Toast.makeText(this, "セクション終了:お疲れさまでした", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplication(), SubActivity.class);
                        startActivity(intent);
                    }
                    buttonSet();
                }

///フラグや変数諸々初期化
                long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                pointerimage.setVisibility(View.GONE);
                systemTrigger_flag = false;
                xmove_flg = false;
                ymove_flg = false;
                kyori_y = 0;
                kyori_x = 0;
                animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。→止まってない。改善する

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
                    ymove_flg = false;
                    xmove_flg = false;
                    long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                    pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                    animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。

                }
                Log.d("tag","buttonTouch");
                return false;
            }
        });//////button/////

        return false;
    }
////////////////ontouch{}--end/////////////////////////

///////////////////////////////////////////////関数//////////////////////////////////////
    // ファイルを保存関数//
    public void saveFile() {
        //ファイル保存用//

        String fileName = "task" + String.valueOf(String.format("%02d",task_count+1)) + ".txt";

        try {
            //text保存
            File extStrageDir =Environment.getExternalStorageDirectory();
            File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS+ "/"+ftext, fileName);//練習
            //File file = new File(getApplicationContext().getFilesDir()+"/1", fileName);
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS, fileName);//1回目
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_ALARMS, fileName);//2
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_MOVIES, fileName);//3
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_MUSIC, fileName);//4
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_NOTIFICATIONS, fileName);//5
            //File file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_PODCASTS, fileName);//6
// /data/data/com.example.tatp_practice
            // /sdcard/Download
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
    // ファイルを保存関数//
    public void saveimageFile() {
        //ファイル保存用//

        String image_fileName = "task" + String.valueOf(String.format("%02d",task_count+1)) + "_i_" + String.valueOf(String.format("%03d",imagecount)) + ".png";
        // try-with-resources
        try {

            //画像保存
            File extStrageDir =Environment.getExternalStorageDirectory();
            File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/"+ ftext, image_fileName);//練習
            //File i_file = new File(getApplicationContext().getFilesDir()+"/1", image_fileName);
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_DOWNLOADS, fileName);//1回目
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_ALARMS, fileName);//2
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_MOVIES, fileName);//3
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_MUSIC, fileName);//4
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_NOTIFICATIONS, fileName);//5
            //File i_file = new File(extStrageDir.getAbsolutePath() + "/" + Environment.DIRECTORY_PODCASTS, fileName);//6

            FileOutputStream outStream = new FileOutputStream(i_file);
            bmpimage.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
        }
        catch (IOException ioExceptione) {
            ioExceptione.printStackTrace();
        }
    }



    public void trans_touchevent(){//タッチ点転送
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


    //////静電容量取得し、ヨー角まで出してpitchトリガーを動かす//////
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
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();//contour = 輪郭List
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

                            size = (float)((box.size.height/2) * (box.size.width) * Math.PI);//楕円の面積で推定
                            //Log.d("パラメータ、サイズ", String.valueOf(size));
                            //Log.d("パラメータ", String.valueOf(size_first) + " , " +String.valueOf(size));

                            touchcentx = (float)box.center.x;
                            touchcenty = (float)box.center.y;
                            //Log.d("x,y", String.valueOf(touchcentx) +" , "+String.valueOf(touchcenty));
                            yo = 180 - box.angle;//yoに真上を0として左90の角度しまう

                            /////カーソル位置調整//////
                            y_kettei();//返り値:y_after
                            x_kettei();//返り値:pointer_x

                        } catch (Exception e) {//楕円取得できないときはExceptionできてる
                            //Toast.makeText(getApplicationContext(), "指が認識できません:画面端を触りすぎです",Toast.LENGTH_SHORT).show();
                            systemTrigger_flag = false;
                            trans_touchevent();
                            Log.d("exception", "楕円取得できずキャッチ");
                        }
                    }
                }
            }
        });
        /////////////静電容量リスナー処理終了。静電容量画像生成の度(50ms秒毎？)起動する。ここで取得するのは「ヨー角」と「輪郭の高さ」////////////
    }
    //y決定//

    public void y_kettei() {

        sa_y = syoki_touch_y - move_y;

        if (!ymove_flg) {
            if (sa_y < 15 && -25 < sa_y) {//yの移動量が-25、+15の範囲内なら基準速(指の下方向は動かしづらいから)
                kyori_y = (float) (sa_y * 3);
                //pointer_y = syoki_pointery - (sa_y * 3);//(sa_y * 3)が(float)kyori
            } else {//-25以下、+15以上のとき加速(3倍)
                kyori_y = (float) (sa_y * 4);
                //pointer_y = syoki_pointery - (sa_y * 4);
            }

        } else {//アニメーションフラグtrueの時
            if (sa_y >= 0) {
                //kyori_y += 20;
                pointer_y -= 20;
                syoki_pointery -= 20;
            } else {
                //kyori_y -= 20;
                pointer_y += 20;
                syoki_pointery += 20;
            }
        }
        /** else{//補正フラグありでアニメーションの時
         if (sa_y >= 0) {
         //Log.d("アニメーション", "y-0.1");
         pointer_y -= (float) (/*(Math.sin(Math.toRadians(syoki_yo)) * keisan_x) + (Math.cos(Math.toRadians(syoki_yo)) * 20));
         syoki_pointery -= (float) (/*(Math.sin(Math.toRadians(syoki_yo)) * syoki_keisanx) + (Math.cos(Math.toRadians(syoki_yo)) * 20));
         } else {
         //Log.d("アニメーション", "y+0.1");
         pointer_y += (float) (/*(Math.sin(Math.toRadians(syoki_yo)) * keisan_x) + (Math.cos(Math.toRadians(syoki_yo)) * 20));
         syoki_pointery += (float) (/*(Math.sin(Math.toRadians(syoki_yo)) * syoki_keisanx) + (Math.cos(Math.toRadians(syoki_yo)) * 20));
         }
         }*/


    }
    ///x座標を設定する関数////

    public void x_kettei() {
        sa_x = syoki_touch_x - move_x;//単純にタッチ点の移動幅を差に
        ///初期位置から30までは差をそのまま。それ以降は加速度的に速度を変えたい//
        if (!xmove_flg) {
            if (sa_x < 25 && -25 < sa_x) {//xの移動差がプラマイ25以下なら加速度なし。基準速(2倍)
                kyori_x = (float) (sa_x * 3);
                //pointer_x = syoki_pointerx - (float) (sa_x * 3);
            } else {//移動差が25以上のとき、大きく動かす(3倍){
                //pointer_x = syoki_pointerx - (float) (sa_x * 4);
                kyori_x = (float) (sa_x * 4);
            }

        } else {//アニメーションフラグtrueの時
            if (sa_x <= 0) {
                //Log.d("アニメーション", "x+0.1");
                pointer_x += 20;
                syoki_pointerx += 20;
                //kyori_x -= 20;
            } else {//移動差が25以上のとき、大きく動かす(3倍){
                //Log.d("アニメーション", "x-0.1");
                pointer_x -= 20;
                syoki_pointerx -= 20;
                //kyori_x += 20;
            }
        }
//この関数で渡すのはkyoriだけ。pointer座標を計算するのは描画スレッド。
        /**
         else{//補正フラグありでアニメーションの時
         if (sa_x <= 0) {
         pointer_x += (float) ((Math.cos(Math.toRadians(syoki_yo)) * 20) + (-Math.sin(Math.toRadians(syoki_yo)) * keisan_y));
         syoki_pointerx += (float) ((Math.cos(Math.toRadians(syoki_yo)) * 20) /*+ (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
         } else {

         pointer_x -= (float) ((Math.cos(Math.toRadians(syoki_yo)) * 20) /*+ (-Math.sin(Math.toRadians(syoki_yo)) * keisan_y));
         syoki_pointerx -= (float) ((Math.cos(Math.toRadians(syoki_yo)) * 20) /*+ (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
         }
         }*/

    }
    public void yohosei() {


    }


    public int[] getPix() {//bitmapに格納するpix配列にRGBデータ代入
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
    ///タッチダウンされたらスレッド開始して、タッチが終わったらやめる//
    class AnimationThread extends Thread{
        @Override public void run() {
            //Log.d("THreadフラグ" , String.valueOf(systemTrigger_flag));
            if (systemTrigger_flag) {
                while (systemTrigger_flag) {

                    try {
                        float startx = move_x;
                        float starty = move_y;

                        //float startx = pointer_x;
                        //float starty = pointer_y;

                        Thread.sleep(500);//指の傾きが固定されているか判定、0.5秒待つ


                        float endx = move_x;
                        float endy = move_y;

                        //float endx = pointer_x;
                        //float endy = pointer_y;


                        //xもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//
                        //float size_sa = Math.abs(start - end);
                        float x_sa = startx - endx;
                        //Log.d("サイズ差", String.valueOf(x_sa));

                        //yもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//
                        float y_sa = starty - endy;
                        //Log.d("サイズ差", String.valueOf(y_sa));
                        if (((-5 <= x_sa && x_sa <= 5)&&(-5 <= y_sa && y_sa <= 5))&& (!(sa_x < 25 && -25 < sa_x) || !(sa_y < 15 && -25 < sa_y))) {
                            //Log.d("xxx", String.valueOf(x_sa) + " , " + String.valueOf(pointer_x) +" , "+String.valueOf(syoki_pointerx));
                            if (/*-5 <= x_sa && x_sa <= 5 && */(pointer_x <= syoki_pointerx - 50 || syoki_pointerx + 50 <= pointer_x)) {//xの位置が変わってなく(プラマイ5以内)、pointerが加速領域に入ってたら
                                xmove_flg = true;//アニメーションフラグtrueに

                            } else {
                                xmove_flg = false;
                            }

                            if (/*-5 <= y_sa && y_sa <= 5 && */(pointer_y <= syoki_pointery - 50 || syoki_pointery + 50 <= pointer_y)) {//yの位置が変わってなく(プラマイ5以内)、pointerが加速領域に入ってたら
                                ymove_flg = true;//アニメーションフラグtrueに

                            } else {
                                ymove_flg = false;
                            }
                        }else{
                            xmove_flg = false;
                            ymove_flg = false;
                        }
                    } catch (InterruptedException interruptedException) {
                        Log.d("Thread", "スレッドを停止");
                        //このログを見たことは無い。機能していない//
                        return;
                    }

                }
            }
        }
}

}
