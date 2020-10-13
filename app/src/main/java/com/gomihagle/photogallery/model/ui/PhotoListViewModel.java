package com.gomihagle.photogallery.model.ui;

import android.app.Application;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Observer;

import com.gomihagle.photogallery.R;
import com.gomihagle.photogallery.databinding.PhotoListItemBinding;
import com.gomihagle.photogallery.model.GalleryItem;
import com.gomihagle.photogallery.model.PhotoRepository;

import java.util.ArrayList;
import java.util.List;

import android.os.StrictMode;

public class PhotoListViewModel extends AndroidViewModel{

    private iUpdatesListener mIUpdatesListener;
    private PhotoRepository mRepository;
    private List<GalleryItem> mGalleryItems;
    private Application mApplication;
    private Drawable mTempDrawable;
    private InitLoadCallback mLoadCallback;

    public void searchByPrefs() {
        mRepository.searchByPrefs();
    }

    public interface InitLoadCallback{
        void onInitLoad();
    }

    public void registerForInitLoadCallback(InitLoadCallback callback)
    {
        mLoadCallback = callback;
    }

    public List<GalleryItem> getGalleryItems() {
        return mGalleryItems;
    }

    public PhotoListViewModel(@NonNull Application application) {
        super(application);
        StrictMode.enableDefaults();
        mApplication = application;
        mRepository = PhotoRepository.get(mApplication);
        mGalleryItems = new ArrayList<>();

        mRepository.initLoad().observeForever(new Observer<List<GalleryItem>>() {
            @Override
            public void onChanged(List<GalleryItem> itemList) {
                mGalleryItems.addAll(itemList);
                if(mIUpdatesListener != null)
                    mIUpdatesListener.dataSetChanged();
                if(mLoadCallback!=null)
                    mLoadCallback.onInitLoad();
            }
        });
        mTempDrawable = ResourcesCompat.getDrawable(
                application.getResources(),
                R.drawable.temp_image,
                null
                );
    }



    public void getThumbnail(PhotoListItemBinding binding)
    {
       String url =  mGalleryItems.get(binding.getListPosition()).getUrl();
        mRepository.loadImage(binding,url);
    }

    public void loadMore(int position)
    {
        if(position== mGalleryItems.size()-10)
             mRepository.loadMore();
    }

   public int getItemsCount()
   {
       return mGalleryItems.size();
   }

   public void subscribeForUpdates(iUpdatesListener listener)
   {
       this.mIUpdatesListener = listener;
   }



    public interface  iUpdatesListener{
        void dataSetChanged();
   }

}


