package com.servidos.app.viewdata

import android.os.Parcelable
import com.servidos.app.entity.Attachment
import com.servidos.app.entity.Status
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttachmentViewData(
    val attachment: Attachment,
    val statusId: String,
    val statusUrl: String
) : Parcelable {
    companion object {
        @JvmStatic
        fun list(status: Status): List<AttachmentViewData> {
            val actionable = status.actionableStatus
            return actionable.attachments.map {
                AttachmentViewData(it, actionable.id, actionable.url!!)
            }
        }
    }
}
