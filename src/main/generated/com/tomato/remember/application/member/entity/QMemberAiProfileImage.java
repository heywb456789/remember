package com.tomato.remember.application.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberAiProfileImage is a Querydsl query type for MemberAiProfileImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberAiProfileImage extends EntityPathBase<MemberAiProfileImage> {

    private static final long serialVersionUID = 1750144435L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberAiProfileImage memberAiProfileImage = new QMemberAiProfileImage("memberAiProfileImage");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final BooleanPath aiProcessed = createBoolean("aiProcessed");

    public final StringPath contentType = createString("contentType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath imageUrl = createString("imageUrl");

    public final QMember member;

    public final StringPath originalFilename = createString("originalFilename");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public QMemberAiProfileImage(String variable) {
        this(MemberAiProfileImage.class, forVariable(variable), INITS);
    }

    public QMemberAiProfileImage(Path<? extends MemberAiProfileImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberAiProfileImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberAiProfileImage(PathMetadata metadata, PathInits inits) {
        this(MemberAiProfileImage.class, metadata, inits);
    }

    public QMemberAiProfileImage(Class<? extends MemberAiProfileImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member"), inits.get("member")) : null;
    }

}

