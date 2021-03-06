/* Copyright 2019 Joel Pyska
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

package com.servidos.app.components.report.adapter

import android.view.View
import com.servidos.app.entity.Status
import com.servidos.app.interfaces.LinkListener

interface AdapterHandler : LinkListener {
    fun showMedia(v: View?, status: Status?, idx: Int)
    fun setStatusChecked(status: Status, isChecked: Boolean)
    fun isStatusChecked(id: String): Boolean
}
