package com.holenstudio.excamera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.holenstudio.excamera2.adapter.ThumbnailAdapter;
import com.holenstudio.excamera2.model.ThumbnailItem;
import com.holenstudio.excamera2.util.CameraUtil;
import com.holenstudio.excamera2.util.FileUtil;
import com.holenstudio.excamera2.util.ImageProcessUtil;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends Activity {

    private ImageView mImageView;
    private RecyclerView mThumbsView;
    private String imagePath;
    private Bitmap mOriginalBmp;
    private Bitmap mThumbBmp;
    BitmapFactory.Options mOptions;
    private ThumbnailAdapter.ThumbnailCallback mThumbnailCallback = new ThumbnailAdapter.ThumbnailCallback() {
        @Override
        public void onThumbnailClick(View view, int position) {
            int width = mImageView.getWidth();
            int height = mImageView.getHeight();
            Bitmap imageBmp = null;
            if (position == 0) {
                imageBmp = mOriginalBmp;
            } else if (position == 1) {
                imageBmp = ImageProcessUtil.getStarLitBitmap(mOriginalBmp, width, height);
            } else if (position == 2) {
                imageBmp = ImageProcessUtil.getBlueMessBitmap(mOriginalBmp, width, height);
            } else if (position == 3) {
                imageBmp = ImageProcessUtil.getAweStruckVibeBitmap(mOriginalBmp, width, height);
            } else if (position == 4) {
                imageBmp = ImageProcessUtil.getLimeStutterBitmap(mOriginalBmp, width, height);
            } else if (position == 5) {
                imageBmp = ImageProcessUtil.getNightWhisperBitmap(mOriginalBmp, width, height);
            }
            mImageView.setImageBitmap(imageBmp);
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
        mOriginalBmp = BitmapFactory.decodeFile(imagePath, mOptions);
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
                BitmapFactory.Options tmpOption = new BitmapFactory.Options();
                tmpOption.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, tmpOption);
                mOptions.inSampleSize = CameraUtil.calculateInSampleSize(mOptions, 200,
                        200);
                tmpOption.inJustDecodeBounds = false;
                Bitmap thumbImage = BitmapFactory.decodeFile(imagePath, tmpOption);
                List<ThumbnailItem> thumbs = new ArrayList<>();
                ThumbnailItem t1 = new ThumbnailItem();
                ThumbnailItem t2 = new ThumbnailItem();
                ThumbnailItem t3 = new ThumbnailItem();
                ThumbnailItem t4 = new ThumbnailItem();
                ThumbnailItem t5 = new ThumbnailItem();
                ThumbnailItem t6 = new ThumbnailItem();

                t1.image = thumbImage;
                t1.text = "Original";
                thumbs.add(t1);
                t2.image = ImageProcessUtil.getStarLitBitmap(thumbImage, 200, 200);
                t2.text = "StarLit";
                thumbs.add(t2);
                t3.image = ImageProcessUtil.getBlueMessBitmap(thumbImage, 200, 200);
                t3.text = "BlueMess";
                thumbs.add(t3);
                t4.image = ImageProcessUtil.getAweStruckVibeBitmap(thumbImage, 200, 200);
                t4.text = "AweStruck";
                thumbs.add(t4);
                t5.image = ImageProcessUtil.getLimeStutterBitmap(thumbImage, 200, 200);
                t5.text = "LimeStutter";
                thumbs.add(t5);
                t6.image = ImageProcessUtil.getNightWhisperBitmap(thumbImage, 200, 200);
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
