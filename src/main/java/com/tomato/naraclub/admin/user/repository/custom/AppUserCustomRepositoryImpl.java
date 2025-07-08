package com.tomato.naraclub.admin.user.repository.custom;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AppUserListRequest;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.application.member.entity.QMember;
import com.tomato.naraclub.common.dto.ListDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository.custom
 * @fileName : AppUserCustomRepositoryImpl
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RequiredArgsConstructor
public class AppUserCustomRepositoryImpl implements AppUserCustomRepository {

    private final JPAQueryFactory query;


    @Override
    public ListDTO<AppUserResponse> getAppUserList(AdminUserDetails user,
        AppUserListRequest request, Pageable pageable) {

        QMember member = QMember.member;
        QMember inviter = new QMember("inviter");

        if (request.getDateRange() != null && !request.getDateRange().isBlank()) {
            request.parseDateRange();
        }

        Predicate condition = ExpressionUtils.allOf(
            request.getSearchCondition(),
            request.getIsNotDeleted(),
            request.getMemberStatusCondition(member),
            request.isPeriod(member.createdAt)
        );

        JPAQuery<Long> countQuery = query
                .select(member.count())
                .from(member)
                .where(condition);

        List<AppUserResponse> appUserList = query
            .select(getAppUserFields(member, inviter))
            .from(member)
            .leftJoin(member.inviter, inviter)
            .where(condition)
            .orderBy(request.getSortOrder())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        return ListDTO.of(countQuery, appUserList, pageable);
    }

    private QBean<AppUserResponse> getAppUserFields(QMember member, QMember inviter) {
        return Projections.fields(
            AppUserResponse.class,
            member.id.as("userId"),
            member.name.as("userName"),
            member.userKey,
            member.phoneNumber,
            member.profileImg.as("profileImageUrl"),
            member.inviteCode,
            member.status,
            member.role,
            member.email,
            member.lastAccessAt,
            member.verified,
            inviter.id.as("inviterId"),            // 수정된 부분
            inviter.name.as("inviterName"),
            member.createdAt,
            member.updatedAt
        );
    }
}
