package com.holenstudio.excamera2.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.holenstudio.excamera2.R;
import com.holenstudio.excamera2.model.ThumbnailItem;

import java.util.List;

/**
 * Created by Holen on 2016/6/17.
 */
public class ThumbnailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "THUMBNAILS_ADAPTER";
    private static int lastPosition = -1;

    private Context mContext;
    private List<ThumbnailItem> mThumbsList;
    private ThumbnailCallback thumbnailCallback;

    public ThumbnailAdapter(Context context, List<ThumbnailItem> thumbsList) {
        this.mContext = context;
        this.mThumbsList = thumbsList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_thumbnail, parent, false);
        return new ThumbsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final int pos = position;
        ThumbnailItem thumb = mThumbsList.get(pos);
        ((ThumbsViewHolder) holder).thumbText.setText(thumb.text);
        ((ThumbsViewHolder) holder).thumbView.setImageBitmap(thumb.image);
        ((ThumbsViewHolder) holder).thumbView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastPosition != pos) {
                    thumbnailCallback.onThumbnailClick(v, pos);
                    lastPosition = pos;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mThumbsList == null? 0 : mThumbsList.size();
    }

    public void setThumbnailCallback (ThumbnailCallback callback) {
        thumbnailCallback = callback;
    }
    public static class ThumbsViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbView;
        public TextView thumbText;

        public ThumbsViewHolder(View view) {
            super(view);
            thumbView = (ImageView) view.findViewById(R.id.thumb_image);
            thumbText = (TextView) view.findViewById(R.id.thumb_text);
        }
    }
    public interface ThumbnailCallback{
        void onThumbnailClick(View view, int position);
    }
}
