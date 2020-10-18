package com.gomihagle.photogallery.model.ui;

import android.app.Application;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gomihagle.photogallery.R;
import com.gomihagle.photogallery.databinding.PhotoListItemBinding;
import com.gomihagle.photogallery.model.GalleryItem;
import com.gomihagle.photogallery.model.PhotoRepository;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import android.os.StrictMode;

public class PhotoListViewModel extends AndroidViewModel{

    private iUpdatesListener mIUpdatesListener;
    private PhotoRepository mRepository;
    private List<GalleryItem> mGalleryItems;
    private Application mApplication;
    private Drawable mTempDrawable;
    private int lastLoadedPosition;
    private MutableLiveData<Boolean> mLoadingLiveData;


    public void searchByPrefs() {
        mLoadingLiveData.setValue(true);
        mRepository.searchByPrefs();
        mGalleryItems.clear();
        lastLoadedPosition = 0;
    }

    public LiveData<Boolean> getLoadingLiveData()
    {
        return mLoadingLiveData;
    }


    public PhotoListViewModel(@NonNull Application application) {
        super(application);
        StrictMode.enableDefaults();
        mApplication = application;
        lastLoadedPosition = 0;
        mRepository = PhotoRepository.get(mApplication);
        mGalleryItems = new ArrayList<>();
        mLoadingLiveData = new MutableLiveData<>();
        mLoadingLiveData.setValue(true);

        mRepository.recentLoad().observeForever(itemList -> {
            mGalleryItems.addAll(itemList);
            if(mIUpdatesListener != null)
                mIUpdatesListener.dataSetChanged();
            mLoadingLiveData.setValue(false);

        });
        mTempDrawable = ResourcesCompat.getDrawable(
                application.getResources(),
                R.drawable.temp_image,
                null
                );
    }

    public void loadRecent()
    {
        mLoadingLiveData.setValue(true);
        mGalleryItems.clear();
        mRepository.recentLoad();
        lastLoadedPosition = 0;

    }


    public void getThumbnail(PhotoListItemBinding binding)
    {
       String url =  mGalleryItems.get(binding.getListPosition()).getUrl();
        mRepository.loadImage(binding,url);
    }

    public void loadMore(int position)
    {
        if(position == mGalleryItems.size()-10)
            if(position > lastLoadedPosition)
            {
                mRepository.loadMore();
                lastLoadedPosition = position;
            }

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


