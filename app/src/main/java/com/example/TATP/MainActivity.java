package com.example.TATP;

import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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

    /**
     * 一応ビルドできる、いらないやつ②つ生成されてた
     */
    //グローバル変数
    public static int[][] capmatrix = null;//静電容量値
    public static int[] flattermatrix = null;//1次元配列静電容量値
    public static double after_yo = 0;//OpenCVで求めたヨー角格納用。ピッチトリガーで使う


    public static double final_yo = 0;//最終的なヨー角代入用
    public static double final_pitch = 500;//最終的なピッチ代入用
    public static float size = 0;//楕円サイズ

    /////フラグ/////
    public static Boolean touchdown_flag = false;//押されている間かどうか
    public LocalDeviceHandler localDeviceHandler = new LocalDeviceHandler();
    public static Boolean ymove_flg = false;//指の傾きが最大のとき&&0.5秒固定していたら、それより奥に0.5秒間隔で20ピクセルずつ動く判定フラグ
    public static Boolean xmove_flg = false;//指の傾きが最大のとき&&0.5秒固定していたら、それより奥に0.5秒間隔で20ピクセルずつ動く判定フラグ
    public Boolean errorflg = false;//楕円が認識されなかった時用

    public Boolean yo_hoseiflg = false;//ヨー角の傾きを補正して真上方向にするかどうか。trueなら補正をかける

    ///OpenCV///
    public RotatedRect box;
    public Bitmap bmpimage;

    //touch転送用変数
    public float syoki_touch_x, syoki_touch_y;
    public float touchcentx, touchcenty;
    public float size_first;
    public float touchcentx_first, touchcenty_first;
    public float move_x, move_y;
    public float pointer_x, pointer_y;//渡すx,y座標
    public float keisan_x, keisan_y;//ヨー角調整時使用
    public float syoki_pointerx;//初期位置
    public float syoki_pointery;
    public float syoki_yo;
    public float pointer_finalx, pointer_finaly;//受け取った最終的なポインタ位置
    public float sa_y;
    public float sa_x;
    public float size_height;
    public float size_height_first;

    static final int SAIBYOUGA_KANKAKU_MS = 50; //pointerの再描画の間隔（ms）。小さいほどアニメーションが滑らかに。

    public static ImageView pointerimage;
    public static ImageView waku;

    //画像用
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
    public View view;
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

        view = new View(getApplicationContext());
        view = findViewById(R.id.frameLayout);

    }

    ////////////onCreate{}--end//////////////////////////

    /////Tablelayout///////
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



    public double[] sousaarray = new double[35];
    public double[] syujyukudo = new double[35];

    @Override
    public boolean onTouchEvent (MotionEvent e){//何らかのタッチ操作中ずっと動いている？？
        AnimationThread animationThread = new AnimationThread();

        ///pointer描画を50ミリ秒間隔でアニメーションさせる///////////////
        ///保の処理で更新されたx,y_afterを受け取って描画するだけ///
        Handler pointerhandler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (touchdown_flag) {
                       if (yo_hoseiflg) {
                           //ヨー角を調整する時//
                           /**
                           if (keisan_x <= 70) {
                               keisan_x = 70;
                           } else if (keisan_x >= 1110) {
                               keisan_x = 1110;
                           }
                           if (keisan_y < 90) {
                               keisan_y = 90;
                           } else if (keisan_y >= 1350) {
                               keisan_y = 1350;
                           }
                           pointer_finalx = keisan_x - 50;
                           pointer_finaly = keisan_y - 50;
                           pointer_kiseki.append(String.valueOf(keisan_x) + " , " + String.valueOf(keisan_y) + " : ");
                            */
                           //初期位置ヨー角補正//
                           /**
                           pointer_x = (float) ((Math.cos(Math.toRadians(syoki_yo)) * pointer_x) + (-Math.sin(Math.toRadians(syoki_yo)) * pointer_y));
                           pointer_y = (float) ((Math.sin(Math.toRadians(syoki_yo)) * pointer_x) + (Math.cos(Math.toRadians(syoki_yo)) * pointer_y));
                           if (xmove_flg) {
                               syoki_pointerx = (float) ((Math.cos(Math.toRadians(syoki_yo)) * syoki_pointerx) + (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
                           }
                           if (ymove_flg) {
                               syoki_pointery = (float) ((Math.sin(Math.toRadians(syoki_yo)) * syoki_pointerx) + (Math.cos(Math.toRadians(syoki_yo)) * syoki_pointery));
                           }
                           pointer_finalx = pointer_x -50;
                           pointer_finaly = pointer_y -50;
                            */
                       } else {

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

                           //静電容量保存
                           saveimageFile();
                           imagecount += 1;

                           pointer_finalx = pointer_x - 50;//受け取った転送先座標から画像の幅/2を引いて、座標を画像の真ん中に。
                           pointer_finaly = pointer_y - 50;

                       }
                    ///ここで、初期位置から微小な範囲(20*20)しか動いてないときは位置を更新せず、初期位置に固定する//
                    if (syoki_pointerx - 5 <= pointer_x && pointer_x <= syoki_pointerx + 5 && syoki_pointery - 5 <= pointer_y && pointer_y <= syoki_pointery + 5) {
                        pointer_finalx = syoki_pointerx;
                        pointer_finaly = syoki_pointery;
                    }
                    pointerimage.setTranslationX(pointer_finalx);
                    pointerimage.setTranslationY(pointer_finaly);
                    pointerimage.setVisibility(View.VISIBLE);


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
                    waku.setTranslationX(syoki_pointerx - 55);
                    waku.setTranslationY(syoki_pointery - 55);
                    waku.setVisibility(View.VISIBLE);
                    Log.d("pointer",String.valueOf(pointer_finalx) +" , "+ String.valueOf(pointer_finaly));
                    Log.d("syokipointer",String.valueOf(syoki_pointerx) +" , "+ String.valueOf(syoki_pointery));

                    pointerhandler.postDelayed(this, SAIBYOUGA_KANKAKU_MS);//0.05秒間隔でアニメーションしてる

                }else{
                    pointerimage.setVisibility(View.GONE);//指が離れたらポインタ隠す
                    waku.setVisibility(View.GONE);
                    //pointer_finalx = syoki_pointerx;
                    //pointer_finaly = syoki_pointery;
                }
            }
        };

        long LONG_PRESS_TIME = 500;    // 長押し時間（ミリ秒）
        Handler long_press_handler = new Handler();
        Runnable long_press_receiver = new Runnable() {
            @Override
            public void run()
            {
                ///タッチダウン後0.3秒後、サイズ20以上なら起動
                //森先生起動しないので30以上の時40°以下のとき起動に→自分の人差し指の大きさくらい
                if (size >= 30 && touchdown_flag == false) {//タッチした時の輪郭サイズが20以上=指の腹で押されたならシステム開始

                    ///初期タッチ時系変数///
                    syoki_touch_x = e.getX();//初期位置計算でのみ使うタッチX座標
                    syoki_touch_y = e.getY();//初期位置計算でのみ使うタッチY座標
                    touchcentx_first = touchcentx;//タッチ時の中心x
                    touchcenty_first = touchcenty;//タッチ時の中心y
                    move_x = 0;
                    move_y = 0;

                    task_starttime = System.nanoTime();//システム起動時のシステム時間

                    //Toast.makeText(MainActivity.this, String.valueOf(task_count+1)+"回目:転送システム起動:指の傾きでポインタ操作可能", Toast.LENGTH_SHORT).show();//ポップアップ的な通知見せる
                    ///長押しタッチ時、フラグ初期化///
                    size_first = size;//初期タッチ時のサイズ
                    size_height_first = size_height;//初期タッチ時のサイズ高さ


                    ///初期タッチ時系変数///
                  //  Log.d("ヨー", String.valueOf(after_yo)+ " , "+String.valueOf(box.angle));
                    syoki_yo = (float)after_yo;
                    float tan_theta = (float)Math.tan(Math.toRadians(syoki_yo));

                    //タッチした際のポインター初期位置表示//
                    pointer_x = syoki_touch_x - ((syoki_touch_y - 600) * tan_theta);
                    //pointer_x = 500;
                    pointer_y = 600;

                    //pointer_finalx = pointer_x -50;//受け取った転送先座標から画像の幅/2を引いて、座標を画像の真ん中に。
                    //pointer_finaly = pointer_y - 50;

                    //pointerimage.setTranslationX(pointer_finalx);
                    //pointerimage.setTranslationY(pointer_finaly);
                    //Log.d("初期ポインター", String.valueOf(pointerimage.getTranslationX()) + " , " + String.valueOf(pointerimage.getTranslationY()));
                    //pointer_kiseki.append(String.valueOf(pointer_x) + " , " + String.valueOf(pointer_y) + " : ");

                    syoki_pointerx = pointer_x;//初期位置のpointerのx座標
                    syoki_pointery = pointer_y;//y座標
                    //pointerimage.setVisibility(View.VISIBLE);

                    touchdown_flag = true;//touch_downをtrueにするのはここだけ→システム起動はここだけ
                    //ポインター移動ハンドラ起動
                    pointerhandler.post(runnable);
                    animationThread.start();//アニメーションスレッドをon

                }
            }
        };
if (errorflg){
    Toast.makeText(getApplicationContext(), "指が認識されませんやり直してください:画面端に寄りすぎです", Toast.LENGTH_SHORT).show();
    errorflg=false;
}


        switch (e.getAction()) {
/////////タッチダウン////////////
            case MotionEvent.ACTION_DOWN:

                long_press_handler.postDelayed( long_press_receiver, LONG_PRESS_TIME);       // 0.3秒長押し判定

                break;

//////タッチアップ////////
            case (MotionEvent.ACTION_UP):
                //Log.d("タッチアップ","タッチアップ");
                ////ヨーとピッチが確定して転送先決定した後、指を離したらタッチ判定を起こす////
                if (touchdown_flag) {//touchdown_flag=trueの時、離れたらその点にタッチ転送
                    ////データ保存用////
                    task_endtime = System.nanoTime();//システム終了時間計測

                    //////////////////
                    if (pointer_y >= by && pointer_y <= buttony && pointer_x >= bx && pointer_x <= buttonx) {
                        seikoukaisuu += 1;
                        trans_touchevent();

                        //Log.d("転送", "タッチ転送した。最後：ポイント点" + String.valueOf(pointer_x) + " , " + String.valueOf(y_after));
                    }
                    touchdown_flag = false;

                    double sousa_time = (double)(task_endtime - task_starttime) / (double)1000000000;
                    //Log.d("button座標",String.valueOf(bx)+" , "+String.valueOf(by));
                    task_kekka = "\r\n"+"タスク"+String.valueOf(task_count+1)+ "\r\n"+"ターゲット座標: "+String.valueOf(bx)+" , "+String.valueOf(by)+"\r\n"+"操作時間: "+ String.valueOf(sousa_time)+"\r\n"+"成功回数: "+String.valueOf(seikoukaisuu)+"\r\n"+"ポインター軌跡:"+pointer_kiseki.toString();

                    ///習熟度計算////
                    int rot = task_count+1;

                    for (int i=0; i<35; i++){
                        if (rot == i) {
                            sousaarray[i] = sousa_time;
                        }
                    }

                    if ((rot % 2) == 0) {

                        syujyukudo[rot] =(sousaarray[rot] / sousaarray[rot/2]) * 100;

                        //Log.d("操作時間", String.valueOf(sousaarray[rot])+" / "+String.valueOf(sousaarray[rot/2]));

                        //Log.d("習熟度", "sousaarray　"+String.valueOf(sousaarray[rot])+"習熟度"+String.valueOf(syujyukudo[rot])+" / "+String.valueOf(syujyukudo[rot-2]));
                        if (rot >= 2) {
                            if (Math.floor(syujyukudo[rot]) <= Math.floor(syujyukudo[(rot - 2)]) +3 && Math.floor(syujyukudo[rot]) >= Math.floor(syujyukudo[(rot - 2)]) -3) {

                                Toast.makeText(this, "習熟度発散: ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    //習熟度計算終わり//



                    ///でーた保存///
                    saveFile();

                    ////1タスク完了後データ等の処理//
                    ///軌跡系変数を初期化or消す//
                    pointer_kiseki = new StringBuilder();//インスタンス再生性してデータ消す
                    imagecount = 1;
                    //結果データ格納、保存//
                    //Log.d("結果", task_kekka);
                    task_count += 1;
                    if (task_count == 35){
                        Toast.makeText(this, "セクション終了:お疲れさまでした", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplication(), SubActivity.class);
                        startActivity(intent);
                    }
                    buttonSet();
                }


                long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                pointerimage.setVisibility(View.GONE);
                touchdown_flag = false;
                xmove_flg = false;
                ymove_flg = false;

                animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。


                break;
/////ムーブ///////////
            case MotionEvent.ACTION_MOVE:
                move_y = e.getY();
                move_x = e.getX();
                break;

            default:///上の条件どれにも当てはまらかった時タッチアップと同じ

                ////ヨーとピッチが確定して転送先決定した後、指を離したらタッチ判定を起こす////
                if (touchdown_flag) {//touchdown_flag=trueの時、離れたらその点にタッチ転送

                    if (pointer_y >= by && pointer_y <= buttony && pointer_x >= bx && pointer_x <= buttonx) {
                        trans_touchevent();
                        //Log.d("転送", "タッチ転送した。最後：ポイント点" + String.valueOf(pointer_x) + " , " + String.valueOf(y_after));
                    }
                    //指を離したらフラグ最初期化→一旦リセット//////
                }
                long_press_handler.removeCallbacks( long_press_receiver );    // 長押し中に指を上げたら長押しhandlerの処理を中止
                pointerhandler.removeCallbacks(runnable);//指を離したらhandler停止
                pointerimage.setVisibility(View.GONE);
                touchdown_flag = false;
                animationThread.interrupt();//加速度スレッドをinterruptに強制的に移す。と、例外処理を認識してスレッドが止まる。


        }
        //////button押された時の動き////
        //ボタンの範囲でタッチ判定起きたらよびだされる呼び出される→多分もっとスマートな実装ある
        // lambda式
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {

                    //////button押された時の動き////
                    //Log.d("systouch", "systemタッチした");
                    touchdown_flag = false;

                }else if (event.getAction() == MotionEvent.ACTION_UP){
                    Log.d("アップ", "システムアップ");
                    ///楕円検出できなかったらこれを呼び出す。全リセット//
                    touchdown_flag = false;
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
            File extStrageDir =
                    Environment.getExternalStorageDirectory();
            File file = new File(
                    extStrageDir.getAbsolutePath()
                            + "/" + Environment.DIRECTORY_DOWNLOADS,
                    fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            //bmpimage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(task_kekka);
            writer.flush();
            writer.close();
            //FileOutputStream outputStream = openFileOutput(fileName, MODE_PRIVATE);
            //OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            //writer.write(task_kekka);
            //writer.flush();
            //writer.close();

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
            File extStrageDir =
                    Environment.getExternalStorageDirectory();
            File i_file = new File(
                    extStrageDir.getAbsolutePath()
                            + "/" + Environment.DIRECTORY_DOWNLOADS,
                    image_fileName);
            FileOutputStream outStream = new FileOutputStream(i_file);
            bmpimage.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
        }
        catch (IOException ioExceptione) {
            ioExceptione.printStackTrace();
        }
    }



    public void trans_touchevent(){//タッチ点転送
        if (touchdown_flag) {
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
                            after_yo = 180 - box.angle;//yoに真上を0として左90の角度しまう

                            /////カーソル位置調整//////
                            y_kettei();//返り値:y_after
                            x_kettei();//返り値:pointer_x

                        } catch (Exception e) {//楕円取得できないときはExceptionできてる
                            //Toast.makeText(getApplicationContext(), "指が認識できません:画面端を触りすぎです",Toast.LENGTH_SHORT).show();
                            touchdown_flag = false;
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
        //ヨー角部回転//
        //sa_y = (syoki_touch_y - move_y) / (float) Math.cos(Math.toRadians(syoki_yo));

        //Log.d("sa_y差","first: "+ String.valueOf(touch_y)+" , move: "+String.valueOf(move_y)+ " , 差: " +String.valueOf(s_a_y));
        if (!yo_hoseiflg){//ヨー補正なし
        if (!ymove_flg) {
            if (sa_y < 15 && -25 < sa_y) {//yの移動量が-25、+15の範囲内なら基準速(指の下方向は動かしづらいから)
                pointer_y = syoki_pointery - (sa_y * 3);
            } else {//-25以下、+15以上のとき加速(3倍)
                pointer_y = syoki_pointery - (sa_y * 4);
            }
        } else {//アニメーションフラグtrueの時
            if (sa_y >= 0) {
                //Log.d("アニメーション", "y-0.1");
                pointer_y -= 20;
                syoki_pointery -= 20;

            } else {
                //Log.d("アニメーション", "y+0.1");
                pointer_y += 20;
                syoki_pointery += 20;

            }


        }}else{//よー補正あり
                /**if (yo_hoseiflg) {
                 ///行列計算してθ分回転させる///
                 keisan_y = (float) ((Math.sin(Math.toRadians(syoki_yo)) * pointer_x) + (Math.cos(Math.toRadians(syoki_yo)) * pointer_y));

                 }
                 if (keisan_y < 90) {
                 keisan_y = 90;
                 } else if (keisan_y >= 1350) {
                 keisan_y = 1350;
                 }
                 if (syoki_pointery < 90) {
                 syoki_pointery = 90;
                 } else if (syoki_pointery >= 1350) {
                 syoki_pointery = 1350;
                 }
                 if (yo_hoseiflg) {
                 //syoki_pointery = (float) ((Math.sin(Math.toRadians(syoki_yo)) * syoki_pointerx) + (Math.cos(Math.toRadians(syoki_yo)) * syoki_pointery));
                 }*/
            }
    }
    ///x座標を設定する関数////

    public void x_kettei() {
        sa_x = syoki_touch_x - move_x;//単純にタッチ点の移動幅を差に

        //自分の親指で最大40くらい、つまり一般的な指だとしたら35くらいの差が最大。つまり25以上は加速度大きくしないといけない
        //Log.d("x差","初期タッチx: "+String.valueOf(syoki_touch_x)+" , 移動タッチｘ"+String.valueOf(move_x));

        ///初期位置から30までは差をそのまま。それ以降は加速度的に速度を変えたい//
        if (!yo_hoseiflg){//よー補正なし
        if (!xmove_flg) {
            if (sa_x < 25 && -25 < sa_x) {//xの移動差がプラマイ25以下なら加速度なし。基準速(2倍)
                pointer_x = syoki_pointerx - (float) (sa_x * 3);
            } else {//移動差が25以上のとき、大きく動かす(3倍){
                pointer_x = syoki_pointerx - (float) (sa_x * 4);
            }
        }else {//アニメーションフラグtrueの時
            if (sa_x <= 0) {
                //Log.d("アニメーション", "x+0.1");
                pointer_x += 20;
                syoki_pointerx += 20;

            } else {//移動差が25以上のとき、大きく動かす(3倍){
                //Log.d("アニメーション", "x-0.1");
                pointer_x -= 20;
                syoki_pointerx -= 20;

            }
        }}else {//よー補正あり
            /**
             if (yo_hoseiflg) {
             ///行列計算してθ分回転させる///
             keisan_x = (float) ((Math.cos(Math.toRadians(syoki_yo)) * pointer_x) + (-Math.sin(Math.toRadians(syoki_yo)) * pointer_y));

             }
             if (keisan_x <= 70) {
             keisan_x = 70;
             } else if (keisan_x >= 1080) {
             keisan_x = 1080;
             }
             if (syoki_pointerx <= 70) {
             syoki_pointerx = 70;
             } else if (syoki_pointerx >= 1080) {
             syoki_pointerx = 1080;
             }*/
            /**
             if (yo_hoseiflg) {
             //syoki_pointerx = (float) ((Math.cos(Math.toRadians(syoki_yo)) * syoki_pointerx) + (-Math.sin(Math.toRadians(syoki_yo)) * syoki_pointery));
             }*/
        }
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
            //Log.d("THreadフラグ" , String.valueOf(touchdown_flag));
            if (touchdown_flag) {
                while (touchdown_flag) {
                    try {
                        float startx = move_x;
                        float starty = move_y;


                        Thread.sleep(500);//指の傾きが固定されているか判定、0.5秒待つ


                        float endx = move_x;
                        float endy = move_y;


                        //xもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//
                        //float size_sa = Math.abs(start - end);
                        float x_sa = startx - endx;
                        //Log.d("サイズ差", String.valueOf(x_sa));

                        //Log.d("xxx", String.valueOf(x_sa) + " , " + String.valueOf(pointer_x) +" , "+String.valueOf(syoki_pointerx));
                        if (-5 <= x_sa && x_sa <= 5 && (pointer_x <= syoki_pointerx - 50 || syoki_pointerx + 50 <= pointer_x)) {//xの位置が変わってなく(プラマイ5以内)、pointerが加速領域に入ってたら
                            xmove_flg = true;//アニメーションフラグtrueに
                            //Log.d("アニメーション", "xアニメーション入った");
                            /**
                            if (pointer_x >= syoki_pointerx + 25) {
                                pointer_x += 30;
                            } else {
                                pointer_x -= 30;
                            }*/
                        } else {
                            xmove_flg = false;
                        }

                        //yもっと動かすアニメーション//
                        //0.5秒後のタッチしている指の座標が変わっていなかったら指がほぼ動いていなかったら//
                        float y_sa = starty - endy;
                        //Log.d("サイズ差", String.valueOf(y_sa));

                        if (-5 <= y_sa && y_sa <= 5 && (pointer_y <= syoki_pointery - 50 || syoki_pointery + 50 <= pointer_y)) {//yの位置が変わってなく(プラマイ5以内)、pointerが加速領域に入ってたら
                            ymove_flg = true;//アニメーションフラグtrueに
                            //Log.d("アニメーション", "yアニメーション入った");
                            /**
                            if (pointer_y >= syoki_pointery + 15) {
                                pointer_y += 30;
                            } else {
                                pointer_y -= 30;
                            }*/
                        } else {
                            ymove_flg = false;
                        }

                    } catch (InterruptedException interruptedException) {
                        Log.d("Thread", "スレッドを停止");
                        //このログを見たことは無い。機能していない//
                        return;
                    }

                    // }
                }
            }
        }
}

}
