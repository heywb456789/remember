package com.tomato.remember.application.family.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFamilyMember is a Querydsl query type for FamilyMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFamilyMember extends EntityPathBase<FamilyMember> {

    private static final long serialVersionUID = -1020801817L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFamilyMember familyMember = new QFamilyMember("familyMember");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final DateTimePath<java.time.LocalDateTime> acceptedAt = createDateTime("acceptedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.tomato.remember.application.member.entity.QMember invitedBy;

    public final StringPath inviteMessage = createString("inviteMessage");

    public final EnumPath<com.tomato.remember.application.family.code.InviteStatus> inviteStatus = createEnum("inviteStatus", com.tomato.remember.application.family.code.InviteStatus.class);

    public final DateTimePath<java.time.LocalDateTime> lastAccessAt = createDateTime("lastAccessAt", java.time.LocalDateTime.class);

    public final com.tomato.remember.application.member.entity.QMember member;

    public final StringPath memberFavoriteFood = createString("memberFavoriteFood");

    public final StringPath memberHobbies = createString("memberHobbies");

    public final StringPath memberPersonality = createString("memberPersonality");

    public final StringPath memberSpecialMemories = createString("memberSpecialMemories");

    public final StringPath memberSpeechHabits = createString("memberSpeechHabits");

    public final com.tomato.remember.application.memorial.entity.QMemorial memorial;

    public final BooleanPath memorialAccess = createBoolean("memorialAccess");

    public final DateTimePath<java.time.LocalDateTime> rejectedAt = createDateTime("rejectedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.tomato.remember.application.member.code.Relationship> relationship = createEnum("relationship", com.tomato.remember.application.member.code.Relationship.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final BooleanPath videoCallAccess = createBoolean("videoCallAccess");

    public QFamilyMember(String variable) {
        this(FamilyMember.class, forVariable(variable), INITS);
    }

    public QFamilyMember(Path<? extends FamilyMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFamilyMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFamilyMember(PathMetadata metadata, PathInits inits) {
        this(FamilyMember.class, metadata, inits);
    }

    public QFamilyMember(Class<? extends FamilyMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.invitedBy = inits.isInitialized("invitedBy") ? new com.tomato.remember.application.member.entity.QMember(forProperty("invitedBy"), inits.get("invitedBy")) : null;
        this.member = inits.isInitialized("member") ? new com.tomato.remember.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.memorial = inits.isInitialized("memorial") ? new com.tomato.remember.application.memorial.entity.QMemorial(forProperty("memorial"), inits.get("memorial")) : null;
    }

}

