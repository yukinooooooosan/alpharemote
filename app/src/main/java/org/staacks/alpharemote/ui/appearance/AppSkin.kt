package org.staacks.alpharemote.ui.appearance

/** Stable storage IDs are independent of enum order and display translations. */
enum class AppSkin(val storageId: String) {
    SIMPLE("simple"), KAWAII("kawaii");

    companion object {
        fun fromStorage(value: String?): AppSkin = entries.firstOrNull { it.storageId == value } ?: SIMPLE
    }
}
