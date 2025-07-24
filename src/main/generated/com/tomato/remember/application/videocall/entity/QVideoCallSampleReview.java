package com.tomato.remember.application.videocall.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVideoCallSampleReview is a Querydsl query type for VideoCallSampleReview
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVideoCallSampleReview extends EntityPathBase<VideoCallSampleReview> {

    private static final long serialVersionUID = -58636979L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVideoCallSampleReview videoCallSampleReview = new QVideoCallSampleReview("videoCallSampleReview");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final com.tomato.remember.application.member.entity.QMember caller;

    public final StringPath contactKey = createString("contactKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath reviewMessage = createString("reviewMessage");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public QVideoCallSampleReview(String variable) {
        this(VideoCallSampleReview.class, forVariable(variable), INITS);
    }

    public QVideoCallSampleReview(Path<? extends VideoCallSampleReview> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVideoCallSampleReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVideoCallSampleReview(PathMetadata metadata, PathInits inits) {
        this(VideoCallSampleReview.class, metadata, inits);
    }

    public QVideoCallSampleReview(Class<? extends VideoCallSampleReview> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.caller = inits.isInitialized("caller") ? new com.tomato.remember.application.member.entity.QMember(forProperty("caller"), inits.get("caller")) : null;
    }

}

