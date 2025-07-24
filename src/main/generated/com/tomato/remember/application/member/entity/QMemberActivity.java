package com.tomato.remember.application.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberActivity is a Querydsl query type for MemberActivity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberActivity extends EntityPathBase<MemberActivity> {

    private static final long serialVersionUID = 1739043336L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberActivity memberActivity = new QMemberActivity("memberActivity");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final EnumPath<com.tomato.remember.application.member.code.ActivityCategory> activityCategory = createEnum("activityCategory", com.tomato.remember.application.member.code.ActivityCategory.class);

    public final EnumPath<com.tomato.remember.application.member.code.ActivityType> activityType = createEnum("activityType", com.tomato.remember.application.member.code.ActivityType.class);

    public final StringPath additionalData = createString("additionalData");

    public final QMember author;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final EnumPath<com.tomato.remember.application.wsvideo.code.DeviceType> deviceType = createEnum("deviceType", com.tomato.remember.application.wsvideo.code.DeviceType.class);

    public final NumberPath<Long> familyMemberId = createNumber("familyMemberId", Long.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<com.tomato.remember.application.member.code.ImportanceLevel> importanceLevel = createEnum("importanceLevel", com.tomato.remember.application.member.code.ImportanceLevel.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final BooleanPath isPublic = createBoolean("isPublic");

    public final StringPath locationInfo = createString("locationInfo");

    public final NumberPath<Long> memorialId = createNumber("memorialId", Long.class);

    public final EnumPath<com.tomato.remember.application.member.code.ActivityStatus> status = createEnum("status", com.tomato.remember.application.member.code.ActivityStatus.class);

    public final StringPath tags = createString("tags");

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final StringPath userAgent = createString("userAgent");

    public final NumberPath<Long> videoCallId = createNumber("videoCallId", Long.class);

    public QMemberActivity(String variable) {
        this(MemberActivity.class, forVariable(variable), INITS);
    }

    public QMemberActivity(Path<? extends MemberActivity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberActivity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberActivity(PathMetadata metadata, PathInits inits) {
        this(MemberActivity.class, metadata, inits);
    }

    public QMemberActivity(Class<? extends MemberActivity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new QMember(forProperty("author"), inits.get("author")) : null;
    }

}

