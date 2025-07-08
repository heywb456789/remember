package com.tomato.naraclub.application.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -72490770L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMember member = new QMember("member1");

    public final com.tomato.naraclub.common.audit.QAudit _super = new com.tomato.naraclub.common.audit.QAudit(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final StringPath email = createString("email");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath inviteCode = createString("inviteCode");

    public final QMember inviter;

    public final DateTimePath<java.time.LocalDateTime> lastAccessAt = createDateTime("lastAccessAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final StringPath password = createString("password");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath profileImg = createString("profileImg");

    public final EnumPath<com.tomato.naraclub.common.code.MemberRole> role = createEnum("role", com.tomato.naraclub.common.code.MemberRole.class);

    public final EnumPath<com.tomato.naraclub.common.code.MemberStatus> status = createEnum("status", com.tomato.naraclub.common.code.MemberStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final StringPath userKey = createString("userKey");

    public final BooleanPath verified = createBoolean("verified");

    public QMember(String variable) {
        this(Member.class, forVariable(variable), INITS);
    }

    public QMember(Path<? extends Member> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMember(PathMetadata metadata, PathInits inits) {
        this(Member.class, metadata, inits);
    }

    public QMember(Class<? extends Member> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.inviter = inits.isInitialized("inviter") ? new QMember(forProperty("inviter"), inits.get("inviter")) : null;
    }

}

