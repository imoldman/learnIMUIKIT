/*
 * Copyright (c) 2022 NetEase, Inc.  All rights reserved.
 * Use of this source code is governed by a MIT license that can be found in the LICENSE file.
 */

package com.netease.yunxin.kit.conversationkit.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.netease.yunxin.kit.common.ui.CommonUIClient;
import com.netease.yunxin.kit.conversationkit.ConversationService;

public class ConversationUIService extends ConversationService {

    @NonNull
    @Override
    public String getServiceName() {
        return "ConversationUIService";
    }

    @NonNull
    @Override
    public String getVersionName() {
        return BuildConfig.versionName;
    }

    @NonNull
    @Override
    public ConversationService create(@NonNull Context context) {
        CommonUIClient.init(context);
        return this;
    }
}