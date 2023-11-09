package com.navinfo.omqs.ui.fragment.evaluationresult;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.navinfo.omqs.R;
import com.navinfo.omqs.databinding.LaneinfoItemBinding;

import java.util.List;

/**
 * 车信图标gridView
 */
public class LaneInfoItems2Adapter extends BaseAdapter {
    List<List<Integer>> dataList;
    //车道类型 0：普通，1，附加车道，2，公交车道
    private int type = 0;

    LaneInfoItems2Adapter(List<List<Integer>> data) {
        dataList = data;
    }


    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LaneinfoItemBinding viewBinding =
                    LaneinfoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            holder = new ViewHolder();
            holder.layout = viewBinding.laneinfoItemLayout;
            convertView = viewBinding.getRoot();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        List<Integer> list = dataList.get(position);
        holder.layout.removeAllViews();
        for (int i : list) {
            View lineViewS = new View(parent.getContext());
            lineViewS.setLayoutParams(new ViewGroup.LayoutParams(24, 90));
            lineViewS.setBackground(parent.getContext().getDrawable(R.drawable.shape_vertical_dashed_line));
            holder.layout.addView(lineViewS);

            ImageView imageView = new ImageView(parent.getContext());
            Drawable drawable = parent.getContext().getDrawable(i);
            int color;
            switch (type) {
                case 1:
                    color = parent.getContext().getResources().getColor(R.color.lane_info_1);
                    break;
                case 2:
                    color = parent.getContext().getResources().getColor(R.color.lane_info_2);
                    break;
                default:
                    color = parent.getContext().getResources().getColor(R.color.white);
                    break;
            }
            // 创建 PorterDuffColorFilter 对象
            PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
            // 将 PorterDuffColorFilter 设置给 Drawable
            drawable.setColorFilter(colorFilter);
            imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            imageView.setBackground(drawable);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(36, 80));
            holder.layout.addView(imageView);
        }
        View lineViewS = new View(parent.getContext());
        lineViewS.setLayoutParams(new ViewGroup.LayoutParams(24, 90));
        lineViewS.setBackground(parent.getContext().getDrawable(R.drawable.shape_vertical_dashed_line));
        holder.layout.addView(lineViewS);
        return convertView;
    }

    private class ViewHolder {
        LinearLayout layout;
    }

    public void setType(int type) {
        if (type != this.type) {
            this.type = type;
            notifyDataSetChanged();
        }
    }
}
