package com.ksyun.media.streamer.demo.sticker.window;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ksyun.live.demo.R;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 贴图列表Adapter
 */
public class StickerAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final String DEFAULT_PATH = "assets://";
    public DisplayImageOptions mImageOption = new DisplayImageOptions.Builder()
            .cacheInMemory(true).showImageOnLoading(null)
            .build();// 下载图片显示

    private Context mContext;
    private ImageClick mImageClick = new ImageClick();
    private List<String> mImagePathList = new ArrayList<String>();// 图片路径列表
    private OnStickerItemClick mOnStickerItemClick;

    public StickerAdapter(Context context) {
        super();
        mContext = context;
        initImageLoader();
    }

    public void setOnStickerItemClick(OnStickerItemClick itemClick) {
        mOnStickerItemClick = itemClick;
    }

    public class ImageHolder extends ViewHolder {
        public ImageView image;

        public ImageHolder(View itemView) {
            super(itemView);
            this.image = (ImageView) itemView.findViewById(R.id.sticker_img);
        }
    }

    @Override
    public int getItemCount() {
        return mImagePathList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewtype) {
        View v;
        v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.sticker_item, parent, false);
        ImageHolder holer = new ImageHolder(v);
        return holer;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ImageHolder imageHoler = (ImageHolder) holder;
        String path = mImagePathList.get(position);
        //此处默认加载assets下面的文件，如果需要加载其它资源文件，需要修改此处
        ImageLoader.getInstance().displayImage(DEFAULT_PATH + path,
                imageHoler.image, mImageOption);
        imageHoler.image.setTag(path);
        imageHoler.image.setOnClickListener(mImageClick);
    }

    public void addStickerImages(String folderPath) {
        mImagePathList.clear();
        try {
            String[] files = mContext.getAssets()
                    .list(folderPath);
            for (String name : files) {
                mImagePathList.add(folderPath + File.separator + name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.notifyDataSetChanged();
    }

    /**
     * 选择贴图
     */
    private final class ImageClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            String data = (String) v.getTag();
            if (mOnStickerItemClick != null) {
                mOnStickerItemClick.selectedStickerItem(data);
            }
        }
    }

    private void initImageLoader() {
        File cacheDir = StorageUtils.getCacheDirectory(mContext);
        int MAXMEMONRY = (int) (Runtime.getRuntime().maxMemory());
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                mContext).memoryCacheExtraOptions(480, 800).defaultDisplayImageOptions(defaultOptions)
                .diskCacheExtraOptions(480, 800, null).threadPoolSize(3)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .tasksProcessingOrder(QueueProcessingType.FIFO)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new LruMemoryCache(MAXMEMONRY / 5))
                .diskCache(new UnlimitedDiskCache(cacheDir))
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
                .imageDownloader(new BaseImageDownloader(mContext)) // default
                .imageDecoder(new BaseImageDecoder(false)) // default
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple()).build();

        ImageLoader.getInstance().init(config);
    }

    public interface OnStickerItemClick {
        void selectedStickerItem(String path);
    }
}
