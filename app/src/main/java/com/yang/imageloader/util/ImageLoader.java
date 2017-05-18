package com.yang.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by yang on 2017/5/14.
 */

public class ImageLoader {

    private static ImageLoader mInstance;

    //图片缓存核心线程
    private LruCache<String,Bitmap> mLruCache;
    //线程池
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    //队列的调度方式
    private Type myType = Type.LIFO;
    private LinkedList<Runnable >mTaskQueue;
    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    //UI线程中的Handler
    private Handler mUIHandler;
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    public enum Type{
        FIFO,LIFO
    }

    private ImageLoader(int threadCount,Type type){
        init(threadCount,type);

    }

    //初始化操作
    private void init(int threadCount, Type type) {
        //后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //通过线程池取出一个任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        //获取应用最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory/8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };
        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        myType= type;
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    private Runnable getTask(){
        if(myType ==Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if (myType == Type.LIFO ){
            return mTaskQueue.removeLast();
        }
        return null;
    }
    public static ImageLoader getInstance(int threadCount,Type type){
        if (mInstance == null){
            synchronized (ImageLoader.class){
                if (mInstance == null){
                    mInstance = new ImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据path为imageview设置图片
     * @param path
     * @param imageview
     */
    public void loadImage(final String path, final ImageView imageview){
        imageview.setTag(path);
        if (mUIHandler == null){
            mUIHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    //获取图片，为imageview回设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    if (imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null){
            refreshBitmap(bm, imageview, path);
        }else {
            addTasks(new Runnable(){
                @Override
                public void run() {
                    //加载图片,图片的压缩
                    //1.获得图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageview);
                    //2.压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(path,imageSize.width,imageSize.height);
                    //3.把图片加入到缓存
                    addBitmapToLruCache(path,bm);
                    refreshBitmap(bm, imageview, path);

                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(Bitmap bm, ImageView imageview, String path) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bm;
        holder.imageView = imageview;
        holder.path = path;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThread == null){
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 把图片加入到LruCahche
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path)==null){
            if (bm != null){
                mLruCache.put(path,bm);
            }
        }
    }

    /**
     * 根据图片需要显示的1宽和高适当对图片进行压缩
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        //或取图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize = caculateInSampleSize(options,width, height);
        //使用获得到的inSampleSize再次解析图片
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path,options);
    }

    /**
     * 根据需求的宽和高以及图片的宽和高计算SampleSize
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1 ;

        if (width > reqWidth || height > reqHeight){
            //Math.round()方法为四舍五入
            int widthRadio = Math.round(width*1.0f/reqWidth);
            int heightRadio = Math.round(height*1.0f/reqHeight);

            inSampleSize = Math.max(widthRadio,heightRadio);

        }
        return inSampleSize;
    }

    /**
     * 根据ImageView适当的获取压缩的宽和高
     * @param imageview
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageview) {
        ImageSize imageSize = new ImageSize();

        DisplayMetrics displayMetrics = imageview.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageview.getLayoutParams();
        int width = imageview.getWidth();//获取imageView的实际宽度
        if (width <= 0){
            width = lp.width;//获取imageView在layout中声明的宽度
        }
        if (width <= 0){
//            width = imageview.getMaxWidth();//检查最大值
            width = getImageViewFieldValue(imageview,"mMaxWidth");//利用反射获取，兼容低版本
        }
        if (width <= 0){
            width = displayMetrics.widthPixels;
        }

        int height = imageview.getHeight();//获取imageView的实际宽度
        if (height <= 0){
            height = lp.height;//获取imageView在layout中声明的宽度
        }
        if (height <= 0){
//            height = imageview.getMaxHeight();//检查最大值
            height = getImageViewFieldValue(imageview,"mMaxHeight");
        }
        if (width <= 0){
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return  imageSize;
    }

    //通过反射获取imageview的某个属性值
    private static int getImageViewFieldValue(Object object,String fieldName){
        int value = 0;
        Field field = null;
        try {
            field =ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue  < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 根据path在缓存中获取bitmap
     * @param path
     * @return
     */
    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private class ImageSize{
        int width;
        int height;
    }

    private class ImageBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
