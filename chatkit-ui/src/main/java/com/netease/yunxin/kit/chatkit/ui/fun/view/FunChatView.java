// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.chatkit.ui.fun.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;
import com.netease.nimlib.sdk.v2.message.V2NIMMessage;
import com.netease.nimlib.sdk.v2.message.V2NIMMessageRefer;
import com.netease.yunxin.kit.chatkit.ui.ChatUIConfig;
import com.netease.yunxin.kit.chatkit.ui.ChatViewHolderDefaultFactory;
import com.netease.yunxin.kit.chatkit.ui.IChatFactory;
import com.netease.yunxin.kit.chatkit.ui.R;
import com.netease.yunxin.kit.chatkit.ui.builder.IChatViewCustom;
import com.netease.yunxin.kit.chatkit.ui.common.ChatMsgCache;
import com.netease.yunxin.kit.chatkit.ui.common.MessageHelper;
import com.netease.yunxin.kit.chatkit.ui.databinding.FunChatViewBinding;
import com.netease.yunxin.kit.chatkit.ui.fun.factory.FunChatViewHolderFactory;
import com.netease.yunxin.kit.chatkit.ui.interfaces.IChatView;
import com.netease.yunxin.kit.chatkit.ui.interfaces.IMessageItemClickListener;
import com.netease.yunxin.kit.chatkit.ui.interfaces.IMessageLoadHandler;
import com.netease.yunxin.kit.chatkit.ui.interfaces.IMessageProxy;
import com.netease.yunxin.kit.chatkit.ui.interfaces.IMessageReader;
import com.netease.yunxin.kit.chatkit.ui.model.ChatMessageBean;
import com.netease.yunxin.kit.chatkit.ui.view.ait.AitManager;
import com.netease.yunxin.kit.chatkit.ui.view.ait.AitTextChangeListener;
import com.netease.yunxin.kit.chatkit.ui.view.input.ActionConstants;
import com.netease.yunxin.kit.chatkit.ui.view.message.ChatMessageListView;
import com.netease.yunxin.kit.common.ui.widgets.BackTitleBar;
import com.netease.yunxin.kit.corekit.im2.model.IMMessageProgress;
import java.util.List;

/** chat view contain all view about chat */
public class FunChatView extends LinearLayout implements IChatView, AitTextChangeListener {

  protected CharSequence titleName;
  AitManager aitTextManager;
  FunChatViewBinding binding;
  private boolean canRender = true;

  protected boolean isMultiSelect;

  protected IMessageProxy messageProxy;

  public FunChatView(Context context) {
    super(context);
    init(null);
  }

  public FunChatView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public FunChatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(AttributeSet attrs) {
    ChatViewHolderDefaultFactory.getInstance().config(FunChatViewHolderFactory.getInstance());
    LayoutInflater layoutInflater = LayoutInflater.from(getContext());
    binding = FunChatViewBinding.inflate(layoutInflater, this, true);
    binding.messageView.setOnListViewEventListener(
        new ChatMessageListView.OnListViewEventListener() {
          @Override
          public void onListViewStartScroll() {
            binding.chatBottomInputLayout.collapse(true);
          }

          @Override
          public void onListViewTouched() {
            binding.chatBottomInputLayout.collapse(true);
          }
        });
    binding.getRoot().setOnClickListener(v -> binding.chatBottomInputLayout.collapse(true));
    binding.chatMsgMultiDeleteLayout.setOnClickListener(
        v -> {
          if (messageProxy != null && binding.chatMsgMultiDeleteIv.isEnabled()) {
            messageProxy.onMultiActionClick(v, ActionConstants.ACTION_TYPE_MULTI_DELETE);
          }
        });
    binding.chatMsgMultiForwardLayout.setOnClickListener(
        v -> {
          if (messageProxy != null && binding.chatMsgMultiForwardIv.isEnabled()) {
            messageProxy.onMultiActionClick(v, ActionConstants.ACTION_TYPE_MULTI_FORWARD);
          }
        });

    binding.chatMsgSingleForwardLayout.setOnClickListener(
        v -> {
          if (messageProxy != null && binding.chatMsgSingleForwardIv.isEnabled()) {
            messageProxy.onMultiActionClick(v, ActionConstants.ACTION_TYPE_SINGLE_FORWARD);
          }
        });

    //设置文本输入模式下，切换富文本按钮点击事件
    binding.chatBottomInputLayout.setRichTextSwitchListener(
        v -> {
          if (binding.chatRichLayout.getVisibility() == GONE
              && !binding.chatBottomInputLayout.isMute()) {
            showRichInputPanel();
          }
        });

    binding.chatRichSwitchLayout.setOnClickListener(v -> hideRichInputPanel());
    binding.chatRichSend.setOnClickListener(
        v -> {
          String msg = binding.chatRichContentEt.getEditableText().toString();
          String title = binding.chatRichTitleEt.getEditableText().toString();
          if (messageProxy != null) {
            if (!TextUtils.isEmpty(title) && TextUtils.getTrimmedLength(title) > 0) {
              if (messageProxy.sendRichTextMessage(
                  title, msg, binding.chatBottomInputLayout.replyMessage)) {
                binding.chatRichTitleEt.setText("");
                binding.chatRichContentEt.setText("");
                hideRichInputPanel();
                binding.chatBottomInputLayout.clearReplyMsg();
              } else {
                clearInvalidInputContent();
              }
            } else {
              if (messageProxy.sendTextMessage(msg, binding.chatBottomInputLayout.replyMessage)) {
                binding.chatRichContentEt.setText("");
                binding.chatRichTitleEt.setText("");
                hideRichInputPanel();
                binding.chatBottomInputLayout.clearReplyMsg();
              } else {
                clearInvalidInputContent();
              }
            }
          }
        });

    binding.chatRichTitleEt.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_NEXT) {
            binding.chatRichContentEt.requestFocus();
            binding.chatRichContentEt.setSelection(
                binding.chatRichContentEt.getEditableText().length());
            return true;
          }
          return true;
        });
  }

  private void clearInvalidInputContent() {
    String msg = binding.chatRichContentEt.getEditableText().toString();
    String title = binding.chatRichTitleEt.getEditableText().toString();
    if (TextUtils.getTrimmedLength(title) < 1) {
      binding.chatRichTitleEt.setText("");
    }
    if (TextUtils.getTrimmedLength(msg) < 1) {
      binding.chatRichContentEt.setText("");
    }
  }

  @Override
  public void onTextAdd(String content, int start, int length, boolean hasAt) {
    if (binding.chatRichContentEt.getVisibility() == VISIBLE) {
      SpannableString spannableString =
          MessageHelper.generateAtSpanString(hasAt ? content : "@" + content);
      binding
          .chatRichContentEt
          .getEditableText()
          .replace(hasAt ? start : start - 1, start, spannableString);
    }
  }

  @Override
  public void onTextDelete(int start, int length) {

    if (binding.chatRichContentEt.getVisibility() == VISIBLE) {
      int end = start + length - 1;
      binding.chatRichContentEt.getEditableText().replace(start, end, "");
    }
  }

  @Override
  public void hideRichInputPanel() {
    if (binding.chatRichLayout.getVisibility() == VISIBLE) {
      String title = binding.chatRichTitleEt.getText().toString();
      boolean titleFocus = binding.chatRichTitleEt.hasFocus() && !TextUtils.isEmpty(title);
      binding.chatBottomInputLayout.switchRichInput(
          titleFocus, title, binding.chatRichContentEt.getText().toString());
      binding.chatRichLayout.setVisibility(GONE);
      if (aitTextManager != null) {
        aitTextManager.setAitTextChangeListener(binding.chatBottomInputLayout);
      }
      binding.chatRichContentEt.removeTextChangedListener(richBodyInputTextWatcher);
      binding.chatRichTitleEt.removeTextChangedListener(richTitleInputTextWatcher);
    }
  }

  @Override
  public void showRichInputPanel() {
    binding.chatRichLayout.setVisibility(VISIBLE);
    String title = binding.chatBottomInputLayout.getRichInputTitle();
    String content = binding.chatBottomInputLayout.getRichInputContent();
    MessageHelper.identifyExpressionForRichTextMsg(
        getContext(),
        binding.chatRichContentEt,
        content,
        aitTextManager != null ? aitTextManager.getAitContactsModel() : null);
    MessageHelper.identifyFaceExpression(
        getContext(), binding.chatRichTitleEt, title, ImageSpan.ALIGN_BOTTOM);
    if (aitTextManager != null) {
      aitTextManager.setAitTextChangeListener(this);
    }
    binding.chatRichContentEt.addTextChangedListener(richBodyInputTextWatcher);
    binding.chatRichTitleEt.addTextChangedListener(richTitleInputTextWatcher);
    binding.chatBottomInputLayout.clearInputEditTextChange();
    binding.chatBottomInputLayout.hideCurrentInput();
    binding.chatBottomInputLayout.hideAndClearRichInput();
  }

  public BackTitleBar getTitleBar() {
    return binding.titleBar;
  }

  public FrameLayout getTitleBarLayout() {
    return binding.titleLayout;
  }

  public ChatMessageListView getMessageListView() {
    return binding.messageView;
  }

  public void setLoadHandler(IMessageLoadHandler loadHandler) {
    binding.messageView.setLoadHandler(loadHandler);
  }

  public void setMessageReader(IMessageReader messageReader) {
    binding.messageView.setMessageReader(messageReader);
  }

  public void setItemClickListener(IMessageItemClickListener itemClickListener) {
    binding.messageView.setItemClickListener(itemClickListener);
  }

  public void setChatConfig(ChatUIConfig config) {
    if (config != null) {
      binding.messageView.setMessageProperties(config.messageProperties);
      binding.chatBottomInputLayout.setInputProperties(config.inputProperties);
    }
  }

  public void clearMessageList() {
    binding.messageView.clearMessageList();
  }

  public void addMessageListForward(List<ChatMessageBean> messageList) {
    binding.messageView.addMessageListForward(messageList);
  }

  public void appendMessageList(List<ChatMessageBean> messageList) {
    binding.messageView.appendMessageList(messageList);
  }

  public void appendMessageList(List<ChatMessageBean> messageList, boolean needToScrollEnd) {
    binding.messageView.appendMessageList(messageList, needToScrollEnd);
  }

  public void updateUserInfo(List<UserInfo> userInfoList) {
    binding.messageView.updateUserInfo(userInfoList);
  }

  @Override
  public void notifyUserInfoChanged(List<String> accountIdList) {
    binding.messageView.notifyUserInfoChanged(accountIdList);
  }

  public void setMessageBackground(Drawable drawable) {
    binding.bodyLayout.setBackground(drawable);
  }

  public void setMessageBackgroundRes(int res) {
    binding.bodyLayout.setBackgroundResource(res);
  }

  public void setMessageBackgroundColor(int color) {
    binding.bodyLayout.setBackgroundColor(color);
  }

  @Override
  public void setTitleBarVisible(int visible) {
    binding.titleLayout.setVisibility(visible);
  }

  @Override
  public void setReeditMessage(String content) {
    hideRichInputPanel();
    binding.chatBottomInputLayout.switchRichInput(false, "", "");
    binding.chatBottomInputLayout.setReEditMessage(content);
  }

  @Override
  public void setReEditRichMessage(String title, String body) {
    showRichInputPanel();
    binding.chatRichTitleEt.setText(title);
    binding.chatRichContentEt.setText(body);
  }

  @Override
  public void setReplyMessage(ChatMessageBean messageBean) {
    binding.chatBottomInputLayout.setReplyMessage(messageBean);
  }

  public void setTypeState(boolean isTyping) {
    if (titleName == null) {
      titleName = binding.titleBar.getTitleTextView().getText();
    }
    CharSequence tempTitleName;
    if (isTyping) {
      tempTitleName = binding.getRoot().getContext().getString(R.string.chat_message_is_typing_fun);
    } else {
      tempTitleName = titleName;
    }
    binding.titleBar.setTitle(String.valueOf(tempTitleName));
  }

  public void setNetWorkState(boolean available) {
    if (available) {
      binding.notificationTextView.setVisibility(GONE);
    } else {
      binding.notificationTextView.setVisibility(VISIBLE);
      binding.notificationTextView.setTextSize(14);
      binding.notificationTextView.setText(R.string.chat_network_error_tip);
      binding.notificationTextView.setTextColor(
          getContext().getResources().getColor(R.color.color_50_000000));
      binding.notificationTextView.setBackgroundResource(R.color.color_fceeee);
    }
  }

  @Override
  public void showMultiSelect(boolean show) {
    binding.chatBottomInputLayout.collapse(true);
    binding.chatMsgMultiSelectLayout.setVisibility(show ? VISIBLE : GONE);
    binding.messageView.setMultiSelect(show);
    binding.bottomLayout.setVisibility(show ? GONE : VISIBLE);
    isMultiSelect = show;
    if (show) {
      binding.titleBar.setActionText(R.string.cancel);
      binding.titleBar.getActionTextView().setVisibility(VISIBLE);
      binding.titleBar.getActionImageView().setVisibility(GONE);
      binding.titleBar.setActionTextListener(v -> showMultiSelect(false));
      ChatMsgCache.clear();
    } else {
      ChatMsgCache.clear();
      binding.titleBar.getActionTextView().setVisibility(GONE);
      binding.titleBar.getActionImageView().setVisibility(VISIBLE);
      binding.titleBar.setActionTextListener(null);
    }
  }

  @Override
  public boolean isMultiSelect() {
    return isMultiSelect;
  }

  @Override
  public void setMultiSelectEnable(boolean enable) {
    binding.chatMsgMultiForwardIv.setEnabled(enable);
    binding.chatMsgMultiDeleteIv.setEnabled(enable);
    binding.chatMsgSingleForwardIv.setEnabled(enable);
  }

  public void appendMessage(ChatMessageBean message) {
    binding.messageView.appendMessage(message);
  }

  @Override
  public void revokeMessage(V2NIMMessageRefer message) {
    binding.messageView.revokeMessage(message);
  }

  @Override
  public void deleteMessage(List<ChatMessageBean> message) {
    binding.messageView.deleteMessage(message);
  }

  @Override
  public void deleteMessages(List<String> clientIds) {
    binding.messageView.deleteMessages(clientIds);
  }

  @Override
  public List<ChatMessageBean> getMessageList() {
    if (binding.messageView.getMessageAdapter() != null) {
      return binding.messageView.getMessageAdapter().getMessageList();
    }
    return null;
  }

  public void updateMessageStatus(ChatMessageBean message) {
    binding.messageView.updateMessageStatus(message);
  }

  public void updateMessage(ChatMessageBean message, Object payload) {
    binding.messageView.updateMessage(message, payload);
  }

  public void updateMessage(V2NIMMessage message, Object payload) {
    binding.messageView.updateMessage(message, payload);
  }

  public void updateProgress(IMMessageProgress progress) {
    binding.messageView.updateAttachmentProgress(progress);
  }

  @Override
  public void hideCurrentInput() {
    binding.chatBottomInputLayout.hideCurrentInput();
  }

  @Override
  public void updateInputHintInfo(String content) {
    binding.chatBottomInputLayout.updateInputHintInfo(content);
    binding.chatRichContentEt.setHint(content);
  }

  @Override
  public void setInputMute(boolean mute) {
    if (mute) {
      hideRichInputPanel();
    }
    binding.chatBottomInputLayout.setMute(mute);
  }

  public void setMessageProxy(IMessageProxy proxy) {
    messageProxy = proxy;
    binding.chatBottomInputLayout.init(proxy);
  }

  public MessageBottomLayout getBottomInputLayout() {
    return binding.chatBottomInputLayout;
  }

  public FrameLayout getChatBodyLayout() {
    return binding.bodyLayout;
  }

  public FrameLayout getChatBottomLayout() {
    return binding.bottomLayout;
  }

  public FunChatViewBinding getChatViewFunLayoutBinding() {
    return binding;
  }

  public FrameLayout getChatBodyTopLayout() {
    return binding.bodyTopLayout;
  }

  public FrameLayout getChatBodyBottomLayout() {
    return binding.bodyBottomLayout;
  }

  public void setAitManager(AitManager manager) {
    manager.setAitTextChangeListener(binding.chatBottomInputLayout);
    aitTextManager = manager;
    binding.chatBottomInputLayout.setAitTextWatcher(manager);
  }

  public void setMessageViewHolderFactory(IChatFactory viewHolderFactory) {
    binding.messageView.setViewHolderFactory(viewHolderFactory);
  }

  public void setLayoutCustom(IChatViewCustom layoutCustom) {
    if (layoutCustom != null) {
      layoutCustom.customizeChatLayout(this);
    }
  }

  public View getRootView() {
    return binding.getRoot();
  }

  private final TextWatcher richTitleInputTextWatcher =
      new TextWatcher() {
        private int start;
        private int count;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          if (!canRender) {
            return;
          }
          this.start = start;
          this.count = count;
          if (messageProxy != null) {
            messageProxy.onTypeStateChange(!TextUtils.isEmpty(s));
          }
        }

        @Override
        public void afterTextChanged(Editable s) {
          if (!canRender) {
            canRender = true;
            return;
          }
          SpannableString spannableString = new SpannableString(s);
          if (MessageHelper.replaceEmoticons(getContext(), spannableString, start, count)) {
            canRender = false;
            binding.chatRichTitleEt.setText(spannableString);
            binding.chatRichTitleEt.setSelection(spannableString.length());
          }
          if (TextUtils.isEmpty(s.toString()) && binding.chatRichTitleEt.isEnabled()) {
            binding.chatRichTitleEt.setHint(
                getRootView().getContext().getString(R.string.chat_message_rich_title_hint));
          }
        }
      };

  private final TextWatcher richBodyInputTextWatcher =
      new TextWatcher() {
        private int start;
        private int count;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          if (!canRender) {
            return;
          }
          if (aitTextManager != null) {
            aitTextManager.beforeTextChanged(s, start, count, after);
          }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
          if (!canRender) {
            return;
          }
          this.start = start;
          this.count = count;
          if (aitTextManager != null) {
            aitTextManager.onTextChanged(s, start, before, count);
          }
          if (messageProxy != null) {
            messageProxy.onTypeStateChange(!TextUtils.isEmpty(s));
          }
        }

        @Override
        public void afterTextChanged(Editable s) {
          if (!canRender) {
            canRender = true;
            return;
          }
          SpannableString spannableString = new SpannableString(s);
          if (MessageHelper.replaceEmoticons(getContext(), spannableString, start, count)) {
            canRender = false;
            binding.chatRichContentEt.setText(spannableString);
            binding.chatRichContentEt.setSelection(spannableString.length());
          }
          if (aitTextManager != null) {
            aitTextManager.afterTextChanged(s);
          }
          if (TextUtils.isEmpty(s.toString()) && binding.chatRichContentEt.isEnabled()) {
            binding.chatRichContentEt.setHint(binding.chatBottomInputLayout.getInputHit());
          }
        }
      };
}
