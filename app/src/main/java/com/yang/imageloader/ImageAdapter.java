package com.yang.imageloader;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.yang.imageloader.util.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {

        private static Set<String> mSelectedImg = new HashSet<>();

        private String mDirPath;
        private List<String> mImgPaths;
        private LayoutInflater mInflater;

        private int mScreenWidth;

        public ImageAdapter(Context context, List<String> mData, String dirPath){
            this.mDirPath = dirPath;
            this.mImgPaths = mData;
            mInflater = LayoutInflater.from(context);
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            mScreenWidth = outMetrics.widthPixels;

        }

        @Override
        public int getCount() {
            return mImgPaths.size();
        }

        @Override
        public Object getItem(int position) {
            return mImgPaths.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null){
                convertView = mInflater.inflate(R.layout.gridview_item,parent,false);
                viewHolder = new ViewHolder();
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.id_img);
                viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //重置状态
            viewHolder.imageView.setImageResource(R.mipmap.pictures_no);
            viewHolder.mSelect.setImageResource(R.mipmap.picture_unselected);
            viewHolder.imageView.setColorFilter(null);

            viewHolder.imageView.setMaxWidth(mScreenWidth/3);

            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).
                    loadImage(mDirPath+ "/"+mImgPaths.get(position),viewHolder.imageView);
            final ViewHolder finalViewHolder = viewHolder;
            viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSelectedImg.contains(mDirPath+"/"+mImgPaths.get(position))){
                        mSelectedImg.remove(mDirPath+"/"+mImgPaths.get(position));
                        finalViewHolder.imageView.setColorFilter(null);
                        finalViewHolder.mSelect.setImageResource(R.mipmap.picture_unselected);
                    }else {
                        mSelectedImg.add(mDirPath+"/"+mImgPaths.get(position));
                        finalViewHolder.imageView.setColorFilter(Color.parseColor("#77000000"));
                        finalViewHolder.mSelect.setImageResource(R.mipmap.pictures_selected);
                    }
//                    notifyDataSetChanged();
                }
            });
            if (mSelectedImg.contains(mDirPath+"/"+mImgPaths.get(position))){
                viewHolder.imageView.setColorFilter(Color.parseColor("#77000000"));
                viewHolder.mSelect.setImageResource(R.mipmap.pictures_selected);
            }
            return convertView;
        }
        private class ViewHolder{
            ImageView imageView;
            ImageButton mSelect;
        }
    }