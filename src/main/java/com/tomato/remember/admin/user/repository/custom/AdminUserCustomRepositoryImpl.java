package com.tomato.remember.admin.user.repository.custom;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.user.dto.AdminUserListRequest;
import com.tomato.remember.admin.user.dto.AdminUserResponse;
import com.tomato.remember.admin.user.entity.QAdmin;
import com.tomato.remember.common.dto.ListDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository.custom
 * @fileName : AdminUserCustomRepositoryImpl
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RequiredArgsConstructor
public class AdminUserCustomRepositoryImpl implements AdminUserCustomRepository {

    private final JPAQueryFactory query;

    @Override
    public ListDTO<AdminUserResponse> getAdminUserList(AdminUserListRequest request,
        AdminUserDetails user, Pageable pageable) {
        QAdmin admin = QAdmin.admin;

        if(request.getDateRange() != null && !request.getDateRange().isBlank()){
            request.parseDateRange();
        }

        Predicate condition = ExpressionUtils.allOf(
                request.getSearchCondition(),
                request.getIsNotDeleted(),
                request.getAdminStatusCondition(admin),
                request.isPeriod(admin.createdAt)
        );

        JPAQuery<Long> countQuery = query
                .select(admin.count())
                .from(admin)
                .where(condition);

        List<AdminUserResponse> adminUserList = query
                .select(getAdminUserFields(admin))
                .from(admin)
                .where(condition)
                .orderBy(request.getSortOrder())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return ListDTO.of(countQuery, adminUserList, pageable);
    }

    public QBean<AdminUserResponse> getAdminUserFields(QAdmin admin){
        return Projections.fields(
                AdminUserResponse.class,
                admin.id.as("adminId"),
                admin.username.as("loginId"),
                admin.name,
                admin.email,
                admin.phoneNumber,
                admin.role,
                admin.status,
                admin.lastAccessAt,
                admin.createdAt,
                admin.updatedAt
        );
    }
}
