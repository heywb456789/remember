package com.tomato.naraclub.application.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRefreshToken is a Querydsl query type for RefreshToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefreshToken extends EntityPathBase<RefreshToken> {

    private static final long serialVersionUID = 770753152L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRefreshToken refreshToken1 = new QRefreshToken("refreshToken1");

    public final com.tomato.naraclub.common.audit.QCreatedAndModifiedAudit _super = new com.tomato.naraclub.common.audit.QCreatedAndModifiedAudit(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final StringPath deviceType = createString("deviceType");

    public final DateTimePath<java.time.LocalDateTime> expiryDate = createDateTime("expiryDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final DateTimePath<java.time.LocalDateTime> lastUsedAt = createDateTime("lastUsedAt", java.time.LocalDateTime.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    public final StringPath refreshToken = createString("refreshToken");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final StringPath userAgent = createString("userAgent");

    public QRefreshToken(String variable) {
        this(RefreshToken.class, forVariable(variable), INITS);
    }

    public QRefreshToken(Path<? extends RefreshToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRefreshToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRefreshToken(PathMetadata metadata, PathInits inits) {
        this(RefreshToken.class, metadata, inits);
    }

    public QRefreshToken(Class<? extends RefreshToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
    }

}

