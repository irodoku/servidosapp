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

package com.servidos.app.components.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.servidos.app.R
import com.servidos.app.settings.PrefKeys
import com.servidos.app.settings.checkBoxPreference
import com.servidos.app.settings.makePreferenceScreen
import com.servidos.app.settings.preferenceCategory

class TabFilterPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        makePreferenceScreen {
            preferenceCategory(R.string.title_home) { category ->
                category.isIconSpaceReserved = false

                checkBoxPreference {
                    setTitle(R.string.pref_title_show_boosts)
                    key = PrefKeys.TAB_FILTER_HOME_BOOSTS
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }

                checkBoxPreference {
                    setTitle(R.string.pref_title_show_replies)
                    key = PrefKeys.TAB_FILTER_HOME_REPLIES
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            }
        }
    }

    companion object {
        fun newInstance(): TabFilterPreferencesFragment {
            return TabFilterPreferencesFragment()
        }
    }
}
