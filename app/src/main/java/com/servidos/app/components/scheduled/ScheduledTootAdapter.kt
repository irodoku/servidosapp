/* Copyright 2019 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.servidos.app.components.scheduled

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.servidos.app.databinding.ItemScheduledTootBinding
import com.servidos.app.entity.ScheduledStatus
import com.servidos.app.util.BindingHolder

interface ScheduledTootActionListener {
    fun edit(item: ScheduledStatus)
    fun delete(item: ScheduledStatus)
}

class ScheduledTootAdapter(
    val listener: ScheduledTootActionListener
) : PagingDataAdapter<ScheduledStatus, BindingHolder<ItemScheduledTootBinding>>(
    object : DiffUtil.ItemCallback<ScheduledStatus>() {
        override fun areItemsTheSame(oldItem: ScheduledStatus, newItem: ScheduledStatus): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScheduledStatus, newItem: ScheduledStatus): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<ItemScheduledTootBinding> {
        val binding = ItemScheduledTootBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BindingHolder(binding)
    }

    override fun onBindViewHolder(holder: BindingHolder<ItemScheduledTootBinding>, position: Int) {
        getItem(position)?.let { item ->
            holder.binding.edit.isEnabled = true
            holder.binding.delete.isEnabled = true
            holder.binding.text.text = item.params.text
            holder.binding.edit.setOnClickListener { v: View ->
                v.isEnabled = false
                listener.edit(item)
            }
            holder.binding.delete.setOnClickListener { v: View ->
                v.isEnabled = false
                listener.delete(item)
            }
        }
    }
}
