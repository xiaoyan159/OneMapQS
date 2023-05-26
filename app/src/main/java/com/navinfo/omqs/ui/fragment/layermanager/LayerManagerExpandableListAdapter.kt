package com.navinfo.omqs.ui.fragment.layermanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.TableInfo

class LayerManagerExpandableListAdapter(private val context: Context, val parentItems: List<ImportConfig>) :
    BaseExpandableListAdapter() {

    override fun getGroupCount(): Int {
        return parentItems.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return parentItems[groupPosition].tableMap.size
    }


    override fun getGroup(groupPosition: Int): Any {
        return parentItems[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): TableInfo? {
        return parentItems[groupPosition].tableMap[parentItems[groupPosition].tableMap.keys.elementAt(childPosition)]
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var view = convertView
        val viewHolder: ParentViewHolder
        if (convertView == null) {
            view =
                LayoutInflater.from(context).inflate(R.layout.layer_manager_checked_parent, parent, false)
            viewHolder = ParentViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ParentViewHolder
            view = convertView
        }

        val parentItem = getGroup(groupPosition) as ImportConfig
        viewHolder.parentName.text = parentItem.tableGroupName
        viewHolder.parentCheckBox.isChecked = parentItem.checked
        viewHolder.parentCheckBox.setOnClickListener {
            parentItem.checked = !parentItem.checked
            parentItem.tableMap.forEach { it.value.checked = parentItem.checked }
            notifyDataSetChanged()
        }
        if (isExpanded) {
            viewHolder.imgGroupIndicator.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
        } else {
            viewHolder.imgGroupIndicator.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
        }
        return view!!
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var view = convertView
        val viewHolder: ChildViewHolder
        if (convertView == null) {
            view =
                LayoutInflater.from(context).inflate(R.layout.layer_manager_checked_child, parent, false)
            viewHolder = ChildViewHolder(view)
            view.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ChildViewHolder
            view = convertView
        }

        val childItem = getChild(groupPosition, childPosition) as TableInfo
        viewHolder.childName.text = childItem.name
        viewHolder.childCheckBox.isChecked = childItem.checked
        viewHolder.childCheckBox.setOnClickListener {
            childItem.checked = !childItem.checked
            parentItems[groupPosition].checked = parentItems[groupPosition].tableMap.all { it.value.checked }
            notifyDataSetChanged()
        }

        return view!!
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    internal class ParentViewHolder(view: View) {
        val parentCheckBox: CheckBox = view.findViewById(R.id.chk_layermanager_parent)
        val parentName: TextView = view.findViewById(R.id.tv_layermanager_parent_name)
        val imgGroupIndicator: ImageView = view.findViewById(R.id.img_group_indicator)
    }

    internal class ChildViewHolder(view: View) {
        val childCheckBox: CheckBox = view.findViewById(R.id.chk_layermanager_child)
        val childName: TextView = view.findViewById(R.id.tv_layermanager_child_name)
    }
}