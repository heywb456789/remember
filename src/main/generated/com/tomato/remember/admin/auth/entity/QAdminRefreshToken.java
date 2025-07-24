package com.tomato.remember.admin.auth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminRefreshToken is a Querydsl query type for AdminRefreshToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminRefreshToken extends EntityPathBase<AdminRefreshToken> {

    private static final long serialVersionUID = -1477005537L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdminRefreshToken adminRefreshToken = new QAdminRefreshToken("adminRefreshToken");

    public final com.tomato.remember.common.audit.QCreatedAndModifiedAudit _super = new com.tomato.remember.common.audit.QCreatedAndModifiedAudit(this);

    public final com.tomato.remember.admin.user.entity.QAdmin admin;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final StringPath deviceType = createString("deviceType");

    public final DateTimePath<java.time.LocalDateTime> expiryDate = createDateTime("expiryDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final DateTimePath<java.time.LocalDateTime> lastUsedAt = createDateTime("lastUsedAt", java.time.LocalDateTime.class);

    public final StringPath refreshToken = createString("refreshToken");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final StringPath userAgent = createString("userAgent");

    public QAdminRefreshToken(String variable) {
        this(AdminRefreshToken.class, forVariable(variable), INITS);
    }

    public QAdminRefreshToken(Path<? extends AdminRefreshToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdminRefreshToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdminRefreshToken(PathMetadata metadata, PathInits inits) {
        this(AdminRefreshToken.class, metadata, inits);
    }

    public QAdminRefreshToken(Class<? extends AdminRefreshToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.admin = inits.isInitialized("admin") ? new com.tomato.remember.admin.user.entity.QAdmin(forProperty("admin")) : null;
    }

}

