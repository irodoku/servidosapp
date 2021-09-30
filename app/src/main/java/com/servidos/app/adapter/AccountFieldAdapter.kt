/* Copyright 2018 Conny Duck
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

package com.servidos.app.adapter

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.servidos.app.R
import com.servidos.app.databinding.ItemAccountFieldBinding
import com.servidos.app.entity.Emoji
import com.servidos.app.entity.Field
import com.servidos.app.entity.IdentityProof
import com.servidos.app.interfaces.LinkListener
import com.servidos.app.util.BindingHolder
import com.servidos.app.util.Either
import com.servidos.app.util.LinkHelper
import com.servidos.app.util.emojify

class AccountFieldAdapter(
    private val linkListener: LinkListener,
    private val animateEmojis: Boolean
) : RecyclerView.Adapter<BindingHolder<ItemAccountFieldBinding>>() {

    var emojis: List<Emoji> = emptyList()
    var fields: List<Either<IdentityProof, Field>> = emptyList()

    override fun getItemCount() = fields.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<ItemAccountFieldBinding> {
        val binding = ItemAccountFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BindingHolder(binding)
    }

    override fun onBindViewHolder(holder: BindingHolder<ItemAccountFieldBinding>, position: Int) {
        val proofOrField = fields[position]
        val nameTextView = holder.binding.accountFieldName
        val valueTextView = holder.binding.accountFieldValue

        if (proofOrField.isLeft()) {
            val identityProof = proofOrField.asLeft()

            nameTextView.text = identityProof.provider
            valueTextView.text = LinkHelper.createClickableText(identityProof.username, identityProof.profileUrl)

            valueTextView.movementMethod = LinkMovementMethod.getInstance()

            valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_circle, 0)
        } else {
            val field = proofOrField.asRight()
            val emojifiedName = field.name.emojify(emojis, nameTextView, animateEmojis)
            nameTextView.text = emojifiedName

            val emojifiedValue = field.value.emojify(emojis, valueTextView, animateEmojis)
            LinkHelper.setClickableText(valueTextView, emojifiedValue, null, linkListener)

            if (field.verifiedAt != null) {
                valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check_circle, 0)
            } else {
                valueTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }
}
