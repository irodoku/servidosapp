/* Copyright 2021 Tusky Contributors
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

package com.servidos.app.components.search.fragments

import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.preference.PreferenceManager
import com.servidos.app.components.search.adapter.SearchAccountsAdapter
import com.servidos.app.entity.Account
import com.servidos.app.settings.PrefKeys
import kotlinx.coroutines.flow.Flow

class SearchAccountsFragment : SearchFragment<Account>() {
    override fun createAdapter(): PagingDataAdapter<Account, *> {
        val preferences = PreferenceManager.getDefaultSharedPreferences(binding.searchRecyclerView.context)

        return SearchAccountsAdapter(
            this,
            preferences.getBoolean(PrefKeys.ANIMATE_GIF_AVATARS, false),
            preferences.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)
        )
    }

    override val data: Flow<PagingData<Account>>
        get() = viewModel.accountsFlow

    companion object {
        fun newInstance() = SearchAccountsFragment()
    }
}
