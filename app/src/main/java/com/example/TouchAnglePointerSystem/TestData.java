package com.example.TouchAnglePointerSystem;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    // シングルトンパターンでインスタンスを共通化
    private static TestData instance = new TestData();

    // コンストラクタ。ここでは何もしない
    private TestData(){};

    public static TestData getInstance(){
        return instance;
    }
    // ここまでの処理で、共通化（シングルトン）を行う

    // テスト問題データの配列
    public  List<List<String>> trajectorylist = new ArrayList<>();
    public List<byte[]> sectionimagearray = new ArrayList<>();

    /**
     * ゲッター
     * @return 保存されているテストデータ
     */
    public List<List<String>> getTrajectorylist() {
        return trajectorylist;
    }

    /**
     * セッター
     * @param trajectorylist 保存するテストデータ
     */
    public void setTrajectorylist(List<List<String>> trajectorylist) {
        this.trajectorylist = trajectorylist;
    }

    /**
     * ゲッター
     * @return 保存されているテストデータ
     */
    public List<byte[]> getSectionimagearray() {
        return sectionimagearray;
    }

    /**
     * セッター
     * @param sectionimagearray 保存するテストデータ
     */
    public void setSectionimagearray(List<byte[]> sectionimagearray) {
        this.sectionimagearray = sectionimagearray;
    }
}
