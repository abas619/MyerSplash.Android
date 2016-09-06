package com.juniperphoton.myersplash.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.juniperphoton.myersplash.R;
import com.juniperphoton.myersplash.callback.INavigationDrawerCallback;
import com.juniperphoton.myersplash.model.UnsplashCategory;
import com.juniperphoton.myersplash.view.RectView;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryListViewHolder> {

    private List<UnsplashCategory> mData;
    private Context mContext;
    private int mSelectedIndex = -1;
    private RectView mLastSelectedRV = null;
    private RelativeLayout mLastSelectedRL = null;
    private INavigationDrawerCallback mCallback = null;

    public CategoryAdapter(List<UnsplashCategory> data, Context context) {
        mData = data;
        mContext = context;
    }

    public void setCallback(INavigationDrawerCallback callback) {
        mCallback = callback;
    }

    @Override
    public CategoryListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_category, parent, false);
        return new CategoryListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CategoryListViewHolder holder, int position) {
        final UnsplashCategory category = mData.get(holder.getAdapterPosition());
        holder.TitleTextView.setText(category.getTitle());
        holder.ItemRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = holder.getAdapterPosition();
                if (index == mSelectedIndex) return;
                mSelectedIndex = index;
                holder.LeftRect.setVisibility(View.VISIBLE);
                holder.ItemRoot.setBackground(new ColorDrawable(ContextCompat.getColor(mContext, R.color.SelectedBackgroundColor)));

                if (mLastSelectedRV != null) {
                    mLastSelectedRV.setVisibility(View.INVISIBLE);
                }
                if (mLastSelectedRL != null) {
                    mLastSelectedRL.setBackground(new ColorDrawable(Color.TRANSPARENT));
                }
                mLastSelectedRV = holder.LeftRect;
                mLastSelectedRL = holder.ItemRoot;
                if (mCallback != null) {
                    mCallback.selectItem(category);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public class CategoryListViewHolder extends RecyclerView.ViewHolder {
        public TextView TitleTextView;
        public RelativeLayout ItemRoot;
        public RectView LeftRect;

        public CategoryListViewHolder(View itemView) {
            super(itemView);
            TitleTextView = (TextView) itemView.findViewById(R.id.row_category);
            ItemRoot = (RelativeLayout) itemView.findViewById(R.id.row_category_rl);
            LeftRect = (RectView) itemView.findViewById(R.id.row_cateogory_rv);
        }
    }
}