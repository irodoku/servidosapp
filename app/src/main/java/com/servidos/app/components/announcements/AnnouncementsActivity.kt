/* Copyright 2020 Tusky Contributors
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

package com.servidos.app.components.announcements

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.servidos.app.BottomSheetActivity
import com.servidos.app.R
import com.servidos.app.ViewTagActivity
import com.servidos.app.adapter.EmojiAdapter
import com.servidos.app.adapter.OnEmojiSelectedListener
import com.servidos.app.databinding.ActivityAnnouncementsBinding
import com.servidos.app.di.Injectable
import com.servidos.app.di.ViewModelFactory
import com.servidos.app.settings.PrefKeys
import com.servidos.app.util.Error
import com.servidos.app.util.Loading
import com.servidos.app.util.Success
import com.servidos.app.util.hide
import com.servidos.app.util.show
import com.servidos.app.util.viewBinding
import com.servidos.app.view.EmojiPicker
import javax.inject.Inject

class AnnouncementsActivity : BottomSheetActivity(), AnnouncementActionListener, OnEmojiSelectedListener, Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: AnnouncementsViewModel by viewModels { viewModelFactory }

    private val binding by viewBinding(ActivityAnnouncementsBinding::inflate)

    private lateinit var adapter: AnnouncementAdapter

    private val picker by lazy { EmojiPicker(this) }
    private val pickerDialog by lazy {
        PopupWindow(this)
            .apply {
                contentView = picker
                isFocusable = true
                setOnDismissListener {
                    currentAnnouncementId = null
                }
            }
    }
    private var currentAnnouncementId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.title_announcements)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshAnnouncements)
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue)

        binding.announcementsList.setHasFixedSize(true)
        binding.announcementsList.layoutManager = LinearLayoutManager(this)
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.announcementsList.addItemDecoration(divider)

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val wellbeingEnabled = preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_POSTS, false)
        val animateEmojis = preferences.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)

        adapter = AnnouncementAdapter(emptyList(), this, wellbeingEnabled, animateEmojis)

        binding.announcementsList.adapter = adapter

        viewModel.announcements.observe(this) {
            when (it) {
                is Success -> {
                    binding.progressBar.hide()
                    binding.swipeRefreshLayout.isRefreshing = false
                    if (it.data.isNullOrEmpty()) {
                        binding.errorMessageView.setup(R.drawable.elephant_friend_empty, R.string.no_announcements)
                        binding.errorMessageView.show()
                    } else {
                        binding.errorMessageView.hide()
                    }
                    adapter.updateList(it.data ?: listOf())
                }
                is Loading -> {
                    binding.errorMessageView.hide()
                }
                is Error -> {
                    binding.progressBar.hide()
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.errorMessageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                        refreshAnnouncements()
                    }
                    binding.errorMessageView.show()
                }
            }
        }

        viewModel.emojis.observe(this) {
            picker.adapter = EmojiAdapter(it, this)
        }

        viewModel.load()
        binding.progressBar.show()
    }

    private fun refreshAnnouncements() {
        viewModel.load()
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun openReactionPicker(announcementId: String, target: View) {
        currentAnnouncementId = announcementId
        pickerDialog.showAsDropDown(target)
    }

    override fun onEmojiSelected(shortcode: String) {
        viewModel.addReaction(currentAnnouncementId!!, shortcode)
        pickerDialog.dismiss()
    }

    override fun addReaction(announcementId: String, name: String) {
        viewModel.addReaction(announcementId, name)
    }

    override fun removeReaction(announcementId: String, name: String) {
        viewModel.removeReaction(announcementId, name)
    }

    override fun onViewTag(tag: String?) {
        val intent = Intent(this, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivityWithSlideInAnimation(intent)
    }

    override fun onViewAccount(id: String?) {
        if (id != null) {
            viewAccount(id)
        }
    }

    override fun onViewUrl(url: String?) {
        if (url != null) {
            viewUrl(url)
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, AnnouncementsActivity::class.java)
    }
}
