package com.tomato.remember.application.family.repository.custom;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.dto.FamilySearchCondition;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.entity.QFamilyMember;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.entity.QMember;
import com.tomato.remember.application.memorial.entity.QMemorial;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class FamilyMemberRepositoryImpl implements FamilyMemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<FamilyMember> searchFamilyMembers(Member owner, FamilySearchCondition condition, Pageable pageable) {
        QFamilyMember familyMember = QFamilyMember.familyMember;
        QMember member = QMember.member;
        QMemorial memorial = QMemorial.memorial;

        BooleanBuilder builder = new BooleanBuilder();

        // 소유자 조건 (기본)
        builder.and(memorial.owner.eq(owner));

        // 키워드 검색 (멤버 이름)
        if (StringUtils.hasText(condition.getKeyword())) {
            builder.and(member.name.containsIgnoreCase(condition.getKeyword()));
        }

        // 메모리얼 ID 조건
        if (condition.getMemorialId() != null) {
            builder.and(memorial.id.eq(condition.getMemorialId()));
        }

        // 관계 조건
        if (StringUtils.hasText(condition.getRelationship())) {
            try {
                Relationship relationship = Relationship.valueOf(condition.getRelationship());
                builder.and(familyMember.relationship.eq(relationship));
            } catch (IllegalArgumentException e) {
                // 잘못된 관계 값은 무시
            }
        }

        // 초대 상태 조건
        if (StringUtils.hasText(condition.getInviteStatus())) {
            try {
                InviteStatus inviteStatus = InviteStatus.valueOf(condition.getInviteStatus());
                builder.and(familyMember.inviteStatus.eq(inviteStatus));
            } catch (IllegalArgumentException e) {
                // 잘못된 상태 값은 무시
            }
        }

        // 데이터 조회 쿼리
        List<FamilyMember> content = queryFactory
                .selectFrom(familyMember)
                .join(familyMember.member, member).fetchJoin()
                .join(familyMember.memorial, memorial).fetchJoin()
                .where(builder)
                .orderBy(familyMember.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 쿼리
        Long total = queryFactory
                .select(familyMember.count())
                .from(familyMember)
                .join(familyMember.member, member)
                .join(familyMember.memorial, memorial)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}