package com.navinfo.omqs.ui.fragment.layermanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import com.navinfo.omqs.R
import com.navinfo.omqs.bean.ImportConfig
import com.navinfo.omqs.bean.TableInfo

class LayerManagerExpandableListAdapter(private val context: Context, private val parentItems: List<ImportConfig>) :
    BaseExpandableListAdapter() {

    override fun getGroupCount(): Int {
        return parentItems.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return parentItems[groupPosition].tables.size
    }


    override fun getGroup(groupPosition: Int): Any {
        return parentItems[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return parentItems[groupPosition].tables[childPosition]
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
        viewHolder.parentCheckBox.text = parentItem.tableGroupName
        viewHolder.parentCheckBox.isChecked = parentItem.checked
        viewHolder.parentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            parentItem.checked = isChecked
            parentItem.tables.forEach { it.checked = isChecked }
            notifyDataSetChanged()
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
        viewHolder.childCheckBox.text = childItem.name
        viewHolder.childCheckBox.isChecked = childItem.checked
        viewHolder.childCheckBox.setOnCheckedChangeListener { _, isChecked ->
            childItem.checked = isChecked
            parentItems[groupPosition].checked = parentItems[groupPosition].tables.all { it.checked }
            notifyDataSetChanged()
        }

        return view!!
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    internal class ParentViewHolder(view: View) {
        val parentCheckBox: CheckBox = view.findViewById(R.id.chk_layermanager_parent)
    }

    internal class ChildViewHolder(view: View) {
        val childCheckBox: CheckBox = view.findViewById(R.id.chk_layermanager_child)
    }
}