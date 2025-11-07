package com.nppang.backend.repository;

import com.nppang.backend.entity.GroupMember;
import com.nppang.backend.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByUserGroup(UserGroup userGroup);
}
