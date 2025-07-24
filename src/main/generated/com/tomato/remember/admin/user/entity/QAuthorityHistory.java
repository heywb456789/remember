package com.tomato.remember.admin.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAuthorityHistory is a Querydsl query type for AuthorityHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAuthorityHistory extends EntityPathBase<AuthorityHistory> {

    private static final long serialVersionUID = -1780365214L;

    public static final QAuthorityHistory authorityHistory = new QAuthorityHistory("authorityHistory");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    public final EnumPath<com.tomato.remember.admin.user.code.AdminRole> adminRole = createEnum("adminRole", com.tomato.remember.admin.user.code.AdminRole.class);

    public final EnumPath<com.tomato.remember.admin.user.code.AdminStatus> adminStatus = createEnum("adminStatus", com.tomato.remember.admin.user.code.AdminStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<com.tomato.remember.common.code.MemberStatus> memberStatus = createEnum("memberStatus", com.tomato.remember.common.code.MemberStatus.class);

    public final StringPath reason = createString("reason");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAuthorityHistory(String variable) {
        super(AuthorityHistory.class, forVariable(variable));
    }

    public QAuthorityHistory(Path<? extends AuthorityHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAuthorityHistory(PathMetadata metadata) {
        super(AuthorityHistory.class, metadata);
    }

}

