package com.gomihagle.photogallery.controller;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gomihagle.photogallery.R;
import com.gomihagle.photogallery.databinding.FragmentPhotoGalleryBinding;
import com.gomihagle.photogallery.databinding.PhotoListItemBinding;
import com.gomihagle.photogallery.model.QueryPreferences;
import com.gomihagle.photogallery.model.ui.PhotoListViewModel;


public class PhotoGalleryFragment extends Fragment implements PhotoListViewModel.iUpdatesListener,
        ViewTreeObserver.OnGlobalLayoutListener {


    private FragmentPhotoGalleryBinding mBinding;
    private PhotoListViewModel mViewModel;
    private PhotoAdapter mPhotoAdapter;
    private GridLayoutManager mLayoutManager;
    private SearchView mSearchView;

    @Override
    public void onGlobalLayout() {

        if(mLayoutManager == null)
        {
            int colCount = mBinding.photoRecyclerView.getWidth()/400;
            mLayoutManager = new GridLayoutManager(getContext(),colCount);
            mBinding.photoRecyclerView.setLayoutManager(mLayoutManager);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       mViewModel = ViewModelProviders.of(this).get(PhotoListViewModel.class);
       mViewModel.subscribeForUpdates(this);
       mPhotoAdapter = new PhotoAdapter();
       setHasOptionsMenu(true);
       mViewModel.getLoadingLiveData().observe(getActivity(), isLoading -> {

           mBinding.setIsLoading(isLoading);
       });

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_gallery,container,false);
        mBinding.photoRecyclerView.setAdapter(mPhotoAdapter);
        mBinding.photoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mBinding.getRoot();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem menuItem  = menu.findItem(R.id.menu_item_search);
         mSearchView = (SearchView) menuItem.getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getContext(),query);
                mViewModel.searchByPrefs();
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
                mSearchView.onActionViewCollapsed();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        mSearchView.setOnSearchClickListener(v -> {
            String query =  QueryPreferences.getStoredQuery(getContext());
            mSearchView.setQuery(query,false);

        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.menu_item_clear:
            {
                QueryPreferences.setStoredQuery(getContext(),null);
                mViewModel.loadRecent();
                mBinding.photoRecyclerView.scrollToPosition(0);
                mSearchView.setQuery("",false);
                return true;
            }

            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void dataSetChanged() {
        mPhotoAdapter.notifyDataSetChanged();
    }

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }



    private  class  PhotoAdapter extends  RecyclerView.Adapter<PhotoHolder>
    {

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            PhotoListItemBinding binding = DataBindingUtil.inflate(
                    getLayoutInflater(),
                    R.layout.photo_list_item,
                    parent,
                    false);

            return new PhotoHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            holder.bind(mViewModel,position);
        }

        @Override
        public int getItemCount() {
            return mViewModel.getItemsCount();
        }
    }

    public class  PhotoHolder extends RecyclerView.ViewHolder {

        private PhotoListItemBinding mBinding;

        public PhotoHolder(PhotoListItemBinding binding) {
            super(binding.getRoot());
            this.mBinding = binding;

        }

        public  void bind(PhotoListViewModel viewModel, int listPosition)
        {
            mBinding.setListPosition(listPosition);
            mBinding.setPhotoViewModel(viewModel);
            mViewModel.loadMore(listPosition);
            mViewModel.getThumbnail(mBinding);

        }
    }


}
