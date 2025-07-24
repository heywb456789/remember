package com.tomato.remember.application.member.entity;

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

    private static final long serialVersionUID = -1049476135L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMember member = new QMember("member1");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final StringPath email = createString("email");

    public final ListPath<com.tomato.remember.application.family.entity.FamilyMember, com.tomato.remember.application.family.entity.QFamilyMember> familyMemberships = this.<com.tomato.remember.application.family.entity.FamilyMember, com.tomato.remember.application.family.entity.QFamilyMember>createList("familyMemberships", com.tomato.remember.application.family.entity.FamilyMember.class, com.tomato.remember.application.family.entity.QFamilyMember.class, PathInits.DIRECT2);

    public final BooleanPath familyNotification = createBoolean("familyNotification");

    public final DateTimePath<java.time.LocalDateTime> freeTrialEndAt = createDateTime("freeTrialEndAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> freeTrialStartAt = createDateTime("freeTrialStartAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath inviteCode = createString("inviteCode");

    public final QMember inviter;

    public final DateTimePath<java.time.LocalDateTime> lastAccessAt = createDateTime("lastAccessAt", java.time.LocalDateTime.class);

    public final BooleanPath marketingAgreed = createBoolean("marketingAgreed");

    public final ListPath<MemberActivity, QMemberActivity> memberActivities = this.<MemberActivity, QMemberActivity>createList("memberActivities", MemberActivity.class, QMemberActivity.class, PathInits.DIRECT2);

    public final BooleanPath memorialNotification = createBoolean("memorialNotification");

    public final StringPath name = createString("name");

    public final ListPath<com.tomato.remember.application.memorial.entity.Memorial, com.tomato.remember.application.memorial.entity.QMemorial> ownedMemorials = this.<com.tomato.remember.application.memorial.entity.Memorial, com.tomato.remember.application.memorial.entity.QMemorial>createList("ownedMemorials", com.tomato.remember.application.memorial.entity.Memorial.class, com.tomato.remember.application.memorial.entity.QMemorial.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final BooleanPath paymentNotification = createBoolean("paymentNotification");

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath preferredLanguage = createString("preferredLanguage");

    public final ListPath<MemberAiProfileImage, QMemberAiProfileImage> profileImages = this.<MemberAiProfileImage, QMemberAiProfileImage>createList("profileImages", MemberAiProfileImage.class, QMemberAiProfileImage.class, PathInits.DIRECT2);

    public final StringPath profileImg = createString("profileImg");

    public final BooleanPath pushNotification = createBoolean("pushNotification");

    public final EnumPath<com.tomato.remember.common.code.MemberRole> role = createEnum("role", com.tomato.remember.common.code.MemberRole.class);

    public final EnumPath<com.tomato.remember.common.code.MemberStatus> status = createEnum("status", com.tomato.remember.common.code.MemberStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final StringPath userKey = createString("userKey");

    public final ListPath<com.tomato.remember.application.wsvideo.entity.VideoCall, com.tomato.remember.application.wsvideo.entity.QVideoCall> videoCalls = this.<com.tomato.remember.application.wsvideo.entity.VideoCall, com.tomato.remember.application.wsvideo.entity.QVideoCall>createList("videoCalls", com.tomato.remember.application.wsvideo.entity.VideoCall.class, com.tomato.remember.application.wsvideo.entity.QVideoCall.class, PathInits.DIRECT2);

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

