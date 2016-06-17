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
import android.widget.ImageView;

import com.holenstudio.excamera2.adapter.ThumbnailAdapter;
import com.holenstudio.excamera2.model.ThumbnailItem;
import com.holenstudio.excamera2.util.CameraUtil;
import com.holenstudio.excamera2.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends Activity {

    private ImageView mImageView;
    private RecyclerView mThumbsView;
    private String imagePath;
    BitmapFactory.Options mOptions;

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


    private void initData() {
        imagePath = getIntent().getStringExtra("image_path");
        setImage();
    }

    private void setImage() {
        mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, mOptions);
        mOptions.inSampleSize = CameraUtil.calculateInSampleSize(mOptions, mImageView.getMeasuredWidth(),
                mImageView.getMeasuredHeight());
        mOptions.inJustDecodeBounds = false;
        mImageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
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
                Bitmap thumbImage = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(imagePath), 640, 640, false);
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
                t2.image = thumbImage;
                t2.text = "StarLit";
                thumbs.add(t2);
                t3.image = thumbImage;
                t3.text = "BlueMess";
                thumbs.add(t3);
                t4.image = thumbImage;
                t4.text = "AweStruck";
                thumbs.add(t4);
                t5.image = thumbImage;
                t5.text = "LimeStutter";
                thumbs.add(t5);
                t6.image = thumbImage;
                t6.text = "NightWhisper";
                thumbs.add(t6);

                ThumbnailAdapter adapter = new ThumbnailAdapter(PhotoActivity.this, thumbs);
                mThumbsView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }


}
