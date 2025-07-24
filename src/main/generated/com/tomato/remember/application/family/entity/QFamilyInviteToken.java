package com.tomato.remember.application.family.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFamilyInviteToken is a Querydsl query type for FamilyInviteToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFamilyInviteToken extends EntityPathBase<FamilyInviteToken> {

    private static final long serialVersionUID = 482065987L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFamilyInviteToken familyInviteToken = new QFamilyInviteToken("familyInviteToken");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final DateTimePath<java.time.LocalDateTime> acceptedAt = createDateTime("acceptedAt", java.time.LocalDateTime.class);

    public final com.tomato.remember.application.member.entity.QMember acceptedMember;

    public final StringPath contact = createString("contact");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath inviteMessage = createString("inviteMessage");

    public final com.tomato.remember.application.member.entity.QMember inviter;

    public final com.tomato.remember.application.memorial.entity.QMemorial memorial;

    public final StringPath method = createString("method");

    public final DateTimePath<java.time.LocalDateTime> rejectedAt = createDateTime("rejectedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.tomato.remember.application.member.code.Relationship> relationship = createEnum("relationship", com.tomato.remember.application.member.code.Relationship.class);

    public final EnumPath<com.tomato.remember.application.family.code.InviteStatus> status = createEnum("status", com.tomato.remember.application.family.code.InviteStatus.class);

    public final StringPath token = createString("token");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final DateTimePath<java.time.LocalDateTime> usedAt = createDateTime("usedAt", java.time.LocalDateTime.class);

    public QFamilyInviteToken(String variable) {
        this(FamilyInviteToken.class, forVariable(variable), INITS);
    }

    public QFamilyInviteToken(Path<? extends FamilyInviteToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFamilyInviteToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFamilyInviteToken(PathMetadata metadata, PathInits inits) {
        this(FamilyInviteToken.class, metadata, inits);
    }

    public QFamilyInviteToken(Class<? extends FamilyInviteToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.acceptedMember = inits.isInitialized("acceptedMember") ? new com.tomato.remember.application.member.entity.QMember(forProperty("acceptedMember"), inits.get("acceptedMember")) : null;
        this.inviter = inits.isInitialized("inviter") ? new com.tomato.remember.application.member.entity.QMember(forProperty("inviter"), inits.get("inviter")) : null;
        this.memorial = inits.isInitialized("memorial") ? new com.tomato.remember.application.memorial.entity.QMemorial(forProperty("memorial"), inits.get("memorial")) : null;
    }

}

