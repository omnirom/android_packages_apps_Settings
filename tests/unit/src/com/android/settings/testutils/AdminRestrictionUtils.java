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
 * limitations under the License.
 */

package com.android.settings.testutils;

import android.app.admin.RoleAuthority;
import android.app.role.RoleManager;

import java.util.Set;

public class AdminRestrictionUtils {

    public static final RoleAuthority SUPERVISION_ROLE_AUTHORITY = new RoleAuthority(Set.of(
            RoleManager.ROLE_SYSTEM_SUPERVISION));
}
