package com.yang.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.yang.imageloader.bean.FolderBean;
import com.yang.imageloader.util.ImageLoader;

import java.util.List;

/**
 * Created by yang on 2017/5/17.
 */

public class ListImgDirPopupWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;

    private List<FolderBean>mData;

    public interface OnDirSelectedListener{
        void onSelected(FolderBean folderBean);
    }

    public OnDirSelectedListener mListener;

    public void setOnDirSelectedListener(OnDirSelectedListener listener) {
        this.mListener = listener;
    }

    public ListImgDirPopupWindow(Context context, List<FolderBean>data) {
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_window_layout,null);
        this.mData = data;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight((int) (mHeight*0.7));
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE){
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews(context);
        initEvent();
    }

    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mListView.setAdapter(new ListDirAdapter(context,mData));
    }

    private void initEvent() {

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null){
                    mListener.onSelected(mData.get(position));
                }
            }
        });

    }

    //计算popupwindow的宽度和高度
    private void calWidthAndHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mWidth  = outMetrics.widthPixels;
        mHeight = outMetrics.heightPixels;
    }
    private class ListDirAdapter extends ArrayAdapter<FolderBean>{

        private LayoutInflater mInflater;
        private List<FolderBean>mData;
        public ListDirAdapter(@NonNull Context context, List<FolderBean>objects) {
            super(context,0,objects);
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null){
                viewHolder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.popup_window_list_item,parent,false);
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.id_dir_item_img);
                viewHolder.DirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                viewHolder.DirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) convertView.getTag();

            }
            FolderBean bean = getItem(position);
            viewHolder.imageView.setImageResource(R.mipmap.pictures_no);
            assert bean != null;
            ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(bean.getFirstImgPath(),viewHolder.imageView);
            viewHolder.DirName.setText(bean.getName());
            viewHolder.DirCount.setText(bean.getCount()+"");
            return convertView;
        }

        private class ViewHolder{
            ImageView imageView;
            TextView DirName;
            TextView DirCount;
        }
    }
}
