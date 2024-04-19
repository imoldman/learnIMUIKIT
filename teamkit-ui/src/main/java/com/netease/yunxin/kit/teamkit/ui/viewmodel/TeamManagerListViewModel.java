// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.teamkit.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.netease.nimlib.sdk.team.model.TeamMember;
import com.netease.nimlib.sdk.v2.team.enums.V2NIMTeamMemberRoleQueryType;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.chatkit.model.TeamMemberListResult;
import com.netease.yunxin.kit.chatkit.model.TeamMemberWithUserInfo;
import com.netease.yunxin.kit.chatkit.repo.TeamRepo;
import com.netease.yunxin.kit.common.ui.viewmodel.FetchResult;
import com.netease.yunxin.kit.common.ui.viewmodel.LoadStatus;
import com.netease.yunxin.kit.corekit.im2.extend.FetchCallback;
import java.util.List;

/**
 * 群管理员列表ViewModel 管理员的添加和移除功能
 *
 * <p>
 */
public class TeamManagerListViewModel extends TeamBaseViewModel {

  private static final String TAG = "TeamManagerListViewModel";
  private static final String LIB_TAG = "TeamKit-UI";

  // 获取群成员信息列表
  private final MutableLiveData<FetchResult<List<TeamMemberWithUserInfo>>> teamManagerWithUserData =
      new MutableLiveData<>();
  private final MutableLiveData<FetchResult<List<TeamMember>>> addRemoveManagerLiveData =
      new MutableLiveData<>();

  public MutableLiveData<FetchResult<List<TeamMember>>> getAddRemoveManagerLiveData() {
    return addRemoveManagerLiveData;
  }

  public MutableLiveData<FetchResult<List<TeamMemberWithUserInfo>>> getTeamManagerWithUserData() {
    return teamManagerWithUserData;
  }

  /**
   * 获取群成员列表信息 管理员业务层现在10个
   *
   * @param teamId 群ID
   */
  public void requestTeamManagers(String teamId) {
    ALog.d(LIB_TAG, TAG, "requestTeamMembers:" + teamId);
    TeamRepo.getTeamMemberListWithUserInfo(
        teamId,
        null,
        100,
        V2NIMTeamMemberRoleQueryType.V2NIM_TEAM_MEMBER_ROLE_QUERY_TYPE_MANAGER,
        new FetchCallback<>() {
          @Override
          public void onSuccess(@Nullable TeamMemberListResult param) {
            ALog.d(
                LIB_TAG,
                TAG,
                "requestTeamManagers,onSuccess:" + (param == null ? "null" : param.isFinished()));
            if (param != null) {
              hasMore = !param.isFinished();
              nextPageTag = param.getNextToken();
              teamManagerWithUserData.setValue(new FetchResult<>(param.getMemberList()));
            }
          }

          @Override
          public void onError(int errorCode, String errorMsg) {
            ALog.d(LIB_TAG, TAG, "requestTeamManagers,onFailed:" + errorCode);
            teamManagerWithUserData.setValue(new FetchResult<>(errorCode, errorMsg));
          }
        });
  }
  /**
   * 添加管理员
   *
   * @param teamId 群ID
   * @param members 成员ID列表
   */
  public void addManager(String teamId, List<String> members) {
    if (members == null || members.size() == 0) {
      return;
    }
    ALog.d(LIB_TAG, TAG, "addManager:" + teamId + "," + members.size());
    TeamRepo.addManagers(
        teamId,
        members,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(LIB_TAG, TAG, "addManager,onSuccess");
            addRemoveManagerLiveData.setValue(new FetchResult<>(LoadStatus.Success, null));
          }

          @Override
          public void onError(int errorCode, @NonNull String errorMsg) {
            ALog.d(LIB_TAG, TAG, "addManager,onFailed:" + errorCode);
            addRemoveManagerLiveData.setValue(new FetchResult<>(LoadStatus.Error));
          }
        });
  }

  /**
   * 移除管理员
   *
   * @param teamId 群ID
   * @param members 成员ID列表
   */
  public void removeManager(String teamId, List<String> members) {
    if (members == null || members.size() == 0) {
      return;
    }
    ALog.d(LIB_TAG, TAG, "removeManager:" + teamId + "," + members.size());
    TeamRepo.removeManagers(
        teamId,
        members,
        new FetchCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void param) {
            ALog.d(LIB_TAG, TAG, "removeManager,onSuccess");
            FetchResult<List<TeamMember>> result = new FetchResult<>(LoadStatus.Success, null);
            result.setType(FetchResult.FetchType.Remove);
            addRemoveManagerLiveData.setValue(result);
          }

          @Override
          public void onError(int errorCode, @NonNull String errorMsg) {
            ALog.d(LIB_TAG, TAG, "removeManager,onFailed:" + errorCode);
            addRemoveManagerLiveData.setValue(new FetchResult<>(LoadStatus.Error));
          }
        });
  }
}
