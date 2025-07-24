package com.tomato.remember.application.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMemberLoginHistory is a Querydsl query type for MemberLoginHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberLoginHistory extends EntityPathBase<MemberLoginHistory> {

    private static final long serialVersionUID = -514666766L;

    public static final QMemberLoginHistory memberLoginHistory = new QMemberLoginHistory("memberLoginHistory");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final StringPath deviceType = createString("deviceType");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath ipAddress = createString("ipAddress");

    public final NumberPath<Long> memberId = createNumber("memberId", Long.class);

    public final EnumPath<com.tomato.remember.application.auth.code.LoginType> type = createEnum("type", com.tomato.remember.application.auth.code.LoginType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final StringPath userAgent = createString("userAgent");

    public QMemberLoginHistory(String variable) {
        super(MemberLoginHistory.class, forVariable(variable));
    }

    public QMemberLoginHistory(Path<? extends MemberLoginHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMemberLoginHistory(PathMetadata metadata) {
        super(MemberLoginHistory.class, metadata);
    }

}

