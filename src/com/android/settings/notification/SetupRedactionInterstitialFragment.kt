/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.notification

import android.app.Activity.RESULT_CANCELED
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS
import android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS
import android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SCREEN_HIDE_WORK_NOTIFICATION_CONTENT
import android.app.admin.DevicePolicyResources.Strings.Settings.LOCK_SCREEN_SHOW_WORK_NOTIFICATION_CONTENT
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS
import android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.settings.NormalRadioButtonItem
import com.android.settings.R
import com.android.settings.RestrictedRadioButtonItem
import com.android.settings.SetupRedactionInterstitial
import com.android.settings.Utils
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin
import com.android.settingslib.RestrictedLockUtilsInternal
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.android.setupdesign.items.ItemGroup
import com.google.android.setupdesign.items.RadioButtonItem
import com.google.android.setupdesign.items.RecyclerItemAdapter

/**
 * Fragment for Setup Wizard's RedactionInterstitial screen. It can only be used in Setup Wizard.
 */
// TODO: b/417603276 - Migrate this class to setup wizard's package.
class SetupRedactionInterstitialFragment : Fragment(), RadioButtonItem.OnCheckedChangeListener {

    // default value, will be intitialized later
    private var userId: Int = -1

    private lateinit var options: ItemGroup
    private lateinit var showAllButton: RestrictedRadioButtonItem
    private lateinit var redactSensitiveButton: RestrictedRadioButtonItem
    private lateinit var hideAllButton: RadioButtonItem
    private lateinit var buttonGroup: ButtonGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.redaction_interstitial_expressive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = view.findViewById<GlifRecyclerLayout>(R.id.setup_wizard_layout)
        val adapter = (layout.getAdapter() as RecyclerItemAdapter)
        options = adapter.findItemById(R.id.items_redaction_interstitial) as ItemGroup

        showAllButton =
            (options.getItemAt(0) as RestrictedRadioButtonItem).apply {
                setOnCheckedChangeListener(this@SetupRedactionInterstitialFragment)
            }
        redactSensitiveButton =
            (options.getItemAt(1) as RestrictedRadioButtonItem).apply {
                setOnCheckedChangeListener(this@SetupRedactionInterstitialFragment)
            }
        hideAllButton =
            (options.getItemAt(2) as NormalRadioButtonItem).apply {
                setOnCheckedChangeListener(this@SetupRedactionInterstitialFragment)
            }
        buttonGroup = ButtonGroup(setOf(showAllButton, redactSensitiveButton, hideAllButton))

        userId = Utils.getUserIdFromBundle(requireContext(), activity?.intent?.extras)
        val devicePolicyManager: DevicePolicyManager =
            requireContext().getSystemService(DevicePolicyManager::class.java)
        if (UserManager.get(requireContext()).isManagedProfile(userId)) {
            layout.descriptionText =
                getString(R.string.lock_screen_notifications_interstitial_message_profile)
            showAllButton.title =
                devicePolicyManager.resources.getString(
                    LOCK_SCREEN_SHOW_WORK_NOTIFICATION_CONTENT
                ) {
                    getString(R.string.lock_screen_notifications_summary_show_profile)
                }
            redactSensitiveButton.title =
                devicePolicyManager.resources.getString(
                    LOCK_SCREEN_HIDE_WORK_NOTIFICATION_CONTENT
                ) {
                    getString(R.string.lock_screen_notifications_summary_hide_profile)
                }
            hideAllButton.setVisible(false)
        }

        val mixin = layout.getMixin(FooterBarMixin::class.java)
        mixin.setPrimaryButton(
            FooterButton.Builder(requireContext())
                .setText(R.string.app_notifications_dialog_done)
                .setListener(this::onDoneButtonClicked)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
        )
    }

    private fun onDoneButtonClicked(view: View) {
        check(activity is SetupRedactionInterstitial) {
            "It can only be called from SetupRedactionInterstitial"
        }
        activity?.apply {
            setResult(RESULT_CANCELED, null)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Disable buttons according to policy.
        checkNotificationFeaturesAndSetDisabled(
            showAllButton,
            KEYGUARD_DISABLE_SECURE_NOTIFICATIONS or KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS,
        )
        checkNotificationFeaturesAndSetDisabled(
            redactSensitiveButton,
            KEYGUARD_DISABLE_SECURE_NOTIFICATIONS,
        )
        loadFromSettings()
    }

    private fun checkNotificationFeaturesAndSetDisabled(
        button: RestrictedRadioButtonItem,
        keyguardNotifications: Int,
    ) {
        // Below line will crash the app. Have no idea why.
        val admin: EnforcedAdmin? =
            RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                activity,
                keyguardNotifications,
                userId,
            )
        button.setDisabledByAdmin(admin)
    }

    private fun loadFromSettings() {
        val showUnRedactedDefault: Boolean =
            requireContext().resources.getBoolean(R.bool.default_allow_sensitive_lockscreen_content)
        val managedProfile: Boolean = UserManager.get(requireContext()).isManagedProfile(userId)

        // Hiding all notifications is device-wide setting, managed profiles can only set
        // whether their notifications are show in full or redacted.
        val showNotifications: Boolean =
            managedProfile ||
                Settings.Secure.getIntForUser(
                    requireContext().contentResolver,
                    LOCK_SCREEN_SHOW_NOTIFICATIONS,
                    0,
                    userId,
                ) != 0
        val showUnredacted: Boolean =
            Settings.Secure.getIntForUser(
                requireContext().contentResolver,
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                if (showUnRedactedDefault) 1 else 0,
                userId,
            ) != 0

        var checkedButton: RadioButtonItem = hideAllButton
        Log.i(TAG, "showNotifications=$showNotifications, showUnredacted=$showUnredacted")
        if (showNotifications) {
            if (showUnredacted && showAllButton.disabledByAdmin.not()) {
                checkedButton = showAllButton
            } else if (redactSensitiveButton.disabledByAdmin.not()) {
                checkedButton = redactSensitiveButton
            }
        }
        Log.i(TAG, "default checked button is ${checkedButton.id}")
        buttonGroup.setCheckedButton(checkedButton)
    }

    override fun onCheckedChange(item: RadioButtonItem, isChecked: Boolean) {
        Log.v(TAG, "onCheckedChange: ${item.id}, isChecked=$isChecked")
        if (isChecked.not()) {
            return
        }
        if (
            item is RestrictedRadioButtonItem && (item as RestrictedRadioButtonItem).disabledByAdmin
        ) {
            // A workaround to make sure the raido button is not checked.
            item.setChecked(false)
            showAdminSupportDetailsDialog(item)
            return
        }
        buttonGroup.setCheckedButton(item)

        val showed: Boolean = item == showAllButton
        val enabled: Boolean = item != hideAllButton
        Settings.Secure.putIntForUser(
            requireContext().contentResolver,
            LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
            if (showed) 1 else 0,
            userId,
        )
        Settings.Secure.putIntForUser(
            requireContext().contentResolver,
            LOCK_SCREEN_SHOW_NOTIFICATIONS,
            if (enabled) 1 else 0,
            userId,
        )

        val checkedButton = buttonGroup.getCheckedButton()
        Log.i(TAG, "checked button is ${checkedButton?.id}")
    }

    private fun showAdminSupportDetailsDialog(item: RestrictedRadioButtonItem) {
        if (item.disabledByAdmin.not()) {
            return
        }
        Log.v(TAG, "showAdminSupportDetailsDialog")
        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(requireContext(), item.enforcedAdmin)
    }

    internal class ButtonGroup(private val buttonCollection: Set<RadioButtonItem>) {

        private var lastCheckedButton: RadioButtonItem? = null

        /** Returns the only one checked button (if any). */
        fun getCheckedButton(): RadioButtonItem? {
            return buttonCollection.firstOrNull { it.isChecked() }
        }

        fun setCheckedButton(button: RadioButtonItem) {
            if (button == lastCheckedButton) {
                return
            }
            lastCheckedButton?.setChecked(false)
            button.setChecked(true)
            lastCheckedButton = button
        }

        fun setCheckedButton(id: Int) {
            if (id == lastCheckedButton?.id) {
                return
            }
            buttonCollection.firstOrNull { it.id == id }?.let { setCheckedButton(it) }
        }
    }

    private companion object {
        const val TAG = "SetupRedactionInterstitial"
    }
}
