/* Copyright 2017 Andrew Dawson
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

package com.servidos.app.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider.from
import autodispose2.autoDispose
import com.google.android.material.snackbar.Snackbar
import com.servidos.app.AccountActivity
import com.servidos.app.AccountListActivity.Type
import com.servidos.app.BaseActivity
import com.servidos.app.R
import com.servidos.app.adapter.AccountAdapter
import com.servidos.app.adapter.BlocksAdapter
import com.servidos.app.adapter.FollowAdapter
import com.servidos.app.adapter.FollowRequestsAdapter
import com.servidos.app.adapter.FollowRequestsHeaderAdapter
import com.servidos.app.adapter.MutesAdapter
import com.servidos.app.databinding.FragmentAccountListBinding
import com.servidos.app.db.AccountManager
import com.servidos.app.di.Injectable
import com.servidos.app.entity.Account
import com.servidos.app.entity.Relationship
import com.servidos.app.interfaces.AccountActionListener
import com.servidos.app.network.MastodonApi
import com.servidos.app.settings.PrefKeys
import com.servidos.app.util.HttpHeaderLink
import com.servidos.app.util.hide
import com.servidos.app.util.show
import com.servidos.app.util.viewBinding
import com.servidos.app.view.EndlessOnScrollListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import retrofit2.Response
import java.io.IOException
import java.util.HashMap
import javax.inject.Inject

class AccountListFragment : Fragment(R.layout.fragment_account_list), AccountActionListener, Injectable {

    @Inject
    lateinit var api: MastodonApi
    @Inject
    lateinit var accountManager: AccountManager

    private val binding by viewBinding(FragmentAccountListBinding::bind)

    private lateinit var type: Type
    private var id: String? = null

    private lateinit var scrollListener: EndlessOnScrollListener
    private lateinit var adapter: AccountAdapter<*>
    private var fetching = false
    private var bottomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getSerializable(ARG_TYPE) as Type
        id = arguments?.getString(ARG_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.layoutManager = layoutManager
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))

        val pm = PreferenceManager.getDefaultSharedPreferences(view.context)
        val animateAvatar = pm.getBoolean(PrefKeys.ANIMATE_GIF_AVATARS, false)
        val animateEmojis = pm.getBoolean(PrefKeys.ANIMATE_CUSTOM_EMOJIS, false)

        adapter = when (type) {
            Type.BLOCKS -> BlocksAdapter(this, animateAvatar, animateEmojis)
            Type.MUTES -> MutesAdapter(this, animateAvatar, animateEmojis)
            Type.FOLLOW_REQUESTS -> {
                val headerAdapter = FollowRequestsHeaderAdapter(accountManager.activeAccount!!.domain, arguments?.get(ARG_ACCOUNT_LOCKED) == true)
                val followRequestsAdapter = FollowRequestsAdapter(this, animateAvatar, animateEmojis)
                binding.recyclerView.adapter = ConcatAdapter(headerAdapter, followRequestsAdapter)
                followRequestsAdapter
            }
            else -> FollowAdapter(this, animateAvatar, animateEmojis)
        }
        if (binding.recyclerView.adapter == null) {
            binding.recyclerView.adapter = adapter
        }

        scrollListener = object : EndlessOnScrollListener(layoutManager) {
            override fun onLoadMore(totalItemsCount: Int, view: RecyclerView) {
                if (bottomId == null) {
                    return
                }
                fetchAccounts(bottomId)
            }
        }

        binding.recyclerView.addOnScrollListener(scrollListener)

        fetchAccounts()
    }

    override fun onViewAccount(id: String) {
        (activity as BaseActivity?)?.let {
            val intent = AccountActivity.getIntent(it, id)
            it.startActivityWithSlideInAnimation(intent)
        }
    }

    override fun onMute(mute: Boolean, id: String, position: Int, notifications: Boolean) {
        if (!mute) {
            api.unmuteAccount(id)
        } else {
            api.muteAccount(id, notifications)
        }
            .autoDispose(from(this))
            .subscribe(
                {
                    onMuteSuccess(mute, id, position, notifications)
                },
                {
                    onMuteFailure(mute, id, notifications)
                }
            )
    }

    private fun onMuteSuccess(muted: Boolean, id: String, position: Int, notifications: Boolean) {
        val mutesAdapter = adapter as MutesAdapter
        if (muted) {
            mutesAdapter.updateMutingNotifications(id, notifications, position)
            return
        }
        val unmutedUser = mutesAdapter.removeItem(position)

        if (unmutedUser != null) {
            Snackbar.make(binding.recyclerView, R.string.confirmation_unmuted, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    mutesAdapter.addItem(unmutedUser, position)
                    onMute(true, id, position, notifications)
                }
                .show()
        }
    }

    private fun onMuteFailure(mute: Boolean, accountId: String, notifications: Boolean) {
        val verb = if (mute) {
            if (notifications) {
                "mute (notifications = true)"
            } else {
                "mute (notifications = false)"
            }
        } else {
            "unmute"
        }
        Log.e(TAG, "Failed to $verb account id $accountId")
    }

    override fun onBlock(block: Boolean, id: String, position: Int) {
        if (!block) {
            api.unblockAccount(id)
        } else {
            api.blockAccount(id)
        }
            .autoDispose(from(this))
            .subscribe(
                {
                    onBlockSuccess(block, id, position)
                },
                {
                    onBlockFailure(block, id)
                }
            )
    }

    private fun onBlockSuccess(blocked: Boolean, id: String, position: Int) {
        if (blocked) {
            return
        }
        val blocksAdapter = adapter as BlocksAdapter
        val unblockedUser = blocksAdapter.removeItem(position)

        if (unblockedUser != null) {
            Snackbar.make(binding.recyclerView, R.string.confirmation_unblocked, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    blocksAdapter.addItem(unblockedUser, position)
                    onBlock(true, id, position)
                }
                .show()
        }
    }

    private fun onBlockFailure(block: Boolean, accountId: String) {
        val verb = if (block) {
            "block"
        } else {
            "unblock"
        }
        Log.e(TAG, "Failed to $verb account accountId $accountId")
    }

    override fun onRespondToFollowRequest(
        accept: Boolean,
        accountId: String,
        position: Int
    ) {

        if (accept) {
            api.authorizeFollowRequest(accountId)
        } else {
            api.rejectFollowRequest(accountId)
        }.observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
            .subscribe(
                {
                    onRespondToFollowRequestSuccess(position)
                },
                { throwable ->
                    val verb = if (accept) {
                        "accept"
                    } else {
                        "reject"
                    }
                    Log.e(TAG, "Failed to $verb account id $accountId.", throwable)
                }
            )
    }

    private fun onRespondToFollowRequestSuccess(position: Int) {
        val followRequestsAdapter = adapter as FollowRequestsAdapter
        followRequestsAdapter.removeItem(position)
    }

    private fun getFetchCallByListType(fromId: String?): Single<Response<List<Account>>> {
        return when (type) {
            Type.FOLLOWS -> {
                val accountId = requireId(type, id)
                api.accountFollowing(accountId, fromId)
            }
            Type.FOLLOWERS -> {
                val accountId = requireId(type, id)
                api.accountFollowers(accountId, fromId)
            }
            Type.BLOCKS -> api.blocks(fromId)
            Type.MUTES -> api.mutes(fromId)
            Type.FOLLOW_REQUESTS -> api.followRequests(fromId)
            Type.REBLOGGED -> {
                val statusId = requireId(type, id)
                api.statusRebloggedBy(statusId, fromId)
            }
            Type.FAVOURITED -> {
                val statusId = requireId(type, id)
                api.statusFavouritedBy(statusId, fromId)
            }
        }
    }

    private fun requireId(type: Type, id: String?): String {
        return requireNotNull(id) { "id must not be null for type " + type.name }
    }

    private fun fetchAccounts(fromId: String? = null) {
        if (fetching) {
            return
        }
        fetching = true

        if (fromId != null) {
            binding.recyclerView.post { adapter.setBottomLoading(true) }
        }

        getFetchCallByListType(fromId)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
            .subscribe(
                { response ->
                    val accountList = response.body()

                    if (response.isSuccessful && accountList != null) {
                        val linkHeader = response.headers()["Link"]
                        onFetchAccountsSuccess(accountList, linkHeader)
                    } else {
                        onFetchAccountsFailure(Exception(response.message()))
                    }
                },
                { throwable ->
                    onFetchAccountsFailure(throwable)
                }
            )
    }

    private fun onFetchAccountsSuccess(accounts: List<Account>, linkHeader: String?) {
        adapter.setBottomLoading(false)

        val links = HttpHeaderLink.parse(linkHeader)
        val next = HttpHeaderLink.findByRelationType(links, "next")
        val fromId = next?.uri?.getQueryParameter("max_id")

        if (adapter.itemCount > 0) {
            adapter.addItems(accounts)
        } else {
            adapter.update(accounts)
        }

        if (adapter is MutesAdapter) {
            fetchRelationships(accounts.map { it.id })
        }

        bottomId = fromId

        fetching = false

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            binding.messageView.setup(
                R.drawable.elephant_friend_empty,
                R.string.message_empty,
                null
            )
        } else {
            binding.messageView.hide()
        }
    }

    private fun fetchRelationships(ids: List<String>) {
        api.relationships(ids)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this))
            .subscribe(::onFetchRelationshipsSuccess) {
                onFetchRelationshipsFailure(ids)
            }
    }

    private fun onFetchRelationshipsSuccess(relationships: List<Relationship>) {
        val mutesAdapter = adapter as MutesAdapter
        val mutingNotificationsMap = HashMap<String, Boolean>()
        relationships.map { mutingNotificationsMap.put(it.id, it.mutingNotifications) }
        mutesAdapter.updateMutingNotificationsMap(mutingNotificationsMap)
    }

    private fun onFetchRelationshipsFailure(ids: List<String>) {
        Log.e(TAG, "Fetch failure for relationships of accounts: $ids")
    }

    private fun onFetchAccountsFailure(throwable: Throwable) {
        fetching = false
        Log.e(TAG, "Fetch failure", throwable)

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            if (throwable is IOException) {
                binding.messageView.setup(R.drawable.elephant_offline, R.string.error_network) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            } else {
                binding.messageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AccountList" // logging tag
        private const val ARG_TYPE = "type"
        private const val ARG_ID = "id"
        private const val ARG_ACCOUNT_LOCKED = "acc_locked"

        fun newInstance(type: Type, id: String? = null, accountLocked: Boolean = false): AccountListFragment {
            return AccountListFragment().apply {
                arguments = Bundle(2).apply {
                    putSerializable(ARG_TYPE, type)
                    putString(ARG_ID, id)
                    putBoolean(ARG_ACCOUNT_LOCKED, accountLocked)
                }
            }
        }
    }
}
