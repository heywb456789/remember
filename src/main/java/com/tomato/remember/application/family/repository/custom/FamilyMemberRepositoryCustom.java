package com.tomato.remember.application.family.repository.custom;

import com.tomato.remember.application.family.dto.FamilySearchCondition;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FamilyMemberRepositoryCustom {
    Page<FamilyMember> searchFamilyMembers(Member owner, FamilySearchCondition condition, Pageable pageable);
}