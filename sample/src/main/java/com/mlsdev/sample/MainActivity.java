package com.mlsdev.sample;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mlsdev.rximagepicker.RxImageConverters;
import com.mlsdev.rximagepicker.RxImagePicker;
import com.mlsdev.rximagepicker.Sources;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Timed;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private ImageView ivPickedImage;
    private RadioGroup converterRadioGroup;
    private Button exitBtn;
    private PublishSubject<Integer> exitSubject = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exitBtn = (Button) findViewById(R.id.btn_exit);

        ivPickedImage = (ImageView) findViewById(R.id.iv_picked_image);
        FloatingActionButton fabCamera = (FloatingActionButton) findViewById(R.id.fab_pick_camera);
        FloatingActionButton fabGallery = (FloatingActionButton) findViewById(R.id.fab_pick_gallery);
        converterRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
        converterRadioGroup.check(R.id.radio_uri);

        exitBtn.setOnClickListener(view -> onBackPressed());
        fabCamera.setOnClickListener(view -> pickImageFromSource(Sources.CAMERA));
        fabGallery.setOnClickListener(view -> pickImageFromSource(Sources.GALLERY));

//        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        activityManager.re
    }

    @Override
    public void onBackPressed() {
        if (exitSubject.hasObservers()) {
            exitSubject.onNext(0);
        } else {// 若没有订阅者
            exitSubject
                    .timeInterval()// 控制事件之间的时间间隔
                    .subscribe(new Consumer<Timed<Integer>>() {
                        @Override
                        public void accept(Timed<Integer> integerTimed) throws Exception {
                            if (integerTimed.time(TimeUnit.MILLISECONDS) < 500) {
                                finish();
                                System.exit(0);
                            } else {
                                Toast.makeText(MainActivity.this, "再按一次退出App", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            Toast.makeText(MainActivity.this, "再按一次退出App", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickImageFromSource(Sources source) {
        RxImagePicker.with(getFragmentManager()).requestImage(source)
//                .subscribe(new Consumer<Uri>() {
//                    @Override
//                    public void accept(Uri uri) throws Exception {
//                        Glide.with(MainActivity.this)
//                                .load(uri)
//                                .into(ivPickedImage);
//                    }
//              });
                .flatMap(new Function<Uri, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Uri uri) throws Exception {
                        switch (converterRadioGroup.getCheckedRadioButtonId()) {
                            case R.id.radio_file:
                                // convert uri to file
                                return RxImageConverters.uriToFile(MainActivity.this, uri, createTempFile());
                            case R.id.radio_bitmap:
                                // convert uri to bitmap
                                return RxImageConverters.uriToBitmap(MainActivity.this, uri);
                        }
                        // load image from uri
                        return Observable.just(uri);
                    }
                })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        MainActivity.this.onImagePicked(o);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(MainActivity.this, String.format("Error: %s", throwable), Toast.LENGTH_SHORT).show();
                    }
                });
//                .flatMap(uri -> {
//                    switch (converterRadioGroup.getCheckedRadioButtonId()) {
//                        case R.id.radio_file:
//                            return RxImageConverters.uriToFile(MainActivity.this, uri, createTempFile());
//                        case R.id.radio_bitmap:
//                            return RxImageConverters.uriToBitmap(MainActivity.this, uri);
//                        default:
//                            return Observable.just(uri);
//                    }
//                })
//                // 这里是 java8 的语法，基本格式是： 类名::引用方法名 。
//                // 也可以写成 o -> MainActivity.this.onImagePicked(o)
//                .subscribe(this::onImagePicked,
//                        throwable -> Toast.makeText(MainActivity.this, String.format("Error: %s", throwable), Toast.LENGTH_LONG).show());
    }

    private void onImagePicked(Object result) {
        Toast.makeText(this, String.format("Result: %s", result), Toast.LENGTH_LONG).show();
        if (result instanceof Bitmap) {
            ivPickedImage.setImageBitmap((Bitmap) result);
        } else {
            Glide.with(this)
                    .load(result) // works for File or Uri
                    .crossFade()
                    .into(ivPickedImage);
        }
    }

    private File createTempFile() {
        return new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + "_image.jpeg");
    }

}
