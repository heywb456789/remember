package com.tomato.naraclub.common.config;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory queryFactory() {
//        return new JPAQueryFactory(entityManager);
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
    }
}

/**
 * <Boolean Builder 동적 사용 >
 * public List<Member> search(String email, MemberStatus status) {
 *     QMember member = QMember.member;
 *     BooleanBuilder builder = new BooleanBuilder();
 *
 *     if (email != null && !email.isBlank()) {
 *         builder.and(member.email.containsIgnoreCase(email));
 *     }
 *
 *     if (status != null) {
 *         builder.and(member.status.eq(status));
 *     }
 *
 *     return queryFactory
 *             .selectFrom(member)
 *             .where(builder)
 *             .fetch();
 * }
 */

/**
 * spring boot 3.x 부터는
 * Spring Boot 3+에서는 QueryDslRepositorySupport 사용 지양됨
 * → 아래처럼 직접 처리하는 방식 추천
 * public Page<Member> searchPaged(String keyword, Pageable pageable) {
 *     QMember member = QMember.member;
 *
 *     List<Member> content = queryFactory
 *             .selectFrom(member)
 *             .where(member.name.contains(keyword))
 *             .offset(pageable.getOffset())
 *             .limit(pageable.getPageSize())
 *             .fetch();
 *
 *     long total = queryFactory
 *             .select(member.count())
 *             .from(member)
 *             .where(member.name.contains(keyword))
 *             .fetchOne();
 *
 *     return new PageImpl<>(content, pageable, total);
 * }
 */