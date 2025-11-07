package com.nppang.backend.service;

import com.nppang.backend.entity.AppUser;
import com.nppang.backend.entity.GroupMember;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.repository.AppUserRepository;
import com.nppang.backend.repository.GroupMemberRepository;
import com.nppang.backend.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    private final UserGroupRepository userGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AppUserRepository appUserRepository;

    @Autowired
    public GroupService(UserGroupRepository userGroupRepository, GroupMemberRepository groupMemberRepository, AppUserRepository appUserRepository) {
        this.userGroupRepository = userGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    public UserGroup createGroup(String name) {
        UserGroup userGroup = new UserGroup();
        userGroup.setName(name);
        return userGroupRepository.save(userGroup);
    }

    public GroupMember addMember(Long groupId, Long userId) {
        UserGroup userGroup = userGroupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        AppUser appUser = appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupMember groupMember = new GroupMember();
        groupMember.setUserGroup(userGroup);
        groupMember.setUser(appUser);

        return groupMemberRepository.save(groupMember);
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        UserGroup userGroup = userGroupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
        return groupMemberRepository.findByUserGroup(userGroup);
    }

    public List<UserGroup> getAllGroups() {
        return userGroupRepository.findAll();
    }
}
