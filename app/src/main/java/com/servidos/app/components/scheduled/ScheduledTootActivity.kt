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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import autodispose2.androidx.lifecycle.autoDispose
import com.servidos.app.BaseActivity
import com.servidos.app.R
import com.servidos.app.appstore.EventHub
import com.servidos.app.appstore.StatusScheduledEvent
import com.servidos.app.components.compose.ComposeActivity
import com.servidos.app.databinding.ActivityScheduledTootBinding
import com.servidos.app.di.Injectable
import com.servidos.app.di.ViewModelFactory
import com.servidos.app.entity.ScheduledStatus
import com.servidos.app.util.hide
import com.servidos.app.util.show
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class ScheduledTootActivity : BaseActivity(), ScheduledTootActionListener, Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var eventHub: EventHub

    private val viewModel: ScheduledTootViewModel by viewModels { viewModelFactory }

    private val adapter = ScheduledTootAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityScheduledTootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.run {
            title = getString(R.string.title_scheduled_toot)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshStatuses)
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue)

        binding.scheduledTootList.setHasFixedSize(true)
        binding.scheduledTootList.layoutManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.scheduledTootList.addItemDecoration(divider)
        binding.scheduledTootList.adapter = adapter

        lifecycleScope.launch {
            viewModel.data.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        adapter.addLoadStateListener { loadState ->
            if (loadState.refresh is Error) {
                binding.progressBar.hide()
                binding.errorMessageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    refreshStatuses()
                }
                binding.errorMessageView.show()
            }
            if (loadState.refresh != LoadState.Loading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
            if (loadState.refresh is LoadState.NotLoading) {
                binding.progressBar.hide()
                if (adapter.itemCount == 0) {
                    binding.errorMessageView.setup(R.drawable.elephant_friend_empty, R.string.no_scheduled_status)
                    binding.errorMessageView.show()
                } else {
                    binding.errorMessageView.hide()
                }
            }
        }

        eventHub.events
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this)
            .subscribe { event ->
                if (event is StatusScheduledEvent) {
                    adapter.refresh()
                }
            }
    }

    private fun refreshStatuses() {
        adapter.refresh()
    }

    override fun edit(item: ScheduledStatus) {
        val intent = ComposeActivity.startIntent(
            this,
            ComposeActivity.ComposeOptions(
                scheduledTootId = item.id,
                tootText = item.params.text,
                contentWarning = item.params.spoilerText,
                mediaAttachments = item.mediaAttachments,
                inReplyToId = item.params.inReplyToId,
                visibility = item.params.visibility,
                scheduledAt = item.scheduledAt,
                sensitive = item.params.sensitive
            )
        )
        startActivity(intent)
    }

    override fun delete(item: ScheduledStatus) {
        viewModel.deleteScheduledStatus(item)
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, ScheduledTootActivity::class.java)
    }
}
