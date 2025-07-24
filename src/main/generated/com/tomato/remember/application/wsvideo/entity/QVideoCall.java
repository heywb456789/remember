package com.tomato.remember.application.wsvideo.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVideoCall is a Querydsl query type for VideoCall
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVideoCall extends EntityPathBase<VideoCall> {

    private static final long serialVersionUID = 318064901L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVideoCall videoCall = new QVideoCall("videoCall");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final StringPath aiAudioFileUrl = createString("aiAudioFileUrl");

    public final NumberPath<Long> aiProcessingTimeMs = createNumber("aiProcessingTimeMs", Long.class);

    public final StringPath aiResponseText = createString("aiResponseText");

    public final DateTimePath<java.time.LocalDateTime> callEndedAt = createDateTime("callEndedAt", java.time.LocalDateTime.class);

    public final com.tomato.remember.application.member.entity.QMember caller;

    public final DateTimePath<java.time.LocalDateTime> callStartedAt = createDateTime("callStartedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.tomato.remember.application.videocall.code.CallType> callType = createEnum("callType", com.tomato.remember.application.videocall.code.CallType.class);

    public final NumberPath<Integer> connectionAttempts = createNumber("connectionAttempts", Integer.class);

    public final EnumPath<com.tomato.remember.application.videocall.code.ConversationTopic> conversationTopic = createEnum("conversationTopic", com.tomato.remember.application.videocall.code.ConversationTopic.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final EnumPath<com.tomato.remember.application.wsvideo.code.DeviceType> deviceType = createEnum("deviceType", com.tomato.remember.application.wsvideo.code.DeviceType.class);

    public final NumberPath<Integer> durationSeconds = createNumber("durationSeconds", Integer.class);

    public final EnumPath<com.tomato.remember.application.videocall.code.EmotionType> emotionAnalysis = createEnum("emotionAnalysis", com.tomato.remember.application.videocall.code.EmotionType.class);

    public final StringPath errorMessage = createString("errorMessage");

    public final StringPath feedback = createString("feedback");

    public final EnumPath<com.tomato.remember.application.family.code.FeedbackCategory> feedbackCategory = createEnum("feedbackCategory", com.tomato.remember.application.family.code.FeedbackCategory.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.tomato.remember.application.memorial.entity.QMemorial memorial;

    public final StringPath metadata = createString("metadata");

    public final NumberPath<Integer> networkQualityScore = createNumber("networkQualityScore", Integer.class);

    public final StringPath responseVideoUrl = createString("responseVideoUrl");

    public final NumberPath<Integer> satisfactionRating = createNumber("satisfactionRating", Integer.class);

    public final EnumPath<com.tomato.remember.application.videocall.code.CallStatus> status = createEnum("status", com.tomato.remember.application.videocall.code.CallStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final StringPath userAudioFileUrl = createString("userAudioFileUrl");

    public final StringPath userSpeechText = createString("userSpeechText");

    public final NumberPath<Integer> voiceQualityScore = createNumber("voiceQualityScore", Integer.class);

    public QVideoCall(String variable) {
        this(VideoCall.class, forVariable(variable), INITS);
    }

    public QVideoCall(Path<? extends VideoCall> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVideoCall(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVideoCall(PathMetadata metadata, PathInits inits) {
        this(VideoCall.class, metadata, inits);
    }

    public QVideoCall(Class<? extends VideoCall> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.caller = inits.isInitialized("caller") ? new com.tomato.remember.application.member.entity.QMember(forProperty("caller"), inits.get("caller")) : null;
        this.memorial = inits.isInitialized("memorial") ? new com.tomato.remember.application.memorial.entity.QMemorial(forProperty("memorial"), inits.get("memorial")) : null;
    }

}

