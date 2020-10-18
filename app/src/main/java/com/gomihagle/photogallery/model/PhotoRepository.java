package com.gomihagle.photogallery.model;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gomihagle.photogallery.R;
import com.gomihagle.photogallery.databinding.PhotoListItemBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PhotoRepository {

    private static final String API_KEY = "56b0af317f495a8540d8effe5a20aa31";
    private static final String TAG = "FlickrFetchr";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static  final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s")
            .build();

    private boolean searched_set;
    private static final int QUERY_SEARCH = 850;
    private static final int QUERY_RECENT = 620;

    private static PhotoRepository instance;
    private MutableLiveData<List<GalleryItem>> mMutableLiveData;
    private Integer pagesCount;
    private static int lastLoadedPage;
    private Context mContext;
    private Drawable mTempDrawable;



    private PhotoRepository(@NonNull Application application) {
        mContext = application.getApplicationContext();
        mMutableLiveData = new MutableLiveData<>();
        Handler responseHandler = new Handler();

        mTempDrawable = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.temp_image,null);

    }


    public  static  PhotoRepository get(Application application)
    {
        if(instance == null)
           instance = new PhotoRepository(application);
        return instance;
    }


    public LiveData<List<GalleryItem>> recentLoad() {
        pagesCount = 0;
        lastLoadedPage = 0;
        searched_set = false;
        new LoadImagesTask().execute(QUERY_RECENT,pagesCount);
        lastLoadedPage++;

        return mMutableLiveData;
    }

    public void loadMore() {
        int reqPage = lastLoadedPage + 1;
        if(reqPage > pagesCount) return;
        if(searched_set)
            new LoadImagesTask().execute(QUERY_SEARCH,reqPage);
        else
            new LoadImagesTask().execute(QUERY_RECENT,reqPage);
        lastLoadedPage++;
    }




    private byte[] getUrlBytes(String urlSpec)throws IOException
    {
        URL url = new URL(urlSpec);
        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                throw new IOException(connection.getResponseMessage()+": with "+urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer))>0)
            {
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }


    }

    private  List<GalleryItem> downloadGalleryItems(String url)
    {
        List<GalleryItem> itemList = new ArrayList<>();
        try{
            String jsonString = getUrlString(url);
            Log.i(TAG,"Received json: "+jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
             pagesCount = parseItems(itemList,jsonBody);

        } catch (IOException e)
        {
            Log.e(TAG,"Failed to fetch items! ",e);
        }
        catch (JSONException je)
        {
            Log.e(TAG,"Failed to parse json! ",je);
        }

        Log.d(TAG,pagesCount.toString());
       return itemList;

    }


    private int parseItems(@NonNull List<GalleryItem> itemList, @NonNull JSONObject jsonBody) throws JSONException
    {

        JSONObject jsonPhotos = jsonBody.getJSONObject("photos");
        JSONArray jsonPhotoArray = jsonPhotos.getJSONArray("photo");

        int pages = jsonPhotos.getInt("pages");

        Gson gson = new Gson();
        Type type = new TypeToken<List<GalleryItem>>(){}.getType();
        List<GalleryItem> items = gson.fromJson(jsonPhotoArray.toString(),type);
        itemList.addAll(items);

        return pages;
    }

    private  String buildUrl(String method,String query,int page)
    {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method",method);
        if(method.equals(SEARCH_METHOD) && query != null)
            uriBuilder.appendQueryParameter("text",query);
        if(method.equals(FETCH_RECENT_METHOD) && page > 0)
            uriBuilder.appendQueryParameter("page",Integer.toString(page));
        return uriBuilder.build().toString();
    }


    private String getUrlString(String urlSpec) throws IOException
    {
        return new String(getUrlBytes(urlSpec));
    }

    public void loadImage(PhotoListItemBinding binding,String url) {
        if(url!=null)
        {
            Picasso.with(mContext.getApplicationContext())
                    .load(url)
                    .placeholder(mTempDrawable)
                    .into(binding.imagePlaceholder);
        }

    }

    public void searchByPrefs() {
        searched_set = true;
        lastLoadedPage = 0;
        new LoadImagesTask().execute(QUERY_SEARCH,lastLoadedPage);

    }


    @SuppressLint("StaticFieldLeak")
    private class LoadImagesTask extends AsyncTask<Integer,Void,Void>{

        @Override
        protected Void doInBackground(Integer... args) {

            if(args.length!=2) return null;
            String url = "";
            switch (args[0])
            {
                case QUERY_RECENT:
                {
                    url = buildUrl(FETCH_RECENT_METHOD,null,args[1]);
                    break;
                }
                case QUERY_SEARCH:
                {
                    String query = QueryPreferences.getStoredQuery(mContext);
                    url = buildUrl(SEARCH_METHOD,query,args[1]);
                    break;
                }
            }


           List<GalleryItem> itemList = downloadGalleryItems(url);
           mMutableLiveData.postValue(itemList);
          return null;
        }

    }

}
