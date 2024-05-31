// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.chatkit.ui.normal.viewholder.collection;

import android.view.View;
import androidx.annotation.NonNull;
import com.netease.nimlib.sdk.v2.message.V2NIMMessage;
import com.netease.nimlib.sdk.v2.message.attachment.V2NIMMessageVideoAttachment;
import com.netease.nimlib.sdk.v2.message.enums.V2NIMMessageAttachmentUploadState;
import com.netease.nimlib.sdk.v2.message.enums.V2NIMMessageSendingState;
import com.netease.yunxin.kit.chatkit.media.BitmapDecoder;
import com.netease.yunxin.kit.chatkit.ui.databinding.CollectionBaseViewHolderBinding;
import com.netease.yunxin.kit.chatkit.ui.model.CollectionBean;
import com.netease.yunxin.kit.common.utils.FileUtils;
import com.netease.yunxin.kit.common.utils.ImageUtils;
import com.netease.yunxin.kit.common.utils.SizeUtils;
import com.netease.yunxin.kit.common.utils.TimeUtils;
import java.util.Locale;

/** 收藏视频消息ViewHolder */
public class CollectionVideoViewHolder extends CollectionThumbViewHolder {
  private static final String TAG = "ChatCollectionVideoViewHolder";
  // 进度条最大值
  private static final int PROGRESS_MAX = 100;

  public CollectionVideoViewHolder(@NonNull CollectionBaseViewHolderBinding parent, int viewType) {
    super(parent, viewType);
  }

  @Override
  public void onBindData(CollectionBean message, int position) {
    super.onBindData(message, position);
    long second = TimeUtils.getSecondsByMilliseconds(getAttachment(message).getDuration());
    if (second <= 0) {
      second = 1;
    }
    binding.duration.setText(String.format(Locale.CHINA, "%02d:%02d", second / 60, second % 60));
    binding.duration.setVisibility(View.VISIBLE);
    binding.progressBar.setMax(PROGRESS_MAX);
    binding.progressBar.setIndeterminate(true);
    updateStatus(message.getMessageData());
  }

  @Override
  protected void onProgressUpdate(CollectionBean data) {
    super.onProgressUpdate(data);
    binding.progressBar.setIndeterminate(false);
    updateProgress((int) data.getLoadProgress());
  }

  private V2NIMMessageVideoAttachment getAttachment(CollectionBean messageBean) {
    return (V2NIMMessageVideoAttachment) messageBean.getMessageData().getAttachment();
  }

  private void updateStatus(V2NIMMessage message) {
    if (message.getSendingState() == V2NIMMessageSendingState.V2NIM_MESSAGE_SENDING_STATE_SENDING
        || message.getAttachmentUploadState()
            == V2NIMMessageAttachmentUploadState.V2NIM_MESSAGE_ATTACHMENT_UPLOAD_STATE_UPLOADING) {
      binding.progressBar.setVisibility(View.VISIBLE);
      binding.progressBarInsideIcon.setVisibility(View.VISIBLE);
      binding.playIcon.setVisibility(View.GONE);
    } else {
      updateProgress(PROGRESS_MAX);
    }
  }

  private void updateProgress(int progress) {
    if (progress >= PROGRESS_MAX) {
      // finish
      binding.progressBar.setVisibility(View.GONE);
      binding.progressBarInsideIcon.setVisibility(View.GONE);
      binding.playIcon.setVisibility(View.VISIBLE);
    } else {
      binding.progressBar.setVisibility(View.VISIBLE);
      binding.progressBarInsideIcon.setVisibility(View.VISIBLE);
      binding.playIcon.setVisibility(View.GONE);
      binding.progressBar.setProgress(progress);
    }
  }

  @Override
  protected String thumbFromSourceFile(String path) {
    V2NIMMessageVideoAttachment attachment =
        (V2NIMMessageVideoAttachment) getMsgInternal().getAttachment();
    String thumbPath = attachment.getPath();
    return BitmapDecoder.extractThumbnail(path, thumbPath) ? thumbPath : attachment.getUrl();
  }

  @Override
  protected int[] getBounds(String path) {
    int[] bounds = null;
    if (path != null && FileUtils.isFileExists(path)) {
      bounds = ImageUtils.getSize(path);
    }
    if (bounds == null) {
      V2NIMMessageVideoAttachment attachment =
          (V2NIMMessageVideoAttachment) getMsgInternal().getAttachment();
      int width = attachment.getWidth() > 0 ? attachment.getWidth() : SizeUtils.dp2px(60);
      int height = attachment.getHeight() > 0 ? attachment.getHeight() : SizeUtils.dp2px(90);
      bounds = new int[] {width, height};
    }
    return bounds;
  }

  @Override
  protected float[] getCorners() {
    float corner = SizeUtils.dp2px(12);
    return new float[] {corner, corner, corner, corner};
  }
}
