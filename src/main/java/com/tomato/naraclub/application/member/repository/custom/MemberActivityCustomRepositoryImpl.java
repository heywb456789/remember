package com.tomato.naraclub.application.member.repository.custom;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tomato.naraclub.application.member.dto.MemberActivityResponse;
import com.tomato.naraclub.application.member.entity.QMemberActivity;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.util.QueryUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.repository.custom
 * @fileName : MemberActivityCustomRepositoryImpl
 * @date : 2025-05-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RequiredArgsConstructor
public class MemberActivityCustomRepositoryImpl implements MemberActivityCustomRepository {

    private final JPAQueryFactory query;

    @Override
    public ListDTO<MemberActivityResponse> getMemberActivities(Long id, Pageable pageable) {
        QMemberActivity activity = QMemberActivity.memberActivity;

        JPAQuery<Long> countQuery = query
            .select(activity.count())
            .from(activity)
            .where(
                activity.author.id.eq(id)
                    .and(activity.isDeleted.eq(false))
            );

        JPAQuery<MemberActivityResponse> activityQuery = this.query
            .select(getActivityFields(activity))
            .from(activity)
            .where(
                activity.author.id.eq(id)
                    .and(activity.isDeleted.eq(false))
            );

        activityQuery = QueryUtil.paging(activityQuery, pageable)
            .orderBy(QueryUtil.convertOrders(pageable.getSort(), MemberActivityResponse.class));

        List<MemberActivityResponse> result = activityQuery.fetch();

        return ListDTO.of(countQuery, result, pageable);
    }

    private QBean<MemberActivityResponse> getActivityFields(QMemberActivity activity) {
        return Projections.fields(
            MemberActivityResponse.class,
            activity.id.as("activityId"),
            activity.author.id.as("memberId"),
            activity.author.name.as("memberName"),
            activity.title,
            activity.shareLink,
            activity.stage,
            activity.isDeleted,
            activity.createdAt
        );
    }
}
