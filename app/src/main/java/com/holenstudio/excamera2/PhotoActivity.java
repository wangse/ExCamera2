package com.holenstudio.excamera2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.holenstudio.excamera2.adapter.ThumbnailAdapter;
import com.holenstudio.excamera2.filter.Filter;
import com.holenstudio.excamera2.model.ThumbnailItem;
import com.holenstudio.excamera2.util.CameraUtil;
import com.holenstudio.excamera2.util.ImageProcessUtil;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity {
    private final static String TAG = "PhotoActivity";

    private ImageView mImageView;
    private RecyclerView mThumbsView;
    private String imagePath;
    private Bitmap mOriginalBmp;
    BitmapFactory.Options mOptions;
    private ThumbnailAdapter.ThumbnailCallback mThumbnailCallback = new ThumbnailAdapter.ThumbnailCallback() {
        @Override
        public void onThumbnailClick(View view, Filter filter) {
            mImageView.setImageBitmap(filter.processFilter(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imagePath, mOptions), 640, 640, false)));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        initView();
        initData();
        initHorizontalList();
    }

    private void initView() {
        mImageView = (ImageView) findViewById(R.id.imageView);
        mThumbsView = (RecyclerView) findViewById(R.id.thumbnails);
    }


    @Override
    protected void onStart() {
        super.onStart();

        setImage();
    }

    private void initData() {
        imagePath = getIntent().getStringExtra("image_path");
    }

    private void setImage() {
        mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, mOptions);
        mOptions.inSampleSize = CameraUtil.calculateInSampleSize(mOptions, 300,
                300);
        mOptions.inJustDecodeBounds = false;
        mOriginalBmp = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imagePath, mOptions), 640, 640, false);
        mImageView.setImageBitmap(mOriginalBmp);
    }

    private void initHorizontalList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.scrollToPosition(0);
        mThumbsView.setLayoutManager(layoutManager);
        mThumbsView.setHasFixedSize(true);
        bindDataToAdapter();
    }

    private void bindDataToAdapter() {
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
//                BitmapFactory.Options tmpOption = new BitmapFactory.Options();
//                tmpOption.inJustDecodeBounds = true;
//                BitmapFactory.decodeFile(imagePath, tmpOption);
//                mOptions.inSampleSize = CameraUtil.calculateInSampleSize(mOptions, 100,
//                        100);
//                tmpOption.inJustDecodeBounds = false;
                Bitmap thumbImage = Bitmap.createScaledBitmap(mOriginalBmp, 150, 150, false);
                List<ThumbnailItem> thumbs = new ArrayList<>();
                ThumbnailItem t1 = new ThumbnailItem();
                ThumbnailItem t2 = new ThumbnailItem();
                ThumbnailItem t3 = new ThumbnailItem();
                ThumbnailItem t4 = new ThumbnailItem();
                ThumbnailItem t5 = new ThumbnailItem();
                ThumbnailItem t6 = new ThumbnailItem();

                t1.image = thumbImage;
                t2.image = thumbImage;
                t3.image = thumbImage;
                t4.image = thumbImage;
                t5.image = thumbImage;
                t6.image = thumbImage;

                t1.text = "Original";
                thumbs.add(t1);
                t2.filter = ImageProcessUtil.getStarLitFilter(thumbImage);
                t2.text = "StarLit";
                thumbs.add(t2);
                t3.filter = ImageProcessUtil.getBlueMessFilter(thumbImage);
                t3.text = "BlueMess";
                thumbs.add(t3);
                t4.filter = ImageProcessUtil.getAweStruckVibeFilter(thumbImage);
                t4.text = "AweStruck";
                thumbs.add(t4);
                t5.filter = ImageProcessUtil.getLimeStutterFilter(thumbImage);
                t5.text = "LimeStutter";
                thumbs.add(t5);
                t6.filter = ImageProcessUtil.getNightWhisperFilter(thumbImage);
                t6.text = "NightWhisper";
                thumbs.add(t6);

                ThumbnailAdapter adapter = new ThumbnailAdapter(PhotoActivity.this, thumbs);
                adapter.setThumbnailCallback(mThumbnailCallback);
                mThumbsView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }


}
