package com.kaltura.magikapp.magikapp.homepage.binders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kaltura.magikapp.R;

import java.util.List;

/**
 * Created by vladir on 01/01/2017.
 */

public class SimpleGridAdapterTemplate1 extends RecyclerView.Adapter<SimpleGridAdapterTemplate1.ViewHolder> {

    private Context mContext;
    private List<String> mUrls;
    int[] mDrawableRes;
    private ItemClick mOnItemClicked;

    public SimpleGridAdapterTemplate1(Context context, int[] drawableRes){
        mContext = context;
        mDrawableRes = drawableRes;
    }

//    public GridAdapter(Context context, String[] urls) {
//        mContext = context;
//        mUrls = new ArrayList(Arrays.asList(urls));
//    }

    @Override
    public SimpleGridAdapterTemplate1.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fourimage_item_layout, parent, false);

        return new SimpleGridAdapterTemplate1.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleGridAdapterTemplate1.ViewHolder holder, final int position) {
        Glide.with(mContext).load(mDrawableRes[position]).centerCrop().crossFade().into(holder.mImageView);
        holder.mRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnItemClicked.onClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    public void setOnClickListener(ItemClick onItemClicked) {
        mOnItemClicked = onItemClicked;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView mImageView;
        View mRoot;

        public ViewHolder(View view) {
            super(view);
            mRoot = view;
            mImageView = (ImageView) view.findViewById(R.id.four_image_item_image_view);
        }
    }

}
